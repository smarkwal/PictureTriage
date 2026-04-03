package net.markwalder.picturetriage.ui;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class PhaseLayoutContainer extends VBox {
    public PhaseLayoutContainer(String titleText, Node content, List<Button> actionButtons) {
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

        HBox actionBar = new HBox(10);
        actionBar.getStyleClass().add("phase-layout-action-bar");
        actionBar.setAlignment(Pos.CENTER);
        if (actionButtons != null && !actionButtons.isEmpty()) {
            actionBar.getChildren().addAll(actionButtons);
        } else {
            Region placeholder = new Region();
            placeholder.setMinWidth(1);
            actionBar.getChildren().add(placeholder);
        }

        getChildren().addAll(titleBar, contentContainer, actionBar);
        VBox.setVgrow(contentContainer, Priority.ALWAYS);
    }
}