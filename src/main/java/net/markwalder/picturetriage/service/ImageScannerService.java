package net.markwalder.picturetriage.service;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import net.markwalder.picturetriage.domain.ImageItem;

public class ImageScannerService {
    public ScanResult scan(Path rootFolder) {
        List<ImageItem> images = new ArrayList<>();
        int skippedUnreadable = 0;
        int skippedErrors = 0;

        try {
            ScanVisitor visitor = new ScanVisitor(images);
            Files.walkFileTree(rootFolder, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, visitor);
            skippedUnreadable = visitor.skippedUnreadable;
            skippedErrors = visitor.skippedErrors;
            
            // Sort by path for deterministic ordering
            images.sort(Comparator.comparing(item -> item.path().toString()));
        } catch (IOException e) {
            System.err.println("Failed to scan folder: " + rootFolder + " - " + e.getMessage());
            // Return what we could scan, even if root access failed
        }

        return new ScanResult(images, skippedUnreadable, skippedErrors);
    }

    private boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }

    /**
     * Custom file visitor that handles errors per-directory instead of failing the entire scan.
     */
    private class ScanVisitor extends SimpleFileVisitor<Path> {
        private final List<ImageItem> images;
        int skippedUnreadable = 0;
        int skippedErrors = 0;

        ScanVisitor(List<ImageItem> images) {
            this.images = images;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            try {
                if (isSupportedImage(file)) {
                    if (Files.isReadable(file)) {
                        images.add(new ImageItem(file));
                    } else {
                        skippedUnreadable++;
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to process file: " + file + " - " + e.getMessage());
                skippedErrors++;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            // Log the error but continue scanning other files
            System.err.println("Failed to access: " + file + " - " + exc.getMessage());
            skippedErrors++;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            try {
                if (!Files.isReadable(dir)) {
                    System.err.println("Cannot read directory: " + dir + " - skipping");
                    return FileVisitResult.SKIP_SUBTREE;
                }
            } catch (Exception e) {
                System.err.println("Failed to check directory: " + dir + " - " + e.getMessage());
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    public record ScanResult(List<ImageItem> images, int skippedUnreadable, int skippedErrors) {
    }
}
