package com.jsoftbharath.palette;

import com.jsoftbharath.palette.Widgets.*;

import javax.swing.*;
import java.awt.*;

/**
 * WCAG 2.1 + APCA contrast checker.
 *  - Ctrl+F on any color box = foreground, Ctrl+B = background.
 *  - While no background is chosen it is automatic (white, or black after Swap).
 *  - Once a background is chosen, contrast is always foreground vs. that background.   (c) JSoft Bharath
 */
final class ContrastPanel extends JPanel implements Themed {
    private final AppContext ctx;
    private Color fg = new Color(0x663399);
    private Color bg;                // null = automatic
    private boolean autoDark;        // automatic background: false = white, true = black
    private boolean fgUserSet;

    private final Sample textOnBg = new Sample(), bgOnText = new Sample();
    private final TLabel apca = new TLabel("", Role.MONO_SMALL);
    private final Badge normal = new Badge("", BadgeKind.PASS), large = new Badge("", BadgeKind.PASS),
        ui = new Badge("", BadgeKind.PASS), vs = new Badge("", BadgeKind.PASS);
    private final TLabel vsLabel = new TLabel("vs. Black (#000000):", Role.MUTED);
    private final TButton fgBtn, bgBtn;

    ContrastPanel(AppContext ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setPreferredSize(new Dimension(10, 24));
        head.add(Widgets.hbox(8, new TLabel("Contrast Checker", Role.TITLE), new Badge("WCAG 2.1", BadgeKind.INFO)), BorderLayout.WEST);
        head.add(apca, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.setOpaque(false);
        JPanel samples = new JPanel(new GridLayout(1, 2, 8, 0));
        samples.setOpaque(false);
        samples.add(textOnBg); samples.add(bgOnText);
        samples.setPreferredSize(new Dimension(10, 66));
        mid.add(samples, BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridLayout(4, 1, 0, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Gfx.box(g, 0, 0, getWidth(), getHeight(), Theme.c("toolbar.bg"), Theme.c("panel.border"));
                g.dispose();
            }
        };
        rows.setOpaque(false);
        rows.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        rows.add(row(new TLabel("Normal Text (<18pt):", Role.MUTED), normal));
        rows.add(row(new TLabel("Large Text (\u226518pt):", Role.MUTED), large));
        rows.add(row(new TLabel("UI Components & Icons:", Role.MUTED), ui));
        rows.add(row(vsLabel, vs));
        mid.add(rows, BorderLayout.CENTER);
        add(mid, BorderLayout.CENTER);

        TButton swap = new TButton("Swap", "swap", Kind.NEUTRAL).compact();
        fgBtn = new TButton("FG...", null, Kind.NEUTRAL).compact();
        bgBtn = new TButton("BG...", null, Kind.NEUTRAL).compact();
        TButton auto = new TButton("", "reset", Kind.NEUTRAL).compact();
        TButton copy = new TButton("Copy Matrix", "copy", Kind.ACCENT).compact();
        swap.setToolTipText("Swap: toggles the automatic background white/black (or swaps FG and BG when a background is chosen)");
        fgBtn.setToolTipText("Choose the foreground color (Ctrl+F on any color box)");
        bgBtn.setToolTipText("Choose the background color (Ctrl+B on any color box)");
        auto.setToolTipText("Reset the background to automatic (white/black)");
        swap.addActionListener(e -> swap());
        fgBtn.addActionListener(e -> { Color c = Dialogs.chooseColor(this, "Foreground color", fg); if (c != null) ctx.setFg(c); });
        bgBtn.addActionListener(e -> { Color c = Dialogs.chooseColor(this, "Background color", effectiveBg()); if (c != null) ctx.setBg(c); });
        auto.addActionListener(e -> { bg = null; update(); ctx.status.accept("Contrast background: automatic"); });
        copy.addActionListener(e -> ctx.copyText(report(), "Copied contrast report"));
        add(Widgets.hbox(6, swap, fgBtn, bgBtn, auto, copy), BorderLayout.SOUTH);
        update();
    }

    private JComponent row(JComponent l, JComponent r) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(l, BorderLayout.WEST);
        JPanel rp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        rp.setOpaque(false);
        rp.add(r);
        p.add(rp, BorderLayout.EAST);
        return p;
    }

    // ------------------------------------------------------------------ state
    void setFg(Color c) { fg = c; fgUserSet = true; update(); }
    void setBg(Color c) { bg = c; update(); }
    /** Default foreground (first generated base) - does not override an explicit choice. */
    void suggestFg(Color c) { if (!fgUserSet) { fg = c; update(); } }

    private Color effectiveBg() { return bg != null ? bg : autoDark ? Color.BLACK : Color.WHITE; }

    private void swap() {
        if (bg == null) {
            autoDark = !autoDark;
            ctx.status.accept("Contrast background: " + (autoDark ? "black" : "white"));
        } else {
            Color t = fg; fg = bg; bg = t; fgUserSet = true;
            ctx.status.accept("Swapped foreground and background");
        }
        update();
    }

    private static String grade(double ratio, boolean large) {
        String g = large ? ColorUtils.gradeLarge(ratio) : ColorUtils.gradeNormal(ratio);
        return g;
    }

    private static void setGrade(Badge b, double ratio, boolean large) {
        String g = grade(ratio, large);
        b.set(g.isEmpty() ? "Fail" : "Pass " + g, g.isEmpty() ? BadgeKind.FAIL : BadgeKind.PASS);
    }

    private void update() {
        Color b = effectiveBg();
        double ratio = ColorUtils.contrast(fg, b);
        String ratioText = String.format("%.1f:1", Math.floor(ratio * 10) / 10.0);
        textOnBg.set(b, fg, ratioText, ColorUtils.toHex(fg, true) + " on " + ColorUtils.toHex(b, true));
        bgOnText.set(fg, b, ratioText, ColorUtils.toHex(b, true) + " on " + ColorUtils.toHex(fg, true));
        apca.setText(String.format("APCA Lc %.1f", ColorUtils.apca(fg, b)));
        setGrade(normal, ratio, false);
        setGrade(large, ratio, true);
        ui.set(ratio >= 3 ? "Pass (3:1)" : "Fail", ratio >= 3 ? BadgeKind.PASS : BadgeKind.FAIL);
        Color other = b.equals(Color.BLACK) ? Color.WHITE : Color.BLACK;
        double r2 = ColorUtils.contrast(fg, other);
        vsLabel.setText("vs. " + (other == Color.BLACK ? "Black (#000000)" : "White (#ffffff)") + ":");
        String g2 = ColorUtils.gradeNormal(r2);
        vs.set((g2.isEmpty() ? "Fail " : "Pass " + g2 + " ") + String.format("%.1f:1", Math.floor(r2 * 10) / 10.0),
            g2.isEmpty() ? BadgeKind.FAIL : BadgeKind.PASS);
        fgBtn.setChip(fg);
        bgBtn.setChip(b);
        revalidate();
        repaint();
    }

    String report() {
        Color b = effectiveBg();
        double ratio = ColorUtils.contrast(fg, b);
        return "Contrast report\n"
            + "Foreground: " + ColorUtils.toHex(fg, true) + "\n"
            + "Background: " + ColorUtils.toHex(b, true) + (bg == null ? " (auto)" : "") + "\n"
            + String.format("Ratio: %.2f:1   APCA Lc: %.1f%n", ratio, ColorUtils.apca(fg, b))
            + "Normal text (<18pt): " + (grade(ratio, false).isEmpty() ? "Fail" : "Pass " + grade(ratio, false)) + "\n"
            + "Large text (>=18pt): " + (grade(ratio, true).isEmpty() ? "Fail" : "Pass " + grade(ratio, true)) + "\n"
            + "UI components & icons (3:1): " + (ratio >= 3 ? "Pass" : "Fail") + "\n"
            + String.format("vs. Black: %.2f:1   vs. White: %.2f:1", ColorUtils.contrast(fg, Color.BLACK), ColorUtils.contrast(fg, Color.WHITE));
    }

    @Override public void applyTheme() { repaint(); }

    /** One sample card: text color on a background. */
    private static final class Sample extends JComponent implements Themed {
        private Color bg = Color.WHITE, fg = Color.BLACK;
        private String ratio = "", caption = "";
        void set(Color bg, Color fg, String ratio, String caption) { this.bg = bg; this.fg = fg; this.ratio = ratio; this.caption = caption; repaint(); }
        @Override public void applyTheme() { repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Gfx.quality(g);
            int w = getWidth(), h = getHeight();
            Gfx.box(g, 0, 0, w, h, bg, Theme.c("panel.border"));
            g.setColor(fg);
            g.setFont(Theme.smallBold);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ratio, (w - fm.stringWidth(ratio)) / 2, 6 + fm.getAscent());
            g.setFont(Theme.bodyBold.deriveFont(Font.BOLD, Theme.bodyBold.getSize2D() + 3f));
            fm = g.getFontMetrics();
            String s = "Aa Sample";
            g.drawString(s, (w - fm.stringWidth(s)) / 2, h / 2 + fm.getAscent() / 2 - 1);
            g.setFont(Theme.monoSmall);
            fm = g.getFontMetrics();
            g.drawString(caption, (w - fm.stringWidth(caption)) / 2, h - 7);
            g.dispose();
        }
    }
}
