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

public class ImageDisplayPane extends VBox {
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

    private final Path displayRoot;
    private ImageItem imageItem;

    public ImageDisplayPane(double cardWidth, double cardHeight, ImageCache imageCache, Path displayRoot) {
        this.imageCache = imageCache;
        this.displayRoot = displayRoot;

        double imageAreaHeight = Math.max(0, cardHeight - FOOTER_HEIGHT);
        double imageFitWidth = Math.max(0, cardWidth - (IMAGE_AREA_PADDING * 2) - (BORDER_WIDTH * 2));
        double imageFitHeight = Math.max(0, imageAreaHeight - (IMAGE_AREA_PADDING * 2) - (BORDER_WIDTH * 2));

        imageView.setPreserveRatio(true);
        imageView.setFitWidth(imageFitWidth);
        imageView.setFitHeight(imageFitHeight);
        imageView.setSmooth(true);

        placeholderLabel.getStyleClass().addAll("label-body", "image-display-placeholder");
        placeholderLabel.setVisible(false);
        placeholderLabel.setManaged(false);

        StackPane imageArea = new StackPane(imageView, placeholderLabel);
        imageArea.getStyleClass().add("image-display-image-area");
        imageArea.setPadding(new Insets(IMAGE_AREA_PADDING));
        imageArea.setAlignment(Pos.CENTER);
        imageArea.setPrefHeight(imageAreaHeight);
        imageArea.setMinHeight(Region.USE_PREF_SIZE);
        imageArea.setMaxHeight(Region.USE_PREF_SIZE);

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

        VBox footer = new VBox(4, firstRow, secondRow);
        footer.getStyleClass().add("image-display-footer");
        footer.setPrefHeight(FOOTER_HEIGHT);
        footer.setMinHeight(FOOTER_HEIGHT);
        footer.setMaxHeight(FOOTER_HEIGHT);

        getChildren().addAll(imageArea, footer);
        getStyleClass().add("image-display-pane");
        setFillWidth(true);
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(cardWidth);
        setMinWidth(Region.USE_PREF_SIZE);
        setMaxWidth(Region.USE_PREF_SIZE);
        setPrefHeight(cardHeight);
        setMinHeight(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
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
