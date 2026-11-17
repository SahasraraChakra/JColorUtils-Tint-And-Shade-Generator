package com.jsoftbharath.palette;

import java.awt.*;
import java.awt.geom.*;
import java.util.Map;

/** Small painting helpers shared by all custom components. (c) JSoft Bharath */
final class Gfx {
    private Gfx() {}

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (hints instanceof Map) g.addRenderingHints((Map<?, ?>) hints);
        else g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static Shape shape(double x, double y, double w, double h) {
        int r = Theme.radius();
        return r <= 0 ? new Rectangle2D.Double(x, y, w, h) : new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
    }

    /** Fills a box and draws a 1px border inside it. */
    static void box(Graphics2D g, int x, int y, int w, int h, Color fill, Color border) {
        if (fill != null) { g.setColor(fill); g.fill(shape(x, y, w, h)); }
        if (border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke(1f));
            g.draw(shape(x + .5, y + .5, w - 1, h - 1));
        }
    }

    /** Keyboard focus ring (2px) drawn just inside the given bounds. */
    static void ring(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Theme.c("focus.ring"));
        g.setStroke(new BasicStroke(2f));
        g.draw(shape(x + 1, y + 1, w - 2, h - 2));
    }

    static int baseline(FontMetrics fm, int y, int h) {
        return y + (h - fm.getHeight()) / 2 + fm.getAscent();
    }

    static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    static Color mix(Color a, Color b, float t) {
        return new Color(
            Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
            Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static String ellipsize(String s, FontMetrics fm, int w) {
        if (fm.stringWidth(s) <= w) return s;
        String e = "\u2026";
        int n = s.length();
        while (n > 0 && fm.stringWidth(s.substring(0, n) + e) > w) n--;
        return n <= 0 ? "" : s.substring(0, n) + e;
    }

    static Color readable(Color bg) {
        return ColorUtils.luminance(bg) > 0.40 ? new Color(0x111111) : Color.WHITE;
    }
}
