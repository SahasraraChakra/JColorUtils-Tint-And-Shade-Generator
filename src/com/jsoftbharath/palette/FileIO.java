package com.jsoftbharath.palette;

import java.awt.*;
import java.io.File;
import java.util.Locale;

/** Native open/save dialogs (java.awt.FileDialog). */
final class FileIO {
    private FileIO() {}

    private static FileDialog make(Component owner, String title, int mode) {
        Window w = owner == null ? null : javax.swing.SwingUtilities.getWindowAncestor(owner);
        if (w == null && owner instanceof Window) w = (Window) owner;
        Frame f = w instanceof Frame ? (Frame) w : null;
        return new FileDialog(f, title, mode);
    }

    static File open(Component owner, String title) {
        FileDialog d = make(owner, title, FileDialog.LOAD);
        d.setVisible(true);
        return d.getFile() == null ? null : new File(d.getDirectory(), d.getFile());
    }

    static File save(Component owner, String title, String suggested, String defaultExt) {
        FileDialog d = make(owner, title, FileDialog.SAVE);
        d.setFile(suggested);
        d.setVisible(true);
        if (d.getFile() == null) return null;
        File f = new File(d.getDirectory(), d.getFile());
        if (!f.getName().contains(".")) f = new File(f.getParentFile(), f.getName() + "." + defaultExt);
        return f;
    }

    static String ext(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1);
    }
}
