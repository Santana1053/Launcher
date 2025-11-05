package com.skcraft.launcher.dialog;

import com.skcraft.launcher.util.FxExecutor;
import com.skcraft.launcher.util.PastebinPoster;
import com.skcraft.launcher.util.SharedLocale;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;

import static com.skcraft.launcher.util.SharedLocale.tr;

/**
 * JavaFX console window.
 */
public class ConsoleFrame {

    private static ConsoleFrame globalFrame;

    @Getter
    private final MessageLogWrapper messageLog;

    protected final Stage stage;

    private boolean registeredGlobalLog = false;

    public ConsoleFrame(int numLines, boolean colorEnabled) {
        this(SharedLocale.tr("console.title"), numLines, colorEnabled, null);
    }

    public ConsoleFrame(String title, int numLines, boolean colorEnabled) {
        this(title, numLines, colorEnabled, null);
    }

    public ConsoleFrame(String title, int numLines, boolean colorEnabled, Window owner) {
        this.stage = new Stage();
        this.messageLog = new MessageLogWrapper(numLines, colorEnabled);

        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        }

        stage.setTitle(title);
        stage.setScene(createScene());
        stage.setOnCloseRequest(event -> {
            event.consume();
            performClose();
        });
    }

    protected Scene createScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        HBox top = createTopBar();
        root.setTop(top);

        root.setCenter(messageLog.getNode());
        BorderPane.setMargin(messageLog.getNode(), new Insets(8, 0, 0, 0));

        return new Scene(root, 650, 400);
    }

    protected HBox createTopBar() {
        Button uploadButton = new Button(SharedLocale.tr("console.uploadLog"));
        Button clearButton = new Button(SharedLocale.tr("console.clearLog"));

        uploadButton.setOnAction(e -> pastebinLog());
        clearButton.setOnAction(e -> messageLog.clear());

        return new HBox(8, uploadButton, clearButton);
    }

    protected void performClose() {
        messageLog.detachGlobalHandler();
        messageLog.clear();
        registeredGlobalLog = false;
        stage.close();
    }

    private void registerLoggerHandler() {
        if (!registeredGlobalLog) {
            messageLog.registerLoggerHandler();
            registeredGlobalLog = true;
        }
    }

    private void pastebinLog() {
        String text = messageLog.getPastableText();
        messageLog.logHighlighted(tr("console.pasteUploading", text.length()) + "\n");

        PastebinPoster.paste(text, new PastebinPoster.PasteCallback() {
            @Override
            public void handleSuccess(String url) {
                messageLog.logHighlighted(tr("console.pasteUploaded", url) + "\n");
                FxExecutor.INSTANCE.execute(() -> {
                    try {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                    } catch (Exception ignored) {
                    }
                });
            }

            @Override
            public void handleError(String err) {
                messageLog.logError(tr("console.pasteFailed", err) + "\n");
            }
        });
    }

    public void show() {
        registerLoggerHandler();
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    public void hide() {
        stage.hide();
    }

    public static void showMessages() {
        FxExecutor.INSTANCE.execute(() -> {
            if (globalFrame == null) {
                globalFrame = new ConsoleFrame(10000, false);
                globalFrame.stage.setTitle(SharedLocale.tr("console.launcherConsoleTitle"));
            }
            globalFrame.show();
        });
    }

    public static void hideMessages() {
        FxExecutor.INSTANCE.execute(() -> {
            if (globalFrame != null) {
                globalFrame.hide();
            }
        });
    }

    public MessageLogWrapper getMessageLog() {
        return messageLog;
    }

    /**
     * Wrapper around MessageLog to preserve API expectations.
     */
    public static class MessageLogWrapper extends com.skcraft.launcher.swing.MessageLog {
        public MessageLogWrapper(int maxLines, boolean colorEnabled) {
            super(maxLines, colorEnabled);
        }

        public javafx.scene.Node getNode() {
            return super.getNode();
        }

        public void log(String message, Object ignored) {
            log(message);
        }

        public Object asHighlighted() {
            return new Object();
        }

        public Object asError() {
            return new Object();
        }

        public Object asDebug() {
            return new Object();
        }

        @Override
        public void logHighlighted(String message) {
            super.logHighlighted(message);
        }

        @Override
        public void logError(String message) {
            super.logError(message);
        }
    }
}
