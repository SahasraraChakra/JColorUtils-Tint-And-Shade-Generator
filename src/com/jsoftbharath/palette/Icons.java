package com.jsoftbharath.palette;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Vector icons drawn in a 16x16 box - they follow the theme colors automatically. (c) JSoft Bharath */
final class Icons {
    private Icons() {}

    static void paint(Graphics2D g0, String name, double x, double y, double size, Color color) {
        Graphics2D g = (Graphics2D) g0.create();
        Gfx.quality(g);
        g.translate(x, y);
        double s = size / 16.0;
        g.scale(s, s);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name) {
            case "close": line(g, 3.5, 3.5, 12.5, 12.5); line(g, 12.5, 3.5, 3.5, 12.5); break;
            case "min": line(g, 3, 8.5, 13, 8.5); break;
            case "max": g.draw(new Rectangle2D.Double(3.2, 3.2, 9.6, 9.6)); break;
            case "restore":
                g.draw(new Rectangle2D.Double(3, 5.4, 7.6, 7.6));
                poly(g, false, 5.4, 5.4, 5.4, 3, 13, 3, 13, 10.6, 10.6, 10.6); break;
            case "new":
                poly(g, true, 4, 2, 9, 2, 12, 5, 12, 14, 4, 14); poly(g, false, 9, 2, 9, 5, 12, 5); break;
            case "import": line(g, 8, 2, 8, 10); poly(g, false, 5, 7.2, 8, 10.2, 11, 7.2);
                poly(g, false, 3, 10.5, 3, 13.5, 13, 13.5, 13, 10.5); break;
            case "export": line(g, 8, 10, 8, 2); poly(g, false, 5, 5, 8, 2, 11, 5);
                poly(g, false, 3, 10.5, 3, 13.5, 13, 13.5, 13, 10.5); break;
            case "shuffle":
                poly(g, false, 2, 4.5, 5, 4.5, 10, 11.5, 13, 11.5); poly(g, false, 11, 9.5, 13, 11.5, 11, 13.5);
                poly(g, false, 2, 11.5, 5, 11.5, 6.6, 9.3); poly(g, false, 8.6, 6.7, 10, 4.5, 13, 4.5);
                poly(g, false, 11, 2.5, 13, 4.5, 11, 6.5); break;
            case "trash":
                line(g, 2.8, 4.2, 13.2, 4.2); poly(g, false, 6, 4.2, 6, 2.5, 10, 2.5, 10, 4.2);
                poly(g, false, 4, 4.2, 4.8, 13.5, 11.2, 13.5, 12, 4.2); line(g, 6.8, 6.5, 6.8, 11.5); line(g, 9.2, 6.5, 9.2, 11.5); break;
            case "copy":
                g.draw(new Rectangle2D.Double(5.5, 5.5, 8, 8)); poly(g, false, 10.5, 2.8, 2.5, 2.8, 2.5, 10.5); break;
            case "paste":
                g.draw(new Rectangle2D.Double(3, 3.5, 10, 10.5)); g.fill(new Rectangle2D.Double(5.8, 2, 4.4, 3));
                line(g, 5.5, 8, 10.5, 8); line(g, 5.5, 10.5, 9, 10.5); break;
            case "swap":
                line(g, 3, 5.5, 13, 5.5); poly(g, false, 10.5, 3, 13, 5.5, 10.5, 8);
                line(g, 13, 10.5, 3, 10.5); poly(g, false, 5.5, 8, 3, 10.5, 5.5, 13); break;
            case "pick":
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                line(g, 6.8, 9.2, 11.2, 4.8);
                g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.fill(new Ellipse2D.Double(10.2, 1.8, 4, 4)); line(g, 6.8, 9.2, 3, 13.2); line(g, 3, 13.2, 2.6, 13.6); break;
            case "choose":
                g.draw(new Ellipse2D.Double(2, 2, 12, 12));
                g.fill(new Ellipse2D.Double(4.4, 6.6, 1.9, 1.9)); g.fill(new Ellipse2D.Double(6.6, 4, 1.9, 1.9));
                g.fill(new Ellipse2D.Double(9.4, 4.8, 1.9, 1.9)); g.fill(new Ellipse2D.Double(10, 8.6, 1.9, 1.9)); break;
            case "sparkle": {
                Path2D p = new Path2D.Double();
                p.moveTo(8, 1.2); p.lineTo(9.7, 6.3); p.lineTo(14.8, 8); p.lineTo(9.7, 9.7); p.lineTo(8, 14.8);
                p.lineTo(6.3, 9.7); p.lineTo(1.2, 8); p.lineTo(6.3, 6.3); p.closePath(); g.fill(p); break; }
            case "reset":
                g.draw(new Arc2D.Double(3, 3, 10, 10, 35, 280, Arc2D.OPEN));
                g.fill(poly(12.9, 2.2, 13.6, 6.6, 9.4, 5.6)); break;
            case "pin": poly(g, true, 6, 2, 10, 2, 10.6, 7, 12.2, 9.2, 3.8, 9.2, 5.4, 7); line(g, 8, 9.2, 8, 14); break;
            case "chevron": poly(g, false, 4, 6, 8, 10, 12, 6); break;
            case "check": poly(g, false, 3, 8.5, 6.5, 12, 13, 4.5); break;
            case "warn": poly(g, true, 8, 2, 14.5, 13.5, 1.5, 13.5); line(g, 8, 6.5, 8, 9.8); g.fill(new Ellipse2D.Double(7.2, 10.8, 1.6, 1.6)); break;
            case "info": g.draw(new Ellipse2D.Double(2, 2, 12, 12)); line(g, 8, 7.2, 8, 11.2); g.fill(new Ellipse2D.Double(7.2, 4.2, 1.6, 1.6)); break;
            case "palette": {
                Path2D p = new Path2D.Double();
                p.moveTo(8, 1.8); p.curveTo(11.9, 1.8, 14.6, 4.4, 14.6, 7.6); p.curveTo(14.6, 9.5, 13.2, 10.2, 11.8, 10.2);
                p.lineTo(10.4, 10.2); p.curveTo(9.5, 10.2, 9.1, 10.9, 9.5, 11.7); p.curveTo(9.9, 12.5, 9.7, 14.2, 8, 14.2);
                p.curveTo(4.3, 14.2, 1.4, 11.5, 1.4, 8); p.curveTo(1.4, 4.5, 4.2, 1.8, 8, 1.8); p.closePath();
                g.fill(p);
                break; }
            default: g.draw(new Rectangle2D.Double(3, 3, 10, 10));
        }
        g.dispose();
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void poly(Graphics2D g, boolean close, double... p) {
        Path2D path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        if (close) path.closePath();
        g.draw(path);
    }

    private static Shape poly(double... p) {
        Path2D path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.closePath();
        return path;
    }

    /** App badge: accent square with a white palette glyph (title bar, dialogs, window icon). */
    static void paintAppBadge(Graphics2D g0, int x, int y, int size, Color bg) {
        Graphics2D g = (Graphics2D) g0.create();
        Gfx.quality(g);
        g.setColor(bg);
        g.fill(new RoundRectangle2D.Double(x, y, size, size, size * 0.22, size * 0.22));
        paint(g, "palette", x + size * 0.2, y + size * 0.2, size * 0.6, Color.WHITE);
        g.setColor(bg);
        double u = size * 0.6 / 16.0, ox = x + size * 0.2, oy = y + size * 0.2;
        for (double[] d : new double[][]{{4.6, 6.2}, {7.2, 4.4}, {10.2, 5.2}}) g.fill(new Ellipse2D.Double(ox + d[0] * u, oy + d[1] * u, 2.2 * u, 2.2 * u));
        g.dispose();
    }

    static Image appImage(int size) {
        File ext = new File(Theme.appDir(), "icon.png");
        if (ext.isFile()) {
            try { return javax.imageio.ImageIO.read(ext); } catch (Exception ignored) {}
        }
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        paintAppBadge(g, 0, 0, size, new Color(0x663399));
        g.dispose();
        return img;
    }
}
