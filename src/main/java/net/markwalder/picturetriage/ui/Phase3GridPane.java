package net.markwalder.picturetriage.ui;

import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.domain.Phase3GridState;
import net.markwalder.picturetriage.service.ImageCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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
        setPadding(new Insets(10));
        setSpacing(10);
        getStyleClass().add("grid-container");

        // Create grid pane
        this.gridPane = new GridPane();
        gridPane.setHgap(GAP);
        gridPane.setVgap(GAP);
        gridPane.setPadding(new Insets(10));
        gridPane.getStyleClass().add("grid-pane-phase3");

        this.thumbnailMap = new HashMap<>();

        // Add scroll pane to handle overflow
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(gridPane);
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
     * Move focus left within the current row, wrapping to the rightmost position.
     */
    private void moveLeft() {
        int column = currentFocusIndex % COLUMNS;
        int row = currentFocusIndex / COLUMNS;
        int rowStart = row * COLUMNS;
        int rowEnd = Math.min(rowStart + COLUMNS, imageOrder.size());
        
        if (column > 0) {
            currentFocusIndex--;
        } else {
            // At leftmost, wrap to rightmost of current row
            currentFocusIndex = rowEnd - 1;
        }
    }
    
    /**
     * Move focus right within the current row, wrapping to the leftmost position.
     */
    private void moveRight() {
        int column = currentFocusIndex % COLUMNS;
        int row = currentFocusIndex / COLUMNS;
        int rowStart = row * COLUMNS;
        int rowEnd = Math.min(rowStart + COLUMNS, imageOrder.size());
        
        if (currentFocusIndex + 1 < rowEnd) {
            currentFocusIndex++;
        } else {
            // At rightmost, wrap to leftmost of current row
            currentFocusIndex = rowStart;
        }
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
            }
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
     * Update the decision for a specific image thumbnail.
     * 
     * @param image the image to update
     * @param decision the new decision
     */
    public void updateImageDecision(ImageItem image, Phase3Decision decision) {
        ImageThumbnailButton thumbnail = thumbnailMap.get(image);
        if (thumbnail != null) {
            thumbnail.setDecision(decision);
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
}

