package net.markwalder.picturetriage.service;

import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase1Decision;
import net.markwalder.picturetriage.domain.Phase1Progress;
import net.markwalder.picturetriage.domain.ResultBundle;

import java.util.ArrayList;
import java.util.List;

public class Phase1WorkflowService {
    private final List<ImageItem> images;
    private final List<ImageItem> kept = new ArrayList<>();
    private final List<ImageItem> triage = new ArrayList<>();
    private final List<ImageItem> deleted = new ArrayList<>();

    private int index;
    private Phase1Progress progress;

    public Phase1WorkflowService(List<ImageItem> images) {
        this.images = List.copyOf(images);
        this.index = 0;
        this.progress = Phase1Progress.empty(images.size());
    }

    public ImageItem currentImage() {
        if (isComplete()) {
            return null;
        }
        return images.get(index);
    }

    public void applyDecision(Phase1Decision decision) {
        if (isComplete()) {
            return;
        }

        ImageItem image = images.get(index);
        switch (decision) {
            case KEEP -> kept.add(image);
            case TRIAGE -> triage.add(image);
            case DELETE -> deleted.add(image);
        }
        progress = progress.withDecision(decision);
        index++;
    }

    public boolean isComplete() {
        return index >= images.size();
    }

    public Phase1Progress progress() {
        return progress;
    }

    public int index() {
        return index;
    }

    public int total() {
        return images.size();
    }

    public ResultBundle partialResult() {
        return new ResultBundle(List.copyOf(kept), List.copyOf(triage), List.copyOf(deleted));
    }
}
