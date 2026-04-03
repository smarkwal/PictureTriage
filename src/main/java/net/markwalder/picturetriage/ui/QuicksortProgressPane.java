package net.markwalder.picturetriage.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

public class QuicksortProgressPane extends VBox {
    private final BlockProgressBar blockProgressBar;

    public QuicksortProgressPane(int totalImages) {
        setSpacing(6);
        setPadding(new Insets(4, 0, 4, 0));
        this.blockProgressBar = new BlockProgressBar(totalImages, 1000, 24);
        blockProgressBar.setStyle("-fx-padding: 0;");
        getChildren().add(blockProgressBar);
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
}

