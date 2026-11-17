package com.jsoftbharath.palette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Full-screen eyedropper with a magnifier loupe.
 * Mouse: move + click.  Keyboard: arrows move 1px (Shift 10px), Enter/Space picks, Esc cancels.  (c) JSoft Bharath
 */
final class ScreenPicker {
    private ScreenPicker() {}

    static void pick(Component owner, Consumer<Color> callback) {
        final Rectangle bounds = new Rectangle();
        for (GraphicsDevice d : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
            bounds.add(d.getDefaultConfiguration().getBounds());
        final BufferedImage shot;
        final Robot robot;
        try {
            robot = new Robot();
            shot = robot.createScreenCapture(bounds);
        } catch (Exception | Error ex) {
            Dialogs.error(owner, "Screen picker", "Screen capture is not available on this system.\n" + ex.getMessage());
            return;
        }
        final Window top = SwingUtilities.getWindowAncestor(owner);
        final JWindow win = new JWindow();
        win.setAlwaysOnTop(true);
        win.setBounds(bounds);

        final Point cur = new Point(bounds.width / 2, bounds.height / 2);
        PointerInfo pi = MouseInfo.getPointerInfo();
        if (pi != null) {
            cur.x = Math.max(0, Math.min(shot.getWidth() - 1, pi.getLocation().x - bounds.x));
            cur.y = Math.max(0, Math.min(shot.getHeight() - 1, pi.getLocation().y - bounds.y));
        }

        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Gfx.quality(g);
                g.drawImage(shot, 0, 0, null);
                paintLoupe(g);
                paintHint(g);
                g.dispose();
            }

            private void paintHint(Graphics2D g) {
                String t = "Click or Enter to pick    \u2022    Arrow keys move 1px (Shift: 10px)    \u2022    Esc to cancel";
                g.setFont(Theme.body);
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(t) + 28, h = 28;
                Rectangle vis = visibleScreenArea();
                int x = vis.x + (vis.width - w) / 2, y = vis.y + 14;
                g.setColor(new Color(0, 0, 0, 200));
                g.fillRect(x, y, w, h);
                g.setColor(Color.WHITE);
                g.drawString(t, x + 14, Gfx.baseline(fm, y, h));
            }

            private Rectangle visibleScreenArea() {
                Rectangle r = new Rectangle(0, 0, getWidth(), getHeight());
                for (GraphicsDevice d : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                    Rectangle b = new Rectangle(d.getDefaultConfiguration().getBounds());
                    b.translate(-bounds.x, -bounds.y);
                    if (b.contains(cur)) return b;
                }
                return r;
            }

            private void paintLoupe(Graphics2D g) {
                final int n = 15, z = 9, size = n * z;
                int lx = cur.x + 26, ly = cur.y + 26;
                Rectangle vis = visibleScreenArea();
                if (lx + size + 2 > vis.x + vis.width) lx = cur.x - 26 - size;
                if (ly + size + 44 > vis.y + vis.height) ly = cur.y - 26 - size - 44;
                g.setColor(Color.BLACK);
                g.fillRect(lx - 2, ly - 2, size + 4, size + 48);
                for (int j = 0; j < n; j++) for (int i = 0; i < n; i++) {
                    int px = cur.x + i - n / 2, py = cur.y + j - n / 2;
                    int rgb = (px >= 0 && py >= 0 && px < shot.getWidth() && py < shot.getHeight()) ? shot.getRGB(px, py) : 0x202020;
                    g.setColor(new Color(rgb));
                    g.fillRect(lx + i * z, ly + j * z, z, z);
                }
                g.setColor(new Color(255, 255, 255, 70));
                for (int i = 0; i <= n; i++) { g.drawLine(lx + i * z, ly, lx + i * z, ly + size); g.drawLine(lx, ly + i * z, lx + size, ly + i * z); }
                g.setStroke(new BasicStroke(2f));
                g.setColor(Color.WHITE);
                g.drawRect(lx + (n / 2) * z - 1, ly + (n / 2) * z - 1, z + 2, z + 2);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(1f));
                g.drawRect(lx + (n / 2) * z, ly + (n / 2) * z, z, z);
                Color c = new Color(shot.getRGB(cur.x, cur.y));
                g.setColor(c);
                g.fillRect(lx + 6, ly + size + 7, 28, 28);
                g.setColor(Color.WHITE);
                g.drawRect(lx + 6, ly + size + 7, 27, 27);
                g.setFont(Theme.mono);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(ColorUtils.toHex(c, true), lx + 42, ly + size + 7 + fm.getAscent());
                g.setFont(Theme.monoSmall);
                g.setColor(new Color(200, 200, 210));
                g.drawString(c.getRed() + ", " + c.getGreen() + ", " + c.getBlue(), lx + 42, ly + size + 7 + 14 + g.getFontMetrics().getAscent());
            }
        };
        panel.setFocusable(true);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        win.setContentPane(panel);

        final Runnable[] finish = new Runnable[1];
        finish[0] = () -> { win.dispose(); if (top != null) { top.toFront(); top.requestFocus(); } };
        Runnable accept = () -> {
            Color c = new Color(shot.getRGB(cur.x, cur.y));
            finish[0].run();
            SwingUtilities.invokeLater(() -> callback.accept(c));
        };
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { cur.setLocation(e.getPoint()); panel.repaint(); }
            @Override public void mouseDragged(MouseEvent e) { cur.setLocation(e.getPoint()); panel.repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) { cur.setLocation(e.getPoint()); accept.run(); }
                else finish[0].run();
            }
        };
        panel.addMouseListener(ma);
        panel.addMouseMotionListener(ma);
        panel.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int step = e.isShiftDown() ? 10 : 1;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE: finish[0].run(); return;
                    case KeyEvent.VK_ENTER: case KeyEvent.VK_SPACE: accept.run(); return;
                    case KeyEvent.VK_LEFT: cur.x -= step; break;
                    case KeyEvent.VK_RIGHT: cur.x += step; break;
                    case KeyEvent.VK_UP: cur.y -= step; break;
                    case KeyEvent.VK_DOWN: cur.y += step; break;
                    default: return;
                }
                cur.x = Math.max(0, Math.min(shot.getWidth() - 1, cur.x));
                cur.y = Math.max(0, Math.min(shot.getHeight() - 1, cur.y));
                try { robot.mouseMove(bounds.x + cur.x, bounds.y + cur.y); } catch (Exception ignored) {}
                panel.repaint();
            }
        });
        win.setVisible(true);
        win.toFront();
        panel.requestFocusInWindow();
    }
}
