package net.markwalder.picturetriage;

import javafx.application.Application;
import javafx.stage.Stage;

public class PictureTriageApplication extends Application {
    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        AppCoordinator coordinator = new AppCoordinator(primaryStage, getApplicationStylesheet());
        coordinator.begin();
    }

    /**
     * Get the URL of the application stylesheet.
     */
    private String getApplicationStylesheet() {
        return this.getClass().getResource("application.css").toExternalForm();
    }
}
