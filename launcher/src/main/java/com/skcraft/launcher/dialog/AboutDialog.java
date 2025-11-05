package com.skcraft.launcher.dialog;

import com.skcraft.launcher.fx.FxDialogs;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class AboutDialog {

    private AboutDialog() {
    }

    public static void showAboutDialog(Window parent) {
        Stage stage = new Stage();
        if (parent != null) {
            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setTitle("About");
        stage.setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        root.getChildren().addAll(
                new Text("Licensed under GNU General Public License, version 3."),
                new Text("You are using SKCraft Launcher, an open-source customizable launcher platform that anyone can use."),
                new Text("SKCraft does not necessarily endorse the version of the launcher that you are using.")
        );

        Button okButton = new Button("OK");
        Button websiteButton = new Button("Website");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(10, spacer, websiteButton, okButton);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(buttonBar);

        okButton.setDefaultButton(true);
        okButton.setOnAction(e -> stage.close());
        websiteButton.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://github.com/SKCraft/Launcher"));
            } catch (Exception ex) {
                FxDialogs.showError(stage, ex.getMessage(), "Error", ex);
            }
        });

        stage.setScene(new Scene(root));
        stage.showAndWait();
    }
}

