package net.markwalder.picturetriage.ui;

import java.util.function.Function;
import java.util.function.IntPredicate;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A generic, reusable block-based progress bar component.
 *
 * Each block represents an item (e.g., an image). The color and highlight state
 * of each block are determined by consumer-provided callbacks, enabling flexible
 * use across different phases and workflows.
 *
 * Blocks are rendered left-to-right and scale to fit the container width with
 * a maximum size of 10 pixels per block, with a processed/total label shown to
 * the right.
 */
public class BlockProgressBar extends HBox {
    private static final int MAX_BLOCK_SIZE = 20;
    private static final int BLOCK_SPACING = 1;
    private static final int HIGHLIGHT_BORDER_WIDTH = 2;
    private static final int MARGIN = 10;

    private static final Color BACKGROUND_COLOR = Color.web("#232638");
    private static final Color BLOCK_BORDER_COLOR = Color.web("#414760");
    private static final Color HIGHLIGHT_BORDER_COLOR = Color.web("#ffffff");

    private final int totalBlocks;
    private final Canvas canvas;
    private final Label countLabel;
    private Function<Integer, Color> colorProvider;
    private IntPredicate isHighlighted;

    public BlockProgressBar(int totalBlocks) {
        this(totalBlocks, 1000, 32);
    }

    public BlockProgressBar(int totalBlocks, double width, double height) {
        this.totalBlocks = Math.max(1, totalBlocks);
        this.canvas = new Canvas(getCanvasWidth(width), height);
        this.countLabel = new Label();
        this.colorProvider = idx -> Color.web("#414760");  // Default gray
        this.isHighlighted = idx -> false;  // Default: no highlights

        setSpacing(20);
        setAlignment(Pos.CENTER_LEFT);
        setFillHeight(true);
        setMaxWidth(Region.USE_PREF_SIZE);
        setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        setPadding(new Insets(0, MARGIN, 0, 0));

        canvas.widthProperty().addListener((obs, oldValue, newValue) -> redraw());
        canvas.heightProperty().addListener((obs, oldValue, newValue) -> redraw());

        countLabel.getStyleClass().add("label-body");
        countLabel.setMinWidth(70);
        countLabel.setAlignment(Pos.CENTER_LEFT);
        countLabel.setText("0 / " + this.totalBlocks);

        HBox.setHgrow(canvas, Priority.NEVER);
        getChildren().addAll(canvas, countLabel);
        redraw();
    }

    private double getCanvasWidth(double requestedWidth) {
        double preferredWidth = 2 * MARGIN
            + totalBlocks * MAX_BLOCK_SIZE
            + Math.max(0, totalBlocks - 1) * BLOCK_SPACING;
        return Math.min(requestedWidth, preferredWidth);
    }

    /**
     * Updates the progress bar with new color and highlight callbacks.
     *
     * @param colorProvider Function that returns the Color for each block index
     * @param isHighlighted Predicate that returns true if block should be highlighted
     */
    public void update(Function<Integer, Color> colorProvider, IntPredicate isHighlighted) {
        this.colorProvider = colorProvider != null ? colorProvider : idx -> Color.web("#414760");
        this.isHighlighted = isHighlighted != null ? isHighlighted : idx -> false;
        redraw();
    }

    public void setProgressCounts(int processed, int total) {
        int safeTotal = Math.max(0, total);
        int safeProcessed = Math.max(0, Math.min(processed, safeTotal));
        countLabel.setText(safeProcessed + " / " + safeTotal);
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // Draw background
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, width, height);

        // Calculate block size with scaling
        double availableWidth = width - 2 * MARGIN;
        double blockSize = Math.min(MAX_BLOCK_SIZE, availableWidth / totalBlocks);
        double offsetX = MARGIN;

        // Draw each block
        for (int blockIdx = 0; blockIdx < totalBlocks; blockIdx++) {
            double x = offsetX + blockIdx * (blockSize + BLOCK_SPACING);
            double y = (height - blockSize) / 2.0;

            // Draw block background
            Color blockColor = colorProvider.apply(blockIdx);
            gc.setFill(blockColor);
            gc.fillRect(x, y, blockSize, blockSize);

            // Draw block border
            gc.setStroke(BLOCK_BORDER_COLOR);
            gc.setLineWidth(1);
            gc.strokeRect(x, y, blockSize, blockSize);

            // Draw highlight border if needed
            if (isHighlighted.test(blockIdx)) {
                gc.setStroke(HIGHLIGHT_BORDER_COLOR);
                gc.setLineWidth(HIGHLIGHT_BORDER_WIDTH);
                double inset = HIGHLIGHT_BORDER_WIDTH / 2.0;
                gc.strokeRect(x + inset, y + inset, blockSize - HIGHLIGHT_BORDER_WIDTH, blockSize - HIGHLIGHT_BORDER_WIDTH);
            }
        }
    }
}
