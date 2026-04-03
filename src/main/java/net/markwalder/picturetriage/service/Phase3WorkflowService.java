package net.markwalder.picturetriage.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.domain.Phase3GridState;

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

        for (ImageItem item : keptImages) {
            decisions.put(item, Phase3Decision.KEEP);
        }
        for (ImageItem item : rankedTriageImages) {
            decisions.put(item, Phase3Decision.KEEP);
        }
        for (ImageItem item : deletedImages) {
            decisions.put(item, Phase3Decision.DELETE);
        }
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
         * @return Phase3GridState with display order and decisions
     */
    public Phase3GridState snapshot() {
        return new Phase3GridState(
                new ArrayList<>(displayOrder),
            new HashMap<>(decisions)
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

    public boolean hasImagesToDelete() {
        return decisions.values().stream().anyMatch(decision -> decision == Phase3Decision.DELETE);
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
