package cinemasystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import cinemasystem.util.WindowUtils;

public class CinemaSystem extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/cinemasystem/view/Login.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root);

        stage.setTitle("Cinema Booking System");
        stage.setScene(scene);

        // Allow the user to resize the window
        stage.setResizable(true);

        // Minimum size of the application
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Initial size
        stage.setWidth(1100);
        stage.setHeight(650);

        WindowUtils.setIcon(stage);
        
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}