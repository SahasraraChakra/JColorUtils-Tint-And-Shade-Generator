package com.jsoftbharath.palette;

import com.jsoftbharath.palette.Widgets.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One generated palette: base color, WCAG summary, tint row and shade row. (c) JSoft Bharath */
final class PaletteCard extends JPanel implements Themed {
    private final AppContext ctx;
    final String kind, source;
    private Color base;
    private int tints, shades;

    private final SwatchRow baseBox, tintRow, shadeRow;
    private final TLabel title = new TLabel("", Role.TITLE), nameLbl = new TLabel("", Role.ACCENT), info = new TLabel("", Role.MONO_MUTED);
    private final TLabel tintsNote = new TLabel("", Role.SMALL_MUTED), shadesNote = new TLabel("", Role.SMALL_MUTED);
    private final Badge tintsCount = new Badge("", BadgeKind.PLAIN), shadesCount = new Badge("", BadgeKind.PLAIN);
    private final Badge onWhite = new Badge("", BadgeKind.PASS), onBlack = new Badge("", BadgeKind.PASS);
    private final TLabel space = new TLabel("", Role.SMALL_MUTED);

    PaletteCard(AppContext ctx, String kind, String source, Color base, int tints, int shades, Consumer<PaletteCard> onClose) {
        super(new BorderLayout(0, 10));
        this.ctx = ctx; this.kind = kind; this.source = source; this.base = base; this.tints = tints; this.shades = shades;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));

        baseBox = new SwatchRow(ctx, SwatchRow.Style.BASE);
        tintRow = new SwatchRow(ctx, SwatchRow.Style.PALETTE);
        shadeRow = new SwatchRow(ctx, SwatchRow.Style.PALETTE);
        baseBox.onDouble(i -> ctx.pin(this.base, "Base"));
        tintRow.onDouble(i -> ctx.pin(tintRow.items().get(i).color, tintRow.items().get(i).label));
        shadeRow.onDouble(i -> ctx.pin(shadeRow.items().get(i).color, shadeRow.items().get(i).label));

        // header ----- (add order = Tab order: base box, buttons, then rows)
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);
        JPanel west = new JPanel(new BorderLayout());
        west.setOpaque(false);
        west.add(baseBox, BorderLayout.NORTH);
        header.add(west, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        TButton change = new TButton("Change", "choose", Kind.ACCENT).compact();
        TButton copyAll = new TButton("Copy All", "copy", Kind.ACCENT).compact();
        TButton close = new TButton("", "close", Kind.NEUTRAL).compact();
        close.setToolTipText("Remove this palette");
        change.setToolTipText("Pick a different base color");
        change.addActionListener(e -> {
            Color c = Dialogs.chooseColor(this, "Change base color", this.base);
            if (c != null) setBase(c);
        });
        copyAll.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (Color c : allColors()) sb.append(ctx.fmt(c)).append('\n');
            ctx.copyText(sb.toString().trim(), "Copied " + allColors().size() + " colors from " + title.getText());
        });
        close.addActionListener(e -> onClose.accept(this));
        btns.add(change); btns.add(copyAll); btns.add(close);
        JPanel east = new JPanel(new BorderLayout());
        east.setOpaque(false);
        east.add(btns, BorderLayout.NORTH);
        header.add(east, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        nameLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        JPanel line1 = Widgets.hbox(8, title, nameLbl);
        line1.setAlignmentX(0f); info.setAlignmentX(0f);
        info.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        text.add(line1); text.add(info);
        center.add(text, BorderLayout.WEST);
        center.add(inspector(), BorderLayout.CENTER);
        header.add(center, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // body -----
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(section("TINTS", tintsNote, tintsCount));
        tintRow.setAlignmentX(0f); shadeRow.setAlignmentX(0f);
        body.add(tintRow);
        body.add(Box.createVerticalStrut(8));
        body.add(section("SHADES", shadesNote, shadesCount));
        body.add(shadeRow);
        add(body, BorderLayout.CENTER);

        rebuild();
        Theme.applyTree(this);
    }

    private JComponent section(String name, TLabel note, Badge count) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        p.add(Widgets.hbox(8, new TLabel(name, Role.SMALL_BOLD), note), BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        r.setOpaque(false);
        r.add(count);
        p.add(r, BorderLayout.EAST);
        p.setAlignmentX(0f);
        return p;
    }

    private JComponent inspector() {
        JPanel box = new JPanel(new GridLayout(2, 1, 0, 2)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Gfx.box(g, 0, 0, getWidth(), getHeight(), Theme.c("toolbar.bg"), Theme.c("panel.border"));
                g.dispose();
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel r1 = Widgets.hbox(6, new TLabel("WCAG:", Role.SMALL_BOLD), onWhite, onBlack);
        JPanel r2 = Widgets.hbox(6, space);
        box.add(r1); box.add(r2);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(box);
        return wrap;
    }

    // ------------------------------------------------------------------ model
    Color getBase() { return base; }
    int getTints() { return tints; }
    int getShades() { return shades; }

    void setBase(Color c) { base = c; rebuild(); }
    void setCounts(int t, int s) { tints = t; shades = s; rebuild(); }

    List<SwatchRow.Item> tintItems() { return tintRow.items(); }
    List<SwatchRow.Item> shadeItems() { return shadeRow.items(); }

    List<Color> allColors() {
        List<Color> l = new ArrayList<>();
        l.add(base);
        for (SwatchRow.Item i : tintRow.items()) l.add(i.color);
        for (SwatchRow.Item i : shadeRow.items()) l.add(i.color);
        return l;
    }

    String titleText() { return kind + (source != null ? " of " + source : "") + ": " + ColorUtils.toHex(base, true); }

    private static String pct(double f) { return Math.round(f * 100) + "%"; }

    private void rebuild() {
        List<SwatchRow.Item> t = new ArrayList<>(), s = new ArrayList<>();
        for (int i = 1; i <= tints; i++) { double f = i / (double) (tints + 1); t.add(new SwatchRow.Item(ColorUtils.tint(base, f), "+" + pct(f))); }
        for (int i = 1; i <= shades; i++) { double f = i / (double) (shades + 1); s.add(new SwatchRow.Item(ColorUtils.shade(base, f), "-" + pct(f))); }
        baseBox.setItems(List.of(new SwatchRow.Item(base, "Base")));
        tintRow.setItems(t);
        shadeRow.setItems(s);

        String hex = ColorUtils.toHex(base, true);
        title.setText(kind + ": " + hex);
        nameLbl.setText(ColorNames.describe(base) + (source != null ? "   (of " + source + ")" : ""));
        float[] hsl = ColorUtils.rgbToHsl(base);
        info.setText(String.format("rgb(%d, %d, %d)  \u2022  hsl(%d\u00B0, %d%%, %d%%)", base.getRed(), base.getGreen(), base.getBlue(),
            Math.round(hsl[0]), Math.round(hsl[1]), Math.round(hsl[2])));
        tintsNote.setText(tints == 0 ? "" : "(Lightened +" + pct(1 / (double) (tints + 1)) + " \u2192 +" + pct(tints / (double) (tints + 1)) + ")");
        shadesNote.setText(shades == 0 ? "" : "(Darkened -" + pct(1 / (double) (shades + 1)) + " \u2192 -" + pct(shades / (double) (shades + 1)) + ")");
        tintsCount.set(tints + " swatches", BadgeKind.PLAIN);
        shadesCount.set(shades + " swatches", BadgeKind.PLAIN);
        wcag(onWhite, "white", ColorUtils.contrast(base, Color.WHITE));
        wcag(onBlack, "black", ColorUtils.contrast(base, Color.BLACK));
        space.setText("Color Space: sRGB   \u2022   Luminance: " + String.format("%.1f%%", ColorUtils.luminance(base) * 100));
        revalidate();
        repaint();
    }

    static void wcag(Badge b, String name, double ratio) {
        double r = Math.floor(ratio * 10) / 10.0;
        String grade; BadgeKind k;
        if (ratio >= 7) { grade = "Pass AAA"; k = BadgeKind.PASS; }
        else if (ratio >= 4.5) { grade = "Pass AA"; k = BadgeKind.PASS; }
        else if (ratio >= 3) { grade = "AA Large"; k = BadgeKind.WARN; }
        else { grade = "Fail"; k = BadgeKind.FAIL; }
        b.set(grade + " (" + name + ": " + String.format("%.1f", r) + ":1)", k);
    }

    @Override public void applyTheme() { repaint(); }

    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Gfx.box(g, 0, 0, getWidth(), getHeight(), Theme.c("panel.bg"), Theme.c("panel.border"));
        g.dispose();
    }
}
