package viewmodel;

import dao.DbConnectivityClass;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainApplication extends Application {

    private static DbConnectivityClass cnUtil;
    private Stage primaryStage;

    public static void main(String[] args) {
        cnUtil = new DbConnectivityClass();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setResizable(false);
        try {
            Image icon = new Image(
                    getClass().getResourceAsStream("/images/DollarClouddatabase.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception ignored) {}
        primaryStage.setTitle("AcademicPro Student Registry");
        showSplash();
    }

    private void showSplash() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/splashscreen.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(
                    getClass().getResource("/css/lightTheme.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
            transitionToLogin();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void transitionToLogin() {
        try {
            Parent loginRoot = FXMLLoader.load(
                    getClass().getResource("/view/login.fxml").toURI().toURL());
            Scene current = primaryStage.getScene();
            FadeTransition fade = new FadeTransition(Duration.seconds(2), current.getRoot());
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(e -> {
                Scene loginScene = new Scene(loginRoot, 900, 600);
                loginScene.getStylesheets().add(
                        getClass().getResource("/css/lightTheme.css").toExternalForm());
                primaryStage.setScene(loginScene);
                primaryStage.show();
            });
            fade.play();
        } catch (Exception e) { e.printStackTrace(); }
    }
}