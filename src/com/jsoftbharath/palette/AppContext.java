package com.jsoftbharath.palette;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Shared services every palette widget talks to (clipboard, pinboard, contrast checker, status bar). */
final class AppContext {
    CopyFormat format = CopyFormat.HEX_HASH;
    Consumer<String> status = s -> {};
    BiFunction<Color, String, Boolean> pinHandler = (c, l) -> false;
    Consumer<Color> fgHandler = c -> {};
    Consumer<Color> bgHandler = c -> {};
    Consumer<Color> toInputHandler = c -> {};
    Component root;
    java.util.function.Supplier<Color> pinBaseSupplier = () -> new Color(0x663399);
    Color pinBase() { return pinBaseSupplier.get(); }

    String fmt(Color c) { return ColorUtils.format(c, format); }

    void copyText(String text, String message) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            status.accept(message);
        } catch (Exception e) {
            status.accept("Clipboard unavailable: " + e.getMessage());
        }
    }

    void copy(Color c, String label) {
        String t = fmt(c);
        copyText(t, "Copied " + t + (label == null || label.isEmpty() ? "" : "  (" + label + ")"));
    }

    void pin(Color c, String label) {
        boolean added = pinHandler.apply(c, label);
        status.accept(added ? "Pinned " + ColorUtils.toHex(c, true) + " to the pinboard"
            : ColorUtils.toHex(c, true) + " is already on the pinboard");
    }

    void setFg(Color c) { fgHandler.accept(c); status.accept("Contrast foreground: " + ColorUtils.toHex(c, true)); }
    void setBg(Color c) { bgHandler.accept(c); status.accept("Contrast background: " + ColorUtils.toHex(c, true)); }
}
