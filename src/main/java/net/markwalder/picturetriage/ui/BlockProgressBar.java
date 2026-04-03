package net.markwalder.picturetriage.ui;

import java.util.function.Function;
import java.util.function.IntPredicate;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * A generic, reusable block-based progress bar component.
 *
 * Each block represents an item (e.g., an image). The color and highlight state
 * of each block are determined by consumer-provided callbacks, enabling flexible
 * use across different phases and workflows.
 *
 * Blocks are rendered left-to-right and scale to fit the container width with
 * a maximum size of 10 pixels per block.
 */
public class BlockProgressBar extends Canvas {
    private static final int MAX_BLOCK_SIZE = 10;
    private static final int BLOCK_SPACING = 1;
    private static final int HIGHLIGHT_BORDER_WIDTH = 2;
    private static final int MARGIN = 10;

    private static final Color BACKGROUND_COLOR = Color.web("#232638");
    private static final Color BLOCK_BORDER_COLOR = Color.web("#414760");
    private static final Color HIGHLIGHT_BORDER_COLOR = Color.web("#ffffff");

    private final int totalBlocks;
    private Function<Integer, Color> colorProvider;
    private IntPredicate isHighlighted;

    public BlockProgressBar(int totalBlocks) {
        this(totalBlocks, 1000, 24);
    }

    public BlockProgressBar(int totalBlocks, double width, double height) {
        super(width, height);
        this.totalBlocks = Math.max(1, totalBlocks);
        this.colorProvider = idx -> Color.web("#414760");  // Default gray
        this.isHighlighted = idx -> false;  // Default: no highlights
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

    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        // Draw background
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, width, height);

        // Calculate block size with scaling
        double availableWidth = width - 2 * MARGIN;
        double blockSize = Math.min(MAX_BLOCK_SIZE, availableWidth / totalBlocks);
        double totalBlocksWidth = blockSize * totalBlocks + BLOCK_SPACING * (totalBlocks - 1);
        double offsetX = (width - totalBlocksWidth) / 2.0;

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
