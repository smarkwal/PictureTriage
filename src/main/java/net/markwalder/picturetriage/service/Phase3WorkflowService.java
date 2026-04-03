package net.markwalder.picturetriage.service;

import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.domain.Phase3GridState;
import net.markwalder.picturetriage.domain.Phase3Progress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the Phase 3 workflow: final review and decision toggle for all images.
 * 
 * Orchestrates the combined set of images (kept + triaged sorted + deleted).
 * Users can toggle the decision for any image between KEEP and DELETE.
 * Tracks which images are marked for deletion and provides a snapshot for UI rendering.
 */
public class Phase3WorkflowService {
    private final List<ImageItem> displayOrder;
    private final Map<ImageItem, Phase3Decision> decisions;
    private final Map<ImageItem, Phase3Decision> originalDecisions;

    /**
     * Initialize Phase 3 with all images from previous phases.
     * 
     * @param keptImages images decided to keep in Phase 1
     * @param rankedTriageImages images triaged in Phase 1, ranked in Phase 2
     * @param deletedImages images decided to delete in Phase 1
     */
    public Phase3WorkflowService(
            List<ImageItem> keptImages,
            List<ImageItem> rankedTriageImages,
            List<ImageItem> deletedImages) {
        // Create display order: kept + triaged (sorted) + deleted
        this.displayOrder = new ArrayList<>();
        this.displayOrder.addAll(keptImages);
        this.displayOrder.addAll(rankedTriageImages);
        this.displayOrder.addAll(deletedImages);

        // Initialize decisions: kept/triaged → KEEP, deleted → DELETE
        this.decisions = new HashMap<>();
        this.originalDecisions = new HashMap<>();

        for (ImageItem item : keptImages) {
            decisions.put(item, Phase3Decision.KEEP);
        }
        for (ImageItem item : rankedTriageImages) {
            decisions.put(item, Phase3Decision.KEEP);
        }
        for (ImageItem item : deletedImages) {
            decisions.put(item, Phase3Decision.DELETE);
        }

        // Track original decisions for comparison
        originalDecisions.putAll(decisions);
    }

    /**
     * Toggle the decision for an image between KEEP and DELETE.
     * 
     * @param image the image to toggle
     */
    public void toggleDecision(ImageItem image) {
        Phase3Decision current = decisions.get(image);
        if (current == null) {
            throw new IllegalArgumentException("Image not found in workflow: " + image);
        }
        Phase3Decision newDecision = current == Phase3Decision.KEEP
                ? Phase3Decision.DELETE
                : Phase3Decision.KEEP;
        decisions.put(image, newDecision);
    }

    /**
     * Return an immutable snapshot of the current grid state.
     * 
     * @return Phase3GridState with display order, decisions, and progress
     */
    public Phase3GridState snapshot() {
        int totalImages = displayOrder.size();
        int keepCount = (int) decisions.values().stream()
                .filter(d -> d == Phase3Decision.KEEP)
                .count();
        int deleteCount = totalImages - keepCount;

        Phase3Progress progress = new Phase3Progress(totalImages, keepCount, deleteCount);
        return new Phase3GridState(
                new ArrayList<>(displayOrder),
                new HashMap<>(decisions),
                progress
        );
    }

    /**
     * Get the list of images marked for deletion.
     * 
     * @return list of images with DELETE decision
     */
    public List<ImageItem> getImagesToDelete() {
        return displayOrder.stream()
                .filter(image -> decisions.get(image) == Phase3Decision.DELETE)
                .toList();
    }

    /**
     * Get the list of images marked for keeping.
     * 
     * @return list of images with KEEP decision
     */
    public List<ImageItem> getImagesToKeep() {
        return displayOrder.stream()
                .filter(image -> decisions.get(image) == Phase3Decision.KEEP)
                .toList();
    }

    /**
     * Check if any decisions have been modified from their original values.
     * 
     * @return true if at least one decision differs from original
     */
    public boolean isModified() {
        for (ImageItem image : displayOrder) {
            if (!decisions.get(image).equals(originalDecisions.get(image))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get total count of images.
     * 
     * @return number of images in the workflow
     */
    public int getTotalImages() {
        return displayOrder.size();
    }
}
