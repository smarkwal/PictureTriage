package net.markwalder.picturetriage.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.markwalder.picturetriage.domain.Phase2Progress;

public class QuicksortProgressPane extends VBox {
    private BlockProgressBar blockProgressBar;
    private final Label label = new Label("0 / 0 comparisons");

    public QuicksortProgressPane(int totalImages) {
        setSpacing(6);
        setPadding(new Insets(4, 0, 4, 0));
        this.blockProgressBar = new BlockProgressBar(totalImages, 1000, 24);
        blockProgressBar.setStyle("-fx-padding: 0;");
        getChildren().addAll(blockProgressBar, label);
    }

    public void update(Phase2Progress progress) {
        label.setText(String.format(
            "Comparisons: %d / %d | Active ranges: %d | Finished ranges: %d",
            progress.comparisonsCompleted(),
            progress.estimatedComparisons(),
            progress.activeRanges(),
            progress.finishedRanges()
        ));
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

