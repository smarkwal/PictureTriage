package net.markwalder.picturetriage.controller;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase1Decision;
import net.markwalder.picturetriage.domain.ResultBundle;
import net.markwalder.picturetriage.service.ImageCache;
import net.markwalder.picturetriage.service.Phase1WorkflowService;
import net.markwalder.picturetriage.ui.BlockProgressBar;
import net.markwalder.picturetriage.ui.ImageDisplayPane;
import net.markwalder.picturetriage.ui.PhaseLayoutContainer;

public class Phase1Controller {
    private final Stage stage;
    private final String styleSheet;
    private final ImageCache imageCache;

    private Phase1WorkflowService phase1Service;
    private Consumer<ResultBundle> onCompleted;
    private Runnable onRestart;
    private Path selectedRootFolder;

    public Phase1Controller(
        Stage stage,
        String styleSheet,
        ImageCache imageCache
    ) {
        this.stage = stage;
        this.styleSheet = styleSheet;
        this.imageCache = imageCache;
    }

    public void start(
        List<ImageItem> images,
        Path selectedRootFolder,
        Consumer<ResultBundle> onCompleted,
        Runnable onRestart
    ) {
        this.phase1Service = new Phase1WorkflowService(images);
        this.selectedRootFolder = selectedRootFolder;
        this.onCompleted = onCompleted;
        this.onRestart = onRestart;
        showPhase1();
    }

    private void showPhase1() {
        ImageDisplayPane imagePane = new ImageDisplayPane(imageCache, selectedRootFolder);
        VBox.setVgrow(imagePane, Priority.ALWAYS);

        BlockProgressBar blockProgressBar = new BlockProgressBar(phase1Service.total(), 1000, 24);

        VBox content = new VBox(imagePane);
        content.setPadding(new Insets(16));

        Button restartButton = new Button("Restart");
        restartButton.getStyleClass().add("button-primary");
        restartButton.setOnAction(e -> {
            if (onRestart != null) {
                onRestart.run();
            }
        });

        Button keepButton = new Button("Keep");
        keepButton.getStyleClass().add("button-keep");
        keepButton.setOnAction(e -> {
            phase1Service.applyDecision(Phase1Decision.KEEP);
            refreshPhase1View(imagePane, blockProgressBar);
        });

        Button triageButton = new Button("Triage");
        triageButton.getStyleClass().add("button-triage");
        triageButton.setOnAction(e -> {
            phase1Service.applyDecision(Phase1Decision.TRIAGE);
            refreshPhase1View(imagePane, blockProgressBar);
        });

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("button-delete");
        deleteButton.setOnAction(e -> {
            phase1Service.applyDecision(Phase1Decision.DELETE);
            refreshPhase1View(imagePane, blockProgressBar);
        });

        Button nextButton = new Button("Next");
        nextButton.getStyleClass().add("button-primary");
        nextButton.setOnAction(e -> {
            phase1Service.triageRemaining();
            refreshPhase1View(imagePane, blockProgressBar);
        });

        PhaseLayoutContainer root = new PhaseLayoutContainer(
            "Phase 1: Triage Images",
            content,
            restartButton,
            List.of(keepButton, triageButton, deleteButton),
            nextButton,
            blockProgressBar
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(styleSheet);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            switch (code) {
                case UP -> phase1Service.applyDecision(Phase1Decision.KEEP);
                case RIGHT -> phase1Service.applyDecision(Phase1Decision.TRIAGE);
                case DOWN -> phase1Service.applyDecision(Phase1Decision.DELETE);
                default -> {
                    return;
                }
            }
            refreshPhase1View(imagePane, blockProgressBar);
            event.consume();
        });

        stage.setScene(scene);
        refreshPhase1View(imagePane, blockProgressBar);
    }

    private void refreshPhase1View(
        ImageDisplayPane imagePane,
        BlockProgressBar blockProgressBar
    ) {
        blockProgressBar.setProgressCounts(phase1Service.index(), phase1Service.total());
        blockProgressBar.update(
            blockIndex -> {
                Phase1Decision decision = phase1Service.getDecisionAtIndex(blockIndex);
                if (decision == null) {
                    return Color.web("#414760");
                }
                return switch (decision) {
                    case KEEP -> Color.web("#2e9f44");
                    case TRIAGE -> Color.web("#8a5cff");
                    case DELETE -> Color.web("#bf2f2f");
                };
            },
            blockIndex -> blockIndex == phase1Service.index()
        );

        if (phase1Service.isComplete()) {
            if (onCompleted != null) {
                onCompleted.accept(phase1Service.partialResult());
            }
            return;
        }

        ImageItem current = phase1Service.currentImage();
        imagePane.setImageItem(current);
    }
}
