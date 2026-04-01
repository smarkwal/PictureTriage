package net.markwalder.picturetriage;

import javafx.application.Application;
import javafx.stage.Stage;

public class PictureTriageApplication extends Application {
    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        AppCoordinator coordinator = new AppCoordinator(primaryStage);
        coordinator.begin();
    }
}
