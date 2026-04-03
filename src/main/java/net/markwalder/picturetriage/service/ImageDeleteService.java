package net.markwalder.picturetriage.service;

import net.markwalder.picturetriage.domain.ImageItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for safely deleting image files from disk.
 * 
 * Handles deletion errors gracefully, accumulating failures so the UI can
 * report which files were successfully deleted and which failed.
 */
public class ImageDeleteService {
    /**
     * Result of a file deletion operation.
     */
    public record DeleteResult(
            int deletedCount,
            int failedCount,
            List<FailedDeletion> failedDeletions
    ) {
        /**
         * Represents a single file deletion failure.
         */
        public record FailedDeletion(Path filePath, String reason) {
        }

        /**
         * Check if all deletions succeeded.
         */
        public boolean allSucceeded() {
            return failedCount == 0;
        }
    }

    /**
     * Delete the specified image files from disk.
     * 
     * Attempts to delete each file individually. If a file deletion fails,
     * the error is recorded and deletion continues for remaining files.
     * 
     * @param imagesToDelete list of ImageItem objects to delete from disk
     * @return DeleteResult with counts and list of failures
     */
    public static DeleteResult deleteFiles(List<ImageItem> imagesToDelete) {
        int deletedCount = 0;
        List<DeleteResult.FailedDeletion> failedDeletions = new ArrayList<>();

        for (ImageItem image : imagesToDelete) {
            try {
                Path path = image.path();
                Files.delete(path);
                deletedCount++;
            } catch (IOException e) {
                String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
                failedDeletions.add(
                        new DeleteResult.FailedDeletion(image.path(), reason)
                );
            }
        }

        int failedCount = failedDeletions.size();
        return new DeleteResult(deletedCount, failedCount, failedDeletions);
    }

    /**
     * Format a deletion result for display to the user.
     * 
     * @param result the DeleteResult to format
     * @return user-friendly message string
     */
    public static String formatResult(DeleteResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Deleted: ").append(result.deletedCount).append(" file(s)");

        if (result.failedCount > 0) {
            sb.append("\n\nFailed to delete ").append(result.failedCount).append(" file(s):");
            for (DeleteResult.FailedDeletion failure : result.failedDeletions) {
                sb.append("\n  • ").append(failure.filePath()).append("\n    ")
                        .append(failure.reason());
            }
        }

        return sb.toString();
    }
}
