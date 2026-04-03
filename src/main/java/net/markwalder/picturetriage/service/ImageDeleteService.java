package net.markwalder.picturetriage.service;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.util.StringUtils;

/**
 * Service for safely removing image files from disk.
 * 
 * Prefers moving files to the OS trash/recycle bin when available. If trash
 * support is unavailable, falls back to permanent deletion.
 * 
 * Handles per-file errors gracefully, accumulating failures so the UI can
 * report which files were successfully removed and which failed.
 */
public class ImageDeleteService {
    /**
    * Result of a file removal operation.
     */
    public record DeleteResult(
            int deletedCount,
            int failedCount,
            List<FailedDeletion> failedDeletions
    ) {
        /**
         * Represents a single file removal failure.
         */
        public record FailedDeletion(Path filePath, String reason) {
        }
    }

    /**
     * Remove the specified image files from disk.
     * 
     * Attempts to move each file to trash when supported by the platform.
     * If trash is unsupported, permanently deletes each file.
     * 
     * If moving a file to trash throws an exception, the file is kept and the
     * error is logged to stderr and recorded in the result.
     * 
     * @param imagesToDelete list of ImageItem objects to remove from disk
     * @return DeleteResult with counts and list of failures
     */
    public static DeleteResult deleteFiles(List<ImageItem> imagesToDelete) {
        int deletedCount = 0;
        List<DeleteResult.FailedDeletion> failedDeletions = new ArrayList<>();
        Desktop desktop = getDesktopIfTrashSupported();
        boolean trashSupported = desktop != null;

        for (ImageItem image : imagesToDelete) {
            Path path = image.path();

            if (trashSupported) {
                try {
                    boolean moved = desktop.moveToTrash(path.toFile());
                    if (moved) {
                        deletedCount++;
                    } else {
                        failedDeletions.add(new DeleteResult.FailedDeletion(path,
                                "Could not move image to trash."));
                    }
                } catch (RuntimeException e) {
                    String reason = formatReason(e);
                    System.err.println("Failed to move image to trash (image kept): " + path + " - " + reason);
                    failedDeletions.add(new DeleteResult.FailedDeletion(path, reason));
                }
                continue;
            }

            try {
                Files.delete(path);
                deletedCount++;
            } catch (IOException e) {
                failedDeletions.add(new DeleteResult.FailedDeletion(path, formatReason(e)));
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
        sb.append("Removed: ").append(result.deletedCount).append(" ")
            .append(StringUtils.pluralize(result.deletedCount, "image", "images"));

        if (result.failedCount > 0) {
            sb.append("\n\nFailed to remove ").append(result.failedCount).append(" ")
                .append(StringUtils.pluralize(result.failedCount, "image", "images")).append(":");
            for (DeleteResult.FailedDeletion failure : result.failedDeletions) {
                sb.append("\n  • ").append(failure.filePath()).append("\n    ")
                        .append(failure.reason());
            }
        }

        return sb.toString();
    }

    private static Desktop getDesktopIfTrashSupported() {
        if (!Desktop.isDesktopSupported()) {
            return null;
        }

        Desktop desktop;
        try {
            desktop = Desktop.getDesktop();
        } catch (UnsupportedOperationException e) {
            return null;
        }

        if (!desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            return null;
        }

        return desktop;
    }

    private static String formatReason(Exception e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
