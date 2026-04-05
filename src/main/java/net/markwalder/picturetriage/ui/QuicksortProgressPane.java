package net.markwalder.picturetriage.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class QuicksortProgressPane extends VBox {
    private final BlockProgressBar blockProgressBar;

    public QuicksortProgressPane(int totalImages) {
        setSpacing(6);
        setPadding(new Insets(4, 0, 4, 0));
        setFillWidth(false);
        setAlignment(Pos.CENTER);
        this.blockProgressBar = new BlockProgressBar(totalImages, 1000, 24);
        blockProgressBar.setPadding(Insets.EMPTY);

        HBox progressBarRow = new HBox(blockProgressBar);
        progressBarRow.setAlignment(Pos.CENTER);
        getChildren().add(progressBarRow);
    }

    /**
     * Updates the block colors and highlights based on provided callbacks.
     * @param colorCallback function to determine color for each block
     * @param highlightCallback predicate to determine which blocks should be highlighted
     */
    public void updateBlocksDisplay(
        java.util.function.Function<Integer, javafx.scene.paint.Color> colorCallback,
        java.util.function.IntPredicate highlightCallback
    ) {
        if (blockProgressBar != null) {
            blockProgressBar.update(colorCallback, highlightCallback);
        }
    }

    public void updateProgressCounts(int processed, int total) {
        if (blockProgressBar != null) {
            blockProgressBar.setProgressCounts(processed, total);
        }
    }
}

