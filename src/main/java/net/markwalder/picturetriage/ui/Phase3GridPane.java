package net.markwalder.picturetriage.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.domain.Phase3GridState;
import net.markwalder.picturetriage.service.ImageCache;

/**
 * Grid layout pane for Phase 3 displaying all images.
 * 
 * Manages a 4-column grid of ImageThumbnailButton components.
 * Provides methods to populate the grid and update individual thumbnail states.
 */
public class Phase3GridPane extends VBox {
    private static final int COLUMNS = 4;
    private static final double GAP = 10.0;

    private final GridPane gridPane;
    private final ScrollPane scrollPane;
    private final Map<ImageItem, ImageThumbnailButton> thumbnailMap;
    private final ImageCache imageCache;
    private BiConsumer<ImageItem, Phase3Decision> onImageDecisionChanged;
    
    // Keyboard navigation state
    private List<ImageItem> imageOrder = new ArrayList<>();
    private Map<ImageItem, Integer> imageIndexMap = new HashMap<>();
    private int currentFocusIndex = 0;

    public Phase3GridPane(ImageCache imageCache) {
        this.imageCache = imageCache;
        // Setup layout

        // Create grid pane
        this.gridPane = new GridPane();
        gridPane.setHgap(GAP);
        gridPane.setVgap(GAP);
        gridPane.setPadding(new Insets(10));
        gridPane.getStyleClass().add("grid-pane-phase3");

        this.thumbnailMap = new HashMap<>();

        // Add scroll pane to handle overflow
        this.scrollPane = new ScrollPane(gridPane);
        scrollPane.getStyleClass().add("phase3-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(false);  // Disable panning so arrow keys work for navigation
        getChildren().add(scrollPane);
        setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        
        // Request focus on the grid pane itself so keyboard events are captured
        setFocusTraversable(true);
    }

    /**
     * Populate the grid with thumbnails from the given grid state.
     * 
     * @param state the Phase3GridState containing images and decisions
     */
    public void populate(Phase3GridState state) {
        gridPane.getChildren().clear();
        thumbnailMap.clear();
        imageOrder.clear();
        imageIndexMap.clear();
        currentFocusIndex = 0;

        List<ImageItem> images = state.imageDisplayOrder();
        imageOrder.addAll(images);
        for (int i = 0; i < images.size(); i++) {
            imageIndexMap.put(images.get(i), i);
        }
        
        int row = 0;
        int col = 0;

        for (ImageItem image : images) {
            Phase3Decision decision = state.getDecision(image);
            ImageThumbnailButton thumbnail = new ImageThumbnailButton(image, decision, imageCache);

            // Register the callback for when this thumbnail is clicked
            thumbnail.setOnDecisionChanged((img, newDecision) -> {
                Integer clickedIndex = imageIndexMap.get(img);
                if (clickedIndex != null) {
                    currentFocusIndex = clickedIndex;
                    focusCurrentThumbnail();
                }
                if (onImageDecisionChanged != null) {
                    onImageDecisionChanged.accept(img, newDecision);
                }
            });

            // Add thumbnail to map for later updates
            thumbnailMap.put(image, thumbnail);

            // Add to grid
            gridPane.add(thumbnail, col, row);

            // Move to next position
            col++;
            if (col >= COLUMNS) {
                col = 0;
                row++;
            }
        }
        
        // Request focus on the first thumbnail for keyboard navigation
        if (!images.isEmpty()) {
            ImageThumbnailButton firstButton = thumbnailMap.get(images.get(0));
            if (firstButton != null) {
                // Set visual focus indicator on first button (don't request focus - let user click or use keyboard)
                updateFocusIndicators(firstButton);
            }
        }
    }

    /**
     * Reset selection to the first image (top-left thumbnail).
     */
    public void selectFirstImage() {
        if (imageOrder.isEmpty()) {
            return;
        }
        currentFocusIndex = 0;
        focusCurrentThumbnail();
    }
    
    /**
     * Handle keyboard navigation for arrow keys and space.
     * Called from the scene's key press handler in AppCoordinator.
     */
    public void handleKeyPress(KeyEvent event) {
        if (imageOrder.isEmpty()) {
            return;
        }
        
        boolean handled = false;
        
        if (event.getCode() == KeyCode.UP) {
            // Move up one row (same column)
            moveUp();
            focusCurrentThumbnail();
            handled = true;
        } else if (event.getCode() == KeyCode.DOWN) {
            // Move down one row (same column)
            moveDown();
            focusCurrentThumbnail();
            handled = true;
        } else if (event.getCode() == KeyCode.LEFT) {
            // Move left within row
            moveLeft();
            focusCurrentThumbnail();
            handled = true;
        } else if (event.getCode() == KeyCode.RIGHT) {
            // Move right within row
            moveRight();
            focusCurrentThumbnail();
            handled = true;
        } else if (event.getCode() == KeyCode.SPACE) {
            // Toggle decision on current thumbnail
            ImageItem currentImage = imageOrder.get(currentFocusIndex);
            ImageThumbnailButton button = thumbnailMap.get(currentImage);
            if (button != null) {
                button.toggleDecision();
                handled = true;
            }
        }
        
        if (handled) {
            event.consume();
        }
    }
    
    /**
     * Move focus to the previous image in the list, wrapping to the last image.
     */
    private void moveLeft() {
        currentFocusIndex = (currentFocusIndex - 1 + imageOrder.size()) % imageOrder.size();
    }
    
    /**
     * Move focus to the next image in the list, wrapping to the first image.
     */
    private void moveRight() {
        currentFocusIndex = (currentFocusIndex + 1) % imageOrder.size();
    }
    
    /**
     * Move focus up one row (same column position), wrapping to the bottom row.
     */
    private void moveUp() {
        int column = currentFocusIndex % COLUMNS;
        int newIndex = currentFocusIndex - COLUMNS;
        
        if (newIndex >= 0) {
            currentFocusIndex = newIndex;
        } else {
            // Wrap to same column in the last row
            int lastRowStart = (imageOrder.size() - 1) / COLUMNS * COLUMNS;
            currentFocusIndex = Math.min(lastRowStart + column, imageOrder.size() - 1);
        }
    }
    
    /**
     * Move focus down one row (same column position), wrapping to the top row.
     */
    private void moveDown() {
        int column = currentFocusIndex % COLUMNS;
        int newIndex = currentFocusIndex + COLUMNS;
        
        if (newIndex < imageOrder.size()) {
            currentFocusIndex = newIndex;
        } else {
            // Wrap to same column in the first row
            currentFocusIndex = column;
        }
    }
    
    /**
     * Focus on the current thumbnail and ensure it's visible.
     */
    private void focusCurrentThumbnail() {
        if (currentFocusIndex >= 0 && currentFocusIndex < imageOrder.size()) {
            ImageItem currentImage = imageOrder.get(currentFocusIndex);
            ImageThumbnailButton button = thumbnailMap.get(currentImage);
            if (button != null) {
                // Update visual focus indicator (don't request focus - use keyboard event interception instead)
                updateFocusIndicators(button);
                ensureThumbnailVisible(button);
            }
        }
    }

    /**
     * Scroll the viewport so the selected thumbnail is visible.
     */
    private void ensureThumbnailVisible(ImageThumbnailButton button) {
        Bounds viewportBounds = scrollPane.getViewportBounds();
        Bounds contentBounds = gridPane.getLayoutBounds();
        Bounds buttonBounds = button.getBoundsInParent();

        double maxScrollableY = contentBounds.getHeight() - viewportBounds.getHeight();
        if (maxScrollableY <= 0) {
            return;
        }

        double currentTopY = scrollPane.getVvalue() * maxScrollableY;
        double currentBottomY = currentTopY + viewportBounds.getHeight();

        double targetTopY = currentTopY;
        if (buttonBounds.getMinY() < currentTopY) {
            targetTopY = buttonBounds.getMinY();
        } else if (buttonBounds.getMaxY() > currentBottomY) {
            targetTopY = buttonBounds.getMaxY() - viewportBounds.getHeight();
        }

        if (targetTopY != currentTopY) {
            double clampedTopY = Math.max(0, Math.min(targetTopY, maxScrollableY));
            scrollPane.setVvalue(clampedTopY / maxScrollableY);
        }
    }
    
    /**
     * Update the visual focus indicator across all thumbnails.
     */
    private void updateFocusIndicators(ImageThumbnailButton focusedButton) {
        for (ImageThumbnailButton button : thumbnailMap.values()) {
            if (button == focusedButton) {
                button.setFocusIndicator(true);
            } else {
                button.setFocusIndicator(false);
            }
        }
    }

    /**
     * Register a callback to be invoked when any thumbnail decision is changed.
     * The callback receives the image and the new decision.
     * 
     * @param callback a BiConsumer to invoke on decision change (image, decision)
     */
    public void setOnImageDecisionChanged(BiConsumer<ImageItem, Phase3Decision> callback) {
        this.onImageDecisionChanged = callback;
    }

    /**
     * Returns the currently selected thumbnail index in display order.
     *
     * @return selected index (0-based), or -1 when there are no images
     */
    public int getCurrentFocusIndex() {
        if (imageOrder.isEmpty()) {
            return -1;
        }
        return currentFocusIndex;
    }
}

