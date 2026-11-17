package com.jsoftbharath.palette;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Developer tool: renders the whole UI to a PNG without a display (headless). Not part of the jar. */
public final class Shot {
    static void layout(Component c) {
        if (c instanceof Container) {
            Container ct = (Container) c;
            ct.doLayout();
            for (Component ch : ct.getComponents()) layout(ch);
        }
    }

    public static void main(String[] a) throws Exception {
        System.setProperty("java.awt.headless", "true");
        String theme = a.length > 0 ? a[0] : "light";
        String out = a.length > 1 ? a[1] : "/tmp/shot.png";
        int w = a.length > 2 ? Integer.parseInt(a[2]) : 1240, h = a.length > 3 ? Integer.parseInt(a[3]) : 760;
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        Theme.use(theme);
        Theme.installDefaults();
        final BufferedImage[] img = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            WindowHost host = new WindowHost() {
                public void minimize() {} public void toggleMaximize() {} public void close() {}
                public boolean isMaximized() { return false; } public int windowWidth() { return 1240; }
                public void moveTo(Point p, Point g) {} public void restoreForDrag(Point s, double r, int y) {}
            };
            AppPanel p = new AppPanel(host);
            if (a.length > 4 && a[4].equals("demo")) {
                try {
                    java.lang.reflect.Field f = AppPanel.class.getDeclaredField("input"); f.setAccessible(true);
                    ((javax.swing.JTextArea) f.get(p)).setText("663399, 2563eb");
                    f = AppPanel.class.getDeclaredField("cComp"); f.setAccessible(true);
                    ((javax.swing.AbstractButton) f.get(p)).setSelected(true);
                    java.lang.reflect.Method m = AppPanel.class.getDeclaredMethod("generate"); m.setAccessible(true); m.invoke(p);
                    f = AppPanel.class.getDeclaredField("ctx"); f.setAccessible(true);
                    AppContext c = (AppContext) f.get(p);
                    for (String hx : new String[]{"663399", "7446a2", "e3daec", "2563eb", "a0662f", "1c0e2a", "99cc33"}) c.pin(Color.decode("#" + hx), hx.equals("663399") ? "Base" : "+9%");
                    c.setFg(Color.decode("#663399")); c.setBg(Color.decode("#e3daec"));
                } catch (Exception ex) { throw new RuntimeException(ex); }
            }
            p.setSize(w, h);
            for (int i = 0; i < 4; i++) layout(p);
            img[0] = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img[0].createGraphics();
            p.printAll(g);
            g.dispose();
        });
        javax.imageio.ImageIO.write(img[0], "png", new File(out));
        System.out.println("fonts: " + Theme.bodyFamily() + " / " + Theme.monoFamily());
        System.exit(0);
    }
}
