package net.markwalder.picturetriage.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import net.markwalder.picturetriage.util.StringUtils;

/**
 * Confirmation dialog for Phase 3 deletion.
 * 
 * Shows a simple alert asking the user to confirm deletion of marked images.
 * Returns a boolean indicating whether the user confirmed the deletion.
 */
public class DeleteConfirmationDialog {
    /**
     * Show a confirmation dialog for deleting the specified number of images.
     * 
     * @param deleteCount the number of images to be deleted
     * @param owner the owner window (for modality)
     * @return true if user confirmed, false if cancelled
     */
    public static boolean showConfirmation(int deleteCount, Stage owner) {
        String imageLabel = StringUtils.pluralize(deleteCount, "image", "images");

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete " + deleteCount + " " + imageLabel + "?");
        alert.setContentText(
            "You are about to delete " + deleteCount + " " + imageLabel + ".\n\n" +
            "Are you sure?"
        );
        alert.initOwner(owner);

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        return result == ButtonType.OK;
    }

    /**
     * Show an information dialog with deletion results.
     * 
     * @param title the dialog title
     * @param message the message to display (can include newlines)
     * @param owner the owner window (for modality)
     */
    public static void showResult(String title, String message, Stage owner) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);

        // Make dialog wider to accommodate longer messages
        alert.getDialogPane().setPrefWidth(500);

        alert.showAndWait();
    }

    /**
     * Show an error dialog.
     * 
     * @param title the dialog title
     * @param message the error message to display
     * @param owner the owner window (for modality)
     */
    public static void showError(String title, String message, Stage owner) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);

        // Make dialog wider to accommodate longer messages
        alert.getDialogPane().setPrefWidth(500);

        alert.showAndWait();
    }
}
