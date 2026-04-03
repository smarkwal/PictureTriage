package net.markwalder.picturetriage.ui;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.service.ImageCache;

import java.util.function.BiConsumer;

/**
 * A clickable thumbnail button for Phase 3 grid display.
 * 
 * Displays a 200x200px image with a colored border indicating the current decision
 * (green for KEEP, red for DELETE). Clicking toggles the decision and updates the border.
 */
public class ImageThumbnailButton extends StackPane {
    private static final double THUMBNAIL_SIZE = 200.0;
    private static final double BASE_BORDER_WIDTH = 4.0;
    private static final double FOCUSED_BORDER_WIDTH = 6.0;
    // Keep total inset constant to avoid layout jitter when focus changes.
    private static final double TOTAL_INSET = 8.0;

    private final ImageItem imageItem;
    private final ImageCache imageCache;
    private Phase3Decision currentDecision;
    private final ImageView imageView;
    private BiConsumer<ImageItem, Phase3Decision> onDecisionChanged;
    private boolean hasFocusIndicator = false;

    public ImageThumbnailButton(ImageItem imageItem, Phase3Decision initialDecision, ImageCache imageCache) {
        this.imageItem = imageItem;
        this.imageCache = imageCache;
        this.currentDecision = initialDecision;

        // Create image view
        this.imageView = new ImageView();
        double imageSize = THUMBNAIL_SIZE - (2 * TOTAL_INSET);
        imageView.setFitWidth(imageSize);
        imageView.setFitHeight(imageSize);
        imageView.setPreserveRatio(true);

        // Load image
        loadImage();

        // Setup layout
        // Fixed size to prevent jittering when border changes
        setPrefSize(THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        setMinSize(THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        setMaxSize(THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        getChildren().add(imageView);
        getStyleClass().add("thumbnail-button");

        // Update border based on initial decision
        updateBorder();

        // Setup click handler
        setOnMouseClicked(event -> toggleDecision());
    }

    /**
     * Load the image from cache into the ImageView.
     * Falls back to a placeholder if image loading fails.
     */
    private void loadImage() {
        Image image = imageCache.get(imageItem.path());
        if (image != null && !image.isError()) {
            imageView.setImage(image);
        } else {
            setPlaceholder();
        }
    }

    /**
     * Set a placeholder image when loading fails.
     */
    private void setPlaceholder() {
        // Create a simple colored rectangle as placeholder
        imageView.getStyleClass().add("thumbnail-image-placeholder");
    }

    /**
     * Toggle the decision for this image and update the display.
     */
    public void toggleDecision() {
        Phase3Decision newDecision = currentDecision == Phase3Decision.KEEP
                ? Phase3Decision.DELETE
                : Phase3Decision.KEEP;
        currentDecision = newDecision;
        updateBorder();

        // Notify listeners with the new decision
        if (onDecisionChanged != null) {
            onDecisionChanged.accept(imageItem, newDecision);
        }
    }

    /**
     * Update the border color based on the current decision.
     * Also applies a thicker border if the thumbnail has keyboard focus.
     */
    private void updateBorder() {
        Color borderColor = currentDecision == Phase3Decision.KEEP
                ? Color.web("#2e9f44")  // Green (KEEP)
                : Color.web("#bf2f2f");  // Red (DELETE)

        double borderWidth = hasFocusIndicator ? FOCUSED_BORDER_WIDTH : BASE_BORDER_WIDTH;
        double padding = TOTAL_INSET - borderWidth;

        BorderStroke stroke = new BorderStroke(
                borderColor,
                javafx.scene.layout.BorderStrokeStyle.SOLID,
                new CornerRadii(4),
                new BorderWidths(borderWidth)
        );
        setBorder(new Border(stroke));
        setPadding(new Insets(padding));
    }
    
    /**
     * Set the keyboard focus indicator (visual highlight).
        * When true, border becomes thicker while padding is reduced to keep outer size stable.
     * 
     * @param focused true to show focus, false to hide
     */
    public void setFocusIndicator(boolean focused) {
        this.hasFocusIndicator = focused;
        updateBorder();
    }

    /**
     * Get the associated image item.
     */
    public ImageItem getImageItem() {
        return imageItem;
    }

    /**
     * Get the current decision for this image.
     */
    public Phase3Decision getCurrentDecision() {
        return currentDecision;
    }

    /**
     * Set the current decision externally (for synchronization from service).
     */
    public void setDecision(Phase3Decision decision) {
        this.currentDecision = decision;
        updateBorder();
    }

    /**
     * Set a callback to be invoked when the decision changes.
     * The callback receives the image and the new decision.
     */
    public void setOnDecisionChanged(BiConsumer<ImageItem, Phase3Decision> callback) {
        this.onDecisionChanged = callback;
    }
}

