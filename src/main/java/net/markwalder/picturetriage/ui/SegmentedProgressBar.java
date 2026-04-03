package net.markwalder.picturetriage.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import net.markwalder.picturetriage.domain.Phase1Decision;

import java.util.ArrayList;
import java.util.List;

public class SegmentedProgressBar extends Canvas {
    private int total = 1;
    private List<Phase1Decision> timeline = List.of();

    public SegmentedProgressBar(double width, double height) {
        super(width, height);
    }

    public void update(List<Phase1Decision> timeline, int total) {
        this.timeline = new ArrayList<>(timeline);
        this.total = Math.max(1, total);
        redraw();
    }

    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        gc.setFill(Color.web("#232638"));
        gc.fillRoundRect(0, 0, width, height, 8, 8);

        double segmentWidth = width / total;
        for (int idx = 0; idx < timeline.size() && idx < total; idx++) {
            Phase1Decision decision = timeline.get(idx);
            gc.setFill(colorFor(decision));
            gc.fillRect(idx * segmentWidth, 0, segmentWidth, height);
        }

        gc.setStroke(Color.web("#414760"));
        gc.strokeRoundRect(0, 0, width, height, 8, 8);
    }

    private Color colorFor(Phase1Decision decision) {
        return switch (decision) {
            case KEEP -> Color.web("#2e9f44");
            case TRIAGE -> Color.web("#8a5cff");
            case DELETE -> Color.web("#bf2f2f");
        };
    }
}
