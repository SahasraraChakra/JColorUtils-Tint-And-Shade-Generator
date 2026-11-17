package com.jsoftbharath.palette;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

/** Developer self-test (headless). Not part of the jar. */
public final class Check {
    static int fails = 0;
    static void ok(boolean c, String m) { System.out.println((c ? "PASS  " : "FAIL  ") + m); if (!c) fails++; }
    @SuppressWarnings("unchecked")
    static <T> T field(Object o, String n) throws Exception { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return (T) f.get(o); }
    static void collect(Component c, List<Component> out) {
        if (c instanceof TabStop && c.isVisible() && c.isFocusable() && c.isEnabled()) out.add(c);
        if (c instanceof Container) for (Component ch : ((Container) c).getComponents()) if (ch.isVisible()) collect(ch, out);
    }
    static String name(Component c) {
        if (c instanceof AbstractButton && !((AbstractButton) c).getText().isEmpty()) return "[" + ((AbstractButton) c).getText() + "]";
        if (c instanceof SwatchRow) return "<row n=" + ((SwatchRow) c).count() + ">";
        return "<" + c.getClass().getSimpleName() + ">";
    }
    static void act(JComponent c, String key) { c.getActionMap().get(key).actionPerformed(new ActionEvent(c, 0, key)); }

    public static void main(String[] a) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        Theme.use("light");
        Theme.installDefaults();
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Exception e) { e.printStackTrace(); fails++; } });
        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
        System.exit(fails);
    }

    static void run() throws Exception {
        WindowHost host = new WindowHost() {
            public void minimize() {} public void toggleMaximize() {} public void close() {}
            public boolean isMaximized() { return false; } public int windowWidth() { return 1240; }
            public void moveTo(Point p, Point g) {} public void restoreForDrag(Point s, double r, int y) {}
        };
        AppPanel p = new AppPanel(host);
        p.setSize(1260, 880);
        Shot.layout(p); Shot.layout(p);
        JTextArea in = field(p, "input");
        in.setText("663399, 2563eb");
        ((AbstractButton) field(p, "cComp")).setSelected(true);
        Method gen = AppPanel.class.getDeclaredMethod("generate"); gen.setAccessible(true); gen.invoke(p);
        Shot.layout(p); Shot.layout(p);
        List<PaletteCard> cards = field(p, "cards");
        ok(cards.size() == 4, "2 bases + 2 complements = 4 palettes (got " + cards.size() + ")");

        // tab order
        List<Component> order = new ArrayList<>();
        collect(p, order);
        StringBuilder sb = new StringBuilder();
        for (Component c : order) sb.append(name(c)).append(' ');
        System.out.println("TAB ORDER: " + sb);
        int iGen = -1, iPin = -1, iFirstBase = -1;
        for (int i = 0; i < order.size(); i++) {
            String n = name(order.get(i));
            if (n.equals("[Generate]")) iGen = i;
            if (n.equals("[Clear]") && iPin < 0) iPin = i;
            if (n.equals("[Change]") && iFirstBase < 0) iFirstBase = i;
        }
        ok(iGen > 0 && iPin > iGen && iFirstBase > iPin, "tab order: toolbar/input -> Generate -> pinboard -> palettes");
        // inside first palette: base box, Change, Copy All, close, tints, shades, next palette base
        int s = iFirstBase - 1;
        ok(order.get(s) instanceof SwatchRow && ((SwatchRow) order.get(s)).count() == 1, "palette: base box comes first");
        ok(name(order.get(s + 1)).equals("[Change]") && name(order.get(s + 2)).equals("[Copy All]"), "palette: Change, Copy All");
        ok(order.get(s + 4) instanceof SwatchRow && ((SwatchRow) order.get(s + 4)).count() == 10, "palette: tint row is one tab stop");
        ok(order.get(s + 5) instanceof SwatchRow && order.get(s + 6) instanceof SwatchRow && ((SwatchRow) order.get(s + 6)).count() == 1, "palette: shade row, then next palette's base box");

        // color box keys
        SwatchRow tint = (SwatchRow) order.get(s + 4);
        AppContext ctx = field(p, "ctx");
        List<String> msgs = new ArrayList<>();
        java.util.function.Consumer<String> old = ctx.status;
        ctx.status = m -> { msgs.add(m); old.accept(m); };
        act(tint, "r"); act(tint, "r");
        ok(tint.getActive() == 2, "Right arrow moves inside the row");
        act(tint, "copy");
        ok(msgs.get(msgs.size() - 1).startsWith("Copied") || msgs.get(msgs.size() - 1).startsWith("Clipboard"), "Enter copies: " + msgs.get(msgs.size() - 1));
        PinboardPanel pb = field(p, "pinboard");
        act(tint, "pin");
        ok(pb.count() == 1 && pb.colors().get(0).equals(tint.activeItem().color), "Ctrl+Enter pins to pinboard");
        act(tint, "pin");
        ok(pb.count() == 1, "pinning the same color twice is ignored");
        ContrastPanel cp = field(p, "contrast");
        act(tint, "fg");
        Color fg = field(cp, "fg");
        ok(fg.equals(tint.activeItem().color), "Ctrl+F sets contrast foreground");
        SwatchRow shade = (SwatchRow) order.get(s + 5);
        act(shade, "bg");
        Color bg = field(cp, "bg");
        ok(bg != null && bg.equals(shade.activeItem().color), "Ctrl+B sets contrast background");
        String rep = cp.report();
        double expect = ColorUtils.contrast(fg, bg);
        ok(rep.contains(String.format("Ratio: %.2f:1", expect)), "contrast is fg vs chosen bg: " + String.format("%.2f", expect));
        // swap with explicit bg swaps fg/bg
        Method swap = ContrastPanel.class.getDeclaredMethod("swap"); swap.setAccessible(true); swap.invoke(cp);
        ok(((Color) field(cp, "fg")).equals(bg) && ((Color) field(cp, "bg")).equals(fg), "Swap with chosen bg swaps FG/BG");

        // pinboard keys on pinned row
        SwatchRow pinRow = field(pb, "row");
        act(shade, "pin");
        ok(pb.count() == 2, "second pin added");
        act(pinRow, "l"); act(pinRow, "h");
        ok(pinRow.getActive() == 0, "Home selects first pinned");

        // auto bg swap
        Method reset = null;
        field(cp, "bg");
        Field bgF = ContrastPanel.class.getDeclaredField("bg"); bgF.setAccessible(true); bgF.set(cp, null);
        boolean before = (boolean) field(cp, "autoDark");
        swap.invoke(cp);
        ok((boolean) field(cp, "autoDark") != before, "Swap with automatic bg toggles white/black");

        // import / export roundtrip
        for (String ext : new String[]{"txt", "json", "css"}) {
            String out = PaletteIO.write(cards, CopyFormat.HEX_HASH, ext);
            ok(out.length() > 100, ext + " export produced " + out.length() + " chars");
            if (!ext.equals("css")) {
                List<PaletteIO.Entry> back = PaletteIO.read(out);
                ok(back.size() == 4 && back.get(1).kind.equals("Complementary") && back.get(0).tints == 10, ext + " import round-trips 4 palettes");
            }
        }
        // math
        ok(Math.abs(ColorUtils.contrast(Color.BLACK, Color.WHITE) - 21) < 1e-9, "contrast black/white = 21");
        ok(Math.abs(ColorUtils.apca(Color.BLACK, Color.WHITE) - 106.0) < 1.0, "APCA black on white ~ Lc 106: " + ColorUtils.apca(Color.BLACK, Color.WHITE));
        ok(ColorUtils.apca(Color.WHITE, Color.BLACK) < -100, "APCA white on black negative: " + ColorUtils.apca(Color.WHITE, Color.BLACK));
        ok(ColorUtils.parseToken("rgb(102, 51, 153)").equals(new Color(0x663399)), "parse rgb()");
        ok(ColorUtils.parseToken("hsl(270, 50%, 40%)") != null, "parse hsl()");
        ok(ColorUtils.parseToken("zzz") == null, "invalid token rejected");
        ok(ColorNames.describe(new Color(0x663399)).equals("RebeccaPurple"), "exact color name");
        List<String> toks = ColorUtils.tokenize("663399, rgb(1, 2, 3)\n#abc");
        ok(toks.size() == 3, "tokenizer keeps rgb(...) together: " + toks);
    }
}
