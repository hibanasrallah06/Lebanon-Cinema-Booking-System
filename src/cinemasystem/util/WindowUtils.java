package cinemasystem.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;

public class WindowUtils {

    public static void setIcon(Stage stage) {

        Image icon = new Image(
            WindowUtils.class.getResourceAsStream(
                "/cinemasystem/images/icon.png"
            )
        );

        stage.getIcons().add(icon);
    }
}