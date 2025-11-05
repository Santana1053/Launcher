package com.skcraft.launcher.util;

import com.google.common.util.concurrent.AbstractListeningExecutorService;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes runnables on the JavaFX application thread.
 */
public final class FxExecutor extends AbstractListeningExecutorService {

    public static final FxExecutor INSTANCE = new FxExecutor();

    private FxExecutor() {
    }

    @Override
    public void execute(Runnable command) {
        if (Platform.isFxApplicationThread()) {
            command.run();
        } else {
            Platform.runLater(command);
        }
    }

    @Override
    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
        return new ArrayList<>();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }
}
