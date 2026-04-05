package net.markwalder.picturetriage;

import java.awt.Taskbar;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class PictureTriageApplication extends Application {
    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Set the macOS Dock icon (and other OS taskbar icons) programmatically.
        // This overrides the generic Java/Gradle icon when running via `./gradlew run`.
        setTaskbarIcon();

        // Load and set application icons in multiple resolutions
        primaryStage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/icon_16.png")),
                new Image(getClass().getResourceAsStream("/icon_32.png")),
                new Image(getClass().getResourceAsStream("/icon_64.png")),
                new Image(getClass().getResourceAsStream("/icon_128.png")),
                new Image(getClass().getResourceAsStream("/icon_256.png")),
                new Image(getClass().getResourceAsStream("/icon_512.png"))
        );

        AppCoordinator coordinator = new AppCoordinator(primaryStage, getApplicationStylesheet());
        coordinator.begin();
    }

    /**
     * Set the OS taskbar/Dock icon using the AWT Taskbar API.
     * This is needed on macOS when the app is launched via Gradle, where the
     * Dock would otherwise show the generic Java launcher icon.
     */
    private void setTaskbarIcon() {
        if (!Taskbar.isTaskbarSupported()) {
            return;
        }
        Taskbar taskbar = Taskbar.getTaskbar();
        if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            return;
        }
        try (InputStream is = getClass().getResourceAsStream("/icon_512.png")) {
            if (is != null) {
                taskbar.setIconImage(ImageIO.read(is));
            }
        } catch (IOException e) {
            // Non-critical: ignore if the icon cannot be loaded
        }
    }

    /**
     * Get the URL of the application stylesheet.
     */
    private String getApplicationStylesheet() {
        return this.getClass().getResource("application.css").toExternalForm();
    }
}
