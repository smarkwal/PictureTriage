package net.markwalder.picturetriage.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase2Progress;

public class QuicksortInteractiveRanker {
    private final Deque<IntRange> stack = new ArrayDeque<>();
    private final List<ImageItem> items = new ArrayList<>();
    private final Set<IntRange> completedRanges = new HashSet<>();

    private ComparisonPair currentPair;
    private int comparisonsCompleted;
    private int finishedRanges;
    private int estimatedComparisons;

    private int lo;
    private int hi;
    private int i;
    private int j;
    private boolean partitionActive;

    public void start(List<ImageItem> triageItems) {
        items.clear();
        items.addAll(triageItems);
        stack.clear();
        completedRanges.clear();
        currentPair = null;
        comparisonsCompleted = 0;
        finishedRanges = 0;
        partitionActive = false;

        estimatedComparisons = estimate(items.size());
        if (items.size() > 1) {
            stack.push(new IntRange(0, items.size() - 1));
        }
        advanceToNextPair();
    }

    public Optional<ComparisonPair> currentPair() {
        return Optional.ofNullable(currentPair);
    }

    public void submitChoice(ComparisonChoice choice) {
        if (currentPair == null || !partitionActive) {
            return;
        }

        // Compare current element (left) with pivot element (right).
        if (choice == ComparisonChoice.LEFT_BETTER) {
            i++;
            swap(i, j);
        }
        comparisonsCompleted++;
        j++;

        if (j >= hi) {
            int pivotIndex = i + 1;
            swap(pivotIndex, hi);
            partitionActive = false;
            finishedRanges++;
            completedRanges.add(new IntRange(pivotIndex, pivotIndex));

            int leftLo = lo;
            int leftHi = pivotIndex - 1;
            int rightLo = pivotIndex + 1;
            int rightHi = hi;

            pushRangeIfValid(rightLo, rightHi);
            pushRangeIfValid(leftLo, leftHi);
        }

        advanceToNextPair();
    }

    public boolean isComplete() {
        return currentPair == null && stack.isEmpty() && !partitionActive;
    }

    /**
     * Returns true if the image at the given index in the triaged list is in a finished range.
     * @param triageIndex the index of the image in the triaged list (0-based)
     * @return true if the image's range is complete, false otherwise
     */
    public boolean isImageInFinishedRange(int triageIndex) {
        return completedRanges.stream()
            .anyMatch(range -> triageIndex >= range.lo() && triageIndex <= range.hi());
    }

    public List<ImageItem> result() {
        return List.copyOf(items);
    }

    public Phase2Progress progress() {
        return new Phase2Progress(
            items.size(),
            comparisonsCompleted,
            estimatedComparisons,
            stack.size() + (partitionActive ? 1 : 0),
            finishedRanges,
            isComplete()
        );
    }

    private void advanceToNextPair() {
        while (true) {
            if (partitionActive) {
                if (j < hi) {
                    currentPair = new ComparisonPair(items.get(j), items.get(hi));
                    return;
                }
                partitionActive = false;
                continue;
            }

            if (stack.isEmpty()) {
                currentPair = null;
                return;
            }

            IntRange range = stack.pop();
            if (range.lo() >= range.hi()) {
                // Range is invalid or single element - mark as complete
                completedRanges.add(new IntRange(range.lo(), range.lo()));
                finishedRanges++;
                continue;
            }

            lo = range.lo();
            hi = range.hi();
            i = lo - 1;
            j = lo;
            partitionActive = true;
        }
    }

    private void pushRangeIfValid(int nextLo, int nextHi) {
        if (nextLo <= nextHi) {
            stack.push(new IntRange(nextLo, nextHi));
        } else {
            if (nextLo == nextHi) {
                completedRanges.add(new IntRange(nextLo, nextHi));
            }
            finishedRanges++;
        }
    }

    private int estimate(int n) {
        if (n <= 1) {
            return 0;
        }
        double log2 = Math.log(n) / Math.log(2);
        return Math.max(1, (int) Math.round(n * log2));
    }

    private void swap(int left, int right) {
        if (left == right) {
            return;
        }
        ImageItem tmp = items.get(left);
        items.set(left, items.get(right));
        items.set(right, tmp);
    }

    private record IntRange(int lo, int hi) {
    }
}
