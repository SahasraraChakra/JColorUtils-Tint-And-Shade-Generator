package com.jsoftbharath.palette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * One focusable row/grid of color boxes (roving focus: Tab enters the row, arrow keys move inside it).
 *   Enter = copy   Ctrl+Enter = pin   Ctrl+F = contrast foreground   Ctrl+B = contrast background
 * Used for tint rows, shade rows, the base color box and the pinboard.   (c) JSoft Bharath
 */
final class SwatchRow extends JComponent implements TabStop, Themed, Scrollable {
    enum Style { PALETTE, PIN, BASE }

    static final class Item {
        final Color color; final String label;
        Item(Color c, String label) { this.color = c; this.label = label; }
    }

    private static final int PAD = 3, GAP = 6;
    private final AppContext ctx;
    private final Style style;
    private final List<Item> items = new ArrayList<>();
    private int active = 0, hover = -1, lastRows = 1;
    private String emptyText = "";
    private IntConsumer onDouble, onRemove, onSelect;
    private boolean selectionAlwaysVisible;

    SwatchRow(AppContext ctx, Style style) {
        this.ctx = ctx;
        this.style = style;
        this.selectionAlwaysVisible = style == Style.PIN;
        setFocusable(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ToolTipManager.sharedInstance().registerComponent(this);
        Widgets.focusRepaint(this);
        installKeys();
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int i = indexAt(e.getPoint());
                if (i >= 0) setActive(i);
                if (e.isPopupTrigger() && i >= 0) menu(e.getX(), e.getY());
            }
            @Override public void mouseReleased(MouseEvent e) {
                int i = indexAt(e.getPoint());
                if (e.isPopupTrigger() && i >= 0) menu(e.getX(), e.getY());
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int i = indexAt(e.getPoint());
                if (i < 0) return;
                if (e.getClickCount() == 1) copyActive();
                else if (e.getClickCount() == 2 && onDouble != null) onDouble.accept(i);
            }
            @Override public void mouseMoved(MouseEvent e) { setHover(indexAt(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { setHover(-1); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int r = rows(getWidth());
                if (r != lastRows) { lastRows = r; revalidate(); }
            }
        });
        applyTheme();
    }

    /** An empty row (e.g. empty pinboard) is skipped by Tab. */
    @Override public boolean isFocusable() { return super.isFocusable() && !items.isEmpty(); }

    // ------------------------------------------------------------------ model
    void setItems(List<Item> list) {
        items.clear();
        items.addAll(list);
        active = Math.max(0, Math.min(active, items.size() - 1));
        revalidate();
        repaint();
    }
    void setEmptyText(String t) { emptyText = t; repaint(); }
    void onDouble(IntConsumer c) { onDouble = c; }
    void onRemove(IntConsumer c) { onRemove = c; }
    void onSelect(IntConsumer c) { onSelect = c; }
    int count() { return items.size(); }
    int getActive() { return active; }
    Item activeItem() { return items.isEmpty() ? null : items.get(active); }
    List<Item> items() { return items; }

    void setActive(int i) {
        if (items.isEmpty()) return;
        i = Math.max(0, Math.min(items.size() - 1, i));
        if (i != active) { active = i; if (onSelect != null) onSelect.accept(i); }
        repaint();
    }
    private void setHover(int i) { if (i != hover) { hover = i; repaint(); } }

    // ------------------------------------------------------------------ keys / actions
    private void installKeys() {
        Widgets.bindKey(this, "LEFT", "l", () -> setActive(active - 1));
        Widgets.bindKey(this, "RIGHT", "r", () -> setActive(active + 1));
        Widgets.bindKey(this, "UP", "u", () -> setActive(active - cols(getWidth())));
        Widgets.bindKey(this, "DOWN", "d", () -> setActive(active + cols(getWidth())));
        Widgets.bindKey(this, "HOME", "h", () -> setActive(0));
        Widgets.bindKey(this, "END", "e", () -> setActive(items.size() - 1));
        Widgets.bindKey(this, "ENTER", "copy", this::copyActive);
        Widgets.bindKey(this, "ctrl C", "copy2", this::copyActive);
        Widgets.bindKey(this, "ctrl ENTER", "pin", this::pinActive);
        Widgets.bindKey(this, "ctrl F", "fg", () -> { Item it = activeItem(); if (it != null) ctx.setFg(it.color); });
        Widgets.bindKey(this, "ctrl B", "bg", () -> { Item it = activeItem(); if (it != null) ctx.setBg(it.color); });
        Widgets.bindKey(this, "DELETE", "del", this::removeActive);
        Widgets.bindKey(this, "BACK_SPACE", "del2", this::removeActive);
        Widgets.bindKey(this, "CONTEXT_MENU", "ctx", this::menuAtActive);
        Widgets.bindKey(this, "shift F10", "ctx2", this::menuAtActive);
    }

    private void copyActive() { Item it = activeItem(); if (it != null) ctx.copy(it.color, it.label); }
    private void pinActive() {
        Item it = activeItem();
        if (it != null) ctx.pin(it.color, it.label);
    }
    private void removeActive() { if (style == Style.PIN && onRemove != null && !items.isEmpty()) onRemove.accept(active); }

    private void menuAtActive() {
        Rectangle r = cellRect(active);
        if (r != null) menu(r.x + r.width / 2, r.y + r.height / 2);
    }

    private void menu(int x, int y) {
        Item it = activeItem();
        if (it == null) return;
        JPopupMenu pm = new JPopupMenu();
        pm.setBorder(new Widgets.PopupBorder());
        pm.setBackground(Theme.c("popup.bg"));
        pm.setOpaque(true);
        pm.add(new Widgets.TMenuItem("Copy " + ctx.fmt(it.color), "Enter", this::copyActive));
        if (style != Style.PIN) pm.add(new Widgets.TMenuItem("Pin to pinboard", "Ctrl+Enter", this::pinActive));
        pm.add(new Widgets.TSeparator());
        pm.add(new Widgets.TMenuItem("Use as contrast foreground", "Ctrl+F", () -> ctx.setFg(it.color)));
        pm.add(new Widgets.TMenuItem("Use as contrast background", "Ctrl+B", () -> ctx.setBg(it.color)));
        if (style == Style.PIN) {
            pm.add(new Widgets.TSeparator());
            pm.add(new Widgets.TMenuItem("Add to base colors input", "Dbl-click", () -> { if (onDouble != null) onDouble.accept(active); }));
            pm.add(new Widgets.TMenuItem("Remove from pinboard", "Del", this::removeActive));
        }
        pm.show(this, x, y);
    }

    // ------------------------------------------------------------------ geometry
    private int minCellW() { return style == Style.PALETTE ? 78 : 70; }

    private int cols(int width) {
        if (style == Style.BASE || items.isEmpty()) return 1;
        int usable = Math.max(1, width - 2 * PAD);
        return Math.max(1, Math.min(items.size(), (usable + GAP) / (minCellW() + GAP)));
    }
    private int rows(int width) {
        if (items.isEmpty()) return 1;
        return (int) Math.ceil(items.size() / (double) cols(width));
    }
    private int codeH() { return getFontMetrics(Theme.monoSmall).getHeight(); }
    private int labelH() { return getFontMetrics(Theme.small).getHeight(); }
    private int swatchH() { return style == Style.PIN ? 26 : 22; }
    private int cellH() {
        if (style == Style.PALETTE) return codeH() + 2 + swatchH();
        return swatchH() + 4 + codeH() + labelH();
    }
    private static final int ROW_GAP = 8;

    private int effWidth() {
        int w = getWidth();
        if (w > 0) return w;
        Container p = getParent();
        return p != null && p.getWidth() > 0 ? p.getWidth() : 900;
    }

    @Override public Dimension getPreferredSize() {
        if (style == Style.BASE) return new Dimension(40 + 2 * PAD, 40 + 2 * PAD);
        int w = effWidth();
        int r = rows(w);
        int h = items.isEmpty() ? 34 : r * cellH() + (r - 1) * ROW_GAP;
        return new Dimension(minCellW() + 2 * PAD, h + 2 * PAD);
    }
    @Override public Dimension getMinimumSize() { return new Dimension(minCellW() + 2 * PAD, 20); }

    private Rectangle cellRect(int i) {
        if (i < 0 || i >= items.size()) return null;
        if (style == Style.BASE) return new Rectangle(PAD, PAD, getWidth() - 2 * PAD, getHeight() - 2 * PAD);
        int cols = cols(getWidth());
        double cw = (getWidth() - 2 * PAD - (cols - 1) * GAP) / (double) cols;
        int col = i % cols, row = i / cols;
        int x = (int) Math.round(PAD + col * (cw + GAP));
        int x2 = (int) Math.round(PAD + col * (cw + GAP) + cw);
        return new Rectangle(x, PAD + row * (cellH() + ROW_GAP), x2 - x, cellH());
    }

    private Rectangle swatchRect(int i) {
        Rectangle c = cellRect(i);
        if (c == null) return null;
        if (style == Style.BASE) return c;
        return style == Style.PALETTE ? new Rectangle(c.x, c.y + codeH() + 2, c.width, swatchH())
            : new Rectangle(c.x, c.y, c.width, swatchH());
    }

    private int indexAt(Point p) {
        for (int i = 0; i < items.size(); i++) {
            Rectangle r = cellRect(i);
            if (r != null && r.contains(p)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ painting
    @Override public void applyTheme() { revalidate(); repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Gfx.quality(g);
        if (items.isEmpty()) {
            g.setFont(Theme.small);
            g.setColor(Theme.c("text.muted"));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(emptyText, PAD + 4, Gfx.baseline(fm, 0, getHeight()));
            g.dispose();
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            Rectangle cell = cellRect(i), sw = swatchRect(i);
            if (cell == null) continue;
            if (style == Style.PALETTE) paintCodes(g, it, cell);
            g.setColor(it.color);
            g.fillRect(sw.x, sw.y, sw.width, sw.height);
            g.setColor(Theme.c("swatch.border"));
            g.setStroke(new BasicStroke(1f));
            g.drawRect(sw.x, sw.y, sw.width - 1, sw.height - 1);
            if (style == Style.PIN) paintPinText(g, it, cell);
            if (i == hover) {
                g.setColor(Gfx.alpha(Gfx.readable(it.color), 150));
                g.drawRect(sw.x + 1, sw.y + 1, sw.width - 3, sw.height - 3);
            }
            boolean sel = i == active;
            if (sel && selectionAlwaysVisible) {
                g.setColor(Theme.c("accent"));
                g.setStroke(new BasicStroke(2f));
                g.drawRect(sw.x - 2, sw.y - 2, sw.width + 3, sw.height + 3);
            }
            if (sel && hasFocus()) {
                g.setColor(Theme.c("focus.ring"));
                g.setStroke(new BasicStroke(2f));
                g.drawRect(sw.x - 2, sw.y - 2, sw.width + 3, sw.height + 3);
                g.setColor(Gfx.alpha(Gfx.readable(it.color), 200));
                g.setStroke(new BasicStroke(1f));
                g.drawRect(sw.x + 1, sw.y + 1, sw.width - 3, sw.height - 3);
            }
        }
        g.dispose();
    }

    private void paintCodes(Graphics2D g, Item it, Rectangle cell) {
        g.setFont(Theme.monoSmall);
        FontMetrics fm = g.getFontMetrics();
        String code = ColorUtils.compact(it.color, ctx.format);
        int base = cell.y + fm.getAscent();
        int lw = fm.stringWidth(it.label == null ? "" : it.label);
        boolean showLabel = it.label != null && fm.stringWidth(code) + lw + 6 <= cell.width;
        g.setColor(Theme.c("code.fg"));
        g.drawString(Gfx.ellipsize(code, fm, showLabel ? cell.width - lw - 6 : cell.width), cell.x, base);
        if (showLabel) {
            g.setColor(Theme.c("text.muted"));
            g.drawString(it.label, cell.x + cell.width - lw, base);
        }
    }

    private void paintPinText(Graphics2D g, Item it, Rectangle cell) {
        g.setFont(Theme.monoSmall);
        FontMetrics fm = g.getFontMetrics();
        String code = Gfx.ellipsize(ColorUtils.compact(it.color, ctx.format), fm, cell.width);
        int y = cell.y + swatchH() + 4;
        g.setColor(Theme.c("code.fg"));
        g.drawString(code, cell.x + (cell.width - fm.stringWidth(code)) / 2, y + fm.getAscent());
        if (it.label != null) {
            g.setFont(Theme.small);
            FontMetrics f2 = g.getFontMetrics();
            g.setColor(Theme.c("text.muted"));
            g.drawString(it.label, cell.x + (cell.width - f2.stringWidth(it.label)) / 2, y + fm.getHeight() + f2.getAscent());
        }
    }

    @Override public String getToolTipText(MouseEvent e) {
        int i = indexAt(e.getPoint());
        if (i < 0) return null;
        Item it = items.get(i);
        String hex = ColorUtils.toHex(it.color, true);
        String lbl = it.label == null ? "" : " &middot; " + it.label;
        return "<html><b>" + hex + "</b>" + lbl + "<br><font size=-2>Enter copy &nbsp;&middot;&nbsp; Ctrl+Enter pin &nbsp;&middot;&nbsp; Ctrl+F fg &nbsp;&middot;&nbsp; Ctrl+B bg</font></html>";
    }

    // ------------------------------------------------------------------ Scrollable (pinboard)
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(20, r.height - 20); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
