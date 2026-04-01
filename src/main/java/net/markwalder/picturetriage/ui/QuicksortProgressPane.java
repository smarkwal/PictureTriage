package net.markwalder.picturetriage.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import net.markwalder.picturetriage.domain.Phase2Progress;

public class QuicksortProgressPane extends VBox {
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label label = new Label("0 / 0 comparisons");

    public QuicksortProgressPane() {
        setSpacing(6);
        setPadding(new Insets(4, 0, 4, 0));
        progressBar.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(progressBar, label);
    }

    public void update(Phase2Progress progress) {
        progressBar.setProgress(progress.fraction());
        label.setText(String.format(
            "Comparisons: %d / %d | Active ranges: %d | Finished ranges: %d",
            progress.comparisonsCompleted(),
            progress.estimatedComparisons(),
            progress.activeRanges(),
            progress.finishedRanges()
        ));
    }
}
