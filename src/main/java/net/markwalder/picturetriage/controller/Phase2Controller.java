package net.markwalder.picturetriage.controller;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.ResultBundle;
import net.markwalder.picturetriage.service.ComparisonChoice;
import net.markwalder.picturetriage.service.ComparisonPair;
import net.markwalder.picturetriage.service.ImageCache;
import net.markwalder.picturetriage.service.QuicksortInteractiveRanker;
import net.markwalder.picturetriage.ui.ImageDisplayPane;
import net.markwalder.picturetriage.ui.PhaseLayoutContainer;
import net.markwalder.picturetriage.ui.QuicksortProgressPane;

public class Phase2Controller {
    private final Stage stage;
    private final String styleSheet;
    private final ImageCache imageCache;

    private QuicksortInteractiveRanker ranker;
    private Consumer<ResultBundle> onCompleted;
    private Runnable onRestart;
    private Path selectedRootFolder;
    private ResultBundle phase1Result;

    public Phase2Controller(
        Stage stage,
        String styleSheet,
        ImageCache imageCache
    ) {
        this.stage = stage;
        this.styleSheet = styleSheet;
        this.imageCache = imageCache;
    }

    public void start(
        ResultBundle phase1Result,
        Path selectedRootFolder,
        Consumer<ResultBundle> onCompleted,
        Runnable onRestart
    ) {
        this.phase1Result = phase1Result;
        this.selectedRootFolder = selectedRootFolder;
        this.onCompleted = onCompleted;
        this.onRestart = onRestart;
        this.ranker = new QuicksortInteractiveRanker();
        this.ranker.start(phase1Result.rankedTriageImages());
        showPhase2();
    }

    private void showPhase2() {
        ImageDisplayPane leftPane = new ImageDisplayPane(imageCache, selectedRootFolder);
        ImageDisplayPane rightPane = new ImageDisplayPane(imageCache, selectedRootFolder);
        leftPane.setCursor(javafx.scene.Cursor.HAND);
        rightPane.setCursor(javafx.scene.Cursor.HAND);
        // Align each card toward the center dividing line
        leftPane.setCardAlignment(HPos.RIGHT);
        rightPane.setCardAlignment(HPos.LEFT);
        int totalImageCount = phase1Result.keptImages().size()
            + phase1Result.rankedTriageImages().size()
            + phase1Result.deletedImages().size();
        QuicksortProgressPane progressPane = new QuicksortProgressPane(totalImageCount);

        HBox compareRow = new HBox(12, leftPane, rightPane);
        compareRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        VBox content = new VBox(compareRow);
        content.setPadding(new Insets(16));
        VBox.setVgrow(compareRow, Priority.ALWAYS);

        Button restartButton = new Button("Restart");
        restartButton.getStyleClass().add("button-primary");
        restartButton.setOnAction(e -> {
            if (onRestart != null) {
                onRestart.run();
            }
        });

        Button leftButton = new Button("Left");
        leftButton.getStyleClass().add("button-primary");
        leftButton.setOnAction(e -> {
            if (!ranker.isComplete()) {
                ranker.submitChoice(ComparisonChoice.LEFT_BETTER);
                refreshPhase2View(leftPane, rightPane, progressPane);
            }
        });

        Button rightButton = new Button("Right");
        rightButton.getStyleClass().add("button-primary");
        rightButton.setOnAction(e -> {
            if (!ranker.isComplete()) {
                ranker.submitChoice(ComparisonChoice.RIGHT_BETTER);
                refreshPhase2View(leftPane, rightPane, progressPane);
            }
        });

        PhaseLayoutContainer root = new PhaseLayoutContainer(
            "Phase 2: Rank Triaged Images",
            content,
            restartButton,
            List.of(leftButton, rightButton),
            null,
            progressPane
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(styleSheet);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (ranker.isComplete()) {
                return;
            }

            switch (event.getCode()) {
                case LEFT -> ranker.submitChoice(ComparisonChoice.LEFT_BETTER);
                case RIGHT -> ranker.submitChoice(ComparisonChoice.RIGHT_BETTER);
                default -> {
                    return;
                }
            }
            refreshPhase2View(leftPane, rightPane, progressPane);
            event.consume();
        });

        leftPane.setOnMouseClicked(event -> {
            if (!ranker.isComplete()) {
                ranker.submitChoice(ComparisonChoice.LEFT_BETTER);
                refreshPhase2View(leftPane, rightPane, progressPane);
            }
        });

        rightPane.setOnMouseClicked(event -> {
            if (!ranker.isComplete()) {
                ranker.submitChoice(ComparisonChoice.RIGHT_BETTER);
                refreshPhase2View(leftPane, rightPane, progressPane);
            }
        });

        stage.setScene(scene);
        refreshPhase2View(leftPane, rightPane, progressPane);
    }

    private void refreshPhase2View(
        ImageDisplayPane leftPane,
        ImageDisplayPane rightPane,
        QuicksortProgressPane progressPane
    ) {
        int keptCount = phase1Result.keptImages().size();
        int triageCount = phase1Result.rankedTriageImages().size();
        int deletedCount = phase1Result.deletedImages().size();
        int totalImageCount = keptCount + triageCount + deletedCount;
        int processedImageCount = keptCount + deletedCount + ranker.getCompletedImageCount();

        progressPane.updateBlocksDisplay(
            blockIndex -> getPhase2BlockColor(blockIndex, keptCount, triageCount, deletedCount),
            blockIndex -> isPhase2BlockHighlighted(blockIndex, keptCount, triageCount)
        );
        progressPane.updateProgressCounts(processedImageCount, totalImageCount);

        if (ranker.isComplete()) {
            ResultBundle finalResult = new ResultBundle(
                phase1Result.keptImages(),
                ranker.result(),
                phase1Result.deletedImages()
            );
            if (onCompleted != null) {
                onCompleted.accept(finalResult);
            }
            return;
        }

        ComparisonPair pair = ranker.currentPair().orElseThrow();
        leftPane.setImageItem(pair.left());
        rightPane.setImageItem(pair.right());
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
}
