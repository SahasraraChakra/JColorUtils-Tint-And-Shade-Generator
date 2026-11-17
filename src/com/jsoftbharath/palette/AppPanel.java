package com.jsoftbharath.palette;

import com.jsoftbharath.palette.Widgets.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The application window: custom title bar, menus, toolbar, input / contrast / pinboard row, palette list. */
final class AppPanel extends JPanel implements Themed {
    private final AppContext ctx = new AppContext();
    private final TitleBar titleBar;
    private final TMenuBar menuBar = new TMenuBar();
    private final TTextArea input = new TTextArea();
    private final TLabel validLbl = new TLabel("", Role.SMALL_MUTED);
    private final TCheck cComp = new TCheck("Comp."), cSplit = new TCheck("Split-Comp."), cAna = new TCheck("Analogous"), cTri = new TCheck("Triadic");
    private final TSpinner tintSp = new TSpinner(10, 0, 30), shadeSp = new TSpinner(10, 0, 30);
    private final TCombo<CopyFormat> formatBox = new TCombo<>(List.of(CopyFormat.values()), 1);
    private final ContrastPanel contrast = new ContrastPanel(ctx);
    private final PinboardPanel pinboard = new PinboardPanel(ctx);
    private final ListPanel list = new ListPanel();
    private final TLabel status = new TLabel("Ready", Role.BODY);
    private final TLabel hints = new TLabel("Tab: move   Enter: copy   Ctrl+Enter: pin   Ctrl+F / Ctrl+B: contrast fg / bg", Role.SMALL_MUTED);
    private final List<PaletteCard> cards = new ArrayList<>();
    private final TCheckItem lightItem = new TCheckItem("Light theme"), darkItem = new TCheckItem("Dark theme"), customItem = new TCheckItem("Custom theme file");
    private final WindowHost host;

    AppPanel(WindowHost host) {
        super(new BorderLayout());
        this.host = host;
        setOpaque(false);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setFocusTraversalPolicy(new TabPolicy());
        setFocusCycleRoot(true);

        ctx.root = this;
        ctx.status = this::setStatus;
        ctx.pinHandler = pinboard::pin;
        ctx.fgHandler = contrast::setFg;
        ctx.bgHandler = contrast::setBg;
        ctx.toInputHandler = c -> { appendInput(ColorUtils.toHex(c, true)); setStatus("Added " + ColorUtils.toHex(c, true) + " to the base colors input"); input.requestFocusInWindow(); };
        ctx.pinBaseSupplier = () -> { List<Color> v = parseInput(null); return v.isEmpty() ? new Color(0x663399) : v.get(0); };

        titleBar = new TitleBar(host);
        buildMenus();

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        titleBar.setAlignmentX(0f); menuBar.setAlignmentX(0f);
        north.add(titleBar);
        north.add(menuBar);
        JComponent toolbar = buildToolbar();
        toolbar.setAlignmentX(0f);
        north.add(toolbar);
        add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        center.add(buildTopRow(), BorderLayout.NORTH);
        TScrollPane sp = new TScrollPane(list);
        center.add(sp, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        formatBox.onChange(f -> { ctx.format = f; Main.PREFS.put("format", f.name()); repaint(); setStatus("Copy format: " + f.label); });
        try { CopyFormat f = CopyFormat.valueOf(Main.PREFS.get("format", "HEX_HASH")); ctx.format = f; formatBox.select(f); } catch (Exception ignored) {}
        tintSp.onChange(v -> applyCounts());
        shadeSp.onChange(v -> applyCounts());
        input.setText("663399");
        input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { inputChanged(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { inputChanged(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { inputChanged(); }
        });
        Widgets.bindKey(input, "ctrl ENTER", "gen", this::generate);
        Theme.addListener(() -> SwingUtilities.invokeLater(this::reTheme));
        inputChanged();
        Theme.applyTree(this);
        updateThemeChecks();
        generate();
    }

    void startFocus() { input.requestFocusInWindow(); }

    // ================================================================== layout pieces
    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.c("toolbar.bg")); g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.c("toolbar.border")); g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        TButton nw = new TButton("New", "new", Kind.NEUTRAL), imp = new TButton("Import...", "import", Kind.NEUTRAL),
            exp = new TButton("Export...", "export", Kind.NEUTRAL), rnd = new TButton("Randomize", "shuffle", Kind.NEUTRAL),
            clr = new TButton("Clear All Generated", "trash", Kind.DANGER);
        nw.addActionListener(e -> newSession());
        imp.addActionListener(e -> importPalettes());
        exp.addActionListener(e -> exportPalettes());
        rnd.addActionListener(e -> randomize());
        clr.addActionListener(e -> clearGenerated());
        left.add(nw); left.add(imp); left.add(exp); left.add(sep()); left.add(rnd); left.add(clr);
        bar.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(new TLabel("Tints:", Role.BODY)); right.add(tintSp);
        right.add(new TLabel("Shades:", Role.BODY)); right.add(shadeSp);
        right.add(new TLabel("Format:", Role.BODY)); right.add(formatBox);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JComponent sep() {
        JComponent s = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(7, 24); }
            @Override protected void paintComponent(Graphics g) { g.setColor(Theme.c("toolbar.separator")); g.fillRect(3, 2, 1, getHeight() - 4); }
        };
        return s;
    }

    private JComponent buildTopRow() {
        JPanel row = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Gfx.box(g, 0, 0, getWidth(), getHeight(), Theme.c("panel.bg"), Theme.c("panel.border"));
                g.setColor(Theme.c("panel.divider"));
                g.fillRect(inputW, 1, 1, getHeight() - 2);
                g.fillRect(inputW + contrastW + 1, 1, 1, getHeight() - 2);
                g.dispose();
            }
            final int inputW = 296, contrastW = 372;
        };
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        c.gridx = 0; c.weightx = 0; row.add(buildInputPanel(), c);
        c.gridx = 1; row.add(contrast, c);
        c.gridx = 2; c.weightx = 1; row.add(pinboard, c);
        row.setPreferredSize(new Dimension(10, 252));
        contrast.setPreferredSize(new Dimension(372, 252));
        return row;
    }

    private JComponent buildInputPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        p.setPreferredSize(new Dimension(296, 252));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setPreferredSize(new Dimension(10, 24));
        head.add(new TLabel("Base color(s)", Role.TITLE), BorderLayout.WEST);
        head.add(validLbl, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setOpaque(false);
        InputFrame box = Widgets.textAreaBox(input);
        box.setPreferredSize(new Dimension(10, 58));
        input.placeholder = "hex, rgb(), hsl() \u2013 separate with comma or new line";
        mid.add(box, BorderLayout.NORTH);

        TButton paste = new TButton("Paste", "paste", Kind.ACCENT).compact();
        TButton choose = new TButton("Choose...", "choose", Kind.ACCENT).compact();
        TButton pick = new TButton("Pick...", "pick", Kind.ACCENT).compact();
        TButton clearIn = new TButton("", "trash", Kind.DANGER).compact();
        clearIn.setToolTipText("Clear the input");
        paste.addActionListener(e -> pasteIntoInput());
        choose.addActionListener(e -> chooseIntoInput());
        pick.addActionListener(e -> pickIntoInput());
        clearIn.addActionListener(e -> clearInput());
        JPanel btnRow = Widgets.hbox(5, paste, choose, pick, clearIn);
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setOpaque(false);
        btnWrap.add(btnRow, BorderLayout.NORTH);
        mid.add(btnWrap, BorderLayout.CENTER);

        JPanel harm = new JPanel(new BorderLayout(0, 2));
        harm.setOpaque(false);
        harm.add(new TLabel("HARMONIES", Role.SMALL_BOLD), BorderLayout.NORTH);
        JPanel hg = new JPanel(new GridLayout(2, 2, 4, 0));
        hg.setOpaque(false);
        hg.add(cComp); hg.add(cSplit); hg.add(cAna); hg.add(cTri);
        harm.add(hg, BorderLayout.CENTER);
        harm.setPreferredSize(new Dimension(10, 62));
        mid.add(harm, BorderLayout.SOUTH);
        p.add(mid, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        TButton copyGen = new TButton("Copy All Generated", "copy", Kind.NEUTRAL);
        TButton gen = new TButton("Generate", "sparkle", Kind.PRIMARY);
        gen.setToolTipText("Generate palettes (F5 or Ctrl+Enter in the input)");
        copyGen.addActionListener(e -> copyAllGenerated());
        gen.addActionListener(e -> generate());
        foot.add(copyGen, BorderLayout.WEST);
        foot.add(gen, BorderLayout.EAST);
        p.add(foot, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.c("statusbar.bg")); g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.c("statusbar.border")); g.fillRect(0, 0, getWidth(), 1);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 12, 5, 12));
        status.setForeground(Theme.c("statusbar.fg"));
        bar.add(status, BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        r.setOpaque(false);
        r.add(hints);
        r.add(new TLabel("\u00A9 " + Main.COMPANY, Role.SMALL_MUTED));
        bar.add(r, BorderLayout.EAST);
        return bar;
    }

    // ================================================================== menus
    private static KeyStroke ks(String s) { return KeyStroke.getKeyStroke(s); }

    private void buildMenus() {
        TMenu file = new TMenu("File", KeyEvent.VK_F);
        file.add("New", KeyEvent.VK_N, ks("ctrl N"), this::newSession);
        file.add("Import palettes...", KeyEvent.VK_I, ks("ctrl O"), this::importPalettes);
        file.add("Export palettes...", KeyEvent.VK_E, ks("ctrl S"), this::exportPalettes);
        file.sep();
        file.add("Exit", KeyEvent.VK_X, ks("ctrl Q"), host::close);

        TMenu edit = new TMenu("Edit", KeyEvent.VK_E);
        edit.add("Paste colors into input", KeyEvent.VK_P, ks("ctrl shift V"), this::pasteIntoInput);
        edit.add("Clear input...", KeyEvent.VK_C, null, this::clearInput);
        edit.sep();
        edit.add("Copy all generated", KeyEvent.VK_G, ks("ctrl shift C"), this::copyAllGenerated);
        edit.add("Copy all pinned", KeyEvent.VK_A, null, pinboard::copyAll);

        TMenu pal = new TMenu("Palette", KeyEvent.VK_P);
        pal.add("Generate", KeyEvent.VK_G, ks("F5"), this::generate);
        pal.add("Randomize", KeyEvent.VK_R, ks("ctrl R"), this::randomize);
        pal.sep();
        pal.add("Clear all generated...", KeyEvent.VK_L, ks("ctrl shift DELETE"), this::clearGenerated);
        pal.add("Clear pinboard...", KeyEvent.VK_B, null, pinboard::clearAll);

        TMenu view = new TMenu("View", KeyEvent.VK_V);
        lightItem.addActionListener(e -> chooseTheme(Theme.LIGHT));
        darkItem.addActionListener(e -> chooseTheme(Theme.DARK));
        customItem.addActionListener(e -> chooseTheme(Theme.CUSTOM));
        view.add(lightItem); view.add(darkItem); view.add(customItem);
        view.sep();
        view.add("Edit theme file...", KeyEvent.VK_T, null, this::editThemeFile);
        view.add("Reload theme", KeyEvent.VK_R, ks("ctrl shift R"), () -> { Theme.use(Theme.active()); setStatus("Theme reloaded"); });

        TMenu tools = new TMenu("Tools", KeyEvent.VK_T);
        tools.add("Screen color picker", KeyEvent.VK_S, ks("ctrl shift P"), this::pickIntoInput);
        tools.add("Choose color...", KeyEvent.VK_C, ks("ctrl shift K"), this::chooseIntoInput);

        TMenu window = new TMenu("Window", KeyEvent.VK_W);
        window.add("Minimize", KeyEvent.VK_N, null, host::minimize);
        window.add("Maximize / Restore", KeyEvent.VK_M, ks("F11"), host::toggleMaximize);

        TMenu help = new TMenu("Help", KeyEvent.VK_H);
        help.add("Keyboard shortcuts", KeyEvent.VK_K, ks("F1"), this::showShortcuts);
        help.add("About", KeyEvent.VK_A, null, this::showAbout);

        for (TMenu m : new TMenu[]{file, edit, pal, view, tools, window, help}) menuBar.add(m);
    }

    private void updateThemeChecks() {
        lightItem.setSelected(Theme.LIGHT.equals(Theme.active()));
        darkItem.setSelected(Theme.DARK.equals(Theme.active()));
        customItem.setSelected(Theme.CUSTOM.equals(Theme.active()));
    }

    private void chooseTheme(String t) {
        if (Theme.CUSTOM.equals(t) && Theme.findCustomFile() == null) {
            setStatus("No theme.properties found - use View > Edit theme file... to create one");
            updateThemeChecks();
            return;
        }
        Theme.use(t);
        if (!Theme.CUSTOM.equals(t)) Main.PREFS.put("theme", t);
        updateThemeChecks();
        setStatus("Theme: " + t);
    }

    private void editThemeFile() {
        try {
            File f = Theme.ensureCustomFile();
            Theme.use(Theme.CUSTOM);
            updateThemeChecks();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop().open(f);
            setStatus("Editing " + f + " \u2013 changes apply when you save");
        } catch (Exception ex) {
            Dialogs.error(this, "Theme file", "Could not create or open the theme file.\n" + ex.getMessage());
        }
    }

    private void showShortcuts() {
        String h = "<b>Navigation</b><br>Tab / Shift+Tab \u2013 buttons, color rows, next palette<br>Arrow keys \u2013 move between color boxes of a row<br>F10 or Alt+letter \u2013 menus<br><br>"
            + "<b>On a color box</b> (palettes and pinboard)<br>Enter \u2013 copy &nbsp;&nbsp; Ctrl+Enter \u2013 pin<br>Ctrl+F \u2013 contrast foreground &nbsp;&nbsp; Ctrl+B \u2013 contrast background<br>Del \u2013 remove (pinboard) &nbsp;&nbsp; Menu key / Shift+F10 \u2013 context menu<br><br>"
            + "<b>Global</b><br>F5 generate &nbsp; Ctrl+R randomize &nbsp; Ctrl+N new<br>Ctrl+O import &nbsp; Ctrl+S export &nbsp; F11 maximize<br>Ctrl+Shift+R reload theme &nbsp; Esc closes dialogs";
        Dialogs.html(this, "Keyboard shortcuts", h, 400);
    }

    private void showAbout() {
        Dialogs.html(this, "About", "<b>" + Dialogs.esc(Main.APP_NAME) + "</b><br>Version " + Main.VERSION + "<br><br>Generate tints, shades and harmonies, check WCAG / APCA contrast, collect favourites on a pinboard.<br><br>\u00A9 " + Main.COMPANY + ". All rights reserved.<br><br>Fonts: " + Theme.bodyFamily() + " / " + Theme.monoFamily(), 340);
    }

    // ================================================================== input handling
    private void appendInput(String text) {
        String cur = input.getText();
        input.setText(cur.trim().isEmpty() ? text : cur.stripTrailing() + "\n" + text);
    }

    private void pasteIntoInput() {
        try {
            String t = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            if (t == null || t.isBlank()) { setStatus("Clipboard has no text"); return; }
            appendInput(t.trim());
            setStatus("Pasted from clipboard");
        } catch (Exception e) { setStatus("Clipboard has no text"); }
        input.requestFocusInWindow();
    }

    private void chooseIntoInput() {
        List<Color> cur = parseInput(null);
        Color c = Dialogs.chooseColor(this, "Choose a base color", cur.isEmpty() ? new Color(0x663399) : cur.get(0));
        if (c != null) { appendInput(ColorUtils.toHex(c, true)); setStatus("Added " + ColorUtils.toHex(c, true)); }
    }

    private void pickIntoInput() {
        ScreenPicker.pick(this, c -> { appendInput(ColorUtils.toHex(c, true)); setStatus("Picked " + ColorUtils.toHex(c, true) + " from the screen"); });
    }

    private void clearInput() {
        if (input.getText().isBlank()) { setStatus("Input is already empty"); return; }
        if (Dialogs.confirm(this, "Clear input", "Clear all base colors from the input box?", "Clear", true)) {
            input.setText("");
            input.requestFocusInWindow();
        }
    }

    /** Parses the input; invalid tokens are appended to {@code invalid} when it is not null. */
    private List<Color> parseInput(List<String> invalid) {
        List<Color> out = new ArrayList<>();
        for (String tok : ColorUtils.tokenize(input.getText())) {
            Color c = ColorUtils.parseToken(tok);
            if (c == null) { if (invalid != null) invalid.add(tok); }
            else if (!out.contains(c)) out.add(c);
        }
        return out;
    }

    private void inputChanged() {
        List<String> bad = new ArrayList<>();
        List<Color> ok = parseInput(bad);
        String t = ok.size() + (ok.size() == 1 ? " valid color" : " valid colors");
        if (!bad.isEmpty()) t += ", " + bad.size() + " invalid";
        validLbl.setText(t);
        validLbl.setForeground(Theme.c(bad.isEmpty() ? "text.muted" : "danger"));
        Color b = ok.isEmpty() ? null : ok.get(0);
        cComp.setDot(b == null ? null : ColorUtils.rotateHue(b, 180));
        cSplit.setDot(b == null ? null : ColorUtils.rotateHue(b, 150));
        cAna.setDot(b == null ? null : ColorUtils.rotateHue(b, 30));
        cTri.setDot(b == null ? null : ColorUtils.rotateHue(b, 120));
    }

    // ================================================================== palettes
    private void addCard(String kind, String source, Color base, int t, int s) {
        PaletteCard card = new PaletteCard(ctx, kind, source, base, t, s, this::removeCard);
        cards.add(card);
        list.add(card);
        list.add(Box.createVerticalStrut(10));
    }

    private void removeCard(PaletteCard card) {
        int i = list.getComponentZOrder(card);
        cards.remove(card);
        list.remove(card);
        if (i < list.getComponentCount()) list.remove(i);
        list.revalidate(); list.repaint();
        setStatus("Removed palette " + ColorUtils.toHex(card.getBase(), true));
    }

    private void generate() {
        List<String> bad = new ArrayList<>();
        List<Color> bases = parseInput(bad);
        if (bases.isEmpty()) {
            Dialogs.error(this, "Nothing to generate", bad.isEmpty() ? "Enter at least one base color, e.g. 663399." : "No valid colors found. Invalid: " + String.join(", ", bad));
            return;
        }
        list.removeAll();
        cards.clear();
        int t = tintSp.getValue(), s = shadeSp.getValue();
        for (Color b : bases) {
            String hex = ColorUtils.toHex(b, true);
            addCard("Base", null, b, t, s);
            if (cComp.isSelected()) addCard("Complementary", hex, ColorUtils.rotateHue(b, 180), t, s);
            if (cSplit.isSelected()) { addCard("Split-Complementary", hex, ColorUtils.rotateHue(b, 150), t, s); addCard("Split-Complementary", hex, ColorUtils.rotateHue(b, 210), t, s); }
            if (cAna.isSelected()) { addCard("Analogous", hex, ColorUtils.rotateHue(b, -30), t, s); addCard("Analogous", hex, ColorUtils.rotateHue(b, 30), t, s); }
            if (cTri.isSelected()) { addCard("Triadic", hex, ColorUtils.rotateHue(b, 120), t, s); addCard("Triadic", hex, ColorUtils.rotateHue(b, 240), t, s); }
        }
        Theme.applyTree(list);
        list.revalidate(); list.repaint();
        contrast.suggestFg(bases.get(0));
        setStatus("Generated " + cards.size() + (cards.size() == 1 ? " palette" : " palettes")
            + (bad.isEmpty() ? "" : "  \u2022  skipped invalid: " + String.join(", ", bad)));
    }

    private void applyCounts() {
        for (PaletteCard c : cards) c.setCounts(tintSp.getValue(), shadeSp.getValue());
        list.revalidate(); list.repaint();
    }

    private void randomize() {
        Color c = ColorUtils.random();
        input.setText(ColorUtils.toHex(c, true));
        generate();
        setStatus("Random base color " + ColorUtils.toHex(c, true));
    }

    private void clearGenerated() {
        if (cards.isEmpty()) { setStatus("There are no generated palettes"); return; }
        if (!Dialogs.confirm(this, "Clear all generated", "Remove all " + cards.size() + " generated palettes?\nThe pinboard is not affected.", "Clear all", true)) return;
        list.removeAll(); cards.clear(); list.revalidate(); list.repaint();
        setStatus("Cleared all generated palettes");
    }

    private void newSession() {
        if (!Dialogs.confirm(this, "New session", "Start a new session? This clears the base colors and all generated palettes.\nThe pinboard is kept.", "Start new", true)) return;
        input.setText("");
        list.removeAll(); cards.clear(); list.revalidate(); list.repaint();
        input.requestFocusInWindow();
        setStatus("New session");
    }

    private void copyAllGenerated() {
        if (cards.isEmpty()) { setStatus("There are no generated palettes"); return; }
        StringBuilder sb = new StringBuilder();
        for (PaletteCard c : cards) {
            sb.append("# ").append(c.titleText()).append('\n');
            for (Color col : c.allColors()) sb.append(ctx.fmt(col)).append('\n');
            sb.append('\n');
        }
        ctx.copyText(sb.toString().trim(), "Copied " + cards.size() + " palettes");
    }

    // ================================================================== import / export
    private void exportPalettes() {
        if (cards.isEmpty()) { setStatus("Generate something first \u2013 nothing to export"); return; }
        File f = FileIO.save(this, "Export palettes (.txt  .json  .css)", "palettes.txt", "txt");
        if (f == null) return;
        try {
            Files.writeString(f.toPath(), PaletteIO.write(cards, ctx.format, FileIO.ext(f)), StandardCharsets.UTF_8);
            setStatus("Exported " + cards.size() + " palettes to " + f.getName());
        } catch (Exception ex) { Dialogs.error(this, "Export failed", ex.getMessage()); }
    }

    private void importPalettes() {
        File f = FileIO.open(this, "Import palettes (.txt  .json)");
        if (f == null) return;
        try {
            List<PaletteIO.Entry> es = PaletteIO.read(Files.readString(f.toPath(), StandardCharsets.UTF_8));
            if (es.isEmpty()) { Dialogs.error(this, "Import", "No colors were found in " + f.getName()); return; }
            list.removeAll(); cards.clear();
            StringBuilder in = new StringBuilder();
            for (PaletteIO.Entry e : es) {
                addCard(e.kind, e.source, e.base, e.tints, e.shades);
                if (e.kind.equals("Base")) in.append(ColorUtils.toHex(e.base, true)).append('\n');
            }
            if (in.length() > 0) input.setText(in.toString().trim());
            Theme.applyTree(list);
            list.revalidate(); list.repaint();
            setStatus("Imported " + es.size() + " palettes from " + f.getName());
        } catch (Exception ex) { Dialogs.error(this, "Import failed", ex.getMessage()); }
    }

    // ================================================================== status / theme
    void setStatus(String s) { status.setText(s); }

    private void reTheme() {
        Theme.applyTree(this);
        status.setForeground(Theme.c("statusbar.fg"));
        inputChanged();
        updateThemeChecks();
        revalidate();
        repaint();
    }

    @Override public void applyTheme() { repaint(); }

    TitleBar titleBar() { return titleBar; }

    @Override protected void paintComponent(Graphics g) { g.setColor(Theme.c("window.bg")); g.fillRect(0, 0, getWidth(), getHeight()); }
    @Override protected void paintBorder(Graphics g) { g.setColor(Theme.c("window.border")); g.drawRect(0, 0, getWidth() - 1, getHeight() - 1); }

    // ================================================================== scrolling list of palettes
    private static final class ListPanel extends JPanel implements Scrollable {
        ListPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 8));
        }
        @Override public Component add(Component c) {
            if (c instanceof JComponent) ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            return super.add(c);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(40, r.height - 40); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
