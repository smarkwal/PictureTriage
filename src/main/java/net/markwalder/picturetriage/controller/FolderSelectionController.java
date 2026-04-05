package net.markwalder.picturetriage.controller;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.service.ImageScannerService;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class FolderSelectionController {
    private final Stage stage;
    private final String styleSheet;
    private final ImageScannerService scannerService;

    private Consumer<FolderScanResult> onScanCompleted;

    public FolderSelectionController(
        Stage stage,
        String styleSheet
    ) {
        this.stage = stage;
        this.styleSheet = styleSheet;
        this.scannerService = new ImageScannerService();
    }

    public void showFolderSelection(Consumer<FolderScanResult> onScanCompleted) {
        this.onScanCompleted = onScanCompleted;

        Label title = new Label("Picture Triage");
        title.getStyleClass().add("label-title-main");

        Label details = new Label("Select a folder to recursively scan JPG, JPEG, PNG, and WEBP images.");
        Button selectButton = new Button("Select Folder");
        selectButton.getStyleClass().add("button-primary");
        selectButton.setOnAction(e -> selectFolder());

        VBox root = new VBox(16, title, details, selectButton);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(styleSheet);
        stage.setScene(scene);
    }

    private void selectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Image Folder");
        Path selected = null;
        if (stage.getScene() != null && stage.getScene().getWindow() != null) {
            var file = chooser.showDialog(stage);
            if (file != null) {
                selected = file.toPath();
            }
        }

        if (selected == null) {
            return;
        }

        final Path folderToScan = selected;

        showScanProgress();

        Task<ImageScannerService.ScanResult> scanTask = new Task<ImageScannerService.ScanResult>() {
            @Override
            protected ImageScannerService.ScanResult call() {
                return scannerService.scan(folderToScan);
            }
        };

        scanTask.setOnSucceeded(event -> {
            ImageScannerService.ScanResult scanResult = scanTask.getValue();
            List<ImageItem> scannedImages = scanResult.images();

            if (scannedImages.isEmpty()) {
                showInfo("No images found", "No supported images were found in this folder.");
                showFolderSelection(onScanCompleted);
                return;
            }

            if (onScanCompleted != null) {
                onScanCompleted.accept(new FolderScanResult(folderToScan, scannedImages));
            }
        });

        scanTask.setOnFailed(event -> {
            Throwable ex = scanTask.getException();
            showInfo("Scan Error", "Failed to scan folder: " + (ex != null ? ex.getMessage() : "Unknown error"));
            showFolderSelection(onScanCompleted);
        });

        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }

    private void showScanProgress() {
        Label message = new Label("Scanning folder for images...");
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

    public record FolderScanResult(Path selectedFolder, List<ImageItem> images) {
    }
}
