package net.markwalder.picturetriage.service;

import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase2Progress;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class QuicksortInteractiveRanker {
    private final Deque<IntRange> stack = new ArrayDeque<>();
    private final List<ImageItem> items = new ArrayList<>();

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
