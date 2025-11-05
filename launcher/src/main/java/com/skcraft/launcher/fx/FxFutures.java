package com.skcraft.launcher.fx;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.skcraft.launcher.LauncherException;
import com.skcraft.launcher.util.FxExecutor;
import com.skcraft.launcher.util.SharedLocale;
import javafx.stage.Window;

import java.util.concurrent.CancellationException;

/**
 * Utility methods for attaching UI callbacks to futures.
 */
public final class FxFutures {

    private FxFutures() {
    }

    public static void addErrorDialogCallback(Window owner, ListenableFuture<?> future) {
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(Object result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof InterruptedException || t instanceof CancellationException) {
                    return;
                }

                Throwable cause = t;
                String message;
                if (t instanceof LauncherException) {
                    message = t.getLocalizedMessage();
                    cause = t.getCause();
                } else {
                    message = t.getLocalizedMessage();
                    if (message == null) {
                        message = SharedLocale.tr("errors.genericError");
                    }
                }

                Throwable finalCause = cause;
                String finalMessage = message;
                FxExecutor.INSTANCE.execute(() -> FxDialogs.showError(owner, finalMessage, SharedLocale.tr("errorTitle"), finalCause));
            }
        }, FxExecutor.INSTANCE);
    }
}
