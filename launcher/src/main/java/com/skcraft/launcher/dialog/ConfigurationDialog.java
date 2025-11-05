package com.skcraft.launcher.dialog;

import com.skcraft.launcher.Configuration;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.launch.runtime.AddJavaRuntime;
import com.skcraft.launcher.launch.runtime.JavaRuntime;
import com.skcraft.launcher.launch.runtime.JavaRuntimeFinder;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.util.SharedLocale;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

public final class ConfigurationDialog {

    private final Launcher launcher;
    private final Configuration config;
    private final Stage stage;

    private final ComboBox<JavaRuntime> jvmRuntime = new ComboBox<>();
    private final TextField jvmArgsField = new TextField();
    private final Spinner<Integer> minMemorySpinner = new Spinner<>();
    private final Spinner<Integer> maxMemorySpinner = new Spinner<>();
    private final Spinner<Integer> permGenSpinner = new Spinner<>();

    private final Spinner<Integer> widthSpinner = new Spinner<>();
    private final Spinner<Integer> heightSpinner = new Spinner<>();

    private final CheckBox useProxyCheck = new CheckBox(SharedLocale.tr("options.useProxyCheck"));
    private final TextField proxyHostField = new TextField();
    private final Spinner<Integer> proxyPortSpinner = new Spinner<>();
    private final TextField proxyUsernameField = new TextField();
    private final PasswordField proxyPasswordField = new PasswordField();

    private final TextField gameKeyField = new TextField();

    private ConfigurationDialog(Window owner, Launcher launcher) {
        this.launcher = launcher;
        this.config = launcher.getConfig();

        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setTitle(SharedLocale.tr("options.title"));
        stage.setResizable(false);

        stage.setScene(new Scene(createContent()));
        stage.setOnCloseRequest(e -> stage.close());

        populateValues();
    }

    public static void show(Window owner, Launcher launcher) {
        new ConfigurationDialog(owner, launcher).show();
    }

    private void show() {
        stage.showAndWait();
    }

    private BorderPane createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                createJavaTab(),
                createGameTab(),
                createProxyTab(),
                createAdvancedTab()
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        root.setCenter(tabPane);
        BorderPane.setMargin(tabPane, new Insets(0, 0, 12, 0));

        root.setBottom(createButtonBar());
        return root;
    }

    private Tab createJavaTab() {
        GridPane grid = createFormGrid();

        configureComboBox();
        configureMemorySpinner(minMemorySpinner, config.getMinMemory());
        configureMemorySpinner(maxMemorySpinner, config.getMaxMemory());
        configureMemorySpinner(permGenSpinner, config.getPermGen());

        grid.add(new Label(SharedLocale.tr("options.jvmPath")), 0, 0);
        grid.add(jvmRuntime, 1, 0);
        grid.add(new Label(SharedLocale.tr("options.jvmArguments")), 0, 1);
        grid.add(jvmArgsField, 1, 1);
        grid.add(new Label(SharedLocale.tr("options.minMemory")), 0, 2);
        grid.add(minMemorySpinner, 1, 2);
        grid.add(new Label(SharedLocale.tr("options.maxMemory")), 0, 3);
        grid.add(maxMemorySpinner, 1, 3);
        grid.add(new Label(SharedLocale.tr("options.permGen")), 0, 4);
        grid.add(permGenSpinner, 1, 4);

        Label warning = new Label(SharedLocale.tr("options.64BitJavaWarning"));
        warning.setWrapText(true);
        GridPane.setColumnSpan(warning, 2);
        grid.add(warning, 0, 5);

        Tab tab = new Tab(SharedLocale.tr("options.javaTab"));
        tab.setContent(grid);
        return tab;
    }

    private Tab createGameTab() {
        GridPane grid = createFormGrid();

        configureDimensionSpinner(widthSpinner, config.getWindowWidth());
        configureDimensionSpinner(heightSpinner, config.getWindowHeight());

        grid.add(new Label(SharedLocale.tr("options.windowWidth")), 0, 0);
        grid.add(widthSpinner, 1, 0);
        grid.add(new Label(SharedLocale.tr("options.windowHeight")), 0, 1);
        grid.add(heightSpinner, 1, 1);

        Tab tab = new Tab(SharedLocale.tr("options.minecraftTab"));
        tab.setContent(grid);
        return tab;
    }

    private Tab createProxyTab() {
        GridPane grid = createFormGrid();

        configurePortSpinner(proxyPortSpinner, config.getProxyPort());

        grid.add(useProxyCheck, 0, 0, 2, 1);
        grid.add(new Label(SharedLocale.tr("options.proxyHost")), 0, 1);
        grid.add(proxyHostField, 1, 1);
        grid.add(new Label(SharedLocale.tr("options.proxyPort")), 0, 2);
        grid.add(proxyPortSpinner, 1, 2);
        grid.add(new Label(SharedLocale.tr("options.proxyUsername")), 0, 3);
        grid.add(proxyUsernameField, 1, 3);
        grid.add(new Label(SharedLocale.tr("options.proxyPassword")), 0, 4);
        grid.add(proxyPasswordField, 1, 4);

        useProxyCheck.selectedProperty().addListener((obs, oldVal, newVal) -> updateProxyState(newVal));

        Tab tab = new Tab(SharedLocale.tr("options.proxyTab"));
        tab.setContent(grid);
        return tab;
    }

    private Tab createAdvancedTab() {
        GridPane grid = createFormGrid();

        grid.add(new Label(SharedLocale.tr("options.gameKey")), 0, 0);
        grid.add(gameKeyField, 1, 0);

        Tab tab = new Tab(SharedLocale.tr("options.advancedTab"));
        tab.setContent(grid);
        return tab;
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setHgrow(Priority.NEVER);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        return grid;
    }

    private HBox createButtonBar() {
        Button logButton = new Button(SharedLocale.tr("options.launcherConsole"));
        Button aboutButton = new Button(SharedLocale.tr("options.about"));
        Button okButton = new Button(SharedLocale.tr("button.ok"));
        Button cancelButton = new Button(SharedLocale.tr("button.cancel"));

        logButton.setOnAction(e -> ConsoleFrame.showMessages());
        aboutButton.setOnAction(e -> AboutDialog.showAboutDialog(stage));
        okButton.setOnAction(e -> save());
        cancelButton.setOnAction(e -> stage.close());
        okButton.setDefaultButton(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(10, logButton, aboutButton, spacer, cancelButton, okButton);
        buttons.setPadding(new Insets(0, 0, 0, 0));
        return buttons;
    }

    private void configureComboBox() {
        List<JavaRuntime> runtimes = JavaRuntimeFinder.getAvailableRuntimes();
        JavaRuntime configRuntime = config.getJavaRuntime();

        if (configRuntime != null && runtimes.stream().noneMatch(r -> r.equals(configRuntime))) {
            runtimes.add(0, configRuntime);
        }

        jvmRuntime.setItems(FXCollections.observableArrayList(runtimes));
        jvmRuntime.getItems().add(AddJavaRuntime.ADD_RUNTIME_SENTINEL);
        if (configRuntime != null) {
            jvmRuntime.getSelectionModel().select(configRuntime);
        }

        jvmRuntime.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(JavaRuntime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        jvmRuntime.setButtonCell(jvmRuntime.getCellFactory().call(null));

        jvmRuntime.setOnAction(e -> {
            JavaRuntime selected = jvmRuntime.getSelectionModel().getSelectedItem();
            if (selected == AddJavaRuntime.ADD_RUNTIME_SENTINEL) {
                jvmRuntime.getSelectionModel().clearSelection();
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Choose a Java executable");
                File file = chooser.showOpenDialog(stage);
                if (file != null) {
                    JavaRuntime runtime = JavaRuntimeFinder.getRuntimeFromPath(file.getAbsolutePath());
                    if (runtime != null) {
                        jvmRuntime.getItems().add(0, runtime);
                        jvmRuntime.getSelectionModel().select(runtime);
                    }
                }
            }
        });
    }

    private void configureMemorySpinner(Spinner<Integer> spinner, int initial) {
        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(256, 32768, Math.max(initial, 256), 128);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
    }

    private void configureDimensionSpinner(Spinner<Integer> spinner, int initial) {
        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(640, 7680, Math.max(initial, 640), 10);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
    }

    private void configurePortSpinner(Spinner<Integer> spinner, int initial) {
        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 65535, Math.max(initial, 0), 1);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
    }

    private void updateProxyState(boolean enabled) {
        proxyHostField.setDisable(!enabled);
        proxyPortSpinner.setDisable(!enabled);
        proxyUsernameField.setDisable(!enabled);
        proxyPasswordField.setDisable(!enabled);
    }

    private void populateValues() {
        jvmArgsField.setText(config.getJvmArgs());
        minMemorySpinner.getValueFactory().setValue(config.getMinMemory());
        maxMemorySpinner.getValueFactory().setValue(config.getMaxMemory());
        permGenSpinner.getValueFactory().setValue(config.getPermGen());

        widthSpinner.getValueFactory().setValue(config.getWindowWidth());
        heightSpinner.getValueFactory().setValue(config.getWindowHeight());

        useProxyCheck.setSelected(config.isProxyEnabled());
        proxyHostField.setText(config.getProxyHost());
        proxyPortSpinner.getValueFactory().setValue(config.getProxyPort());
        proxyUsernameField.setText(config.getProxyUsername());
        proxyPasswordField.setText(config.getProxyPassword());
        updateProxyState(config.isProxyEnabled());

        gameKeyField.setText(config.getGameKey());
    }

    private void save() {
        config.setJvmArgs(emptyToNull(jvmArgsField.getText()));
        config.setMinMemory(minMemorySpinner.getValue());
        config.setMaxMemory(maxMemorySpinner.getValue());
        config.setPermGen(permGenSpinner.getValue());

        config.setWindowWidth(widthSpinner.getValue());
        config.setWindowHeight(heightSpinner.getValue());

        config.setProxyEnabled(useProxyCheck.isSelected());
        config.setProxyHost(emptyToNull(proxyHostField.getText()));
        config.setProxyPort(proxyPortSpinner.getValue());
        config.setProxyUsername(emptyToNull(proxyUsernameField.getText()));
        config.setProxyPassword(emptyToNull(proxyPasswordField.getText()));

        config.setGameKey(emptyToNull(gameKeyField.getText()));

        JavaRuntime selectedRuntime = jvmRuntime.getSelectionModel().getSelectedItem();
        if (selectedRuntime == AddJavaRuntime.ADD_RUNTIME_SENTINEL) {
            selectedRuntime = null;
        }
        config.setJavaRuntime(selectedRuntime);

        Persistence.commitAndForget(config);
        stage.close();
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
