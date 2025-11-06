package com.skcraft.launcher.fx;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Clipboard helper for JavaFX.
 */
public final class FxClipboard {

    private FxClipboard() {
    }

    public static void setText(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
