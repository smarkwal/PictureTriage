package net.markwalder.picturetriage.domain;

public record Phase2Progress(
    int triageTotal,
    int comparisonsCompleted,
    int estimatedComparisons,
    int activeRanges,
    int finishedRanges,
    boolean done
) {
}
