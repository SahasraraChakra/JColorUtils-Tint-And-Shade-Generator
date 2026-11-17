package com.jsoftbharath.palette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Custom window title bar: app badge, title, version, minimize / maximize / close, drag + double-click. */
final class TitleBar extends JPanel implements Themed {
    static final class CaptionButton extends JButton implements Themed {
        private String icon;
        private final boolean close;
        private final int w, h;
        CaptionButton(String icon, boolean close, int w, int h) {
            this.icon = icon; this.close = close; this.w = w; this.h = h;
            setFocusable(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }
        void setIcon(String i) { icon = i; repaint(); }
        @Override public void applyTheme() { repaint(); }
        @Override public Dimension getPreferredSize() { return new Dimension(w, h); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            boolean hot = getModel().isRollover() || getModel().isPressed();
            if (hot) { g.setColor(Theme.c(close ? "titlebar.close.hover" : "titlebar.hover")); g.fillRect(0, 0, getWidth(), getHeight()); }
            Color fg = hot && close ? Theme.c("titlebar.close.fg") : Theme.c("titlebar.fg");
            Icons.paint(g, icon, (getWidth() - 14) / 2.0, (getHeight() - 14) / 2.0, 14, fg);
            g.dispose();
        }
    }

    private final WindowHost frame;
    private final CaptionButton max;

    TitleBar(WindowHost frame) {
        super(new BorderLayout());
        this.frame = frame;
        setOpaque(false);
        setPreferredSize(new Dimension(10, 34));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btns.setOpaque(false);
        CaptionButton min = new CaptionButton("min", false, 46, 34);
        max = new CaptionButton("max", false, 46, 34);
        CaptionButton close = new CaptionButton("close", true, 46, 34);
        min.addActionListener(e -> frame.minimize());
        max.addActionListener(e -> frame.toggleMaximize());
        close.addActionListener(e -> frame.close());
        btns.add(min); btns.add(max); btns.add(close);
        add(btns, BorderLayout.EAST);

        MouseAdapter ma = new MouseAdapter() {
            Point grab;
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                grab = e.getPoint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) frame.toggleMaximize();
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (grab == null) return;
                if (frame.isMaximized()) {
                    double ratio = e.getX() / (double) Math.max(1, getWidth());
                    frame.restoreForDrag(e.getLocationOnScreen(), ratio, grab.y);
                    grab = new Point((int) (ratio * frame.windowWidth()), grab.y);
                    return;
                }
                frame.moveTo(e.getLocationOnScreen(), grab);
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    void setMaximized(boolean m) { max.setIcon(m ? "restore" : "max"); }

    @Override public void applyTheme() { repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Gfx.quality(g);
        g.setColor(Theme.c("titlebar.bg"));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.c("titlebar.border"));
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
        Icons.paintAppBadge(g, 10, (getHeight() - 18) / 2, 18, Theme.c("accent"));
        g.setFont(Theme.bodyBold);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.c("titlebar.fg"));
        int x = 36;
        g.drawString(Main.APP_NAME, x, Gfx.baseline(fm, 0, getHeight()));
        x += fm.stringWidth(Main.APP_NAME) + 10;
        g.setFont(Theme.body);
        fm = g.getFontMetrics();
        g.setColor(Theme.c("titlebar.border"));
        g.drawString("|", x, Gfx.baseline(fm, 0, getHeight()));
        x += 12;
        g.setColor(Theme.c("titlebar.muted"));
        g.drawString("v" + Main.VERSION + " / " + Main.COMPANY, x, Gfx.baseline(fm, 0, getHeight()));
        g.dispose();
    }
}
