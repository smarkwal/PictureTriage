package net.markwalder.picturetriage.ui;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class PhaseLayoutContainer extends VBox {
    public PhaseLayoutContainer(String titleText, Node content, List<Button> actionButtons) {
        this(titleText, content, null, actionButtons, null, null);
    }

    public PhaseLayoutContainer(String titleText, Node content, Button leftButton, List<Button> actionButtons) {
        this(titleText, content, leftButton, actionButtons, null, null);
    }

    public PhaseLayoutContainer(
        String titleText,
        Node content,
        Button leftButton,
        List<Button> actionButtons,
        Button rightButton
    ) {
        this(titleText, content, leftButton, actionButtons, rightButton, null);
    }

    public PhaseLayoutContainer(
        String titleText,
        Node content,
        Button leftButton,
        List<Button> actionButtons,
        Button rightButton,
        Node progressBar
    ) {
        getStyleClass().add("phase-layout-container");
        setFillWidth(true);

        Label titleLabel = new Label(titleText);
        titleLabel.getStyleClass().add("label-title-secondary");

        HBox titleBar = new HBox(titleLabel);
        titleBar.getStyleClass().add("phase-layout-title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);

        VBox contentContainer = new VBox(content);
        contentContainer.getStyleClass().add("phase-layout-content");
        VBox.setVgrow(contentContainer, Priority.ALWAYS);
        // Make the content node fill the container vertically
        VBox.setVgrow(content, Priority.ALWAYS);

        BorderPane actionBar = new BorderPane();
        actionBar.getStyleClass().add("phase-layout-action-bar");

        if (leftButton != null) {
            HBox leftBox = new HBox(leftButton);
            leftBox.setAlignment(Pos.CENTER_LEFT);
            actionBar.setLeft(leftBox);
        }

        HBox centerActions = new HBox(10);
        centerActions.setAlignment(Pos.CENTER);
        if (actionButtons != null && !actionButtons.isEmpty()) {
            centerActions.getChildren().addAll(actionButtons);
        } else {
            Region placeholder = new Region();
            placeholder.setMinWidth(1);
            centerActions.getChildren().add(placeholder);
        }
        actionBar.setCenter(centerActions);

        if (rightButton != null) {
            HBox rightBox = new HBox(rightButton);
            rightBox.setAlignment(Pos.CENTER_RIGHT);
            actionBar.setRight(rightBox);
        }

        // The footer wraps the optional progress bar row above the action buttons
        VBox footer = new VBox();
        footer.getStyleClass().add("phase-layout-footer");

        if (progressBar != null) {
            // Progress bar row sits above the buttons inside the footer
            HBox progressBarRow = new HBox(progressBar);
            progressBarRow.getStyleClass().add("phase-layout-progress-bar-row");
            progressBarRow.setAlignment(Pos.CENTER);
            footer.getChildren().add(progressBarRow);
        }

        footer.getChildren().add(actionBar);

        getChildren().addAll(titleBar, contentContainer, footer);
        VBox.setVgrow(contentContainer, Priority.ALWAYS);
    }
}