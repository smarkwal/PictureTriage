package net.markwalder.picturetriage.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
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
 * Displays images in a responsive grid whose column count is computed from the
 * available viewport width at layout time. Keyboard navigation adapts automatically.
 */
public class Phase3GridPane extends VBox {
    private static final double THUMBNAIL_SIZE = 200.0;
    private static final double GAP = 10.0;

    private final GridPane gridPane;
    private final ScrollPane scrollPane;
    private final Map<ImageItem, ImageThumbnailButton> thumbnailMap;
    private final ImageCache imageCache;
    private BiConsumer<ImageItem, Phase3Decision> onImageDecisionChanged;
    
    // Keyboard navigation state
    private final List<ImageItem> imageOrder = new ArrayList<>();
    private final Map<ImageItem, Integer> imageIndexMap = new HashMap<>();
    private int currentFocusIndex = 0;
    private int currentColumns = 1;

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

        // Recompute columns whenever the viewport width changes
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            int newColumns = computeColumns(newBounds.getWidth());
            if (newColumns != currentColumns) {
                currentColumns = newColumns;
                relayout();
            }
        });

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

        // Use current viewport width to determine initial column count if available
        double vpWidth = scrollPane.getViewportBounds().getWidth();
        if (vpWidth > 0) {
            currentColumns = computeColumns(vpWidth);
        }

        List<ImageItem> images = state.imageDisplayOrder();
        imageOrder.addAll(images);
        for (int i = 0; i < images.size(); i++) {
            imageIndexMap.put(images.get(i), i);
        }

        for (int i = 0; i < images.size(); i++) {
            ImageItem image = images.get(i);
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

            // Add thumbnail to map and grid
            thumbnailMap.put(image, thumbnail);
            gridPane.add(thumbnail, i % currentColumns, i / currentColumns);
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

        switch (event.getCode()) {
            case UP -> {
                // Move up one row (same column)
                moveUp();
                focusCurrentThumbnail();
                handled = true;
            }
            case DOWN -> {
                // Move down one row (same column)
                moveDown();
                focusCurrentThumbnail();
                handled = true;
            }
            case LEFT -> {
                // Move left within row
                moveLeft();
                focusCurrentThumbnail();
                handled = true;
            }
            case RIGHT -> {
                // Move right within row
                moveRight();
                focusCurrentThumbnail();
                handled = true;
            }
            case SPACE -> {
                // Toggle decision on current thumbnail
                ImageItem currentImage = imageOrder.get(currentFocusIndex);
                ImageThumbnailButton button = thumbnailMap.get(currentImage);
                if (button != null) {
                    button.toggleDecision();
                    handled = true;
                }
            }
            default -> { }
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
        int column = currentFocusIndex % currentColumns;
        int newIndex = currentFocusIndex - currentColumns;

        if (newIndex >= 0) {
            currentFocusIndex = newIndex;
        } else {
            // Wrap to same column in the last row
            int lastRowStart = (imageOrder.size() - 1) / currentColumns * currentColumns;
            currentFocusIndex = Math.min(lastRowStart + column, imageOrder.size() - 1);
        }
    }

    /**
     * Move focus down one row (same column position), wrapping to the top row.
     */
    private void moveDown() {
        int column = currentFocusIndex % currentColumns;
        int newIndex = currentFocusIndex + currentColumns;

        if (newIndex < imageOrder.size()) {
            currentFocusIndex = newIndex;
        } else {
            // Wrap to same column in the first row
            currentFocusIndex = column;
        }
    }

    /**
     * Compute the number of columns that fit in the given viewport width.
     */
    private int computeColumns(double viewportWidth) {
        // Return 1 column minimum when width is not yet known
        if (viewportWidth <= 0) {
            return 1;
        }
        // Account for grid padding on both sides
        double usable = viewportWidth - 2 * GAP;
        int cols = (int) Math.max(1, (usable + GAP) / (THUMBNAIL_SIZE + GAP));
        return cols;
    }

    /**
     * Re-assign each thumbnail to the correct grid cell after a column-count change.
     */
    private void relayout() {
        for (int i = 0; i < imageOrder.size(); i++) {
            ImageThumbnailButton btn = thumbnailMap.get(imageOrder.get(i));
            if (btn != null) {
                GridPane.setColumnIndex(btn, i % currentColumns);
                GridPane.setRowIndex(btn, i / currentColumns);
            }
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

