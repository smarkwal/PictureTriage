package net.markwalder.picturetriage.domain;

public record Phase2Progress(
    int triageTotal,
    int comparisonsCompleted,
    int estimatedComparisons,
    int activeRanges,
    int finishedRanges,
    boolean done
) {
    public double fraction() {
        if (done) {
            return 1.0;
        }
        if (estimatedComparisons <= 0) {
            return 0.0;
        }
        return Math.min(0.99, comparisonsCompleted / (double) estimatedComparisons);
    }
}
