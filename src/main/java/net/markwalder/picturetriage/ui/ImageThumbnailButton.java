package net.markwalder.picturetriage.ui;

import java.util.function.BiConsumer;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.service.ImageCache;

/**
 * A clickable thumbnail button for Phase 3 grid display.
 *
 * Left click toggles the keep/delete decision and updates the border.
 * Right click toggles the full-image preview popup open/closed.
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
    private ImagePreviewPopup previewPopup;

    public ImageThumbnailButton(ImageItem imageItem, Phase3Decision initialDecision, ImageCache imageCache) {
        this.imageItem = imageItem;
        this.imageCache = imageCache;
        this.currentDecision = initialDecision;

        // Create image view
        this.imageView = new ImageView();
        imageView.setPreserveRatio(true);

        // Load image (also sets fit dimensions to prevent upscaling)
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

        // Left click: toggle the keep/delete decision
        // Right click: toggle the preview popup open/closed
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                toggleDecision();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                togglePreview();
            }
        });
    }

    /**
     * Load the image from cache into the ImageView.
     * Caps fit dimensions to the image's natural size to prevent upscaling.
     * Falls back to a placeholder if image loading fails.
     */
    private void loadImage() {
        Image image = imageCache.get(imageItem.path());
        if (image != null && !image.isError()) {
            imageView.setImage(image);
            applyFitSize(image);
        } else {
            setPlaceholder();
        }
    }

    /**
     * Set the fit dimensions to the minimum of the cell size and the image's natural size,
     * ensuring the image is never upscaled. For background-loaded images the dimensions are
     * not yet known; a one-shot listener updates the fit once they become available.
     */
    private void applyFitSize(Image image) {
        double cap = THUMBNAIL_SIZE - (2 * TOTAL_INSET);
        if (image.getWidth() > 0) {
            // Dimensions already known (synchronously loaded image)
            imageView.setFitWidth(Math.min(cap, image.getWidth()));
            imageView.setFitHeight(Math.min(cap, image.getHeight()));
        } else {
            // Background-loading image: apply max constraint now and tighten once dimensions arrive
            imageView.setFitWidth(cap);
            imageView.setFitHeight(cap);
            image.widthProperty().addListener((obs, oldW, newW) -> {
                if (newW.doubleValue() > 0) {
                    imageView.setFitWidth(Math.min(cap, newW.doubleValue()));
                    imageView.setFitHeight(Math.min(cap, image.getHeight()));
                }
            });
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
     * Set a callback to be invoked when the decision changes.
     * The callback receives the image and the new decision.
     */
    public void setOnDecisionChanged(BiConsumer<ImageItem, Phase3Decision> callback) {
        this.onDecisionChanged = callback;
    }

    /**
     * Set the preview popup used to show the full image on left click.
     * The popup is shared across all thumbnails in the grid.
     */
    public void setPreviewPopup(ImagePreviewPopup previewPopup) {
        this.previewPopup = previewPopup;
    }

    /**
     * Toggle the preview popup: show it if hidden, hide it if already showing.
     * Does nothing if no preview popup is configured or the image cannot be loaded.
     */
    private void togglePreview() {
        if (previewPopup == null || getScene() == null) {
            return;
        }
        if (previewPopup.isShowing()) {
            previewPopup.hide();
            return;
        }
        Image image = imageCache.get(imageItem.path());
        if (image != null && !image.isError()) {
            // Obtain the owner window from the current scene at the moment the timer fires
            Window owner = getScene().getWindow();
            previewPopup.show(image, owner);
        }
    }
}

