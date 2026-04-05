package net.markwalder.picturetriage.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.service.ImageCache;

public class ImageDisplayPane extends Region {
    private static final double IMAGE_AREA_PADDING = 20.0;
    private static final double FOOTER_HEIGHT = 74.0;
    private static final double BORDER_WIDTH = 2.0;

    private final ImageCache imageCache;
    private final Map<Path, ImageMetadata> metadataCache = new HashMap<>();
    private final ImageView imageView = new ImageView();
    private final Label placeholderLabel = new Label("Image unavailable");
    private final Label pathLabel = new Label();
    private final Label dimensionsLabel = new Label();
    private final Label fileSizeLabel = new Label();

    // Fields so layoutChildren() can position them directly
    private final Region card = new Region();
    private final StackPane imageArea;
    private final VBox footer;

    private final Path displayRoot;
    private ImageItem imageItem;
    // Controls horizontal placement of the card within the allocated pane bounds
    private HPos cardAlignment = HPos.CENTER;

    public ImageDisplayPane(ImageCache imageCache, Path displayRoot) {
        this.imageCache = imageCache;
        this.displayRoot = displayRoot;

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        placeholderLabel.getStyleClass().addAll("label-body", "image-display-placeholder");
        placeholderLabel.setVisible(false);
        placeholderLabel.setManaged(false);

        // The image area size is computed in layoutChildren() to always be square
        imageArea = new StackPane(imageView, placeholderLabel);
        imageArea.getStyleClass().add("image-display-image-area");
        imageArea.setPadding(new Insets(IMAGE_AREA_PADDING));
        imageArea.setAlignment(Pos.CENTER);

        configureLabel(pathLabel, "image-display-path");
        configureLabel(dimensionsLabel, "image-display-meta");
        configureLabel(fileSizeLabel, "image-display-meta");
        pathLabel.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        pathLabel.setWrapText(false);
        HBox.setHgrow(pathLabel, Priority.ALWAYS);
        dimensionsLabel.setAlignment(Pos.CENTER_RIGHT);
        fileSizeLabel.setAlignment(Pos.CENTER_RIGHT);
        fileSizeLabel.setMaxWidth(Double.MAX_VALUE);

        HBox firstRow = new HBox(12, pathLabel, dimensionsLabel);
        firstRow.setAlignment(Pos.CENTER_LEFT);
        HBox secondRow = new HBox(fileSizeLabel);
        secondRow.setAlignment(Pos.CENTER_RIGHT);

        footer = new VBox(4, firstRow, secondRow);
        footer.getStyleClass().add("image-display-footer");

        // card is the visual background/border layer; it is sized in layoutChildren() to the computed square
        card.getStyleClass().add("image-display-pane");

        getChildren().addAll(card, imageArea, footer);

        // Allow this pane to grow to fill the space allocated by its parent
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
    }

    @Override
    protected void layoutChildren() {
        double w = snapSizeX(getWidth());
        double h = snapSizeY(getHeight());
        double footerH = snapSizeY(FOOTER_HEIGHT);

        // 1. maxImageAreaHeight = maxPaneHeight - footerHeight
        double maxImageAreaHeight = h - footerH;
        // 2. maxImageAreaWidth = maxPaneWidth
        double maxImageAreaWidth = w;
        // 3. imageAreaWidth = imageAreaHeight = min(maxImageAreaWidth, maxImageAreaHeight)
        double side = snapSizeX(Math.max(0, Math.min(maxImageAreaWidth, maxImageAreaHeight)));

        // Compute x based on the requested card alignment
        double x = switch (cardAlignment) {
            case LEFT -> 0;
            case RIGHT -> snapPositionX(w - side);
            default -> snapPositionX((w - side) / 2.0);
        };

        // Visual card background/border is sized to the card area only (not the full pane bounds)
        card.resizeRelocate(x, 0, side, side + footerH);
        // Image area occupies the square portion of the card
        imageArea.resizeRelocate(x, 0, side, side);
        // Footer sits directly below the image area
        footer.resizeRelocate(x, side, side, footerH);

        // ImageView fit size stays square, centered within the image area padding
        double fitSize = Math.max(0, side - (IMAGE_AREA_PADDING * 2) - (BORDER_WIDTH * 2));
        imageView.setFitWidth(fitSize);
        imageView.setFitHeight(fitSize);
    }

    @Override
    protected double computePrefWidth(double height) {
        // Return 0 so that parents (e.g. HBox) use growth constraints alone to distribute space,
        // preventing image pixel dimensions from influencing the layout baseline.
        return 0;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 100;
    }

    @Override
    protected double computeMinHeight(double width) {
        return FOOTER_HEIGHT + 50;
    }

    public void setCardAlignment(HPos alignment) {
        this.cardAlignment = alignment;
        requestLayout();
    }

    public void setImageItem(ImageItem imageItem) {
        this.imageItem = imageItem;
        refreshImage();
        refreshMetadata();
    }

    private void configureLabel(Label label, String styleClass) {
        label.getStyleClass().addAll("label-body", styleClass);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setMaxWidth(Double.MAX_VALUE);
    }

    private void refreshImage() {
        if (imageItem == null) {
            imageView.setImage(null);
            placeholderLabel.setVisible(false);
            placeholderLabel.setManaged(false);
            return;
        }

        Image image = imageCache.get(imageItem.path());
        boolean hasImage = image != null && !image.isError();
        imageView.setImage(hasImage ? image : null);
        placeholderLabel.setVisible(!hasImage);
        placeholderLabel.setManaged(!hasImage);
    }

    private void refreshMetadata() {
        if (imageItem == null) {
            pathLabel.setText("");
            dimensionsLabel.setText("");
            fileSizeLabel.setText("");
            return;
        }

        Image loadedImage = imageView.getImage();
        ImageMetadata metadata = metadataCache.computeIfAbsent(
            imageItem.path().toAbsolutePath().normalize(),
            path -> loadImageMetadata(path, loadedImage)
        );

        pathLabel.setText(displayPath(imageItem));
        dimensionsLabel.setText(metadata.width() + " x " + metadata.height());
        fileSizeLabel.setText(formatFileSize(metadata.fileSizeBytes()));
    }

    private String displayPath(ImageItem item) {
        if (displayRoot == null) {
            return item.path().toString();
        }

        Path normalizedRoot = displayRoot.toAbsolutePath().normalize();
        Path normalizedItem = item.path().toAbsolutePath().normalize();
        if (normalizedItem.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalizedItem).toString();
        }

        return item.path().toString();
    }

    private ImageMetadata loadImageMetadata(Path path, Image loadedImage) {
        long fileSizeBytes = 0L;
        try {
            fileSizeBytes = Files.size(path);
        } catch (IOException ignored) {
        }

        int width = (int) Math.round(loadedImage != null ? loadedImage.getWidth() : 0);
        int height = (int) Math.round(loadedImage != null ? loadedImage.getHeight() : 0);
        if (width > 0 && height > 0) {
            return new ImageMetadata(width, height, fileSizeBytes);
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(path.toFile());
            if (bufferedImage != null) {
                return new ImageMetadata(bufferedImage.getWidth(), bufferedImage.getHeight(), fileSizeBytes);
            }
        } catch (IOException ignored) {
        }

        return new ImageMetadata(0, 0, fileSizeBytes);
    }

    private String formatFileSize(long fileSizeBytes) {
        String[] units = {"B", "KB", "MB", "GB"};
        double value = Math.max(fileSizeBytes, 0L);
        int unitIndex = 0;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }

        DecimalFormat format = new DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(value) + " " + units[unitIndex];
    }

    private record ImageMetadata(int width, int height, long fileSizeBytes) {
    }
}
