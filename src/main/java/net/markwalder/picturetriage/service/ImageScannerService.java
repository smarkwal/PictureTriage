package net.markwalder.picturetriage.service;

import net.markwalder.picturetriage.domain.ImageItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class ImageScannerService {
    public ScanResult scan(Path rootFolder) {
        List<ImageItem> images = new ArrayList<>();
        int skippedUnreadable = 0;

        try (Stream<Path> walk = Files.walk(rootFolder)) {
            List<Path> candidates = walk
                .filter(Files::isRegularFile)
                .filter(this::isSupportedImage)
                .sorted(Comparator.comparing(Path::toString))
                .toList();

            for (Path candidate : candidates) {
                if (Files.isReadable(candidate)) {
                    images.add(new ImageItem(candidate));
                } else {
                    skippedUnreadable++;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan folder: " + rootFolder, e);
        }

        return new ScanResult(images, skippedUnreadable);
    }

    private boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }

    public record ScanResult(List<ImageItem> images, int skippedUnreadable) {
    }
}
