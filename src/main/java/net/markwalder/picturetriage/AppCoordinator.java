package net.markwalder.picturetriage;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase1Decision;
import net.markwalder.picturetriage.domain.ResultBundle;
import net.markwalder.picturetriage.service.ComparisonChoice;
import net.markwalder.picturetriage.service.ComparisonPair;
import net.markwalder.picturetriage.service.ImageScannerService;
import net.markwalder.picturetriage.service.Phase1WorkflowService;
import net.markwalder.picturetriage.service.QuicksortInteractiveRanker;
import net.markwalder.picturetriage.service.ResultsPrinter;
import net.markwalder.picturetriage.ui.QuicksortProgressPane;
import net.markwalder.picturetriage.ui.SegmentedProgressBar;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public class AppCoordinator {
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 820;

    private final Stage stage;
    private final ImageScannerService scannerService = new ImageScannerService();
    private final ResultsPrinter resultsPrinter = new ResultsPrinter();

    private List<ImageItem> scannedImages = List.of();
    private Phase1WorkflowService phase1Service;
    private QuicksortInteractiveRanker ranker;

    public AppCoordinator(Stage stage) {
        this.stage = stage;
    }

    public void begin() {
        stage.setTitle("Picture Triage");
        showFolderSelection();
        stage.show();
    }

    private void showFolderSelection() {
        Label title = new Label("Picture Triage");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label details = new Label("Select a folder to recursively scan JPG, JPEG, PNG, and WEBP files.");
        Button selectButton = new Button("Select Folder");
        selectButton.setOnAction(e -> selectFolder());

        VBox root = new VBox(16, title, details, selectButton);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);

        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    private void selectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Picture Folder");
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

        ImageScannerService.ScanResult scanResult = scannerService.scan(selected);
        scannedImages = scanResult.images();
        if (scannedImages.isEmpty()) {
            showInfo("No images found", "No supported image files were found in this folder.");
            return;
        }

        phase1Service = new Phase1WorkflowService(scannedImages);
        showPhase1();
    }

    private void showPhase1() {
        Label instructions = new Label("Phase 1: Up = keep, Right = triage, Down = delete");
        instructions.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label indexLabel = new Label();
        Label countsLabel = new Label();
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(1000);
        imageView.setFitHeight(620);

        SegmentedProgressBar segmentedBar = new SegmentedProgressBar(1000, 24);

        VBox root = new VBox(10, instructions, indexLabel, imageView, segmentedBar, countsLabel);
        root.setPadding(new Insets(16));

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
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
            refreshPhase1View(imageView, indexLabel, countsLabel, segmentedBar);
        });

        stage.setScene(scene);
        refreshPhase1View(imageView, indexLabel, countsLabel, segmentedBar);
    }

    private void refreshPhase1View(ImageView imageView, Label indexLabel, Label countsLabel, SegmentedProgressBar segmentedBar) {
        var progress = phase1Service.progress();
        segmentedBar.update(progress.decisionTimeline(), progress.totalImages());
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
                showResults(phase1Result);
            } else {
                startPhase2(phase1Result);
            }
            return;
        }

        ImageItem current = phase1Service.currentImage();
        imageView.setImage(loadImage(current));
        indexLabel.setText("Image " + (phase1Service.index() + 1) + " of " + phase1Service.total() + ": " + current.path());
    }

    private void startPhase2(ResultBundle phase1Result) {
        ranker = new QuicksortInteractiveRanker();
        ranker.start(phase1Result.rankedTriageImages());
        showPhase2(phase1Result);
    }

    private void showPhase2(ResultBundle phase1Result) {
        Label instructions = new Label("Phase 2: Left = left picture is better, Right = right picture is better");
        instructions.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ImageView leftView = new ImageView();
        ImageView rightView = new ImageView();
        configurePhase2Image(leftView);
        configurePhase2Image(rightView);

        Label pairLabel = new Label();
        QuicksortProgressPane progressPane = new QuicksortProgressPane();

        HBox compareRow = new HBox(12, leftView, rightView);
        compareRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(leftView, Priority.ALWAYS);
        HBox.setHgrow(rightView, Priority.ALWAYS);

        VBox root = new VBox(10, instructions, pairLabel, compareRow, progressPane);
        root.setPadding(new Insets(16));

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
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
            refreshPhase2View(leftView, rightView, pairLabel, progressPane, phase1Result);
        });

        stage.setScene(scene);
        refreshPhase2View(leftView, rightView, pairLabel, progressPane, phase1Result);
    }

    private void refreshPhase2View(
        ImageView leftView,
        ImageView rightView,
        Label pairLabel,
        QuicksortProgressPane progressPane,
        ResultBundle phase1Result
    ) {
        progressPane.update(ranker.progress());

        if (ranker.isComplete()) {
            ResultBundle finalResult = new ResultBundle(
                phase1Result.keptImages(),
                ranker.result(),
                phase1Result.deletedImages()
            );
            showResults(finalResult);
            return;
        }

        ComparisonPair pair = ranker.currentPair().orElseThrow();
        leftView.setImage(loadImage(pair.left()));
        rightView.setImage(loadImage(pair.right()));
        pairLabel.setText("Choose better picture: left = " + pair.left().displayName() + " | right = " + pair.right().displayName());
    }

    private void showResults(ResultBundle resultBundle) {
        resultsPrinter.print(resultBundle, System.out);

        Label header = new Label("Results");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextArea textArea = new TextArea(renderResults(resultBundle));
        textArea.setEditable(false);
        textArea.setWrapText(false);

        Button restart = new Button("Start Over");
        restart.setOnAction(e -> showFolderSelection());

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(header);
        root.setCenter(textArea);
        root.setBottom(restart);
        BorderPane.setAlignment(restart, Pos.CENTER_LEFT);

        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    private String renderResults(ResultBundle result) {
        StringBuilder sb = new StringBuilder();
        sb.append("1. Kept in phase 1\n");
        appendList(sb, result.keptImages());
        sb.append("\n2. Ranked in phase 2\n");
        appendList(sb, result.rankedTriageImages());
        sb.append("\n3. Deleted in phase 1\n");
        appendList(sb, result.deletedImages());
        return sb.toString();
    }

    private void appendList(StringBuilder sb, List<ImageItem> items) {
        if (items.isEmpty()) {
            sb.append("   (none)\n");
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            sb.append("   ").append(i + 1).append(". ").append(items.get(i).path()).append("\n");
        }
    }

    private void configurePhase2Image(ImageView imageView) {
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(560);
        imageView.setFitHeight(620);
    }

    private Image loadImage(ImageItem item) {
        String name = item.path().getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".webp")) {
            return loadWebpImage(item);
        }
        try {
            return new Image(item.path().toUri().toString(), true);
        } catch (Exception ex) {
            return null;
        }
    }

    private Image loadWebpImage(ImageItem item) {
        try {
            BufferedImage bufferedImage = ImageIO.read(item.path().toFile());
            if (bufferedImage == null) return null;
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (IOException ex) {
            return null;
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
