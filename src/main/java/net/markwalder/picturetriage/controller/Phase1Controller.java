package net.markwalder.picturetriage.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
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

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class Phase1Controller {
    private final Stage stage;
    private final String styleSheet;
    private final double windowWidth;
    private final double windowHeight;
    private final ImageCache imageCache;

    private Phase1WorkflowService phase1Service;
    private Consumer<ResultBundle> onCompleted;
    private Path selectedRootFolder;

    public Phase1Controller(
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

    public void start(List<ImageItem> images, Path selectedRootFolder, Consumer<ResultBundle> onCompleted) {
        this.phase1Service = new Phase1WorkflowService(images);
        this.selectedRootFolder = selectedRootFolder;
        this.onCompleted = onCompleted;
        showPhase1();
    }

    private void showPhase1() {
        Label instructions = new Label("Phase 1: Up = keep, Right = triage, Down = delete");
        instructions.getStyleClass().add("label-instructions");
        instructions.setTooltip(new Tooltip("Use arrow keys: UP->Keep, RIGHT->Triage, DOWN->Delete"));

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

        Scene scene = new Scene(root, windowWidth, windowHeight);
        scene.getStylesheets().add(styleSheet);
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            switch (code) {
                case UP -> phase1Service.applyDecision(Phase1Decision.KEEP);
                case RIGHT -> phase1Service.applyDecision(Phase1Decision.TRIAGE);
                case DOWN -> phase1Service.applyDecision(Phase1Decision.DELETE);
                default -> {
                    return;
                }
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
        countsLabel.setText(String.format(
            "Reviewed: %d/%d | Keep: %d | Triage: %d | Delete: %d",
            progress.reviewedCount(),
            progress.totalImages(),
            progress.keepCount(),
            progress.triageCount(),
            progress.deleteCount()
        ));

        if (phase1Service.isComplete()) {
            if (onCompleted != null) {
                onCompleted.accept(phase1Service.partialResult());
            }
            return;
        }

        ImageItem current = phase1Service.currentImage();
        imagePane.setImageItem(current);
        indexLabel.setText("Image " + (phase1Service.index() + 1) + " of " + phase1Service.total());
    }
}
