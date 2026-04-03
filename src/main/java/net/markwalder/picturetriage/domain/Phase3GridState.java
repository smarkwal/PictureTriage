package net.markwalder.picturetriage.domain;

import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of the Phase 3 grid state.
 * Contains the display order of all images and their current decisions.
 * Used by UI components to render the grid with correct visual states.
 */
public record Phase3GridState(
    List<ImageItem> imageDisplayOrder,
    Map<ImageItem, Phase3Decision> decisions
) {
    public Phase3GridState {
        if (imageDisplayOrder == null || decisions == null) {
            throw new IllegalArgumentException("All fields must be non-null");
        }
        imageDisplayOrder = List.copyOf(imageDisplayOrder);
        decisions = Map.copyOf(decisions);
        // Verify decisions map covers all images
        if (!decisions.keySet().containsAll(imageDisplayOrder)) {
            throw new IllegalArgumentException("Decisions map must contain an entry for each image");
        }
    }

    /**
     * Returns the decision for a specific image.
     * @param image the image to look up
     * @return the Phase3Decision for this image
     */
    public Phase3Decision getDecision(ImageItem image) {
        return decisions.get(image);
    }
}
