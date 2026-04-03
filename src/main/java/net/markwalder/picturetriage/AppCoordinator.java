package net.markwalder.picturetriage;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase1Decision;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.domain.ResultBundle;
import net.markwalder.picturetriage.service.ComparisonChoice;
import net.markwalder.picturetriage.service.ComparisonPair;
import net.markwalder.picturetriage.service.ImageCache;
import net.markwalder.picturetriage.service.ImageDeleteService;
import net.markwalder.picturetriage.service.ImageScannerService;
import net.markwalder.picturetriage.service.Phase1WorkflowService;
import net.markwalder.picturetriage.service.Phase3WorkflowService;
import net.markwalder.picturetriage.service.QuicksortInteractiveRanker;

import net.markwalder.picturetriage.ui.DeleteConfirmationDialog;
import net.markwalder.picturetriage.ui.ImageDisplayPane;
import net.markwalder.picturetriage.ui.Phase3GridPane;
import net.markwalder.picturetriage.ui.QuicksortProgressPane;
import net.markwalder.picturetriage.ui.BlockProgressBar;
import net.markwalder.picturetriage.util.StringUtils;

import java.nio.file.Path;
import java.util.List;

public class AppCoordinator {
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 820;

    private final Stage stage;
    private final String styleSheet;
    private final ImageScannerService scannerService = new ImageScannerService();
    private final ImageCache imageCache = new ImageCache();

    private Path selectedRootFolder;
    private List<ImageItem> scannedImages = List.of();
    private Phase1WorkflowService phase1Service;
    private QuicksortInteractiveRanker ranker;
    private Phase3WorkflowService phase3Service;

    public AppCoordinator(Stage stage, String styleSheet) {
        this.stage = stage;
        this.styleSheet = styleSheet;
    }

    public void begin() {
        stage.setTitle("Picture Triage");
        // Handle cleanup when application closes
        stage.setOnCloseRequest(event -> {
            imageCache.clear();
        });
        showFolderSelection();
        stage.show();
    }

    private void showFolderSelection() {
        Label title = new Label("Picture Triage");
        title.getStyleClass().add("label-title-main");

        Label details = new Label("Select a folder to recursively scan JPG, JPEG, PNG, and WEBP images.");
        Button selectButton = new Button("Select Folder");
        selectButton.getStyleClass().add("button-primary");
        selectButton.setOnAction(e -> selectFolder());

        VBox root = new VBox(16, title, details, selectButton);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
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
        selectedRootFolder = folderToScan;
        
        // Show scanning progress dialog
        showScanProgress();
        
        // Run scanning on background thread
        Task<ImageScannerService.ScanResult> scanTask = new Task<ImageScannerService.ScanResult>() {
            @Override
            protected ImageScannerService.ScanResult call() {
                return scannerService.scan(folderToScan);
            }
        };
        
        // Handle task completion
        scanTask.setOnSucceeded(event -> {
            ImageScannerService.ScanResult scanResult = scanTask.getValue();
            scannedImages = scanResult.images();
            
            if (scannedImages.isEmpty()) {
                showInfo("No images found", "No supported images were found in this folder.");
                showFolderSelection();
                return;
            }
            
            phase1Service = new Phase1WorkflowService(scannedImages);
            showPhase1();
        });
        
        // Handle task failure
        scanTask.setOnFailed(event -> {
            Throwable ex = scanTask.getException();
            showInfo("Scan Error", "Failed to scan folder: " + (ex != null ? ex.getMessage() : "Unknown error"));
            showFolderSelection();
        });
        
        // Run task on background thread
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
        
        Scene scene = new Scene(content, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(styleSheet);
        stage.setScene(scene);
    }

    private void showPhase1() {
        Label instructions = new Label("Phase 1: Up = keep, Right = triage, Down = delete");
        instructions.getStyleClass().add("label-instructions");
        instructions.setTooltip(new Tooltip("Use arrow keys: UP→Keep, RIGHT→Triage, DOWN→Delete"));

        Label indexLabel = new Label();
        indexLabel.setMaxWidth(Double.MAX_VALUE);
        indexLabel.setAlignment(Pos.CENTER);
        indexLabel.getStyleClass().add("label-body");

        Label countsLabel = new Label();
        countsLabel.setMaxWidth(Double.MAX_VALUE);
        countsLabel.setAlignment(Pos.CENTER);
        countsLabel.getStyleClass().add("label-body");

        ImageDisplayPane imagePane = new ImageDisplayPane(1000, 620, imageCache, selectedRootFolder);

        BlockProgressBar blockProgressBar = new BlockProgressBar(phase1Service.total(), 1000, 24);
        VBox.setVgrow(imagePane, Priority.ALWAYS);

        HBox progressBarRow = new HBox(blockProgressBar);
        progressBarRow.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, instructions, indexLabel, imagePane, progressBarRow, countsLabel);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(styleSheet);
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.UP) {
                phase1Service.applyDecision(Phase1Decision.KEEP);
            } else if (code == KeyCode.RIGHT) {
                phase1Service.applyDecision(Phase1Decision.TRIAGE);
            } else if (code == KeyCode.DOWN) {
                phase1Service.applyDecision(Phase1Decision.DELETE);
            } else {
                return;
            }
            refreshPhase1View(imagePane, indexLabel, countsLabel, blockProgressBar);
        });

        stage.setScene(scene);
        refreshPhase1View(imagePane, indexLabel, countsLabel, blockProgressBar);
    }

    private void refreshPhase1View(
        ImageDisplayPane imagePane,
        Label indexLabel,
        Label countsLabel,
        BlockProgressBar blockProgressBar
    ) {
        var progress = phase1Service.progress();
        // Update block colors and highlights based on phase 1 state
        blockProgressBar.update(
            blockIndex -> {
                Phase1Decision decision = phase1Service.getDecisionAtIndex(blockIndex);
                if (decision == null) {
                    return Color.web("#414760");  // Gray for undecided
                }
                return switch (decision) {
                    case KEEP -> Color.web("#2e9f44");    // Green
                    case TRIAGE -> Color.web("#8a5cff");  // Violet
                    case DELETE -> Color.web("#bf2f2f");  // Red
                };
            },
            blockIndex -> blockIndex == phase1Service.index()  // Highlight current cursor
        );
        countsLabel.setText(String.format(
            "Reviewed: %d/%d | Keep: %d | Triage: %d | Delete: %d",
            progress.reviewedCount(),
            progress.totalImages(),
            progress.keepCount(),
            progress.triageCount(),
            progress.deleteCount()
        ));

        if (phase1Service.isComplete()) {
            ResultBundle phase1Result = phase1Service.partialResult();
            if (phase1Result.rankedTriageImages().isEmpty()) {
                startPhase3(phase1Result);
            } else {
                startPhase2(phase1Result);
            }
            return;
        }

        ImageItem current = phase1Service.currentImage();
        imagePane.setImageItem(current);
        indexLabel.setText("Image " + (phase1Service.index() + 1) + " of " + phase1Service.total());
    }

    private void startPhase2(ResultBundle phase1Result) {
        ranker = new QuicksortInteractiveRanker();
        ranker.start(phase1Result.rankedTriageImages());
        showPhase2(phase1Result);
    }

    private void showPhase2(ResultBundle phase1Result) {
        Label instructions = new Label("Phase 2: Left = left image is better, Right = right image is better");
        instructions.getStyleClass().add("label-instructions");

        ImageDisplayPane leftPane = new ImageDisplayPane(560, 620, imageCache, selectedRootFolder);
        ImageDisplayPane rightPane = new ImageDisplayPane(560, 620, imageCache, selectedRootFolder);
        leftPane.setCursor(javafx.scene.Cursor.HAND);
        rightPane.setCursor(javafx.scene.Cursor.HAND);
        int totalImageCount = phase1Result.keptImages().size() + phase1Result.rankedTriageImages().size() + phase1Result.deletedImages().size();
        QuicksortProgressPane progressPane = new QuicksortProgressPane(totalImageCount);

        HBox compareRow = new HBox(12, leftPane, rightPane);
        compareRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        VBox root = new VBox(10, instructions, compareRow, progressPane);
        root.setPadding(new Insets(16));

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(styleSheet);
        scene.setOnKeyPressed(event -> {
            if (ranker.isComplete()) {
                return;
            }

            if (event.getCode() == KeyCode.LEFT) {
                ranker.submitChoice(ComparisonChoice.LEFT_BETTER);
            } else if (event.getCode() == KeyCode.RIGHT) {
                ranker.submitChoice(ComparisonChoice.RIGHT_BETTER);
            } else {
                return;
            }
            refreshPhase2View(leftPane, rightPane, progressPane, phase1Result);
        });

        leftPane.setOnMouseClicked(event -> {
            if (!ranker.isComplete()) {
                ranker.submitChoice(ComparisonChoice.LEFT_BETTER);
                refreshPhase2View(leftPane, rightPane, progressPane, phase1Result);
            }
        });

        rightPane.setOnMouseClicked(event -> {
            if (!ranker.isComplete()) {
                ranker.submitChoice(ComparisonChoice.RIGHT_BETTER);
                refreshPhase2View(leftPane, rightPane, progressPane, phase1Result);
            }
        });

        stage.setScene(scene);
        refreshPhase2View(leftPane, rightPane, progressPane, phase1Result);
    }

    private void refreshPhase2View(
        ImageDisplayPane leftPane,
        ImageDisplayPane rightPane,
        QuicksortProgressPane progressPane,
        ResultBundle phase1Result
    ) {
        progressPane.update(ranker.progress());
        
        // Update block display with phase 2 colors and highlights
        int keptCount = phase1Result.keptImages().size();
        int triageCount = phase1Result.rankedTriageImages().size();
        int deletedCount = phase1Result.deletedImages().size();
        
        progressPane.updateBlocksDisplay(
            blockIndex -> getPhase2BlockColor(blockIndex, keptCount, triageCount, deletedCount),
            blockIndex -> isPhase2BlockHighlighted(blockIndex, keptCount, triageCount)
        );

        if (ranker.isComplete()) {
            ResultBundle finalResult = new ResultBundle(
                phase1Result.keptImages(),
                ranker.result(),
                phase1Result.deletedImages()
            );
            startPhase3(finalResult);
            return;
        }

        ComparisonPair pair = ranker.currentPair().orElseThrow();
        leftPane.setImageItem(pair.left());
        rightPane.setImageItem(pair.right());
    }

    private void startPhase3(ResultBundle phase2Result) {
        phase3Service = new Phase3WorkflowService(
                phase2Result.keptImages(),
                phase2Result.rankedTriageImages(),
                phase2Result.deletedImages()
        );
        showPhase3(phase2Result);
    }

    private void showPhase3(ResultBundle phase2Result) {
        Label title = new Label("Phase 3: Final Review");
        title.getStyleClass().add("label-title-secondary");

        Label instructions = new Label("Click images to toggle between keep (green) and delete (red)");
        instructions.getStyleClass().add("label-instructions-secondary");
        instructions.setTooltip(new Tooltip("Use arrow keys to navigate, SPACE to toggle, or click to toggle"));

        // Create grid pane
        Phase3GridPane gridPane = new Phase3GridPane(imageCache);
        gridPane.populate(phase3Service.snapshot());

        // Progress label
        Label progressLabel = new Label();
        progressLabel.getStyleClass().add("label-body");
        progressLabel.getStyleClass().add("label-centered");
        updatePhase3ProgressLabel(progressLabel);

        // Mini-map: one block per image showing current keep/delete state.
        BlockProgressBar phase3MiniMap = new BlockProgressBar(phase3Service.getTotalImages(), 1000, 24);
        updatePhase3MiniMap(phase3MiniMap, gridPane.getCurrentFocusIndex());

        // Button bar
        Button finishButton = new Button("Finish & Delete");
        finishButton.getStyleClass().add("button-primary");
        finishButton.setOnAction(e -> onFinishAndDelete(progressLabel));

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("button-primary");
        cancelButton.setOnAction(e -> showFolderSelection());

        HBox buttonBar = new HBox(10, finishButton, cancelButton);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10));

        // Register click handler for thumbnails
        gridPane.setOnImageDecisionChanged((image, newDecision) -> {
            // Update the service with the new decision
            phase3Service.toggleDecision(image);
            // Update the progress label
            updatePhase3ProgressLabel(progressLabel);
            // Update mini-map to reflect current keep/delete distribution and selection
            updatePhase3MiniMap(phase3MiniMap, gridPane.getCurrentFocusIndex());
        });

        VBox root = new VBox(10, title, instructions, gridPane, phase3MiniMap, progressLabel, buttonBar);
        root.setPadding(new Insets(16));
        VBox.setVgrow(gridPane, Priority.ALWAYS);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(styleSheet);
        
        // Add keyboard navigation for Phase 3 grid using event FILTER (runs before focus traversal)
        // This ensures keyboard events are handled before ScrollPane or other components intercept them
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.UP || 
                event.getCode() == javafx.scene.input.KeyCode.DOWN ||
                event.getCode() == javafx.scene.input.KeyCode.LEFT ||
                event.getCode() == javafx.scene.input.KeyCode.RIGHT ||
                event.getCode() == javafx.scene.input.KeyCode.SPACE) {
                // Route navigation keys to grid pane
                gridPane.handleKeyPress(event);
                // Keep mini-map highlight aligned with current grid selection.
                if (event.getCode() != javafx.scene.input.KeyCode.SPACE) {
                    updatePhase3MiniMap(phase3MiniMap, gridPane.getCurrentFocusIndex());
                }
            }
        });
        
        stage.setScene(scene);
        Platform.runLater(() -> {
            gridPane.selectFirstImage();
            updatePhase3MiniMap(phase3MiniMap, gridPane.getCurrentFocusIndex());
        });
    }

    private void updatePhase3ProgressLabel(Label label) {
        var progress = phase3Service.snapshot().progress();
        label.setText(String.format("Keep: %d | Delete: %d | Total: %d",
                progress.keepCount(), progress.deleteCount(), progress.totalImages()));
        label.setAlignment(Pos.CENTER);
    }

    private void updatePhase3MiniMap(BlockProgressBar miniMap, int selectedIndex) {
        var snapshot = phase3Service.snapshot();
        var orderedImages = snapshot.imageDisplayOrder();
        var decisions = snapshot.decisions();

        miniMap.update(
            blockIndex -> {
                if (blockIndex < 0 || blockIndex >= orderedImages.size()) {
                    return Color.web("#414760");
                }
                ImageItem image = orderedImages.get(blockIndex);
                Phase3Decision decision = decisions.get(image);
                return decision == Phase3Decision.KEEP
                    ? Color.web("#2e9f44")
                    : Color.web("#bf2f2f");
            },
            blockIndex -> blockIndex == selectedIndex
        );
    }

    private void onFinishAndDelete(Label progressLabel) {
        // Get images to delete
        List<ImageItem> imagesToDelete = phase3Service.getImagesToDelete();

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
        
        Scene scene = new Scene(content, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(styleSheet);
        stage.setScene(scene);
    }

    private String displayPath(ImageItem item) {
        if (selectedRootFolder == null) {
            return item.path().toString();
        }

        Path normalizedRoot = selectedRootFolder.toAbsolutePath().normalize();
        Path normalizedItem = item.path().toAbsolutePath().normalize();
        if (normalizedItem.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalizedItem).toString();
        }

        return item.path().toString();
    }

    private Color getPhase2BlockColor(int blockIndex, int keptCount, int triageCount, int deletedCount) {
        if (blockIndex < keptCount) {
            return Color.web("#2e9f44");
        }
        if (blockIndex < keptCount + triageCount) {
            int triageIndex = blockIndex - keptCount;
            return ranker.isImageInFinishedRange(triageIndex)
                ? Color.web("#8a5cff")
                : Color.web("#414760");
        }
        if (blockIndex < keptCount + triageCount + deletedCount) {
            return Color.web("#bf2f2f");
        }
        return Color.web("#414760");
    }

    private boolean isPhase2BlockHighlighted(int blockIndex, int keptCount, int triageCount) {
        if (blockIndex < keptCount || blockIndex >= keptCount + triageCount) {
            return false;
        }

        var currentPair = ranker.currentPair();
        if (currentPair.isEmpty()) {
            return false;
        }

        ComparisonPair pair = currentPair.get();
        int triageIndex = blockIndex - keptCount;
        List<ImageItem> triageImages = ranker.result();
        if (triageIndex >= triageImages.size()) {
            return false;
        }

        ImageItem blockImage = triageImages.get(triageIndex);
        return blockImage.equals(pair.left()) || blockImage.equals(pair.right());
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
