package com.jsoftbharath.palette;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** Themed, keyboard-friendly building blocks. (c) JSoft Bharath */
final class Widgets {
    private Widgets() {}

    static void bindKey(JComponent c, String key, String name, Runnable r) {
        c.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        });
    }

    /** Left-aligned horizontal group with fixed gaps and no leading/trailing padding. */
    static JPanel hbox(int gap, Component... cs) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (int i = 0; i < cs.length; i++) {
            if (i > 0 && gap > 0) p.add(Box.createHorizontalStrut(gap));
            if (cs[i] instanceof JComponent) ((JComponent) cs[i]).setAlignmentY(0.5f);
            p.add(cs[i]);
        }
        return p;
    }

    static void focusRepaint(JComponent c) {
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { c.repaint(); }
            @Override public void focusLost(FocusEvent e) { c.repaint(); }
        });
    }

    // ================================================================= labels
    enum Role { BODY, BOLD, MUTED, SMALL_MUTED, SMALL_BOLD, TITLE, MONO, MONO_MUTED, MONO_SMALL, ACCENT }

    static final class TLabel extends JLabel implements Themed {
        private Role role;
        TLabel(String text, Role role) { super(text); this.role = role; setOpaque(false); applyTheme(); }
        void setRole(Role r) { role = r; applyTheme(); }
        @Override public void applyTheme() {
            switch (role) {
                case BOLD: setFont(Theme.bodyBold); setForeground(Theme.c("text.main")); break;
                case MUTED: setFont(Theme.body); setForeground(Theme.c("text.muted")); break;
                case SMALL_MUTED: setFont(Theme.small); setForeground(Theme.c("text.muted")); break;
                case SMALL_BOLD: setFont(Theme.smallBold); setForeground(Theme.c("text.main")); break;
                case TITLE: setFont(Theme.title); setForeground(Theme.c("text.main")); break;
                case MONO: setFont(Theme.mono); setForeground(Theme.c("text.main")); break;
                case MONO_MUTED: setFont(Theme.mono); setForeground(Theme.c("text.muted")); break;
                case MONO_SMALL: setFont(Theme.monoSmall); setForeground(Theme.c("text.muted")); break;
                case ACCENT: setFont(Theme.bodyBold); setForeground(Theme.c("accent")); break;
                default: setFont(Theme.body); setForeground(Theme.c("text.main"));
            }
        }
    }

    // ================================================================= badge
    enum BadgeKind { PASS, WARN, FAIL, INFO, PLAIN }

    static final class Badge extends JComponent implements Themed {
        private String text;
        private BadgeKind kind;
        private boolean mono;
        Badge(String text, BadgeKind kind) { this.text = text; this.kind = kind; applyTheme(); }
        Badge mono() { mono = true; applyTheme(); return this; }
        void set(String text, BadgeKind kind) { this.text = text; this.kind = kind; revalidate(); repaint(); }
        @Override public void applyTheme() { setFont(mono ? Theme.monoSmall : Theme.smallBold); revalidate(); repaint(); }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 14, fm.getHeight() + 6);
        }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Gfx.quality(g);
            String k = kind == BadgeKind.PASS ? "badge.pass" : kind == BadgeKind.WARN ? "badge.warn"
                : kind == BadgeKind.FAIL ? "badge.fail" : "badge.info";
            Color bg = kind == BadgeKind.PLAIN ? Theme.c("panel.divider") : Theme.c(k + ".bg");
            Color fg = kind == BadgeKind.PLAIN ? Theme.c("text.muted") : Theme.c(k + ".fg");
            Gfx.box(g, 0, 0, getWidth(), getHeight(), bg, null);
            g.setFont(getFont());
            g.setColor(fg);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, Gfx.baseline(fm, 0, getHeight()));
            g.dispose();
        }
    }

    // ================================================================= buttons
    enum Kind { NEUTRAL, ACCENT, PRIMARY, DANGER }

    static final class TButton extends JButton implements TabStop, Themed {
        private final Kind kind;
        private String icon;
        private boolean compact;
        private Color chip;
        private boolean iconOnly;

        TButton(String text, String icon, Kind kind) {
            super(text);
            this.kind = kind;
            this.icon = icon;
            iconOnly = text == null || text.isEmpty();
            setFocusable(true);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "pressed");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("released ENTER"), "released");
            focusRepaint(this);
            applyTheme();
        }
        TButton compact() { compact = true; revalidate(); return this; }
        void setChip(Color c) { chip = c; repaint(); }
        @Override public void applyTheme() { setFont(Theme.body); revalidate(); repaint(); }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int h = compact ? 24 : 28;
            int w = 0;
            if (icon != null) w += 14;
            if (chip != null) w += 12;
            String t = getText();
            if (t != null && !t.isEmpty()) w += fm.stringWidth(t) + ((icon != null || chip != null) ? 6 : 0);
            return new Dimension(w + (iconOnly ? 12 : compact ? 16 : 20), h);
        }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        private String key(String attr) {
            String p = kind == Kind.NEUTRAL ? "button" : kind == Kind.ACCENT ? "button.accent"
                : kind == Kind.PRIMARY ? "button.primary" : "button.danger";
            return p + "." + attr;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Gfx.quality(g);
            int w = getWidth(), h = getHeight();
            boolean en = isEnabled();
            Color bg = Theme.c(key("bg"));
            if (en && getModel().isPressed() && getModel().isArmed()) bg = Theme.c(key("press"));
            else if (en && getModel().isRollover()) bg = Theme.c(key("hover"));
            Color fg = en ? Theme.c(key("fg")) : Theme.c("text.disabled");
            Gfx.box(g, 0, 0, w, h, en ? bg : Theme.c("panel.divider"), Theme.c(key("border")));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            String t = getText() == null ? "" : getText();
            int cw = 0;
            if (icon != null) cw += 14;
            if (chip != null) cw += 12;
            if (!t.isEmpty()) cw += fm.stringWidth(t) + ((icon != null || chip != null) ? 6 : 0);
            int x = (w - cw) / 2;
            if (icon != null) { Icons.paint(g, icon, x, (h - 14) / 2.0, 14, fg); x += 14 + (t.isEmpty() && chip == null ? 0 : 6); }
            if (chip != null) {
                g.setColor(chip); g.fillRect(x, (h - 10) / 2, 10, 10);
                g.setColor(Theme.c("swatch.border")); g.drawRect(x, (h - 10) / 2, 9, 9);
                x += 12 + (t.isEmpty() ? 0 : 6);
            }
            if (!t.isEmpty()) { g.setColor(fg); g.drawString(t, x, Gfx.baseline(fm, 0, h)); }
            if (hasFocus()) Gfx.ring(g, 0, 0, w, h);
            g.dispose();
        }
    }

    // ================================================================= inputs
    /** Paints the themed input border; the focus ring follows focus of any registered inner component. */
    static class InputFrame extends JPanel implements Themed {
        private boolean focused;
        InputFrame(LayoutManager lm) { super(lm); setOpaque(false); }
        void track(Component c) {
            c.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }
        @Override public void applyTheme() { repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Gfx.quality(g);
            Gfx.box(g, 0, 0, getWidth(), getHeight(), Theme.c("input.bg"), Theme.c(focused ? "input.focus" : "input.border"));
            if (focused) { g.setColor(Theme.c("input.focus")); g.setStroke(new BasicStroke(1f)); g.draw(Gfx.shape(1.5, 1.5, getWidth() - 3, getHeight() - 3)); }
            g.dispose();
        }
    }

    static void styleText(JTextComponent t, boolean mono) {
        t.setFont(mono ? Theme.mono : Theme.body);
        t.setForeground(Theme.c("input.fg"));
        t.setBackground(Theme.c("input.bg"));
        t.setCaretColor(Theme.c("input.fg"));
        t.setSelectionColor(Theme.c("input.selection"));
        t.setSelectedTextColor(Theme.c("input.selection.fg"));
        t.setDisabledTextColor(Theme.c("text.disabled"));
        t.setOpaque(false);
    }

    static final class TTextField extends JTextField implements TabStop, Themed {
        private final boolean mono;
        TTextField(String text, int cols, boolean mono) {
            super(text, cols);
            this.mono = mono;
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            applyTheme();
        }
        @Override public void applyTheme() { styleText(this, mono); }
    }

    static final class TTextArea extends JTextArea implements TabStop, Themed {
        String placeholder = "";
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (placeholder.isEmpty() || !getText().isEmpty()) return;
            Graphics2D g = (Graphics2D) g0.create();
            Gfx.quality(g);
            g.setFont(Theme.small);
            g.setColor(Theme.c("text.disabled"));
            Insets in = getInsets();
            g.drawString(placeholder, in.left, in.top + g.getFontMetrics().getAscent());
            g.dispose();
        }
        TTextArea() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Set.of(KeyStroke.getKeyStroke("TAB"), KeyStroke.getKeyStroke("ctrl TAB")));
            setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Set.of(KeyStroke.getKeyStroke("shift TAB"), KeyStroke.getKeyStroke("ctrl shift TAB")));
            applyTheme();
        }
        @Override public void applyTheme() { styleText(this, true); }
    }

    /** Text area inside a themed bordered frame with thin scrollbars. */
    static InputFrame textAreaBox(TTextArea area) {
        InputFrame f = new InputFrame(new BorderLayout());
        TScrollPane sp = new TScrollPane(area);
        sp.getViewport().setOpaque(false);
        f.add(sp, BorderLayout.CENTER);
        f.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        f.track(area);
        return f;
    }

    // ================================================================= spinner
    static final class TSpinner extends InputFrame implements Themed {
        private final TTextField field;
        private int value, min, max;
        private final List<Consumer<Integer>> listeners = new ArrayList<>();

        TSpinner(int value, int min, int max) {
            super(null);
            this.value = value; this.min = min; this.max = max;
            field = new TTextField(String.valueOf(value), 2, false);
            field.setHorizontalAlignment(SwingConstants.CENTER);
            field.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override public void replace(FilterBypass fb, int o, int l, String t, AttributeSet a) throws BadLocationException {
                    if (t == null) t = "";
                    if (!t.matches("\\d*")) return;
                    String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                    if ((cur.length() - l + t.length()) > String.valueOf(max).length()) return;
                    super.replace(fb, o, l, t, a);
                }
                @Override public void insertString(FilterBypass fb, int o, String t, AttributeSet a) throws BadLocationException {
                    replace(fb, o, 0, t, a);
                }
            });
            setLayout(new BorderLayout());
            add(field, BorderLayout.CENTER);
            JComponent arrows = new JComponent() {
                @Override public Dimension getPreferredSize() { return new Dimension(18, 10); }
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Gfx.quality(g);
                    int h = getHeight(), w = getWidth();
                    g.setColor(Theme.c("input.border"));
                    g.drawLine(0, 2, 0, h - 3);
                    g.setColor(Theme.c("text.muted"));
                    int cx = w / 2 + 1;
                    g.fillPolygon(new int[]{cx - 3, cx + 3, cx}, new int[]{h / 2 - 3, h / 2 - 3, h / 2 - 6}, 3);
                    g.fillPolygon(new int[]{cx - 3, cx + 3, cx}, new int[]{h / 2 + 3, h / 2 + 3, h / 2 + 6}, 3);
                    g.dispose();
                }
            };
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    field.requestFocusInWindow();
                    step(e.getY() < arrows.getHeight() / 2 ? 1 : -1);
                }
                @Override public void mouseWheelMoved(MouseWheelEvent e) { step(-e.getWheelRotation()); }
            };
            arrows.addMouseListener(ma);
            add(arrows, BorderLayout.EAST);
            addMouseWheelListener(ma::mouseWheelMoved);
            field.addMouseWheelListener(ma::mouseWheelMoved);
            track(field);
            bindKey(field, "UP", "inc", () -> step(1));
            bindKey(field, "DOWN", "dec", () -> step(-1));
            bindKey(field, "PAGE_UP", "inc10", () -> step(5));
            bindKey(field, "PAGE_DOWN", "dec10", () -> step(-5));
            bindKey(field, "ENTER", "commit", this::commit);
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) { commit(); }
                @Override public void focusGained(FocusEvent e) { field.selectAll(); }
            });
        }
        int getValue() { return value; }
        void onChange(Consumer<Integer> c) { listeners.add(c); }
        void setValue(int v) { set(v, false); }
        private void step(int d) { commit(); set(value + d, true); }
        private void commit() {
            try { set(Integer.parseInt(field.getText().trim()), true); } catch (NumberFormatException e) { field.setText(String.valueOf(value)); }
        }
        private void set(int v, boolean fire) {
            v = Math.max(min, Math.min(max, v));
            field.setText(String.valueOf(v));
            if (v != value) { value = v; if (fire) for (Consumer<Integer> c : listeners) c.accept(v); }
        }
        @Override public Dimension getPreferredSize() { return new Dimension(70, 28); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public void applyTheme() { field.applyTheme(); repaint(); }
    }

    // ================================================================= combo
    static final class TCombo<T> extends JComponent implements TabStop, Themed {
        private final List<T> items;
        private int index;
        private final List<Consumer<T>> listeners = new ArrayList<>();
        private boolean hover;

        TCombo(List<T> items, int index) {
            this.items = items; this.index = index;
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            focusRepaint(this);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); popup(); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
            bindKey(this, "UP", "up", () -> select(index - 1, true));
            bindKey(this, "DOWN", "down", () -> select(index + 1, true));
            bindKey(this, "HOME", "home", () -> select(0, true));
            bindKey(this, "END", "end", () -> select(items.size() - 1, true));
            bindKey(this, "SPACE", "open", this::popup);
            bindKey(this, "ENTER", "open2", this::popup);
            bindKey(this, "alt DOWN", "open3", this::popup);
            applyTheme();
        }
        T getSelected() { return items.get(index); }
        void select(T item) { int i = items.indexOf(item); if (i >= 0) index = i; repaint(); }
        void onChange(Consumer<T> c) { listeners.add(c); }
        private void select(int i, boolean fire) {
            i = Math.max(0, Math.min(items.size() - 1, i));
            if (i == index) return;
            index = i; repaint();
            if (fire) for (Consumer<T> c : listeners) c.accept(items.get(index));
        }
        private void popup() {
            JPopupMenu pm = new JPopupMenu();
            pm.setBorder(new PopupBorder());
            pm.setBackground(Theme.c("popup.bg"));
            pm.setOpaque(true);
            JMenuItem sel = null;
            for (int i = 0; i < items.size(); i++) {
                final int idx = i;
                TCheckItem mi = new TCheckItem(items.get(i).toString());
                mi.setSelected(i == index);
                mi.addActionListener(e -> { select(idx, true); requestFocusInWindow(); });
                pm.add(mi);
                if (i == index) sel = mi;
            }
            int w = Math.max(getWidth(), pm.getPreferredSize().width);
            pm.setPopupSize(w, pm.getPreferredSize().height);
            pm.show(this, 0, getHeight());
            final JMenuItem fsel = sel;
            SwingUtilities.invokeLater(() -> MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{pm, fsel}));
        }
        @Override public void applyTheme() { setFont(Theme.body); revalidate(); repaint(); }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = 0;
            for (T t : items) w = Math.max(w, fm.stringWidth(t.toString()));
            return new Dimension(w + 40, 28);
        }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Gfx.quality(g);
            int w = getWidth(), h = getHeight();
            Gfx.box(g, 0, 0, w, h, Theme.c("input.bg"), Theme.c(hasFocus() || hover ? "input.focus" : "input.border"));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.c("input.fg"));
            g.drawString(Gfx.ellipsize(items.get(index).toString(), fm, w - 32), 9, Gfx.baseline(fm, 0, h));
            Icons.paint(g, "chevron", w - 21, (h - 14) / 2.0, 14, Theme.c("text.muted"));
            if (hasFocus()) Gfx.ring(g, 0, 0, w, h);
            g.dispose();
        }
    }

    // ================================================================= check box
    static final class TCheck extends JCheckBox implements TabStop, Themed {
        private Color dot;
        TCheck(String text) {
            super(text);
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            focusRepaint(this);
            applyTheme();
        }
        void setDot(Color c) { dot = c; repaint(); }
        @Override public void applyTheme() { setFont(Theme.body); revalidate(); repaint(); }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(14 + 7 + fm.stringWidth(getText()) + 6 + 10 + 6, 22);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Gfx.quality(g);
            int h = getHeight();
            int by = (h - 14) / 2;
            boolean on = isSelected();
            Gfx.box(g, 3, by, 14, 14, on ? Theme.c("accent") : Theme.c("input.bg"), on ? Theme.c("accent") : Theme.c("input.border"));
            if (on) Icons.paint(g, "check", 3, by, 14, Theme.c("accent.fg"));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(isEnabled() ? Theme.c("text.main") : Theme.c("text.disabled"));
            int tx = 3 + 14 + 7;
            g.drawString(getText(), tx, Gfx.baseline(fm, 0, h));
            if (dot != null) {
                int dx = tx + fm.stringWidth(getText()) + 6;
                g.setColor(dot); g.fillRect(dx, (h - 8) / 2, 8, 8);
                g.setColor(Theme.c("swatch.border")); g.drawRect(dx, (h - 8) / 2, 7, 7);
            }
            if (hasFocus()) Gfx.ring(g, 0, 1, getWidth(), h - 2);
            g.dispose();
        }
    }

    // ================================================================= menus
    static final class PopupBorder implements Border {
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(Theme.c("popup.border"));
            g.drawRect(x, y, w - 1, h - 1);
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(3, 1, 3, 1); }
        @Override public boolean isBorderOpaque() { return true; }
    }

    private static String accelText(JMenuItem mi) {
        if (mi instanceof TMenuItem && ((TMenuItem) mi).hint != null) return ((TMenuItem) mi).hint;
        KeyStroke ks = mi.getAccelerator();
        if (ks == null) return "";
        String mods = ks.getModifiers() == 0 ? "" : java.awt.event.InputEvent.getModifiersExText(ks.getModifiers()) + "+";
        return mods + KeyEvent.getKeyText(ks.getKeyCode());
    }

    private static Dimension itemSize(JMenuItem mi) {
        FontMetrics fm = mi.getFontMetrics(mi.getFont());
        String acc = accelText(mi);
        int w = 26 + fm.stringWidth(mi.getText()) + (acc.isEmpty() ? 0 : 36 + fm.stringWidth(acc)) + 16;
        return new Dimension(Math.max(w, 150), 26);
    }

    private static void paintItem(Graphics g0, JMenuItem mi, boolean checked) {
        Graphics2D g = (Graphics2D) g0.create();
        Gfx.quality(g);
        int w = mi.getWidth(), h = mi.getHeight();
        boolean hot = mi.isEnabled() && (mi.getModel().isArmed() || mi.isArmed());
        g.setColor(hot ? Theme.c("menu.hover.bg") : Theme.c("popup.bg"));
        g.fillRect(0, 0, w, h);
        Color fg = !mi.isEnabled() ? Theme.c("menu.disabled") : hot ? Theme.c("menu.hover.fg") : Theme.c("menu.fg");
        g.setFont(mi.getFont());
        FontMetrics fm = g.getFontMetrics();
        if (checked) Icons.paint(g, "check", 6, (h - 14) / 2.0, 14, fg);
        g.setColor(fg);
        g.drawString(mi.getText(), 26, Gfx.baseline(fm, 0, h));
        String acc = accelText(mi);
        if (!acc.isEmpty()) {
            g.setColor(mi.isEnabled() ? (hot ? fg : Theme.c("text.muted")) : fg);
            g.drawString(acc, w - 12 - fm.stringWidth(acc), Gfx.baseline(fm, 0, h));
        }
        g.dispose();
    }

    static final class TMenuItem extends JMenuItem implements Themed {
        String hint;
        TMenuItem(String text, String hint, Runnable r) { this(text); this.hint = hint; addActionListener(e -> r.run()); }
        TMenuItem(String text) { super(text); setOpaque(true); applyTheme(); }
        TMenuItem(String text, int mnemonic, KeyStroke acc, Runnable r) {
            this(text);
            if (mnemonic != 0) setMnemonic(mnemonic);
            if (acc != null) setAccelerator(acc);
            addActionListener(e -> r.run());
        }
        @Override public void applyTheme() { setFont(Theme.body); setBorder(null); }
        @Override public Dimension getPreferredSize() { return itemSize(this); }
        @Override protected void paintComponent(Graphics g) { paintItem(g, this, false); }
    }

    static final class TCheckItem extends JCheckBoxMenuItem implements Themed {
        TCheckItem(String text) { super(text); setOpaque(true); applyTheme(); }
        @Override public void applyTheme() { setFont(Theme.body); setBorder(null); }
        @Override public Dimension getPreferredSize() { return itemSize(this); }
        @Override protected void paintComponent(Graphics g) { paintItem(g, this, isSelected()); }
    }

    static final class TSeparator extends JPopupMenu.Separator implements Themed {
        @Override public void applyTheme() {}
        @Override public Dimension getPreferredSize() { return new Dimension(10, 7); }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(Theme.c("popup.bg")); g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.c("popup.separator")); g.fillRect(8, getHeight() / 2, getWidth() - 16, 1);
        }
    }

    static final class TMenu extends JMenu implements Themed {
        TMenu(String text, int mnemonic) {
            super(text);
            setMnemonic(mnemonic);
            setOpaque(false);
            setFocusable(false);
            applyTheme();
        }
        TMenuItem add(String text, int mn, KeyStroke acc, Runnable r) {
            TMenuItem mi = new TMenuItem(text, mn, acc, r);
            super.add(mi);
            return mi;
        }
        void sep() { getPopupMenu().add(new TSeparator()); }
        @Override public void applyTheme() {
            setFont(Theme.body);
            JPopupMenu pm = getPopupMenu();
            pm.setBorder(new PopupBorder());
            pm.setBackground(Theme.c("popup.bg"));
            pm.setOpaque(true);
        }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 18, 26);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Gfx.quality(g);
            boolean hot = isSelected() || getModel().isArmed() || getModel().isRollover();
            if (hot) { g.setColor(Theme.c("menu.hover.bg")); g.fillRect(0, 0, getWidth(), getHeight()); }
            g.setFont(getFont());
            g.setColor(hot ? Theme.c("menu.hover.fg") : Theme.c("menu.fg"));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), 9, Gfx.baseline(fm, 0, getHeight()));
            g.dispose();
        }
    }

    static final class TMenuBar extends JMenuBar implements Themed {
        TMenuBar() { setOpaque(false); setBorder(null); setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0)); }
        @Override public void applyTheme() { repaint(); }
        @Override public Dimension getPreferredSize() { return new Dimension(10, 28); }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(Theme.c("menu.bg")); g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.c("toolbar.border")); g.fillRect(0, getHeight() - 1, getWidth(), 1);
        }
    }

    // ================================================================= scrolling
    static final class TScrollPane extends JScrollPane implements Themed {
        TScrollPane(Component view) {
            super(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
            setBorder(null);
            setViewportBorder(null);
            setOpaque(false);
            getViewport().setOpaque(false);
            getVerticalScrollBar().setUI(new ThinBar());
            getHorizontalScrollBar().setUI(new ThinBar());
            getVerticalScrollBar().setUnitIncrement(18);
            getVerticalScrollBar().setOpaque(false);
            getHorizontalScrollBar().setOpaque(false);
        }
        @Override public void applyTheme() { repaint(); }
    }

    static final class ThinBar extends BasicScrollBarUI {
        private boolean hover;
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); b.setFocusable(false); return b; }
        @Override public Dimension getPreferredSize(JComponent c) { return new Dimension(10, 10); }
        @Override protected void installListeners() {
            super.installListeners();
            scrollbar.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; scrollbar.repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; scrollbar.repaint(); }
            });
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(Theme.c("scrollbar.track")); g.fillRect(r.x, r.y, r.width, r.height);
        }
        @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(Theme.c(isDragging || hover ? "scrollbar.thumb.hover" : "scrollbar.thumb"));
            g.fillRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
            g.dispose();
        }
    }

    // ================================================================= focus order
    /** Tab order = component creation (tree) order, restricted to TabStop components. */
    static final class TabPolicy extends FocusTraversalPolicy {
        private List<Component> order(Container root) {
            List<Component> out = new ArrayList<>();
            walk(root, out);
            return out;
        }
        private void walk(Component c, List<Component> out) {
            if (c instanceof TabStop && c.isShowing() && c.isEnabled() && c.isFocusable()) out.add(c);
            if (c instanceof Container) for (Component ch : ((Container) c).getComponents()) if (ch.isVisible()) walk(ch, out);
        }
        private int indexOf(List<Component> l, Component c) {
            for (Component x = c; x != null; x = x.getParent()) { int i = l.indexOf(x); if (i >= 0) return i; }
            return -1;
        }
        @Override public Component getComponentAfter(Container r, Component c) {
            List<Component> l = order(r); if (l.isEmpty()) return null;
            int i = indexOf(l, c); return l.get((i + 1) % l.size());
        }
        @Override public Component getComponentBefore(Container r, Component c) {
            List<Component> l = order(r); if (l.isEmpty()) return null;
            int i = indexOf(l, c); return l.get(i <= 0 ? l.size() - 1 : i - 1);
        }
        @Override public Component getFirstComponent(Container r) { List<Component> l = order(r); return l.isEmpty() ? null : l.get(0); }
        @Override public Component getLastComponent(Container r) { List<Component> l = order(r); return l.isEmpty() ? null : l.get(l.size() - 1); }
        @Override public Component getDefaultComponent(Container r) { return getFirstComponent(r); }
    }

    /** Scrolls the focused component into view when it lives inside a scroll pane. */
    static void installAutoScroll() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", e -> {
            Object o = e.getNewValue();
            if (!(o instanceof JComponent) || o instanceof JTextComponent) return;
            JComponent c = (JComponent) o;
            if (SwingUtilities.getAncestorOfClass(JViewport.class, c) == null) return;
            SwingUtilities.invokeLater(() -> c.scrollRectToVisible(new Rectangle(-6, -6, c.getWidth() + 12, c.getHeight() + 12)));
        });
    }
}
