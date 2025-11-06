package com.skcraft.launcher.fx;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

/**
 * Utility helpers for JavaFX dialogs.
 */
public final class FxDialogs {

    private FxDialogs() {
    }

    public static void showError(Window owner, String message, String title) {
        showError(owner, message, title, null);
    }

    public static void showError(Window owner, String message, String title, Throwable throwable) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR, message, ButtonType.OK);
            if (owner != null) {
                Stage stage = asStage(owner);
                if (stage != null) {
                    alert.initOwner(stage);
                }
            }
            alert.setTitle(title);
            alert.setHeaderText(null);

            if (throwable != null) {
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
                alert.getDialogPane().setExpanded(false);
            }

            alert.showAndWait();
        });
    }

    public static boolean confirm(Window owner, String message, String title) {
        if (Platform.isFxApplicationThread()) {
            return showConfirm(owner, message, title);
        }

        final boolean[] result = new boolean[1];
        final Object monitor = new Object();
        Platform.runLater(() -> {
            result[0] = showConfirm(owner, message, title);
            synchronized (monitor) {
                monitor.notifyAll();
            }
        });
        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return result[0];
    }

    private static boolean showConfirm(Window owner, String message, String title) {
        Alert alert = new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        if (owner != null) {
            Stage stage = asStage(owner);
            if (stage != null) {
                alert.initOwner(stage);
            }
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> response = alert.showAndWait();
        return response.isPresent() && response.get() == ButtonType.YES;
    }

    public static void openURL(URL url, HostServices hostServices) {
        if (hostServices != null) {
            hostServices.showDocument(url.toExternalForm());
            return;
        }

        try {
            openURI(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL", e);
        }
    }

    public static void openURI(URI uri) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", uri.toString()});
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to open browser", e);
        }
    }

    private static Stage asStage(Window window) {
        if (window instanceof Stage) {
            return (Stage) window;
        }

        if (window == null) {
            return null;
        }

        Scene scene = window.getScene();
        if (scene != null && scene.getWindow() instanceof Stage) {
            return (Stage) scene.getWindow();
        }

        return null;
    }
}
