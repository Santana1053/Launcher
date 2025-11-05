package com.skcraft.launcher.dialog;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.launcher.fx.FxDialogs;
import com.skcraft.launcher.util.FxExecutor;
import com.skcraft.launcher.util.SharedLocale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.extern.java.Log;

import java.lang.ref.WeakReference;

import static com.skcraft.launcher.util.SharedLocale.tr;

@Log
public class ProgressDialog {

    private static WeakReference<ProgressDialog> lastDialogRef;

    private final Stage stage;
    private final ProgressBar progressBar = new ProgressBar();
    private final Label messageLabel = new Label();
    private final TextArea logArea = new TextArea();
    private final Button detailsButton = new Button();
    private final Button logButton = new Button(SharedLocale.tr("progress.viewLog"));
    private final Button cancelButton = new Button(SharedLocale.tr("button.cancel"));
    private final VBox detailsBox = new VBox();

    private final String defaultTitle;
    private final String defaultMessage;

    private final Timeline updater;

    private final ProgressObservable observable;
    private final Runnable cancelAction;

    private ProgressDialog(Window owner, ProgressObservable observable, String title, String message, Runnable cancelAction) {
        this.observable = observable;
        this.cancelAction = cancelAction;
        this.stage = new Stage();
        this.defaultTitle = title;
        this.defaultMessage = message;

        stage.initModality(owner != null ? Modality.WINDOW_MODAL : Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setResizable(true);
        stage.setTitle(title);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        progressBar.setPrefWidth(320);
        progressBar.setMinHeight(16);
        progressBar.setProgress(-1);

        VBox content = new VBox(10, messageLabel, progressBar);
        messageLabel.setWrapText(true);
        messageLabel.setText(message);
        root.setTop(content);

        logArea.setWrapText(true);
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);

        detailsBox.getChildren().add(logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        detailsBox.setVisible(false);
        detailsBox.setManaged(false);
        root.setCenter(detailsBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        detailsButton.setText(SharedLocale.tr("progress.details"));
        logButton.setVisible(false);

        HBox buttons = new HBox(10, detailsButton, logButton, spacer, cancelButton);
        buttons.setPadding(new Insets(12, 0, 0, 0));
        root.setBottom(buttons);

        detailsButton.setOnAction(event -> toggleDetails());
        logButton.setOnAction(event -> ConsoleFrame.showMessages());
        cancelButton.setOnAction(event -> attemptCancel());

        stage.setScene(new Scene(root, 400, 140));
        stage.setOnCloseRequest(event -> {
            event.consume();
            attemptCancel();
        });

        updater = new Timeline(new KeyFrame(Duration.millis(400), event -> update()));
        updater.setCycleCount(Timeline.INDEFINITE);
        updater.play();
    }

    private void attemptCancel() {
        if (FxDialogs.confirm(stage, SharedLocale.tr("progress.confirmCancel"), SharedLocale.tr("progress.confirmCancelTitle"))) {
            cancelAction.run();
            close();
        }
    }

    private void toggleDetails() {
        boolean showing = detailsBox.isVisible();
        detailsBox.setVisible(!showing);
        detailsBox.setManaged(!showing);
        logButton.setVisible(!showing);
        detailsButton.setText(showing ? SharedLocale.tr("progress.details") : SharedLocale.tr("progress.less"));
        if (!showing) {
            stage.setHeight(320);
        } else {
            stage.setHeight(160);
        }
    }

    private void update() {
        double progress = observable.getProgress();
        if (progress >= 0) {
            progressBar.setProgress(progress);
            stage.setTitle(tr("progress.percentTitle", Math.round(progress * 10000) / 100.0, defaultTitle));
        } else {
            progressBar.setProgress(-1);
            stage.setTitle(defaultTitle);
        }

        String status = observable.getStatus();
        if (status == null) {
            messageLabel.setText(defaultMessage);
            logArea.setText(SharedLocale.tr("progress.defaultStatus"));
        } else {
            int index = status.indexOf('\n');
            if (index == -1) {
                messageLabel.setText(status);
            } else {
                messageLabel.setText(status.substring(0, index));
            }
            logArea.setText(status);
        }
        logArea.positionCaret(0);
    }

    private void show() {
        stage.show();
    }

    private void close() {
        updater.stop();
        stage.close();
    }

    public static void showProgress(Window owner, ObservableFuture<?> future, String title, String message) {
        showProgress(owner, future, future, title, message);
    }

    public static void showProgress(Window owner, ListenableFuture<?> future, ProgressObservable observable, String title, String message) {
        ProgressDialog dialog = new ProgressDialog(owner, observable, title, message, () -> future.cancel(true));
        lastDialogRef = new WeakReference<>(dialog);

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(Object result) {
                FxExecutor.INSTANCE.execute(dialog::close);
            }

            @Override
            public void onFailure(Throwable t) {
                FxExecutor.INSTANCE.execute(dialog::close);
            }
        }, FxExecutor.INSTANCE);

        FxExecutor.INSTANCE.execute(dialog::show);
    }

    public static ProgressDialog getLastDialog() {
        WeakReference<ProgressDialog> ref = lastDialogRef;
        return ref != null ? ref.get() : null;
    }
}
