package com.skcraft.launcher.dialog;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.fx.FxClipboard;
import com.skcraft.launcher.fx.FxDialogs;
import com.skcraft.launcher.fx.FxFutures;
import com.skcraft.launcher.launch.LaunchListener;
import com.skcraft.launcher.launch.LaunchOptions;
import com.skcraft.launcher.launch.LaunchOptions.UpdatePolicy;
import com.skcraft.launcher.util.FxExecutor;
import com.skcraft.launcher.util.SharedLocale;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Optional;

import static com.skcraft.launcher.util.SharedLocale.tr;

@Log
public class LauncherFrame {

    private final Launcher launcher;
    @Getter
    private final Stage stage;

    private final TableView<Instance> instancesTable = new TableView<>();
    private final ObservableList<Instance> tableItems = FXCollections.observableArrayList();
    private final WebViewWrapper newsView;
    private final Button launchButton = new Button(SharedLocale.tr("launcher.launch"));
    private final Button refreshButton = new Button(SharedLocale.tr("launcher.checkForUpdates"));
    private final Button optionsButton = new Button(SharedLocale.tr("launcher.options"));
    private final Button selfUpdateButton = new Button(SharedLocale.tr("launcher.updateLauncher"));
    private final CheckBox updateCheck = new CheckBox(SharedLocale.tr("launcher.downloadUpdates"));

    private final Image instanceIcon = loadIcon("instance_icon.png", 16, 16);
    private final Image customInstanceIcon = loadIcon("custom_instance_icon.png", 16, 16);
    private final Image downloadIcon = loadIcon("download_icon.png", 14, 14);

    public LauncherFrame(@NonNull Launcher launcher) {
        this(launcher, new Stage());
    }

    public LauncherFrame(@NonNull Launcher launcher, Stage stage) {
        this.launcher = launcher;
        this.stage = stage;

        stage.setTitle(tr("launcher.title", launcher.getVersion()));
        stage.setMinWidth(400);
        stage.setMinHeight(300);
        stage.setScene(createScene());
        stage.getIcons().add(loadIcon("icon.png", 64, 64));

        stage.setOnShown(event -> Platform.runLater(this::loadInitialState));

        selfUpdateButton.setVisible(launcher.getUpdateManager().getPendingUpdate());
        launcher.getUpdateManager().addPropertyChangeListener(evt -> {
            if ("pendingUpdate".equals(evt.getPropertyName())) {
                boolean pending = Optional.ofNullable(evt.getNewValue()).map(Boolean.class::cast).orElse(false);
                Platform.runLater(() -> selfUpdateButton.setVisible(pending));
            }
        });
    }

    public void show() {
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12, 12, 12, 12));

        newsView = new WebViewWrapper();

        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.4);
        splitPane.getItems().add(createInstancesRegion());
        splitPane.getItems().add(newsView.getNode());
        splitPane.setPadding(new Insets(0, 0, 12, 0));

        root.setCenter(splitPane);
        root.setBottom(createFooter());

        return new Scene(root, 720, 420);
    }

    private Region createInstancesRegion() {
        instancesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        instancesTable.setPlaceholder(new Label(tr("launcher.noInstances")));
        instancesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        instancesTable.setRowFactory(tv -> {
            TableRow<Instance> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    launch();
                }
            });
            row.setOnContextMenuRequested(event -> {
                Instance instance = row.getItem();
                if (instance != null) {
                    ContextMenu menu = createInstanceContextMenu(instance);
                    menu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        instancesTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                launch();
                event.consume();
            }
        });

        TableColumn<Instance, ImageView> iconColumn = new TableColumn<>();
        iconColumn.setPrefWidth(36);
        iconColumn.setMaxWidth(36);
        iconColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(createIconView(param.getValue())));
        iconColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(ImageView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Instance, String> titleColumn = new TableColumn<>(SharedLocale.tr("launcher.modpackColumn"));
        titleColumn.setCellValueFactory(param -> new SimpleStringProperty(Optional.ofNullable(param.getValue().getTitle()).orElse(param.getValue().getName())));
        titleColumn.setSortable(false);

        instancesTable.getColumns().setAll(iconColumn, titleColumn);
        instancesTable.setItems(tableItems);

        VBox container = new VBox(instancesTable);
        VBox.setVgrow(instancesTable, Priority.ALWAYS);
        return container;
    }

    private Region createFooter() {
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        launchButton.getStyleClass().add("primary-button");

        footer.getChildren().addAll(refreshButton, updateCheck, selfUpdateButton, spacer, optionsButton, launchButton);

        refreshButton.setOnAction(e -> {
            loadInstances();
            launcher.getUpdateManager().checkForUpdate();
            newsView.load(launcher.getNewsURL());
        });

        optionsButton.setOnAction(e -> showOptions());

        launchButton.setOnAction(e -> launch());

        selfUpdateButton.setOnAction(e -> launcher.getUpdateManager().performUpdate(stage));

        updateCheck.setSelected(true);

        launchButton.disableProperty().bind(instancesTable.getSelectionModel().selectedItemProperty().isNull());

        return footer;
    }

    private void loadInitialState() {
        refreshInstancesView();
        if (!tableItems.isEmpty()) {
            instancesTable.getSelectionModel().select(0);
        }
        newsView.load(launcher.getNewsURL());
        loadInstances();
    }

    private void refreshInstancesView() {
        tableItems.setAll(launcher.getInstances().getInstances());
        tableItems.sort(null);
    }

    private void loadInstances() {
        ObservableFuture<InstanceList> future = launcher.getInstanceTasks().reloadInstances(stage);
        future.addListener(() -> Platform.runLater(() -> {
            refreshInstancesView();
            if (!tableItems.isEmpty()) {
                instancesTable.getSelectionModel().select(0);
            }
        }), FxExecutor.INSTANCE);

        ProgressDialog.showProgress(stage, future, SharedLocale.tr("launcher.checkingTitle"), SharedLocale.tr("launcher.checkingStatus"));
        FxFutures.addErrorDialogCallback(stage, future);
    }

    private void launch() {
        Instance selected = instancesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean permitUpdate = updateCheck.isSelected();

        LaunchOptions options = new LaunchOptions.Builder()
                .setInstance(selected)
                .setListener(new LaunchListenerImpl(this))
                .setUpdatePolicy(permitUpdate ? UpdatePolicy.UPDATE_IF_SESSION_ONLINE : UpdatePolicy.NO_UPDATE)
                .setWindow(stage)
                .build();
        launcher.getLaunchSupervisor().launch(options);
    }

    private void showOptions() {
        ConfigurationDialog.show(stage, launcher);
    }

    private ContextMenu createInstanceContextMenu(Instance instance) {
        ContextMenu menu = new ContextMenu();

        MenuItem primary = new MenuItem(instance.isLocal() ? SharedLocale.tr("instance.launch") : SharedLocale.tr("instance.install"));
        primary.setOnAction(e -> launch());
        menu.getItems().add(primary);

        if (instance.isLocal()) {
            menu.getItems().add(new SeparatorMenuItem());

            menu.getItems().add(createBrowseItem(SharedLocale.tr("instance.openFolder"), instance.getContentDir()));
            menu.getItems().add(createBrowseItem(SharedLocale.tr("instance.openSaves"), new File(instance.getContentDir(), "saves")));
            menu.getItems().add(createBrowseItem(SharedLocale.tr("instance.openResourcePacks"), new File(instance.getContentDir(), "resourcepacks")));
            menu.getItems().add(createBrowseItem(SharedLocale.tr("instance.openScreenshots"), new File(instance.getContentDir(), "screenshots")));

            MenuItem copyPath = new MenuItem(SharedLocale.tr("instance.copyAsPath"));
            copyPath.setOnAction(e -> {
                File dir = instance.getContentDir();
                dir.mkdirs();
                FxClipboard.setText(dir.getAbsolutePath());
            });
            menu.getItems().add(copyPath);

            MenuItem openSettings = new MenuItem(SharedLocale.tr("instance.openSettings"));
            openSettings.setOnAction(e -> InstanceSettingsDialog.open(stage, instance));
            menu.getItems().add(openSettings);

            menu.getItems().add(new SeparatorMenuItem());

            if (!instance.isUpdatePending()) {
                MenuItem forceUpdate = new MenuItem(SharedLocale.tr("instance.forceUpdate"));
                forceUpdate.setOnAction(e -> {
                    instance.setUpdatePending(true);
                    launch();
                    refreshInstancesView();
                });
                menu.getItems().add(forceUpdate);
            }

            MenuItem hardUpdate = new MenuItem(SharedLocale.tr("instance.hardForceUpdate"));
            hardUpdate.setOnAction(e -> confirmHardUpdate(instance));
            menu.getItems().add(hardUpdate);

            MenuItem deleteFiles = new MenuItem(SharedLocale.tr("instance.deleteFiles"));
            deleteFiles.setOnAction(e -> confirmDelete(instance));
            menu.getItems().add(deleteFiles);
        }

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem refresh = new MenuItem(SharedLocale.tr("launcher.refreshList"));
        refresh.setOnAction(e -> loadInstances());
        menu.getItems().add(refresh);

        return menu;
    }

    private MenuItem createBrowseItem(String label, File dir) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> browseDirectory(dir));
        return item;
    }

    private void browseDirectory(File dir) {
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            FxDialogs.showError(stage, SharedLocale.tr("errors.openDirError", dir.getAbsolutePath()), SharedLocale.tr("errorTitle"), e);
        }
    }

    private void confirmDelete(Instance instance) {
        if (!FxDialogs.confirm(stage, tr("instance.confirmDelete", instance.getTitle()), SharedLocale.tr("confirmTitle"))) {
            return;
        }

        ObservableFuture<Instance> future = launcher.getInstanceTasks().delete(stage, instance);
        future.addListener(this::loadInstances, FxExecutor.INSTANCE);
        FxFutures.addErrorDialogCallback(stage, future);
    }

    private void confirmHardUpdate(Instance instance) {
        if (!FxDialogs.confirm(stage, SharedLocale.tr("instance.confirmHardUpdate"), SharedLocale.tr("confirmTitle"))) {
            return;
        }

        ObservableFuture<Instance> future = launcher.getInstanceTasks().hardUpdate(stage, instance);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(Instance result) {
                loadInstances();
                launch();
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, FxExecutor.INSTANCE);
        FxFutures.addErrorDialogCallback(stage, future);
    }

    private Image loadIcon(String resource, int width, int height) {
        URL url = Launcher.class.getResource(resource);
        if (url == null) {
            log.warning("Missing icon resource: " + resource);
            return null;
        }
        return new Image(url.toExternalForm(), width, height, true, true);
    }

    private ImageView createIconView(Instance instance) {
        Image image;
        if (!instance.isLocal()) {
            image = downloadIcon;
        } else if (instance.getManifestURL() != null) {
            image = instanceIcon;
        } else {
            image = customInstanceIcon;
        }
        ImageView view = image != null ? new ImageView(image) : new ImageView();
        view.setFitWidth(16);
        view.setFitHeight(16);
        return view;
    }

    private static class LaunchListenerImpl implements LaunchListener {
        private final WeakReference<LauncherFrame> frameRef;
        private final Launcher launcher;

        private LaunchListenerImpl(LauncherFrame frame) {
            this.frameRef = new WeakReference<>(frame);
            this.launcher = frame.launcher;
        }

        @Override
        public void instancesUpdated() {
            LauncherFrame frame = frameRef.get();
            if (frame != null) {
                Platform.runLater(frame::refreshInstancesView);
            }
        }

        @Override
        public void gameStarted() {
            LauncherFrame frame = frameRef.get();
            if (frame != null) {
                Platform.runLater(() -> frame.stage.hide());
            }
        }

        @Override
        public void gameClosed() {
            Platform.runLater(launcher::showLauncherWindow);
        }
    }

    private static class WebViewWrapper {
        private final javafx.scene.web.WebView webView = new javafx.scene.web.WebView();

        Region getNode() {
            VBox.setVgrow(webView, Priority.ALWAYS);
            return webView;
        }

        void load(URL url) {
            if (url != null) {
                Platform.runLater(() -> webView.getEngine().load(url.toExternalForm()));
            }
        }
    }
}
