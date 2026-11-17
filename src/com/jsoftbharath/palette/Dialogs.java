package com.jsoftbharath.palette;

import com.jsoftbharath.palette.Widgets.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Themed modal dialogs with the custom title bar: confirm / message / color chooser. (c) JSoft Bharath */
final class Dialogs {
    private Dialogs() {}

    // ================================================================= base dialog
    static final class TDialog extends JDialog implements Themed {
        private final JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) { g.setColor(Theme.c("window.bg")); g.fillRect(0, 0, getWidth(), getHeight()); }
            @Override protected void paintBorder(Graphics g) { g.setColor(Theme.c("window.border")); g.drawRect(0, 0, getWidth() - 1, getHeight() - 1); }
        };
        private final JPanel bar;
        private final TLabel titleLabel;
        private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        private Component initialFocus;
        boolean result;

        TDialog(Window owner, String title) {
            super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
            setUndecorated(true);
            setIconImage(Icons.appImage(32));
            setFocusTraversalPolicy(new TabPolicy());
            root.setBorder(BorderFactory.createLineBorder(Theme.c("window.border")));
            titleLabel = new TLabel(title, Role.BOLD);
            bar = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setColor(Theme.c("titlebar.bg")); g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Theme.c("titlebar.border")); g.fillRect(0, getHeight() - 1, getWidth(), 1);
                    Icons.paintAppBadge(g, 9, (getHeight() - 16) / 2, 16, Theme.c("accent"));
                    g.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setPreferredSize(new Dimension(10, 30));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 0));
            titleLabel.setForeground(Theme.c("titlebar.fg"));
            bar.add(titleLabel, BorderLayout.CENTER);
            TitleBar.CaptionButton close = new TitleBar.CaptionButton("close", true, 40, 30);
            close.addActionListener(e -> cancel());
            bar.add(close, BorderLayout.EAST);
            MouseAdapter drag = new MouseAdapter() {
                Point start;
                @Override public void mousePressed(MouseEvent e) { start = e.getPoint(); }
                @Override public void mouseDragged(MouseEvent e) {
                    Point p = getLocation();
                    setLocation(p.x + e.getX() - start.x, p.y + e.getY() - start.y);
                }
            };
            bar.addMouseListener(drag);
            bar.addMouseMotionListener(drag);
            buttons.setOpaque(false);
            buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 14, 16));
            root.add(bar, BorderLayout.NORTH);
            root.add(buttons, BorderLayout.SOUTH);
            setContentPane(root);
            getRootPane().registerKeyboardAction(e -> cancel(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
            addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent e) { if (initialFocus != null) initialFocus.requestFocusInWindow(); }
            });
        }

        void setBody(JComponent body) { root.add(body, BorderLayout.CENTER); }

        TButton addButton(String text, Kind kind, Runnable action, boolean focus) {
            TButton b = new TButton(text, null, kind);
            b.addActionListener(e -> action.run());
            Widgets.bindKey(b, "LEFT", "prevBtn", () -> b.transferFocusBackward());
            Widgets.bindKey(b, "RIGHT", "nextBtn", () -> b.transferFocus());
            buttons.add(b);
            if (focus) initialFocus = b;
            return b;
        }

        void setInitialFocus(Component c) { initialFocus = c; }

        void cancel() { result = false; dispose(); }
        void accept() { result = true; dispose(); }

        boolean open() {
            Theme.applyTree(root);
            pack();
            setLocationRelativeTo(getOwner());
            setVisible(true);
            return result;
        }

        @Override public void applyTheme() { titleLabel.setForeground(Theme.c("titlebar.fg")); }
    }

    // ================================================================= simple dialogs
    private static JComponent messageBody(String iconName, boolean danger, String html, int width) {
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JComponent icon = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(28, 28); }
            @Override protected void paintComponent(Graphics g) {
                Icons.paint((Graphics2D) g, iconName, 0, 0, 28, Theme.c(danger ? "danger" : "accent"));
            }
        };
        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(icon, BorderLayout.NORTH);
        p.add(iconWrap, BorderLayout.WEST);
        TLabel l = new TLabel("<html><body style='width:" + width + "px'>" + html + "</body></html>", Role.BODY);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    static boolean confirm(Component owner, String title, String message, String okText, boolean danger) {
        TDialog d = new TDialog(SwingUtilities.getWindowAncestor(owner), title);
        d.setBody(messageBody(danger ? "warn" : "info", danger, esc(message), 330));
        TButton cancel = d.addButton("Cancel", Kind.NEUTRAL, d::cancel, danger);
        d.addButton(okText, danger ? Kind.DANGER : Kind.PRIMARY, d::accept, !danger);
        return d.open();
    }

    static void info(Component owner, String title, String message) {
        TDialog d = new TDialog(SwingUtilities.getWindowAncestor(owner), title);
        d.setBody(messageBody("info", false, esc(message), 330));
        d.addButton("OK", Kind.PRIMARY, d::accept, true);
        d.open();
    }

    static void error(Component owner, String title, String message) {
        TDialog d = new TDialog(SwingUtilities.getWindowAncestor(owner), title);
        d.setBody(messageBody("warn", true, esc(message), 330));
        d.addButton("OK", Kind.PRIMARY, d::accept, true);
        d.open();
    }

    static void html(Component owner, String title, String html, int width) {
        TDialog d = new TDialog(SwingUtilities.getWindowAncestor(owner), title);
        d.setBody(messageBody("info", false, html, width));
        d.addButton("Close", Kind.PRIMARY, d::accept, true);
        d.open();
    }

    // ================================================================= color chooser
    static Color chooseColor(Component owner, String title, Color initial) {
        TDialog d = new TDialog(SwingUtilities.getWindowAncestor(owner), title);
        ColorChooser cc = new ColorChooser(initial);
        d.setBody(cc);
        d.addButton("Cancel", Kind.NEUTRAL, d::cancel, false);
        d.addButton("OK", Kind.PRIMARY, d::accept, true);
        cc.onEnter(d::accept);
        d.setInitialFocus(cc.focusTarget());
        return d.open() ? cc.getColor() : null;
    }

    static final class ColorChooser extends JPanel implements Themed {
        private float h, s, v;
        private final Color original;
        private final SV sv = new SV();
        private final Hue hue = new Hue();
        private final TTextField hex = new TTextField("", 8, true);
        private final TSpinner r = new TSpinner(0, 0, 255), g = new TSpinner(0, 0, 255), b = new TSpinner(0, 0, 255);
        private boolean updating;
        private Runnable enter = () -> {};

        ColorChooser(Color initial) {
            super(new BorderLayout(16, 0));
            original = initial;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
            float[] hsb = Color.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
            h = hsb[0]; s = hsb[1]; v = hsb[2];

            JPanel left = new JPanel(new BorderLayout(0, 8));
            left.setOpaque(false);
            left.add(sv, BorderLayout.CENTER);
            left.add(hue, BorderLayout.SOUTH);
            add(left, BorderLayout.CENTER);

            JPanel right = new JPanel(new GridBagLayout());
            right.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 10, 0);
            JComponent preview = new JComponent() {
                @Override public Dimension getPreferredSize() { return new Dimension(140, 46); }
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D gg = (Graphics2D) g0.create();
                    gg.setColor(original); gg.fillRect(0, 0, getWidth() / 2, getHeight());
                    gg.setColor(getColor()); gg.fillRect(getWidth() / 2, 0, getWidth() - getWidth() / 2, getHeight());
                    gg.setColor(Theme.c("swatch.border")); gg.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                    gg.setFont(Theme.small);
                    gg.setColor(Gfx.readable(original)); gg.drawString("Old", 6, getHeight() - 7);
                    gg.setColor(Gfx.readable(getColor())); gg.drawString("New", getWidth() / 2 + 6, getHeight() - 7);
                    gg.dispose();
                }
            };
            sv.previewRef = preview;
            right.add(preview, c);
            c.gridwidth = 1; c.insets = new Insets(0, 0, 6, 8);
            addRow(right, c, 1, "Hex", wrap(hex));
            addRow(right, c, 2, "R", r);
            addRow(right, c, 3, "G", g);
            addRow(right, c, 4, "B", b);
            add(right, BorderLayout.EAST);

            hex.addActionListener(e -> applyHex());
            Widgets.bindKey(hex, "ENTER", "hexEnter", () -> { applyHex(); });
            hex.addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent e) { applyHex(); } });
            java.util.function.Consumer<Integer> rgb = x -> {
                if (updating) return;
                setColor(new Color(r.getValue(), g.getValue(), b.getValue()), false);
            };
            r.onChange(rgb::accept); g.onChange(rgb::accept); b.onChange(rgb::accept);
            syncFields();
        }

        private static JComponent wrap(TTextField f) {
            InputFrame fr = new InputFrame(new BorderLayout());
            fr.add(f, BorderLayout.CENTER);
            fr.track(f);
            fr.setPreferredSize(new Dimension(70, 28));
            return fr;
        }

        private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent comp) {
            c.gridy = row; c.gridx = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.WEST; c.weightx = 0;
            p.add(new TLabel(label, Role.MUTED), c);
            c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
            p.add(comp, c);
        }

        void onEnter(Runnable r) { enter = r; }
        Component focusTarget() { return sv; }
        Color getColor() { return Color.getHSBColor(h, s, v); }

        private void applyHex() {
            if (updating) return;
            Color c = ColorUtils.parseToken(hex.getText());
            if (c == null) { syncFields(); return; }
            setColor(c, true);
        }

        private void setColor(Color c, boolean fields) {
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            if (hsb[1] > 0 && hsb[2] > 0) h = hsb[0];
            s = hsb[1]; v = hsb[2];
            sv.invalidateImage();
            syncFields();
            repaint();
        }

        private void setHSV(float nh, float ns, float nv) {
            h = nh; s = ns; v = nv;
            sv.invalidateImage();
            syncFields();
            repaint();
        }

        private void syncFields() {
            updating = true;
            Color c = getColor();
            if (!hex.hasFocus() || true) hex.setText(ColorUtils.toHex(c, true));
            r.setValue(c.getRed()); g.setValue(c.getGreen()); b.setValue(c.getBlue());
            updating = false;
        }

        @Override public void applyTheme() { repaint(); }

        // --- saturation / value square
        final class SV extends JComponent implements TabStop {
            BufferedImage img;
            float imgHue = -1;
            JComponent previewRef;
            SV() {
                setFocusable(true);
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                Widgets.focusRepaint(this);
                MouseAdapter ma = new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); pick(e); }
                    @Override public void mouseDragged(MouseEvent e) { pick(e); }
                };
                addMouseListener(ma); addMouseMotionListener(ma);
                key("LEFT", -0.01f, 0); key("RIGHT", 0.01f, 0); key("UP", 0, 0.01f); key("DOWN", 0, -0.01f);
                key("shift LEFT", -0.05f, 0); key("shift RIGHT", 0.05f, 0); key("shift UP", 0, 0.05f); key("shift DOWN", 0, -0.05f);
                Widgets.bindKey(this, "ENTER", "ok", () -> enter.run());
            }
            private void key(String k, float ds, float dv) {
                Widgets.bindKey(this, k, k, () -> setHSV(h, Math.max(0, Math.min(1, s + ds)), Math.max(0, Math.min(1, v + dv))));
            }
            private void pick(MouseEvent e) {
                float ns = Math.max(0, Math.min(1, e.getX() / (float) Math.max(1, getWidth() - 1)));
                float nv = Math.max(0, Math.min(1, 1 - e.getY() / (float) Math.max(1, getHeight() - 1)));
                setHSV(h, ns, nv);
            }
            void invalidateImage() { imgHue = -1; }
            @Override public Dimension getPreferredSize() { return new Dimension(260, 176); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D gg = (Graphics2D) g0.create();
                Gfx.quality(gg);
                int w = getWidth(), hh = getHeight();
                if (img == null || img.getWidth() != w || img.getHeight() != hh || imgHue != h) {
                    img = new BufferedImage(Math.max(1, w), Math.max(1, hh), BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < hh; y++) for (int x = 0; x < w; x++)
                        img.setRGB(x, y, Color.HSBtoRGB(h, x / (float) Math.max(1, w - 1), 1 - y / (float) Math.max(1, hh - 1)));
                    imgHue = h;
                }
                gg.drawImage(img, 0, 0, null);
                int cx = Math.round(s * (w - 1)), cy = Math.round((1 - v) * (hh - 1));
                gg.setStroke(new BasicStroke(2f));
                gg.setColor(Color.BLACK); gg.drawOval(cx - 6, cy - 6, 12, 12);
                gg.setStroke(new BasicStroke(1.5f));
                gg.setColor(Color.WHITE); gg.drawOval(cx - 5, cy - 5, 10, 10);
                gg.setColor(Theme.c("swatch.border")); gg.setStroke(new BasicStroke(1f)); gg.drawRect(0, 0, w - 1, hh - 1);
                if (hasFocus()) Gfx.ring(gg, 0, 0, w, hh);
                gg.dispose();
            }
        }

        // --- hue slider
        final class Hue extends JComponent implements TabStop {
            Hue() {
                setFocusable(true);
                setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                Widgets.focusRepaint(this);
                MouseAdapter ma = new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); pick(e); }
                    @Override public void mouseDragged(MouseEvent e) { pick(e); }
                };
                addMouseListener(ma); addMouseMotionListener(ma);
                key("LEFT", -1); key("RIGHT", 1); key("shift LEFT", -10); key("shift RIGHT", 10);
                Widgets.bindKey(this, "HOME", "home", () -> setHSV(0, s, v));
                Widgets.bindKey(this, "END", "end", () -> setHSV(359f / 360f, s, v));
                Widgets.bindKey(this, "ENTER", "ok", () -> enter.run());
            }
            private void key(String k, int deg) {
                Widgets.bindKey(this, k, k, () -> setHSV(((h * 360f + deg) % 360f + 360f) % 360f / 360f, s, v));
            }
            private void pick(MouseEvent e) {
                setHSV(Math.max(0, Math.min(0.9999f, e.getX() / (float) Math.max(1, getWidth() - 1))), s, v);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(260, 18); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D gg = (Graphics2D) g0.create();
                int w = getWidth(), hh = getHeight();
                for (int x = 0; x < w; x++) { gg.setColor(Color.getHSBColor(x / (float) Math.max(1, w - 1), 1, 1)); gg.drawLine(x, 0, x, hh); }
                int mx = Math.round(h * (w - 1));
                gg.setColor(Color.BLACK); gg.fillRect(mx - 3, 0, 7, hh);
                gg.setColor(Color.WHITE); gg.fillRect(mx - 2, 1, 5, hh - 2);
                gg.setColor(Color.getHSBColor(h, 1, 1)); gg.fillRect(mx - 1, 2, 3, hh - 4);
                gg.setColor(Theme.c("swatch.border")); gg.drawRect(0, 0, w - 1, hh - 1);
                if (hasFocus()) Gfx.ring(gg, 0, 0, w, hh);
                gg.dispose();
            }
        }
    }
}
