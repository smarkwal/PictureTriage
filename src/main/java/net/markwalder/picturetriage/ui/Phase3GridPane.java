package net.markwalder.picturetriage.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import net.markwalder.picturetriage.domain.ImageItem;
import net.markwalder.picturetriage.domain.Phase3Decision;
import net.markwalder.picturetriage.domain.Phase3GridState;

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
    private BiConsumer<ImageItem, Phase3Decision> onImageDecisionChanged;

    public Phase3GridPane() {
        // Setup layout
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 1;");

        // Create grid pane
        this.gridPane = new GridPane();
        gridPane.setHgap(GAP);
        gridPane.setVgap(GAP);
        gridPane.setPadding(new Insets(10));

        this.thumbnailMap = new HashMap<>();

        // Add scroll pane to handle overflow
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        getChildren().add(scrollPane);
        setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Populate the grid with thumbnails from the given grid state.
     * 
     * @param state the Phase3GridState containing images and decisions
     */
    public void populate(Phase3GridState state) {
        gridPane.getChildren().clear();
        thumbnailMap.clear();

        List<ImageItem> images = state.imageDisplayOrder();
        int row = 0;
        int col = 0;

        for (ImageItem image : images) {
            Phase3Decision decision = state.getDecision(image);
            ImageThumbnailButton thumbnail = new ImageThumbnailButton(image, decision);

            // Register the callback for when this thumbnail is clicked
            thumbnail.setOnDecisionChanged((img, newDecision) -> {
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

