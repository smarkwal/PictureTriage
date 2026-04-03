package net.markwalder.picturetriage.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase1Decision;
import net.markwalder.picturetriage.domain.Phase1Progress;
import net.markwalder.picturetriage.domain.ResultBundle;

public class Phase1WorkflowService {
    private final List<ImageItem> images;
    private final List<ImageItem> kept = new ArrayList<>();
    private final List<ImageItem> triage = new ArrayList<>();
    private final List<ImageItem> deleted = new ArrayList<>();

    private int index;
    private Phase1Progress progress;

    public Phase1WorkflowService(List<ImageItem> images) {
        List<ImageItem> shuffled = new ArrayList<>(images);
        Collections.shuffle(shuffled);
        this.images = List.copyOf(shuffled);
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

    public void triageRemaining() {
        while (!isComplete()) {
            applyDecision(Phase1Decision.TRIAGE);
        }
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

    /**
     * Returns the decision for the image at the given block index, or null if not yet decided.
     * @param blockIndex the index in the block timeline (0-based)
     * @return the Phase1Decision if decided, or null if not yet decided
     */
    public Phase1Decision getDecisionAtIndex(int blockIndex) {
        List<Phase1Decision> timeline = progress.decisionTimeline();
        if (blockIndex >= 0 && blockIndex < timeline.size()) {
            return timeline.get(blockIndex);
        }
        return null;  // Not yet decided
    }
}
