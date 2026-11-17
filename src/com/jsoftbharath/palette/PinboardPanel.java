package com.jsoftbharath.palette;

import com.jsoftbharath.palette.Widgets.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** The custom palette pinboard. (c) JSoft Bharath */
final class PinboardPanel extends JPanel implements Themed {
    private final AppContext ctx;
    private final SwatchRow row;
    private final Badge count = new Badge("0 pinned", BadgeKind.INFO);
    private final List<SwatchRow.Item> pinned = new ArrayList<>();

    PinboardPanel(AppContext ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setPreferredSize(new Dimension(10, 24));
        head.add(Widgets.hbox(8, new TLabel("Custom Palette Pinboard", Role.TITLE), count), BorderLayout.WEST);
        TButton imp = new TButton("Import", null, Kind.ACCENT).compact();
        TButton exp = new TButton("Export", null, Kind.ACCENT).compact();
        imp.addActionListener(e -> importPins());
        exp.addActionListener(e -> exportPins());
        head.add(Widgets.hbox(6, new TLabel("Ctrl+Enter / double-click to pin", Role.SMALL_MUTED), imp, exp), BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        row = new SwatchRow(ctx, SwatchRow.Style.PIN);
        row.setEmptyText("Nothing pinned yet \u2013 focus a color box and press Ctrl+Enter, or double-click it.");
        row.onRemove(i -> deleteSelected());
        row.onDouble(i -> ctx.toInputHandler.accept(pinned.get(i).color));
        TScrollPane sp = new TScrollPane(row);
        InputFrame frame = new InputFrame(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Gfx.box(g, 0, 0, getWidth(), getHeight(), Theme.c("toolbar.bg"), Theme.c("panel.border"));
                g.dispose();
            }
        };
        frame.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        frame.add(sp, BorderLayout.CENTER);
        add(frame, BorderLayout.CENTER);
        // Tab order: pinboard colors first, then the buttons below (row was added before them)

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        TButton copy = new TButton("Copy", "copy", Kind.NEUTRAL).compact();
        TButton copyAll = new TButton("Copy All Pinned", null, Kind.NEUTRAL).compact();
        copy.addActionListener(e -> { SwatchRow.Item it = row.activeItem(); if (it != null) ctx.copy(it.color, it.label); else ctx.status.accept("Nothing pinned"); });
        copyAll.addActionListener(e -> copyAll());
        TButton choose = new TButton("Choose...", "choose", Kind.ACCENT).compact();
        TButton picker = new TButton("Picker", "pick", Kind.ACCENT).compact();
        TButton delete = new TButton("Delete", "trash", Kind.NEUTRAL).compact();
        TButton clear = new TButton("Clear", "trash", Kind.DANGER).compact();
        choose.setToolTipText("Choose a color and pin it");
        picker.setToolTipText("Pick a color from the screen and pin it");
        delete.setToolTipText("Delete the selected pinned color (Del)");
        choose.addActionListener(e -> { Color c = Dialogs.chooseColor(this, "Choose a color to pin", ctx.pinBase()); if (c != null) ctx.pin(c, "Chosen"); });
        picker.addActionListener(e -> ScreenPicker.pick(this, c -> ctx.pin(c, "Picked")));
        delete.addActionListener(e -> deleteSelected());
        clear.addActionListener(e -> clearAll());
        foot.add(Widgets.hbox(6, copy, copyAll), BorderLayout.WEST);
        foot.add(Widgets.hbox(6, choose, picker, delete, clear), BorderLayout.EAST);
        add(foot, BorderLayout.SOUTH);
        refresh();
    }

    boolean pin(Color c, String label) {
        for (SwatchRow.Item i : pinned) if (i.color.equals(c)) return false;
        pinned.add(new SwatchRow.Item(c, label));
        refresh();
        row.setActive(pinned.size() - 1);
        return true;
    }

    List<Color> colors() {
        List<Color> l = new ArrayList<>();
        for (SwatchRow.Item i : pinned) l.add(i.color);
        return l;
    }

    int count() { return pinned.size(); }

    private void refresh() {
        row.setItems(pinned);
        count.set(pinned.size() + " pinned", BadgeKind.INFO);
    }

    void copyAll() {
        if (pinned.isEmpty()) { ctx.status.accept("Nothing pinned"); return; }
        StringBuilder sb = new StringBuilder();
        for (SwatchRow.Item i : pinned) sb.append(ctx.fmt(i.color)).append('\n');
        ctx.copyText(sb.toString().trim(), "Copied " + pinned.size() + " pinned colors");
    }

    void deleteSelected() {
        SwatchRow.Item it = row.activeItem();
        if (it == null) { ctx.status.accept("Nothing to delete"); return; }
        if (!Dialogs.confirm(this, "Delete pinned color", "Remove " + ColorUtils.toHex(it.color, true) + " from the pinboard?", "Delete", true)) return;
        int i = row.getActive();
        pinned.remove(i);
        refresh();
        row.setActive(Math.min(i, pinned.size() - 1));
        ctx.status.accept("Removed " + ColorUtils.toHex(it.color, true) + " from the pinboard");
        row.requestFocusInWindow();
    }

    void clearAll() {
        if (pinned.isEmpty()) { ctx.status.accept("Pinboard is already empty"); return; }
        if (!Dialogs.confirm(this, "Clear pinboard", "Remove all " + pinned.size() + " pinned colors from the pinboard?", "Clear", true)) return;
        pinned.clear();
        refresh();
        ctx.status.accept("Pinboard cleared");
    }

    void exportPins() {
        if (pinned.isEmpty()) { ctx.status.accept("Nothing pinned to export"); return; }
        File f = FileIO.save(this, "Export pinboard", "pinboard.txt", "txt");
        if (f == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            for (SwatchRow.Item i : pinned) sb.append(ctx.fmt(i.color)).append(i.label != null ? "   # " + i.label : "").append('\n');
            Files.writeString(f.toPath(), sb.toString(), StandardCharsets.UTF_8);
            ctx.status.accept("Exported " + pinned.size() + " pinned colors to " + f.getName());
        } catch (Exception ex) { Dialogs.error(this, "Export failed", ex.getMessage()); }
    }

    void importPins() {
        File f = FileIO.open(this, "Import pinboard colors");
        if (f == null) return;
        try {
            String text = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            int added = 0;
            for (String line : text.split("\\R")) {
                String l = line.contains("#") && !line.trim().startsWith("#") && line.indexOf('#') > 0 && line.matches(".*\\s#\\s.*") ? line.substring(0, line.indexOf("   #")) : line;
                for (String tok : ColorUtils.tokenize(l)) {
                    Color c = ColorUtils.parseToken(tok);
                    if (c != null && pin(c, "Imported")) added++;
                }
            }
            ctx.status.accept("Imported " + added + " colors into the pinboard");
        } catch (Exception ex) { Dialogs.error(this, "Import failed", ex.getMessage()); }
    }

    @Override public void applyTheme() { repaint(); }
}
