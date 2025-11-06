package com.skcraft.launcher.fx;

import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.dialog.LauncherFrame;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import lombok.extern.java.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * JavaFX bootstrap for the launcher.
 */
@Log
public class LauncherFxApplication extends Application {

    private Launcher launcher;
    private Throwable startupError;
    private LauncherFrame currentFrame;

    public static void launch(String[] args) {
        Application.launch(LauncherFxApplication.class, args);
    }

    @Override
    public void init() {
        Launcher.setupLogger();
        String[] args = getParameters().getRaw().toArray(new String[0]);
        try {
            launcher = Launcher.createFromArguments(args);
        } catch (Throwable t) {
            startupError = t;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        if (startupError != null) {
            showStartupFailure(startupError);
            return;
        }

        currentFrame = new LauncherFrame(launcher, primaryStage);
        currentFrame.getStage().setOnHidden(event -> {
            if (currentFrame != null && event.getSource() == currentFrame.getStage()) {
                currentFrame = null;
            }
        });

        launcher.setMainWindowSupplier(() -> {
            if (currentFrame == null) {
                currentFrame = new LauncherFrame(launcher);
                currentFrame.getStage().setOnHidden(event -> {
                    if (currentFrame != null && event.getSource() == currentFrame.getStage()) {
                        currentFrame = null;
                    }
                });
            }
            return currentFrame.getStage();
        });

        currentFrame.show();
    }

    @Override
    public void stop() {
        if (launcher != null) {
            launcher.getExecutor().shutdown();
        }
    }

    private void showStartupFailure(Throwable throwable) {
        log.warning("Launcher failed to initialize" + (throwable.getMessage() != null ? ": " + throwable.getMessage() : ""));

        Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
        alert.setTitle("Launcher error");
        alert.setHeaderText("Uh oh! The updater couldn't be opened because a problem was encountered.");

        String content = Optional.ofNullable(throwable.getMessage())
                .orElse("Unexpected error occurred during startup.");
        alert.setContentText(content);

        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String exceptionText = sw.toString();

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expandableContent = new GridPane();
        expandableContent.setMaxWidth(Double.MAX_VALUE);
        expandableContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expandableContent);
        alert.getDialogPane().setExpanded(true);

        alert.showAndWait();
        Platform.exit();
    }
}
