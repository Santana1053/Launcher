package com.skcraft.launcher.swing;

import com.skcraft.launcher.util.FxExecutor;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * JavaFX-backed message log retained under the legacy package name.
 */
public class MessageLog extends VBox {

    private static final Logger ROOT_LOGGER = Logger.getLogger("");
    private static final ExecutorService CONSUMER_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "message-log");
        thread.setDaemon(true);
        return thread;
    });

    private final int maxLines;
    private final TextArea textArea = new TextArea();
    private final List<String> lines = new LinkedList<>();
    private Handler loggerHandler;

    public MessageLog(int maxLines, boolean colorEnabled) {
        this.maxLines = maxLines;

        textArea.setEditable(false);
        textArea.setWrapText(true);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        getChildren().add(textArea);
    }

    public Node getNode() {
        return textArea;
    }

    public String getPastableText() {
        String text = textArea.getText().replaceAll("[\r\n]+", "\n");
        return text.replaceAll("Session ID is [A-Fa-f0-9]+", "Session ID is [redacted]");
    }

    public void clear() {
        lines.clear();
        textArea.clear();
    }

    public void log(String message) {
        FxExecutor.INSTANCE.execute(() -> {
            lines.add(message);
            while (lines.size() > maxLines) {
                lines.remove(0);
            }
            textArea.setText(String.join("", lines));
            textArea.positionCaret(textArea.getText().length());
        });
    }

    public PrintWriter getWriter() {
        return new PrintWriter(new java.io.Writer() {
            private final StringBuilder builder = new StringBuilder();

            @Override
            public void write(char[] cbuf, int off, int len) {
                builder.append(cbuf, off, len);
            }

            @Override
            public void flush() {
                String data = builder.toString();
                builder.setLength(0);
                if (!data.isEmpty()) {
                    log(data);
                }
            }

            @Override
            public void close() {
                flush();
            }
        }, true);
    }

    public void consume(InputStream stream) {
        consume(stream, getWriter());
    }

    public void consume(InputStream stream, PrintWriter writer) {
        CONSUMER_EXECUTOR.execute(() -> {
            try (InputStream in = stream; PrintWriter out = writer) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    String s = new String(buffer, 0, len);
                    System.out.print(s);
                    out.print(s);
                    out.flush();
                }
            } catch (IOException ignored) {
            }
        });
    }

    public void registerLoggerHandler() {
        loggerHandler = new ConsoleLoggerHandler();
        ROOT_LOGGER.addHandler(loggerHandler);
    }

    public void detachGlobalHandler() {
        if (loggerHandler != null) {
            ROOT_LOGGER.removeHandler(loggerHandler);
            loggerHandler = null;
        }
    }

    private class ConsoleLoggerHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            Level level = record.getLevel();
            String formatted = String.format("[%s] %s%n", level.getName(), record.getMessage());
            if (record.getThrown() != null) {
                java.io.StringWriter sw = new java.io.StringWriter();
                record.getThrown().printStackTrace(new java.io.PrintWriter(sw));
                formatted += sw + "\n";
            }
            log(formatted);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
