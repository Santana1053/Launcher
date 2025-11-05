/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.dialog;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.launcher.Configuration;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.AuthenticationException;
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.auth.YggdrasilLoginService;
import com.skcraft.launcher.fx.FxDialogs;
import com.skcraft.launcher.fx.FxFutures;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.util.FxExecutor;
import com.skcraft.launcher.util.SharedLocale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

public class LoginDialog {

    private final Launcher launcher;
    private final Stage stage;
    @Getter
    private Session session;

    private final Label messageLabel = new Label(SharedLocale.tr("login.defaultMessage"));
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button(SharedLocale.tr("login.login"));
    private final Hyperlink recoverLink = new Hyperlink(SharedLocale.tr("login.recoverAccount"));
    private final Button cancelButton = new Button(SharedLocale.tr("button.cancel"));

    private LoginDialog(Window owner, @NonNull Launcher launcher, Optional<ReloginDetails> reloginDetails) {
        this.launcher = launcher;
        this.stage = new Stage();
        stage.initModality(owner != null ? Modality.WINDOW_MODAL : Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(SharedLocale.tr("login.title"));
        stage.setResizable(false);
        stage.setScene(createScene());
        stage.setOnCloseRequest(event -> {
            session = null;
        });

        reloginDetails.ifPresent(details -> {
            messageLabel.setText(details.message);
            usernameField.setText(details.username);
        });

        passwordField.setPromptText(SharedLocale.tr("login.password"));
        usernameField.setPromptText(SharedLocale.tr("login.idEmail"));
        stage.sizeToScene();
    }

    private Scene createScene() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20, 24, 20, 24));

        GridPane form = new GridPane();
        form.setVgap(12);
        form.setHgap(12);

        form.add(messageLabel, 0, 0, 2, 1);
        form.add(new Label(SharedLocale.tr("login.idEmail")), 0, 1);
        form.add(usernameField, 1, 1);
        form.add(new Label(SharedLocale.tr("login.password")), 0, 2);
        form.add(passwordField, 1, 2);

        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().addAll(recoverLink, spacer, cancelButton, loginButton);

        loginButton.setDefaultButton(true);

        root.getChildren().addAll(form, actions);

        hookActions();

        return new Scene(root, 420, 200);
    }

    private void hookActions() {
        recoverLink.setOnAction(e -> launcher.getExecutor().execute(() -> {
            String url = launcher.getProperties().getProperty("resetPasswordUrl");
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } catch (Exception ex) {
                FxExecutor.INSTANCE.execute(() -> FxDialogs.showError(stage, SharedLocale.tr("errors.openUrlError", url), SharedLocale.tr("errorTitle"), ex));
            }
        }));

        loginButton.setOnAction(e -> prepareLogin());
        cancelButton.setOnAction(e -> {
            session = null;
            stage.close();
        });
    }

    private void prepareLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty()) {
            FxDialogs.showError(stage, SharedLocale.tr("login.noLoginError"), SharedLocale.tr("login.noLoginTitle"));
            return;
        }

        if (password == null || password.isEmpty()) {
            FxDialogs.showError(stage, SharedLocale.tr("login.noPasswordError"), SharedLocale.tr("login.noPasswordTitle"));
            return;
        }

        attemptLogin(username.trim(), password);
    }

    private void attemptLogin(String username, String password) {
        LoginCallable callable = new LoginCallable(username, password);
        ObservableFuture<Session> future = new ObservableFuture<>(launcher.getExecutor().submit(callable), callable);

        Futures.addCallback(future, new FutureCallback<Session>() {
            @Override
            public void onSuccess(Session result) {
                setResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, FxExecutor.INSTANCE);

        ProgressDialog.showProgress(stage, future, SharedLocale.tr("login.loggingInTitle"), SharedLocale.tr("login.loggingInStatus"));
        FxFutures.addErrorDialogCallback(stage, future);
    }

    private void setResult(Session session) {
        this.session = session;
        stage.close();
    }

    public static Session showLoginRequest(Window owner, Launcher launcher) {
        return showLoginRequest(owner, launcher, null);
    }

    public static Session showLoginRequest(Window owner, Launcher launcher, ReloginDetails details) {
        LoginDialog dialog = new LoginDialog(owner, launcher, Optional.ofNullable(details));
        dialog.stage.showAndWait();
        return dialog.getSession();
    }

    @RequiredArgsConstructor
    private class LoginCallable implements Callable<Session>, ProgressObservable {
        private final String username;
        private final String password;

        @Override
        public Session call() throws AuthenticationException, IOException, InterruptedException {
            YggdrasilLoginService service = launcher.getYggdrasil();
            Session identity = service.login(username, password);

            if (identity != null) {
                Configuration config = launcher.getConfig();
                if (!config.isOfflineEnabled()) {
                    config.setOfflineEnabled(true);
                    Persistence.commitAndForget(config);
                }
                return identity;
            } else {
                throw new AuthenticationException("Minecraft not owned", SharedLocale.tr("login.minecraftNotOwnedError"));
            }
        }

        @Override
        public double getProgress() {
            return -1;
        }

        @Override
        public String getStatus() {
            return SharedLocale.tr("login.loggingInStatus");
        }
    }

    @Data
    public static class ReloginDetails {
        private final String username;
        private final String message;
    }
}
