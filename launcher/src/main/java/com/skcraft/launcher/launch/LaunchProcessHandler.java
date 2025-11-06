/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.launch;

import com.google.common.base.Function;
import com.skcraft.launcher.dialog.ProcessConsoleFrame;
import com.skcraft.launcher.util.FxExecutor;
import lombok.NonNull;
import lombok.extern.java.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Handles post-process creation during launch.
 */
@Log
public class LaunchProcessHandler implements Function<Process, ProcessConsoleFrame> {

    private static final int CONSOLE_NUM_LINES = 10000;

    private final Launcher launcher;
    private ProcessConsoleFrame consoleFrame;

    public LaunchProcessHandler(@NonNull Launcher launcher) {
        this.launcher = launcher;
    }

    @Override
    public ProcessConsoleFrame apply(final Process process) {
        log.info("Watching process " + process);

        CountDownLatch latch = new CountDownLatch(1);

        FxExecutor.INSTANCE.execute(() -> {
            try {
                consoleFrame = new ProcessConsoleFrame(CONSOLE_NUM_LINES, false);
                consoleFrame.setProcess(process);
                consoleFrame.show();
                consoleFrame.getMessageLog().consume(process.getInputStream());
                consoleFrame.getMessageLog().consume(process.getErrorStream());
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Process ended, re-showing launcher...");

        FxExecutor.INSTANCE.execute(() -> {
            if (consoleFrame != null) {
                consoleFrame.setProcess(null);
                consoleFrame.show();
            }
        });

        return consoleFrame;
    }

}
