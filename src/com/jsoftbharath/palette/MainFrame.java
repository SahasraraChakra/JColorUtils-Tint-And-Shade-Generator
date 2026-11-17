package com.jsoftbharath.palette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** What the title bar needs from the window that hosts it. */
interface WindowHost {
    void minimize();
    void toggleMaximize();
    void close();
    boolean isMaximized();
    int windowWidth();
    void moveTo(Point screenPoint, Point grabOffset);
    void restoreForDrag(Point screen, double ratio, int grabY);
}

/** The undecorated top-level window: hosts {@link AppPanel}, provides move / resize / maximize. (c) JSoft Bharath */
final class MainFrame extends JFrame implements WindowHost {
    private final AppPanel panel;
    private Rectangle normalBounds;
    private boolean maximized;

    MainFrame() {
        super(Main.APP_NAME);
        setUndecorated(true);
        setIconImage(Icons.appImage(64));
        setMinimumSize(new Dimension(1180, 640));
        setSize(1260, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        panel = new AppPanel(this);
        setContentPane(panel);
        installResizeGlass();
        try {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE))
                Taskbar.getTaskbar().setIconImage(Icons.appImage(128));
        } catch (Exception ignored) {}
    }

    void startFocus() { panel.startFocus(); }

    @Override public void minimize() { setState(Frame.ICONIFIED); }
    @Override public void close() { dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)); }
    @Override public boolean isMaximized() { return maximized; }
    @Override public int windowWidth() { return getWidth(); }
    @Override public void moveTo(Point p, Point grab) { setLocation(p.x - grab.x - 1, p.y - grab.y - 1); }

    @Override public void toggleMaximize() {
        if (maximized) {
            if (normalBounds != null) setBounds(normalBounds);
            maximized = false;
        } else {
            normalBounds = getBounds();
            GraphicsConfiguration gc = getGraphicsConfiguration();
            Rectangle b = gc.getBounds();
            Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            setBounds(b.x + in.left, b.y + in.top, b.width - in.left - in.right, b.height - in.top - in.bottom);
            maximized = true;
        }
        panel.titleBar().setMaximized(maximized);
        revalidate();
        repaint();
    }

    @Override public void restoreForDrag(Point screen, double ratio, int grabY) {
        if (normalBounds == null) normalBounds = new Rectangle(100, 100, 1240, 760);
        maximized = false;
        panel.titleBar().setMaximized(false);
        setSize(normalBounds.width, normalBounds.height);
        setLocation(screen.x - (int) (ratio * normalBounds.width), screen.y - grabY);
    }

    /** Edge / corner resizing for the undecorated window: a glass pane that only "exists" over the border. */
    private void installResizeGlass() {
        final int E = 5, C = 12;
        JPanel glass = new JPanel() {
            @Override public boolean contains(int x, int y) {
                if (maximized) return false;
                return x < E || y < E || x >= getWidth() - E || y >= getHeight() - E;
            }
        };
        glass.setOpaque(false);
        glass.setLayout(null);
        MouseAdapter ma = new MouseAdapter() {
            int dir; Point start; Rectangle startB;
            int dirAt(Point p) {
                int w = getWidth(), h = getHeight();
                boolean l = p.x < E, r = p.x >= w - E, t = p.y < E, b = p.y >= h - E;
                if (p.x < C && p.y < E || p.x < E && p.y < C) { l = true; t = true; }
                if (p.x >= w - C && p.y < E || p.x >= w - E && p.y < C) { r = true; t = true; }
                if (p.x < C && p.y >= h - E || p.x < E && p.y >= h - C) { l = true; b = true; }
                if (p.x >= w - C && p.y >= h - E || p.x >= w - E && p.y >= h - C) { r = true; b = true; }
                return (l ? 1 : 0) | (r ? 2 : 0) | (t ? 4 : 0) | (b ? 8 : 0);
            }
            @Override public void mouseMoved(MouseEvent e) {
                int cur;
                switch (dirAt(e.getPoint())) {
                    case 1: cur = Cursor.W_RESIZE_CURSOR; break; case 2: cur = Cursor.E_RESIZE_CURSOR; break;
                    case 4: cur = Cursor.N_RESIZE_CURSOR; break; case 8: cur = Cursor.S_RESIZE_CURSOR; break;
                    case 5: cur = Cursor.NW_RESIZE_CURSOR; break; case 6: cur = Cursor.NE_RESIZE_CURSOR; break;
                    case 9: cur = Cursor.SW_RESIZE_CURSOR; break; case 10: cur = Cursor.SE_RESIZE_CURSOR; break;
                    default: cur = Cursor.DEFAULT_CURSOR;
                }
                glass.setCursor(Cursor.getPredefinedCursor(cur));
            }
            @Override public void mousePressed(MouseEvent e) { dir = dirAt(e.getPoint()); start = e.getLocationOnScreen(); startB = getBounds(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (dir == 0) return;
                Point p = e.getLocationOnScreen();
                int dx = p.x - start.x, dy = p.y - start.y;
                Rectangle b = new Rectangle(startB);
                Dimension min = getMinimumSize();
                if ((dir & 1) != 0) { b.x = startB.x + dx; b.width = startB.width - dx; }
                if ((dir & 2) != 0) b.width = startB.width + dx;
                if ((dir & 4) != 0) { b.y = startB.y + dy; b.height = startB.height - dy; }
                if ((dir & 8) != 0) b.height = startB.height + dy;
                if (b.width < min.width) { if ((dir & 1) != 0) b.x -= min.width - b.width; b.width = min.width; }
                if (b.height < min.height) { if ((dir & 4) != 0) b.y -= min.height - b.height; b.height = min.height; }
                setBounds(b);
            }
        };
        glass.addMouseListener(ma);
        glass.addMouseMotionListener(ma);
        setGlassPane(glass);
        glass.setVisible(true);
    }
}
