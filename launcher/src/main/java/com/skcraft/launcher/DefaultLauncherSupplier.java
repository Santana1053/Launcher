/*
 * SKCraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher;

import com.google.common.base.Supplier;
import com.skcraft.launcher.dialog.LauncherFrame;
import javafx.stage.Stage;

public class DefaultLauncherSupplier implements Supplier<Stage> {

    private final Launcher launcher;

    public DefaultLauncherSupplier(Launcher launcher) {
        this.launcher = launcher;
    }

    @Override
    public Stage get() {
        LauncherFrame frame = new LauncherFrame(launcher);
        return frame.getStage();
    }

}
