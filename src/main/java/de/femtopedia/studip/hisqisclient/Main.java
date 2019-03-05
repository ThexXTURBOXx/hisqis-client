package de.femtopedia.studip.hisqisclient;

import de.femtopedia.studip.hisqis.HisqisAPI;
import de.femtopedia.studip.hisqis.parsed.Account;
import de.femtopedia.studip.hisqis.parsed.Category;
import de.femtopedia.studip.hisqis.parsed.CourseOfStudy;
import de.femtopedia.studip.hisqis.parsed.Mark;
import de.femtopedia.studip.hisqis.parsed.Student;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Pair;

public class Main extends Application implements Initializable {

	private static Main instance;

	private Stage stage;
	@FXML
	private ListView subjectList;
	@FXML
	private Label current;
	private String username;
	private String password;
	private Student student;
	private List<Object> objects;
	private Scene scene;

	public static Main getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		instance = this;
		this.stage = stage;
		Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));

		scene = new Scene(root);
		scene.getStylesheets().add("/styles/Styles.css");

		stage.setTitle("HISQIS Client 1.0");
		stage.setScene(scene);

		stage.setOnCloseRequest((t) -> {
			Platform.exit();
			System.exit(0);
		});

		stage.setScene(scene);
		stage.show();

		stage.show();
	}

	public Stage getStage() {
		return stage;
	}

	public void alert(Exception e) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Exception Dialog");
		alert.setHeaderText("An error occurred!");
		alert.setContentText("See Details for a stacktrace");

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String exceptionText = sw.toString();

		Label label = new Label("The exception stacktrace was:");

		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);

		alert.getDialogPane().setExpandableContent(expContent);

		alert.showAndWait();
		System.exit(1);
	}

	public void loginDialog() {
		Dialog<Pair<String, String>> dialog = new Dialog<>();
		dialog.setTitle("Login");
		dialog.setHeaderText("Please enter your Credentials!");

		ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		TextField usernameField = new TextField();
		usernameField.setPromptText("Username");
		PasswordField passwordField = new PasswordField();
		passwordField.setPromptText("Password");

		grid.add(new Label("Username:"), 0, 0);
		grid.add(usernameField, 1, 0);
		grid.add(new Label("Password:"), 0, 1);
		grid.add(passwordField, 1, 1);

		Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
		loginButton.setDisable(true);

		usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
			loginButton.setDisable(newValue.trim().isEmpty());
		});

		dialog.getDialogPane().setContent(grid);

		Platform.runLater(usernameField::requestFocus);

		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == loginButtonType) {
				return new Pair<>(usernameField.getText(), passwordField.getText());
			}
			return null;
		});

		Optional<Pair<String, String>> result = dialog.showAndWait();

		result.ifPresent(usernamePassword -> {
			username = usernamePassword.getKey();
			password = usernamePassword.getValue();
		});
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		try {
			current.setWrapText(true);
			HisqisAPI api = new HisqisAPI();
			loginDialog();
			api.authenticate(username, password);
			student = api.getStudent();
			objects = new ArrayList<>();
			for (CourseOfStudy c : student.getCourses()) {
				objects.add(c);
				subjectList.getItems().add(c.getGraduationName());
				for (Category ca : c.getCategories()) {
					objects.add(ca);
					subjectList.getItems().add("\t" + ca.getSubject());
					for (Account a : ca.getAccounts()) {
						objects.add(a);
						subjectList.getItems().add("\t\t" + a.getSubject());
						for (Mark m : a.getMarks()) {
							objects.add(m);
							subjectList.getItems().add("\t\t\t" + m.getSubject());
						}
					}
				}
			}
			api.shutdown();
			subjectList.setOnMouseClicked((event) -> {
				int index = subjectList.getSelectionModel().getSelectedIndex();
				Object clicked = objects.get(index);
				if (clicked instanceof CourseOfStudy) {
					CourseOfStudy course = (CourseOfStudy) clicked;
					AtomicInteger ects = new AtomicInteger(0);
					course.getCategories().forEach((cat) -> ects.addAndGet(cat.getEcts()));
					current.setText(
							"Graduation Name: " + course.getGraduationName() + "\n\n" +
									"Average Grade: " + course.getAverageGrade() + "\n\n" +
									"ECTS: " + ects.intValue());
				} else if (clicked instanceof Category) {
					Category category = (Category) clicked;
					current.setText(
							"Number: " + category.getNumber() + "\n\n" +
									"Subject: " + category.getSubject() + "\n\n" +
									"Average Grade: " + category.getGrade() + "\n\n" +
									"ECTS: " + category.getEcts());
				} else if (clicked instanceof Account) {
					Account account = (Account) clicked;
					current.setText(
							"Number: " + account.getNumber() + "\n\n" +
									"Subject: " + account.getSubject() + "\n\n" +
									"Semester: " + account.getSemester() + "\n\n" +
									"State: " + account.getState() + "\n\n" +
									"ECTS: " + account.getEcts());
				} else if (clicked instanceof Mark) {
					Mark mark = (Mark) clicked;
					current.setText(
							"Number: " + mark.getNumber() + "\n\n" +
									"Subject: " + mark.getSubject() + "\n\n" +
									"Semester: " + mark.getSemester() + "\n\n" +
									"Grade: " + mark.getGrade() + "\n\n" +
									"State: " + mark.getState() + "\n\n" +
									"ECTS: " + mark.getEcts() + "\n\n" +
									"Note: " + mark.getNote() + "\n\n" +
									"Attempt: " + mark.getAttempt() + "\n\n" +
									"Date: " +
									DateFormat.getDateInstance().format(mark.getDate()));
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			alert(e);
		}
	}

}
