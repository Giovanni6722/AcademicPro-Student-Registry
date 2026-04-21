package viewmodel;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.MyLogger;
import service.UserSession;

import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class SignUpController {

    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         statusLabel;
    @FXML private Button        createAccountBtn;

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z'\\-]{1,24}$");
    private static final Pattern USER_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._\\-]{3,30}$");
    private static final Pattern PASS_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@#$!%*?&]{8,64}$");

    @FXML
    public void initialize() {
        firstNameField.textProperty().addListener((o, v, n)       -> validateAll());
        lastNameField.textProperty().addListener((o, v, n)        -> validateAll());
        usernameField.textProperty().addListener((o, v, n)        -> validateAll());
        passwordField.textProperty().addListener((o, v, n)        -> validateAll());
        confirmPasswordField.textProperty().addListener((o, v, n) -> validateAll());
        if (createAccountBtn != null) createAccountBtn.setDisable(true);
    }

    private void validateAll() {
        boolean firstOk  = NAME_PATTERN.matcher(firstNameField.getText()).matches();
        boolean lastOk   = NAME_PATTERN.matcher(lastNameField.getText()).matches();
        boolean userOk   = USER_PATTERN.matcher(usernameField.getText()).matches();
        boolean passOk   = PASS_PATTERN.matcher(passwordField.getText()).matches();
        boolean matchOk  = passwordField.getText().equals(confirmPasswordField.getText())
                && !confirmPasswordField.getText().isEmpty();

        applyStyle(firstNameField,       firstOk);
        applyStyle(lastNameField,        lastOk);
        applyStyle(usernameField,        userOk);
        applyStyle(passwordField,        passOk);
        applyStyle(confirmPasswordField, matchOk);

        if (createAccountBtn != null) {
            createAccountBtn.setDisable(!(firstOk && lastOk && userOk && passOk && matchOk));
        }
    }

    private void applyStyle(TextField f, boolean valid) {
        if (f.getText().isEmpty()) { f.setStyle(""); return; }
        f.setStyle(valid
                ? "-fx-border-color: #4CAF50; -fx-border-width: 0 0 2 0;"
                : "-fx-border-color: #F44336; -fx-border-width: 0 0 2 0;");
    }

    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showStatus("Passwords do not match.", true);
            return;
        }

        String username  = usernameField.getText().trim();
        String password  = passwordField.getText();

        Preferences prefs = Preferences.userRoot();
        prefs.put("SIGNUP_USERNAME",  username);
        prefs.put("SIGNUP_PASSWORD",  password);
        prefs.put("SIGNUP_FIRSTNAME", firstNameField.getText().trim());
        prefs.put("SIGNUP_LASTNAME",  lastNameField.getText().trim());

        UserSession session = UserSession.getInstance(username, password, "ADMIN");
        MyLogger.makeLog("New account created: " + session);

        showStatus("Account created! Redirecting to login…", false);

        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> navigateTo("/view/login.fxml", actionEvent));
        pause.play();
    }

    @FXML
    public void goBack(ActionEvent actionEvent) {
        navigateTo("/view/login.fxml", actionEvent);
    }

    private void navigateTo(String fxmlPath, ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(
                    getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showStatus(String message, boolean isError) {
        if (statusLabel == null) return;
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #F44336; -fx-font-weight: bold;"
                : "-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        statusLabel.setOpacity(1.0);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), statusLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.seconds(4));
        ft.play();
    }
}
