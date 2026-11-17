package com.jsoftbharath.palette;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Theme engine (FlatLaf-style).  Colors, fonts and shape come from .properties files:
 *   bundled  /themes/light.properties and /themes/dark.properties  (inside the jar)
 *   custom   theme.properties next to the jar (or ./ or ~/.jsoftbharath/palette/) - reloaded live.
 * (c) JSoft Bharath
 */
final class Theme {
    static final String LIGHT = "light", DARK = "dark", CUSTOM = "custom";
    static final String FILE_NAME = "theme.properties";

    private static final Map<String, String> raw = new HashMap<>();
    private static final Map<String, Color> cache = new HashMap<>();
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static String active = LIGHT;
    private static File customFile;
    private static long customStamp;
    private static boolean fontsRegistered;

    static Font body, bodyBold, small, smallBold, title, mono, monoBold, monoSmall;

    private Theme() {}

    // ------------------------------------------------------------------ loading
    static synchronized void use(String which) {
        raw.clear();
        cache.clear();
        readResource(raw, "/themes/light.properties");
        String effective = which;
        if (DARK.equals(which)) {
            readResource(raw, "/themes/dark.properties");
        } else if (CUSTOM.equals(which)) {
            File f = findCustomFile();
            if (f == null) {
                effective = LIGHT;
            } else {
                Map<String, String> m = new HashMap<>();
                readFile(m, f);
                if ("dark".equalsIgnoreCase(m.get("extends"))) readResource(raw, "/themes/dark.properties");
                raw.putAll(m);
                customFile = f;
                customStamp = f.lastModified();
            }
        }
        active = effective;
        registerFonts();
        buildFonts();
        fire();
    }

    static String active() { return active; }

    static void addListener(Runnable r) { listeners.add(r); }

    private static void fire() { for (Runnable r : listeners) r.run(); }

    /** Polls the custom theme file and reloads it when it changes on disk. */
    static void startWatcher() {
        javax.swing.Timer t = new javax.swing.Timer(1000, e -> {
            if (!CUSTOM.equals(active)) return;
            File f = customFile;
            if (f == null) return;
            if (!f.exists()) { use(LIGHT); return; }
            long m = f.lastModified();
            if (m != customStamp) use(CUSTOM);
        });
        t.setRepeats(true);
        t.start();
    }

    static File appDir() {
        try {
            File f = new File(Theme.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() ? f.getParentFile() : f;
        } catch (Exception e) {
            return new File(".").getAbsoluteFile();
        }
    }

    static File homeDir() {
        return new File(System.getProperty("user.home"), ".jsoftbharath" + File.separator + "palette");
    }

    static File findCustomFile() {
        File[] candidates = {new File(appDir(), FILE_NAME), new File(FILE_NAME), new File(homeDir(), FILE_NAME)};
        for (File f : candidates) if (f.isFile()) return f.getAbsoluteFile();
        return null;
    }

    /** Writes a fresh copy of the light theme as the custom theme file (if none exists) and returns it. */
    static File ensureCustomFile() throws IOException {
        File f = findCustomFile();
        if (f != null) return f;
        File[] targets = {new File(appDir(), FILE_NAME), new File(homeDir(), FILE_NAME)};
        IOException last = null;
        for (File t : targets) {
            try {
                if (t.getParentFile() != null) t.getParentFile().mkdirs();
                try (InputStream in = Theme.class.getResourceAsStream("/themes/" + (DARK.equals(active) ? "dark" : "light") + ".properties");
                     OutputStream out = new FileOutputStream(t)) {
                    if (in == null) throw new IOException("bundled theme missing");
                    if (DARK.equals(active)) out.write("extends = dark\n".getBytes(StandardCharsets.UTF_8));
                    in.transferTo(out);
                }
                return t.getAbsoluteFile();
            } catch (IOException ex) { last = ex; }
        }
        throw last != null ? last : new IOException("cannot write theme file");
    }

    private static void readResource(Map<String, String> into, String path) {
        try (InputStream in = Theme.class.getResourceAsStream(path)) {
            if (in != null) parse(into, new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }

    private static void readFile(Map<String, String> into, File f) {
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            parse(into, r);
        } catch (IOException ignored) {}
    }

    private static void parse(Map<String, String> into, Reader r) throws IOException {
        Properties p = new Properties();
        p.load(r);
        for (String k : p.stringPropertyNames()) into.put(k.trim(), p.getProperty(k).trim());
    }

    // ------------------------------------------------------------------ accessors
    static Color c(String key) {
        Color c = cache.get(key);
        if (c != null) return c;
        c = parseColor(raw.get(key), 0);
        if (c == null) c = Color.MAGENTA;
        cache.put(key, c);
        return c;
    }

    private static Color parseColor(String v, int depth) {
        if (v == null || depth > 6) return null;
        v = v.trim();
        if (v.startsWith("@")) return parseColor(raw.get(v.substring(1)), depth + 1);
        try {
            if (v.startsWith("#")) v = v.substring(1);
            if (v.length() == 3) v = "" + v.charAt(0) + v.charAt(0) + v.charAt(1) + v.charAt(1) + v.charAt(2) + v.charAt(2);
            if (v.length() == 6) return new Color(Integer.parseInt(v, 16));
            if (v.length() == 8) {
                int rgb = (int) Long.parseLong(v.substring(0, 6), 16);
                int a = Integer.parseInt(v.substring(6), 16);
                return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a);
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    static int num(String key, int def) {
        try { return Integer.parseInt(raw.getOrDefault(key, "").trim()); } catch (Exception e) { return def; }
    }

    static int radius() { return Math.max(0, Math.min(12, num("shape.radius", 0))); }

    static String text(String key, String def) { return raw.getOrDefault(key, def); }

    // ------------------------------------------------------------------ fonts
    private static void registerFonts() {
        if (fontsRegistered) return;
        fontsRegistered = true;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] bundled = {"Inter-Regular.otf", "Inter-Bold.otf", "Inter-Regular.ttf", "Inter-Bold.ttf",
            "CascadiaCode-Regular.ttf", "CascadiaCode.ttf", "CascadiaCode-Regular.otf", "CascadiaMono-Regular.ttf"};
        for (String n : bundled) {
            try (InputStream in = Theme.class.getResourceAsStream("/fonts/" + n)) {
                if (in != null) ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
            } catch (Exception ignored) {}
        }
        File[] dirs = {new File(appDir(), "fonts"), new File("fonts"), new File(homeDir(), "fonts")};
        for (File d : dirs) {
            File[] fs = d.listFiles((dir, name) -> {
                String l = name.toLowerCase(Locale.ROOT);
                return l.endsWith(".ttf") || l.endsWith(".otf");
            });
            if (fs == null) continue;
            for (File f : fs) {
                try { ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, f)); } catch (Exception ignored) {}
            }
        }
    }

    private static Set<String> families() {
        Set<String> s = new HashSet<>();
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
            s.add(f.toLowerCase(Locale.ROOT));
        return s;
    }

    private static String resolveFamily(String list, String fallback) {
        Set<String> have = families();
        for (String f : list.split(",")) {
            String n = f.trim();
            if (n.isEmpty()) continue;
            if (have.contains(n.toLowerCase(Locale.ROOT))) return n;
            if (n.equals(Font.DIALOG) || n.equals(Font.MONOSPACED) || n.equals(Font.SANS_SERIF)) return n;
        }
        return fallback;
    }

    private static void buildFonts() {
        String bf = resolveFamily(text("font.body", "Inter"), Font.DIALOG);
        String mf = resolveFamily(text("font.mono", "Cascadia Code"), Font.MONOSPACED);
        int bs = Math.max(8, num("font.body.size", 11));
        int ms = Math.max(7, num("font.mono.size", 10));
        int mss = Math.max(7, num("font.mono.small.size", 9));
        body = new Font(bf, Font.PLAIN, bs);
        bodyBold = new Font(bf, Font.BOLD, bs);
        small = new Font(bf, Font.PLAIN, Math.max(8, bs - 1));
        smallBold = new Font(bf, Font.BOLD, Math.max(8, bs - 1));
        title = new Font(bf, Font.BOLD, bs + 1);
        mono = new Font(mf, Font.PLAIN, ms);
        monoBold = new Font(mf, Font.BOLD, ms);
        monoSmall = new Font(mf, Font.PLAIN, mss);
    }

    static String bodyFamily() { return body.getFamily(); }
    static String monoFamily() { return mono.getFamily(); }

    // ------------------------------------------------------------------ applying
    static void applyTree(Component c) {
        if (c instanceof Themed) ((Themed) c).applyTheme();
        if (c instanceof JMenu) for (Component m : ((JMenu) c).getMenuComponents()) applyTree(m);
        if (c instanceof Container) for (Component ch : ((Container) c).getComponents()) applyTree(ch);
    }

    static void installDefaults() {
        UIManager.put("ToolTip.background", c("tooltip.bg"));
        UIManager.put("ToolTip.foreground", c("tooltip.fg"));
        UIManager.put("ToolTip.font", small);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c("tooltip.border")), BorderFactory.createEmptyBorder(3, 7, 3, 7)));
        UIManager.put("ToolTip.backgroundInactive", c("tooltip.bg"));
        UIManager.put("ToolTip.foregroundInactive", c("tooltip.fg"));
    }
}

/** Marker for components that take part in Tab navigation. */
interface TabStop {}

/** Components that re-read fonts/colors from {@link Theme} when it changes. */
interface Themed { void applyTheme(); }
