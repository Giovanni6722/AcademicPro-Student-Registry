package viewmodel;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.MyLogger;
import service.UserSession;

import java.util.prefs.Preferences;

public class LoginController {

    @FXML private GridPane      rootpane;
    @FXML private TextField     usernameTextField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @FXML
    public void initialize() {
        rootpane.setBackground(new Background(createBgImage(
                "https://edencoding.com/wp-content/uploads/2021/03/layer_06_1920x1080.png"),
                null, null, null, null, null));
        rootpane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.seconds(1.5), rootpane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // Pre-fill username from last signup (UX convenience)
        String lastUser = Preferences.userRoot().get("SIGNUP_USERNAME", "");
        if (!lastUser.isEmpty() && usernameTextField != null) {
            usernameTextField.setText(lastUser);
        }
    }

    private static BackgroundImage createBgImage(String url) {
        return new BackgroundImage(
                new Image(url),
                BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT,
                new BackgroundPosition(Side.LEFT, 0, true, Side.BOTTOM, 0, true),
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO,
                        true, true, false, true));
    }

    @FXML
    public void login(ActionEvent actionEvent) {
        if (usernameTextField == null || passwordField == null) {
            navigateToMain(actionEvent);
            return;
        }

        String username = usernameTextField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        Preferences prefs    = Preferences.userRoot();
        String savedUser     = prefs.get("SIGNUP_USERNAME", "");
        String savedPass     = prefs.get("SIGNUP_PASSWORD", "");

        if (savedUser.isEmpty()) {
            showError("No account found. Please sign up first.");
            return;
        }

        if (!username.equals(savedUser) || !password.equals(savedPass)) {
            showError("Invalid username or password. Please try again.");
            passwordField.clear();
            return;
        }

        UserSession session = UserSession.getInstance(username, password, "ADMIN");
        MyLogger.makeLog("User logged in: " + session);
        navigateToMain(actionEvent);
    }

    private void navigateToMain(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/db_interface_gui.fxml"));
            Scene scene = new Scene(root, 950, 600);
            scene.getStylesheets().add(
                    getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load the application. Check console.");
        }
    }

    @FXML
    public void signUp(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/signUp.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(
                    getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String message) {
        if (errorLabel == null) return;
        errorLabel.setText(message);
        errorLabel.setOpacity(1.0);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), errorLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.seconds(4));
        ft.play();
    }
}