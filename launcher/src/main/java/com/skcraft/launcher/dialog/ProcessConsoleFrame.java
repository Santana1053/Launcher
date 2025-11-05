package com.skcraft.launcher.dialog;

import com.skcraft.launcher.fx.FxDialogs;
import com.skcraft.launcher.util.FxExecutor;
import com.skcraft.launcher.util.SharedLocale;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;

import static com.skcraft.launcher.util.SharedLocale.tr;

public class ProcessConsoleFrame extends ConsoleFrame {

    private final Button killButton = new Button(SharedLocale.tr("console.forceClose"));
    private final Button hideButton = new Button();

    private Process process;
    private boolean killOnClose;

    public ProcessConsoleFrame(int numLines, boolean colorEnabled) {
        super(SharedLocale.tr("console.title"), numLines, colorEnabled);
        updateButtons();
    }

    @Override
    protected HBox createTopBar() {
        HBox base = super.createTopBar();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        killButton.setOnAction(e -> performKill());
        hideButton.setOnAction(e -> stage.hide());

        HBox container = new HBox(8);
        container.getChildren().addAll(base.getChildren());
        container.getChildren().addAll(spacer, killButton, hideButton);
        container.setPadding(new Insets(0, 0, 0, 0));
        return container;
    }

    public synchronized void setProcess(Process process) {
        if (this.process != null) {
            try {
                getMessageLog().log(tr("console.processEndCode", this.process.exitValue()) + "\n");
            } catch (IllegalThreadStateException ignored) {
            }
        }

        this.process = process;

        if (process != null) {
            getMessageLog().log(SharedLocale.tr("console.attachedToProcess") + "\n");
        }

        updateButtons();
    }

    public synchronized Process getProcess() {
        return process;
    }

    public synchronized boolean isKillOnClose() {
        return killOnClose;
    }

    public synchronized void setKillOnClose(boolean killOnClose) {
        this.killOnClose = killOnClose;
    }

    @Override
    protected void performClose() {
        synchronized (this) {
            if (process != null && killOnClose) {
                process.destroy();
                process = null;
            }
        }
        super.performClose();
    }

    private void performKill() {
        if (!confirmKill()) {
            return;
        }
        synchronized (this) {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
        updateButtons();
    }

    private boolean confirmKill() {
        if (System.getProperty("skcraftLauncher.killWithoutConfirm", "false").equalsIgnoreCase("true")) {
            return true;
        }
        return FxDialogs.confirm(stage, SharedLocale.tr("console.confirmKill"), SharedLocale.tr("console.confirmKillTitle"));
    }

    private void updateButtons() {
        FxExecutor.INSTANCE.execute(() -> {
            boolean hasProcess = process != null;
            killButton.setDisable(!hasProcess);
            hideButton.setText(hasProcess ? SharedLocale.tr("console.hideWindow") : SharedLocale.tr("console.closeWindow"));
        });
    }
}
