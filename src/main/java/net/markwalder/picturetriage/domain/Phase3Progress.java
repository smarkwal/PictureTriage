package net.markwalder.picturetriage.domain;

/**
 * Immutable snapshot of Phase 3 (final review) progress.
 * Tracks counts of images marked for keeping or deletion.
 */
public record Phase3Progress(
    int totalImages,
    int keepCount,
    int deleteCount
) {
    public Phase3Progress {
        if (totalImages < 0) {
            throw new IllegalArgumentException("totalImages must be non-negative");
        }
        if (keepCount < 0 || deleteCount < 0) {
            throw new IllegalArgumentException("keepCount and deleteCount must be non-negative");
        }
        if (keepCount + deleteCount > totalImages) {
            throw new IllegalArgumentException("keepCount + deleteCount must not exceed totalImages");
        }
    }
}
