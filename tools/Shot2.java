package com.jsoftbharath.palette;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Developer tool: renders dialog bodies + menu items headless. Not part of the jar. */
public final class Shot2 {
    public static void main(String[] a) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        Theme.use(a.length > 0 ? a[0] : "light");
        Theme.installDefaults();
        SwingUtilities.invokeAndWait(() -> {
            try {
                JPanel root = new JPanel(null) {
                    @Override protected void paintComponent(Graphics g) { g.setColor(Theme.c("window.bg")); g.fillRect(0, 0, getWidth(), getHeight()); }
                };
                Dialogs.ColorChooser cc = new Dialogs.ColorChooser(new Color(0x663399));
                cc.setBounds(10, 10, 470, 270);
                java.lang.reflect.Method m = Dialogs.class.getDeclaredMethod("messageBody", String.class, boolean.class, String.class, int.class);
                m.setAccessible(true);
                JComponent body = (JComponent) m.invoke(null, "warn", true, Dialogs.esc("Remove all 4 generated palettes?\nThe pinboard is not affected."), 330);
                body.setBounds(500, 10, 420, 110);
                Widgets.TButton c1 = new Widgets.TButton("Cancel", null, Widgets.Kind.NEUTRAL), c2 = new Widgets.TButton("Clear all", null, Widgets.Kind.DANGER);
                c1.setBounds(740, 125, c1.getPreferredSize().width, 28); c2.setBounds(740 + c1.getWidth() + 8, 125, c2.getPreferredSize().width, 28);
                Widgets.TMenuItem i1 = new Widgets.TMenuItem("Generate", 0, KeyStroke.getKeyStroke("F5"), () -> {});
                Widgets.TMenuItem i2 = new Widgets.TMenuItem("Randomize", 0, KeyStroke.getKeyStroke("ctrl R"), () -> {});
                Widgets.TCheckItem i3 = new Widgets.TCheckItem("Dark theme"); i3.setSelected(true);
                JPanel pop = new JPanel(new GridLayout(0, 1));
                pop.add(i1); pop.add(i2); pop.add(new Widgets.TSeparator()); pop.add(i3);
                pop.setBounds(500, 170, 220, 100);
                pop.setBorder(new Widgets.PopupBorder());
                Widgets.TCheck chk = new Widgets.TCheck("Comp."); chk.setDot(Color.ORANGE); chk.setSelected(true); chk.setBounds(740, 180, 100, 22);
                for (Component c : new Component[]{cc, body, c1, c2, pop, chk}) root.add(c);
                root.setSize(940, 290);
                Theme.applyTree(root);
                for (int k = 0; k < 3; k++) Shot.layout(root);
                BufferedImage img = new BufferedImage(940, 290, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                root.printAll(g);
                g.dispose();
                javax.imageio.ImageIO.write(img, "png", new File(a.length > 1 ? a[1] : "/tmp/shot_dlg.png"));
            } catch (Exception e) { e.printStackTrace(); }
        });
        System.exit(0);
    }
}
