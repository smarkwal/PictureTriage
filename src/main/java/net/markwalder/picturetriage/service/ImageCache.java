package net.markwalder.picturetriage.service;

import javafx.scene.image.Image;
import java.nio.file.Path;
import java.util.Locale;
import java.util.WeakHashMap;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * Simple image cache using WeakHashMap to automatically release images when no longer referenced.
 * Prevents duplicate image loads and reduces memory footprint.
 */
public class ImageCache {
    private final WeakHashMap<String, Image> cache = new WeakHashMap<>();

    /**
     * Get or load an image from the cache.
     * Uses path string as key for caching.
     * 
     * @param path the path to the image file
     * @return the cached or newly loaded image, or null if loading fails
     */
    public Image get(Path path) {
        String key = path.toString();
        Image cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        // Load and cache the image
        Image image = loadImage(path);
        if (image != null) {
            cache.put(key, image);
        }
        return image;
    }

    /**
     * Load an image from disk, with special handling for WebP format.
     * 
     * @param path the path to the image file
     * @return the loaded image, or null if loading fails
     */
    private Image loadImage(Path path) {
        try {
            String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);

            // Check if WebP format
            if (fileName.endsWith(".webp")) {
                BufferedImage bufferedImage = ImageIO.read(path.toFile());
                if (bufferedImage != null) {
                    return SwingFXUtils.toFXImage(bufferedImage, null);
                }
            }

            // For other formats, use JavaFX ImageIO
            return new Image(path.toUri().toString(), true);
        } catch (IOException ex) {
            System.err.println("Failed to load image: " + path + " - " + ex.getMessage());
            return null;
        }
    }

    /**
     * Clear the cache (for testing or memory reclamation).
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get the current cache size.
     */
    public int size() {
        return cache.size();
    }
}
