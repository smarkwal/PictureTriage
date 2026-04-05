package net.markwalder.picturetriage;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.markwalder.picturetriage.controller.FolderSelectionController;
import net.markwalder.picturetriage.controller.Phase1Controller;
import net.markwalder.picturetriage.controller.Phase2Controller;
import net.markwalder.picturetriage.controller.Phase3Controller;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.ResultBundle;
import net.markwalder.picturetriage.service.ImageCache;
import net.markwalder.picturetriage.service.ImageDeleteService;

import net.markwalder.picturetriage.ui.DeleteConfirmationDialog;
import java.nio.file.Path;
import java.util.List;

public class AppCoordinator {
    private final double WINDOW_WIDTH;
    private final double WINDOW_HEIGHT;

    private final Stage stage;
    private final String styleSheet;
    private final ImageCache imageCache = new ImageCache();
    private final FolderSelectionController folderSelectionController;
    private final Phase1Controller phase1Controller;
    private final Phase2Controller phase2Controller;
    private final Phase3Controller phase3Controller;

    private Path selectedRootFolder;
    private List<ImageItem> scannedImages;

    public AppCoordinator(Stage stage, String styleSheet) {
        // Compute window size as 80% of the primary screen's usable area
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        WINDOW_WIDTH = screenBounds.getWidth() * 0.8;
        WINDOW_HEIGHT = screenBounds.getHeight() * 0.8;

        this.stage = stage;
        this.styleSheet = styleSheet;
        this.folderSelectionController = new FolderSelectionController(stage, styleSheet);
        this.phase1Controller = new Phase1Controller(stage, styleSheet, imageCache);
        this.phase2Controller = new Phase2Controller(stage, styleSheet, imageCache);
        this.phase3Controller = new Phase3Controller(stage, styleSheet, imageCache);
    }

    public void begin() {
        stage.setTitle("Picture Triage");
        // Handle cleanup when application closes
        stage.setOnCloseRequest(event -> {
            imageCache.clear();
        });
        // Set initial window size to a fixed compact size for folder selection
        stage.setWidth(800);
        stage.setHeight(600);
        showFolderSelection();
        // Center the window on the primary screen before displaying
        stage.centerOnScreen();
        stage.show();
    }

    private void showFolderSelection() {
        folderSelectionController.showFolderSelection(this::onFolderScanCompleted);
    }

    private void onFolderScanCompleted(FolderSelectionController.FolderScanResult result) {
        selectedRootFolder = result.selectedFolder();
        scannedImages = result.images();
        // Expand window to 80% of the primary screen's usable area before entering Phase 1
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT);
        stage.centerOnScreen();
        phase1Controller.start(result.images(), selectedRootFolder, this::onPhase1Completed, this::backToPhase1Start);
    }

    private void onPhase1Completed(ResultBundle phase1Result) {
        if (phase1Result.rankedTriageImages().isEmpty()) {
            startPhase3(phase1Result);
        } else {
            startPhase2(phase1Result);
        }
    }

    private void startPhase2(ResultBundle phase1Result) {
        phase2Controller.start(phase1Result, selectedRootFolder, this::startPhase3, this::backToPhase1Start);
    }

    private void startPhase3(ResultBundle phase2Result) {
        phase3Controller.start(phase2Result, this::onFinishAndDelete, this::backToPhase1Start);
    }

    private void backToPhase1Start() {
        if (scannedImages == null || selectedRootFolder == null) {
            showFolderSelection();
            return;
        }
        phase1Controller.start(scannedImages, selectedRootFolder, this::onPhase1Completed, this::backToPhase1Start);
    }

    private void onFinishAndDelete(List<ImageItem> imagesToDelete) {
        if (imagesToDelete.isEmpty()) {
            DeleteConfirmationDialog.showResult(
                    "No Deletions",
                    "No images marked for deletion.",
                    stage
            );
            return;
        }

        // Show confirmation dialog
        boolean confirmed = DeleteConfirmationDialog.showConfirmation(imagesToDelete.size(), stage);
        if (!confirmed) {
            return;
        }

        // Show deletion progress dialog
        showDeletionProgress();
        
        // Create task for file deletion
        Task<ImageDeleteService.DeleteResult> deleteTask = new Task<ImageDeleteService.DeleteResult>() {
            @Override
            protected ImageDeleteService.DeleteResult call() {
                return ImageDeleteService.deleteFiles(imagesToDelete);
            }
        };
        
        // Handle deletion completion
        deleteTask.setOnSucceeded(event -> {
            ImageDeleteService.DeleteResult deleteResult = deleteTask.getValue();

            // Show result
            String resultMessage = ImageDeleteService.formatResult(deleteResult);
            DeleteConfirmationDialog.showResult("Deletion Complete", resultMessage, stage);

            // Return to folder selection
            showFolderSelection();
        });
        
        // Handle deletion failure
        deleteTask.setOnFailed(event -> {
            Throwable ex = deleteTask.getException();
            showInfo("Deletion Error", "An error occurred during deletion: " + (ex != null ? ex.getMessage() : "Unknown error"));
            showFolderSelection();
        });
        
        // Run task on background thread
        Thread deleteThread = new Thread(deleteTask);
        deleteThread.setDaemon(true);
        deleteThread.start();
    }
    
    private void showDeletionProgress() {
        Label message = new Label("Deleting files...");
        message.getStyleClass().add("label-body");
        
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(50, 50);
        
        VBox content = new VBox(16, message, progress);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(32));
        
        Scene scene = new Scene(content);
        scene.getStylesheets().add(styleSheet);
        stage.setScene(scene);
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(styleSheet);
        alert.getDialogPane().getStyleClass().add("app-dialog");
        alert.showAndWait();
    }
}
