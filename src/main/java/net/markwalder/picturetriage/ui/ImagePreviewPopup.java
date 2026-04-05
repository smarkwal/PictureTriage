package net.markwalder.picturetriage.ui;

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;

/**
 * Lightweight popup overlay for showing a full-resolution image preview.
 *
 * Shown via right-click on a thumbnail; hidden by clicking anywhere inside the popup
 * or by right-clicking the same thumbnail again. Centers the preview on the primary screen.
 */
public class ImagePreviewPopup {

    private static final double SCREEN_FRACTION = 0.80;
    private static final double PADDING = 16.0;

    private final Popup popup;
    private final ImageView imageView;

    public ImagePreviewPopup() {
        this.imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Dark background container matching the application color scheme
        StackPane container = new StackPane(imageView);
        container.setStyle(
            "-fx-background-color: #0f111a;" +
            "-fx-border-color: #414760;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;"
        );
        container.setPadding(new Insets(PADDING));

        this.popup = new Popup();
        popup.getContent().add(container);
        popup.setAutoHide(false);

        // Clicking anywhere inside the popup closes it
        container.setOnMouseClicked(e -> popup.hide());
    }

    /**
     * Show the image preview centered on the primary screen.
     *
     * @param image the image to display
     * @param owner the owner window required by the Popup API
     */
    public void show(Image image, Window owner) {
        if (image == null || owner == null) {
            return;
        }

        // Constrain the image to a fraction of the primary screen bounds
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double maxImageWidth = screenBounds.getWidth() * SCREEN_FRACTION - 2 * PADDING;
        double maxImageHeight = screenBounds.getHeight() * SCREEN_FRACTION - 2 * PADDING;

        // Cap fit dimensions to the image's natural size to prevent upscaling
        double natW = image.getWidth();
        double natH = image.getHeight();
        imageView.setFitWidth((natW > 0) ? Math.min(maxImageWidth, natW) : maxImageWidth);
        imageView.setFitHeight((natH > 0) ? Math.min(maxImageHeight, natH) : maxImageHeight);
        imageView.setImage(image);

        // Show off-screen first to force layout and obtain actual popup dimensions
        popup.show(owner, -10000, -10000);

        // Reposition to center on the primary screen
        double x = screenBounds.getMinX() + (screenBounds.getWidth() - popup.getWidth()) / 2;
        double y = screenBounds.getMinY() + (screenBounds.getHeight() - popup.getHeight()) / 2;
        popup.setX(x);
        popup.setY(y);
    }

    /**
     * Hide the image preview popup.
     */
    public void hide() {
        popup.hide();
    }

    /**
     * Returns true if the preview is currently visible.
     */
    public boolean isShowing() {
        return popup.isShowing();
    }
}
