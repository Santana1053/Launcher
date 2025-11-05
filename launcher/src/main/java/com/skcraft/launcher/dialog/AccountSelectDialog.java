package com.skcraft.launcher.dialog;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.concurrency.SettableProgress;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.*;
import com.skcraft.launcher.fx.FxDialogs;
import com.skcraft.launcher.fx.FxFutures;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.util.FxExecutor;
import com.skcraft.launcher.util.SharedLocale;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Callable;

public class AccountSelectDialog {

    private final Launcher launcher;
    private final Stage stage;
    private final ListView<SavedSession> accountList = new ListView<>();

    private Session selected;

    private final Button loginButton = new Button(SharedLocale.tr("accounts.play"));
    private final Button cancelButton = new Button(SharedLocale.tr("button.cancel"));
    private final Button addMojangButton = new Button(SharedLocale.tr("accounts.addMojang"));
    private final Button addMicrosoftButton = new Button(SharedLocale.tr("accounts.addMicrosoft"));
    private final Button removeSelectedButton = new Button(SharedLocale.tr("accounts.removeSelected"));
    private final Button offlineButton = new Button(SharedLocale.tr("login.playOffline"));

    private final java.util.function.Consumer<com.skcraft.launcher.auth.AccountList> accountListener = list -> FxExecutor.INSTANCE.execute(this::refreshAccounts);

    private AccountSelectDialog(Window owner, Launcher launcher) {
        this.launcher = launcher;
        this.stage = new Stage();
        stage.initModality(owner != null ? Modality.WINDOW_MODAL : Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(SharedLocale.tr("accounts.title"));
        stage.setResizable(false);
        stage.setScene(createScene());
        stage.setOnCloseRequest(event -> {
            selected = null;
            unregisterListeners();
        });

        registerListeners();
        refreshAccounts();
        if (!accountList.getItems().isEmpty()) {
            accountList.getSelectionModel().select(0);
        }
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        accountList.setCellFactory(list -> new AccountCell());
        accountList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        accountList.setPrefHeight(160);

        VBox listContainer = new VBox(accountList);
        VBox.setVgrow(accountList, Priority.ALWAYS);

        VBox addButtons = new VBox(8, addMojangButton, addMicrosoftButton, removeSelectedButton);
        addButtons.setAlignment(Pos.TOP_CENTER);
        addButtons.setPadding(new Insets(0, 0, 0, 12));

        HBox middle = new HBox(listContainer, addButtons);
        HBox.setHgrow(listContainer, Priority.ALWAYS);
        root.setCenter(middle);

        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(16, 0, 0, 0));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (launcher.getConfig().isOfflineEnabled()) {
            bottom.getChildren().add(offlineButton);
        }
        bottom.getChildren().addAll(spacer, cancelButton, loginButton);
        root.setBottom(bottom);

        loginButton.setDefaultButton(true);

        hookActions();

        return new Scene(root, 420, 260);
    }

    private void hookActions() {
        loginButton.setOnAction(e -> attemptExistingLogin(accountList.getSelectionModel().getSelectedItem()));
        cancelButton.setOnAction(e -> {
            selected = null;
            stage.close();
        });

        addMojangButton.setOnAction(e -> {
            Session newSession = LoginDialog.showLoginRequest(stage, launcher);
            if (newSession != null) {
                launcher.getAccounts().update(newSession.toSavedSession());
                setResult(newSession);
            }
        });

        addMicrosoftButton.setOnAction(e -> attemptMicrosoftLogin());

        offlineButton.setOnAction(e -> setResult(new OfflineSession(launcher.getProperties().getProperty("offlinePlayerName"))));

        removeSelectedButton.setOnAction(e -> {
            SavedSession selectedSession = accountList.getSelectionModel().getSelectedItem();
            if (selectedSession != null && FxDialogs.confirm(stage, SharedLocale.tr("accounts.confirmForget"), SharedLocale.tr("accounts.confirmForgetTitle"))) {
                launcher.getAccounts().remove(selectedSession);
            }
        });

        accountList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.isStillSincePress()) {
                attemptExistingLogin(accountList.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void registerListeners() {
        launcher.getAccounts().addListener(accountListener);
    }

    private void unregisterListeners() {
        launcher.getAccounts().removeListener(accountListener);
    }

    private void refreshAccounts() {
        List<SavedSession> snapshot = launcher.getAccounts().snapshot();
        accountList.getItems().setAll(snapshot);
    }

    private void attemptMicrosoftLogin() {
        String status = SharedLocale.tr("login.microsoft.seeBrowser");
        SettableProgress progress = new SettableProgress(status, -1);

        ListenableFuture<?> future = launcher.getExecutor().submit(() -> {
            Session newSession = launcher.getMicrosoftLogin().login(() -> progress.set(SharedLocale.tr("login.loggingInStatus"), -1));
            if (newSession != null) {
                launcher.getAccounts().update(newSession.toSavedSession());
                FxExecutor.INSTANCE.execute(() -> setResult(newSession));
            }
            return null;
        });

        ProgressDialog.showProgress(stage, future, progress, SharedLocale.tr("login.loggingInTitle"), status);
        FxFutures.addErrorDialogCallback(stage, future);
    }

    private void attemptExistingLogin(SavedSession session) {
        if (session == null) {
            return;
        }

        LoginService service = launcher.getLoginService(session.getType());
        RestoreSessionCallable callable = new RestoreSessionCallable(service, session);
        ObservableFuture<Session> future = new ObservableFuture<>(launcher.getExecutor().submit(callable), callable);

        Futures.addCallback(future, new FutureCallback<Session>() {
            @Override
            public void onSuccess(Session result) {
                setResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof AuthenticationException && ((AuthenticationException) t).isInvalidatedSession()) {
                    LoginDialog.ReloginDetails details = new LoginDialog.ReloginDetails(session.getUsername(), t.getLocalizedMessage());
                    Session newSession = LoginDialog.showLoginRequest(stage, launcher, details);
                    if (newSession != null) {
                        setResult(newSession);
                    }
                } else if (t != null) {
                    FxDialogs.showError(stage, t.getLocalizedMessage(), SharedLocale.tr("errorTitle"), t);
                }
            }
        }, FxExecutor.INSTANCE);

        ProgressDialog.showProgress(stage, future, SharedLocale.tr("login.loggingInTitle"), SharedLocale.tr("login.loggingInStatus"));
        FxFutures.addErrorDialogCallback(stage, future);
    }

    private void setResult(Session session) {
        if (session != null) {
            selected = session;
            unregisterListeners();
            stage.close();
        }
    }

    public static Session showAccountRequest(Window owner, Launcher launcher) {
        AccountSelectDialog dialog = new AccountSelectDialog(owner, launcher);
        dialog.stage.showAndWait();

        Session result = dialog.selected;
        if (result != null && result.isOnline()) {
            launcher.getAccounts().update(result.toSavedSession());
        }

        Persistence.commitAndForget(launcher.getAccounts());
        return result;
    }

    @RequiredArgsConstructor
    private static class RestoreSessionCallable implements Callable<Session>, ProgressObservable {
        private final LoginService service;
        private final SavedSession session;

        @Override
        public Session call() throws Exception {
            return service.restore(session);
        }

        @Override
        public String getStatus() {
            return SharedLocale.tr("accounts.refreshingStatus");
        }

        @Override
        public double getProgress() {
            return -1;
        }
    }

    private static class AccountCell extends ListCell<SavedSession> {
        private final ImageView avatarView = new ImageView();

        AccountCell() {
            avatarView.setFitWidth(24);
            avatarView.setFitHeight(24);
            avatarView.setPreserveRatio(true);
            setGraphic(new HBox(10, avatarView, new Label()));
        }

        @Override
        protected void updateItem(SavedSession item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                avatarView.setImage(null);
            } else {
                setText(item.getUsername());
                BufferedImage avatar = item.getAvatarImage();
                if (avatar != null) {
                    avatarView.setImage(SwingFXUtils.toFXImage(avatar, null));
                } else {
                    avatarView.setImage(null);
                }
            }
        }
    }
}
