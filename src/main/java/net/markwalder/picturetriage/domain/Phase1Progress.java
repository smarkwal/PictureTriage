package net.markwalder.picturetriage.domain;

import java.util.ArrayList;
import java.util.List;

public record Phase1Progress(
    int totalImages,
    int reviewedCount,
    int keepCount,
    int triageCount,
    int deleteCount,
    List<Phase1Decision> decisionTimeline
) {
    public static Phase1Progress empty(int totalImages) {
        return new Phase1Progress(totalImages, 0, 0, 0, 0, List.of());
    }

    public Phase1Progress withDecision(Phase1Decision decision) {
        List<Phase1Decision> timeline = new ArrayList<>(decisionTimeline);
        timeline.add(decision);
        return switch (decision) {
            case KEEP -> new Phase1Progress(totalImages, reviewedCount + 1, keepCount + 1, triageCount, deleteCount, List.copyOf(timeline));
            case TRIAGE -> new Phase1Progress(totalImages, reviewedCount + 1, keepCount, triageCount + 1, deleteCount, List.copyOf(timeline));
            case DELETE -> new Phase1Progress(totalImages, reviewedCount + 1, keepCount, triageCount, deleteCount + 1, List.copyOf(timeline));
        };
    }
}
