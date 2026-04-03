package net.markwalder.picturetriage.controller;

import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.domain.ResultBundle;
import net.markwalder.picturetriage.service.ImageCache;
import net.markwalder.picturetriage.service.Phase3WorkflowService;
import net.markwalder.picturetriage.ui.BlockProgressBar;
import net.markwalder.picturetriage.ui.Phase3GridPane;
import net.markwalder.picturetriage.ui.PhaseLayoutContainer;

public class Phase3Controller {
    private final Stage stage;
    private final String styleSheet;
    private final double windowWidth;
    private final double windowHeight;
    private final ImageCache imageCache;

    private Phase3WorkflowService phase3Service;
    private Consumer<List<ImageItem>> onDeletionRequested;
    private Runnable onRestart;

    public Phase3Controller(
        Stage stage,
        String styleSheet,
        double windowWidth,
        double windowHeight,
        ImageCache imageCache
    ) {
        this.stage = stage;
        this.styleSheet = styleSheet;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.imageCache = imageCache;
    }

    public void start(
        ResultBundle phase2Result,
        Consumer<List<ImageItem>> onDeletionRequested,
        Runnable onRestart
    ) {
        this.onDeletionRequested = onDeletionRequested;
        this.onRestart = onRestart;
        this.phase3Service = new Phase3WorkflowService(
            phase2Result.keptImages(),
            phase2Result.rankedTriageImages(),
            phase2Result.deletedImages()
        );
        showPhase3();
    }

    private void showPhase3() {
        Phase3GridPane gridPane = new Phase3GridPane(imageCache);
        gridPane.populate(phase3Service.snapshot());

        BlockProgressBar phase3MiniMap = new BlockProgressBar(phase3Service.getTotalImages(), 1000, 24);
        updatePhase3MiniMap(phase3MiniMap, gridPane.getCurrentFocusIndex());

        Button finishButton = new Button("Finish & Delete");
        finishButton.getStyleClass().add("button-primary");
        updateFinishButtonState(finishButton);
        finishButton.setOnAction(e -> {
            if (onDeletionRequested != null) {
                onDeletionRequested.accept(phase3Service.getImagesToDelete());
            }
        });

        Button restartButton = new Button("Restart");
        restartButton.getStyleClass().add("button-primary");
        restartButton.setOnAction(e -> {
            if (onRestart != null) {
                onRestart.run();
            }
        });

        gridPane.setOnImageDecisionChanged((image, newDecision) -> {
            phase3Service.toggleDecision(image);
            updatePhase3MiniMap(phase3MiniMap, gridPane.getCurrentFocusIndex());
            updateFinishButtonState(finishButton);
        });

        VBox content = new VBox(10, gridPane, phase3MiniMap);
        VBox.setVgrow(gridPane, Priority.ALWAYS);

        PhaseLayoutContainer root = new PhaseLayoutContainer(
            "Phase 3: Final Review",
            content,
            restartButton,
            List.of(finishButton)
        );

        Scene scene = new Scene(root, windowWidth, windowHeight);
        scene.getStylesheets().add(styleSheet);

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.UP
                || event.getCode() == javafx.scene.input.KeyCode.DOWN
                || event.getCode() == javafx.scene.input.KeyCode.LEFT
                || event.getCode() == javafx.scene.input.KeyCode.RIGHT
                || event.getCode() == javafx.scene.input.KeyCode.SPACE) {

                gridPane.handleKeyPress(event);
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

    private void updateFinishButtonState(Button finishButton) {
        finishButton.setDisable(!phase3Service.hasImagesToDelete());
    }
}
