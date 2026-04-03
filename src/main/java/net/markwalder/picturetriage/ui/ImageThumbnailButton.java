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

import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * A clickable thumbnail button for Phase 3 grid display.
 * 
 * Displays a 200x200px image with a colored border indicating the current decision
 * (green for KEEP, red for DELETE). Clicking toggles the decision and updates the border.
 */
public class ImageThumbnailButton extends StackPane {
    private static final double THUMBNAIL_SIZE = 200.0;
    private static final double BORDER_WIDTH = 4.0;

    private final ImageItem imageItem;
    private Phase3Decision currentDecision;
    private final ImageView imageView;
    private BiConsumer<ImageItem, Phase3Decision> onDecisionChanged;

    public ImageThumbnailButton(ImageItem imageItem, Phase3Decision initialDecision) {
        this.imageItem = imageItem;
        this.currentDecision = initialDecision;

        // Create image view
        this.imageView = new ImageView();
        imageView.setFitWidth(THUMBNAIL_SIZE);
        imageView.setFitHeight(THUMBNAIL_SIZE);
        imageView.setPreserveRatio(true);

        // Load image
        loadImage();

        // Setup layout
        setPrefSize(THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        setPadding(new Insets(4));
        getChildren().add(imageView);

        // Update border based on initial decision
        updateBorder();

        // Setup click handler
        setOnMouseClicked(event -> toggleDecision());

        // Make cursor change to hand on hover
        setStyle("-fx-cursor: hand;");
    }

    /**
     * Load the image from disk into the ImageView.
     * Falls back to a placeholder if image loading fails.
     */
    private void loadImage() {
        try {
            String filePath = imageItem.path().toString();

            // Check if WebP format
            if (filePath.toLowerCase().endsWith(".webp")) {
                // Use ImageIO with TwelveMonkeys plugin
                BufferedImage bufferedImage = ImageIO.read(imageItem.path().toFile());
                if (bufferedImage != null) {
                    Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    imageView.setImage(fxImage);
                    return;
                }
            }

            // For other formats, use JavaFX ImageIO
            Image image = new Image(imageItem.path().toUri().toString(), true);
            image.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (image.isError()) {
                    setPlaceholder();
                }
            });
            imageView.setImage(image);
        } catch (IOException e) {
            setPlaceholder();
        }
    }

    /**
     * Set a placeholder image when loading fails.
     */
    private void setPlaceholder() {
        // Create a simple colored rectangle as placeholder
        imageView.setStyle("-fx-fill: #cccccc;");
    }

    /**
     * Toggle the decision for this image and update the display.
     */
    private void toggleDecision() {
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
     */
    private void updateBorder() {
        Color borderColor = currentDecision == Phase3Decision.KEEP
                ? Color.web("#00aa00")  // Green
                : Color.web("#cc0000");  // Red

        BorderStroke stroke = new BorderStroke(
                borderColor,
                javafx.scene.layout.BorderStrokeStyle.SOLID,
                new CornerRadii(4),
                new BorderWidths(BORDER_WIDTH)
        );
        setBorder(new Border(stroke));
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

