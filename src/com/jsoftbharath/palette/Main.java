package com.jsoftbharath.palette;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.prefs.Preferences;

/** Tint & Shade Palette Generator - entry point.  (c) JSoft Bharath */
public final class Main {
    static final String APP_NAME = "Tint & Shade Palette Generator";
    static final String VERSION = "1.0.0";
    static final String COMPANY = "JSoft Bharath";
    static final Preferences PREFS = Preferences.userNodeForPackage(Main.class);

    private Main() {}

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(new MetalLookAndFeel()); } catch (Exception ignored) {}
            String saved = PREFS.get("theme", Theme.LIGHT);
            // a theme.properties file next to the jar always wins - that is the "edit me" theme
            Theme.use(Theme.findCustomFile() != null ? Theme.CUSTOM : saved);
            Theme.installDefaults();
            Theme.addListener(Theme::installDefaults);
            Theme.startWatcher();
            Widgets.installAutoScroll();
            MainFrame f = new MainFrame();
            f.setVisible(true);
            f.startFocus();
        });
    }
}
