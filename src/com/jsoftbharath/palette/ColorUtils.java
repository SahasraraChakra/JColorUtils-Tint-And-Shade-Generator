package com.jsoftbharath.palette;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Output formats for copied color codes. */
enum CopyFormat {
    HEX("HEX"), HEX_HASH("HEX (#)"), RGB("RGB"), HSL("HSL"), HSB("HSB / HSV"),
    JAVA("Java Color"), ANDROID("Android ARGB");

    final String label;
    CopyFormat(String label) { this.label = label; }
    @Override public String toString() { return label; }
}

/** Color math, parsing and formatting. Core routines are carried over from the original PaletteGeneratorApp. */
final class ColorUtils {
    private ColorUtils() {}

    // ---------------------------------------------------------------- parsing
    static Color hexToColor(String s) {
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() == 3) {
            StringBuilder sb = new StringBuilder();
            for (char c : h.toCharArray()) sb.append(c).append(c);
            h = sb.toString();
        }
        if (h.length() != 6 || !h.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid hex color: " + s);
        return new Color(Integer.parseInt(h, 16));
    }

    private static final Pattern RGB_P = Pattern.compile("rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HSL_P = Pattern.compile("hsl\\(\\s*(\\d+)\\s*,\\s*(\\d+)%\\s*,\\s*(\\d+)%\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HSB_P = Pattern.compile("hsb\\(\\s*(\\d+)\\s*,\\s*(\\d+)%\\s*,\\s*(\\d+)%\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVA_P = Pattern.compile("0x([0-9a-fA-F]{6})\\b");
    private static final Pattern HEX_ANY = Pattern.compile("#?[0-9a-fA-F]{6}\\b");

    /** Strict single-token parse (hex, rgb(), hsl(), hsb(), 0xRRGGBB, #AARRGGBB). Returns null if not a color. */
    static Color parseToken(String s) {
        String t = s.trim();
        if (t.isEmpty()) return null;
        Matcher m = RGB_P.matcher(t);
        if (m.matches()) return new Color(clamp(Integer.parseInt(m.group(1))), clamp(Integer.parseInt(m.group(2))), clamp(Integer.parseInt(m.group(3))));
        m = HSL_P.matcher(t);
        if (m.matches()) return hslToColor(Integer.parseInt(m.group(1)), Math.min(100, Integer.parseInt(m.group(2))), Math.min(100, Integer.parseInt(m.group(3))));
        m = HSB_P.matcher(t);
        if (m.matches()) return Color.getHSBColor(Integer.parseInt(m.group(1)) / 360f, Math.min(100, Integer.parseInt(m.group(2))) / 100f, Math.min(100, Integer.parseInt(m.group(3))) / 100f);
        m = JAVA_P.matcher(t);
        if (m.find() && t.toLowerCase().startsWith("0x")) return new Color(Integer.parseInt(m.group(1), 16));
        if (t.matches("#[0-9a-fA-F]{8}")) return new Color(Integer.parseInt(t.substring(3), 16));
        try { return hexToColor(t); } catch (IllegalArgumentException e) { return null; }
    }

    /** Splits free text on commas / whitespace / semicolons (not inside parentheses). */
    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (char ch : text.toCharArray()) {
            if (ch == '(') depth++;
            if (ch == ')') depth = Math.max(0, depth - 1);
            boolean sep = depth == 0 && (ch == ',' || ch == ';' || Character.isWhitespace(ch));
            if (sep) { if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); } }
            else cur.append(ch);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    /** Finds every hex color inside arbitrary text (used for import fallbacks). */
    static List<Color> extractHex(String text) {
        List<Color> out = new ArrayList<>();
        Matcher m = HEX_ANY.matcher(text);
        while (m.find()) out.add(hexToColor(m.group()));
        return out;
    }

    // ---------------------------------------------------------------- conversions
    static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    static String toHex(Color c, boolean withHash) {
        return String.format("%s%02x%02x%02x", withHash ? "#" : "", c.getRed(), c.getGreen(), c.getBlue());
    }

    static float[] rgbToHsl(Color c) {
        float r = c.getRed() / 255f, g = c.getGreen() / 255f, b = c.getBlue() / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float h, s, l = (max + min) / 2f;
        if (max == min) { h = s = 0f; }
        else {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) h = (g - b) / d + (g < b ? 6f : 0f);
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h *= 60f;
        }
        return new float[]{h, s * 100f, l * 100f};
    }

    static Color hslToColor(float h, float s, float l) {
        h = ((h % 360) + 360) % 360;
        s /= 100f; l /= 100f;
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = l - c / 2f;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return new Color(clamp(Math.round((r + m) * 255)), clamp(Math.round((g + m) * 255)), clamp(Math.round((b + m) * 255)));
    }

    static Color rotateHue(Color c, double degrees) {
        float[] hsl = rgbToHsl(c);
        float nh = (float) ((hsl[0] + degrees) % 360);
        if (nh < 0) nh += 360;
        return hslToColor(nh, hsl[1], hsl[2]);
    }

    static Color tint(Color c, double f) {
        return new Color(clamp((int) Math.round(c.getRed() + (255 - c.getRed()) * f)),
            clamp((int) Math.round(c.getGreen() + (255 - c.getGreen()) * f)),
            clamp((int) Math.round(c.getBlue() + (255 - c.getBlue()) * f)));
    }

    static Color shade(Color c, double f) {
        return new Color(clamp((int) Math.round(c.getRed() * (1 - f))), clamp((int) Math.round(c.getGreen() * (1 - f))),
            clamp((int) Math.round(c.getBlue() * (1 - f))));
    }

    static Color random() {
        java.util.Random r = new java.util.Random();
        return hslToColor(r.nextInt(360), 45 + r.nextInt(40), 35 + r.nextInt(25));
    }

    // ---------------------------------------------------------------- formatting
    static String format(Color c, CopyFormat fmt) {
        switch (fmt) {
            case HEX: return toHex(c, false);
            case HEX_HASH: return toHex(c, true);
            case RGB: return String.format("rgb(%d, %d, %d)", c.getRed(), c.getGreen(), c.getBlue());
            case HSL: { float[] h = rgbToHsl(c); return String.format("hsl(%d, %d%%, %d%%)", Math.round(h[0]), Math.round(h[1]), Math.round(h[2])); }
            case HSB: { float[] h = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                return String.format("hsb(%d, %d%%, %d%%)", Math.round(h[0] * 360), Math.round(h[1] * 100), Math.round(h[2] * 100)); }
            case JAVA: return String.format("new Color(0x%02X%02X%02X)", c.getRed(), c.getGreen(), c.getBlue());
            case ANDROID: return String.format("#FF%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
            default: return toHex(c, true);
        }
    }

    /** Terser form used for the small code label above each swatch. */
    static String compact(Color c, CopyFormat fmt) {
        switch (fmt) {
            case RGB: return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
            case HSL: { float[] h = rgbToHsl(c); return Math.round(h[0]) + "\u00B0," + Math.round(h[1]) + "%," + Math.round(h[2]) + "%"; }
            case HSB: { float[] h = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                return Math.round(h[0] * 360) + "\u00B0," + Math.round(h[1] * 100) + "%," + Math.round(h[2] * 100) + "%"; }
            case JAVA: return String.format("0x%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
            default: return format(c, fmt);
        }
    }

    // ---------------------------------------------------------------- contrast
    private static double lin(int v) {
        double s = v / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    static double luminance(Color c) {
        return 0.2126 * lin(c.getRed()) + 0.7152 * lin(c.getGreen()) + 0.0722 * lin(c.getBlue());
    }

    static double contrast(Color a, Color b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double apcaY(Color c) {
        return Math.pow(c.getRed() / 255.0, 2.4) * 0.2126729 + Math.pow(c.getGreen() / 255.0, 2.4) * 0.7151522
            + Math.pow(c.getBlue() / 255.0, 2.4) * 0.0721750;
    }

    /** APCA (SAPC 0.98G-4g) lightness contrast Lc for text on background. Negative = light text on dark. */
    static double apca(Color text, Color bg) {
        double yt = apcaY(text), yb = apcaY(bg);
        final double thr = 0.022, clmp = 1.414;
        if (yt < thr) yt += Math.pow(thr - yt, clmp);
        if (yb < thr) yb += Math.pow(thr - yb, clmp);
        if (Math.abs(yb - yt) < 0.0005) return 0;
        double out;
        if (yb > yt) { double s = (Math.pow(yb, 0.56) - Math.pow(yt, 0.57)) * 1.14; out = s < 0.1 ? 0 : s - 0.027; }
        else { double s = (Math.pow(yb, 0.65) - Math.pow(yt, 0.62)) * 1.14; out = s > -0.1 ? 0 : s + 0.027; }
        return out * 100;
    }

    /** "AAA", "AA" or "" for normal text. */
    static String gradeNormal(double r) { return r >= 7 ? "AAA" : r >= 4.5 ? "AA" : ""; }
    static String gradeLarge(double r) { return r >= 4.5 ? "AAA" : r >= 3 ? "AA" : ""; }
}

/** CSS named colors, used to label a color with its nearest name. */
final class ColorNames {
    private ColorNames() {}

    private static final String DATA =
        "AliceBlue f0f8ff,AntiqueWhite faebd7,Aqua 00ffff,Aquamarine 7fffd4,Azure f0ffff,Beige f5f5dc,Bisque ffe4c4,Black 000000,"
        + "BlanchedAlmond ffebcd,Blue 0000ff,BlueViolet 8a2be2,Brown a52a2a,BurlyWood deb887,CadetBlue 5f9ea0,Chartreuse 7fff00,"
        + "Chocolate d2691e,Coral ff7f50,CornflowerBlue 6495ed,Cornsilk fff8dc,Crimson dc143c,DarkBlue 00008b,DarkCyan 008b8b,"
        + "DarkGoldenRod b8860b,DarkGray a9a9a9,DarkGreen 006400,DarkKhaki bdb76b,DarkMagenta 8b008b,DarkOliveGreen 556b2f,"
        + "DarkOrange ff8c00,DarkOrchid 9932cc,DarkRed 8b0000,DarkSalmon e9967a,DarkSeaGreen 8fbc8f,DarkSlateBlue 483d8b,"
        + "DarkSlateGray 2f4f4f,DarkTurquoise 00ced1,DarkViolet 9400d3,DeepPink ff1493,DeepSkyBlue 00bfff,DimGray 696969,"
        + "DodgerBlue 1e90ff,FireBrick b22222,FloralWhite fffaf0,ForestGreen 228b22,Fuchsia ff00ff,Gainsboro dcdcdc,GhostWhite f8f8ff,"
        + "Gold ffd700,GoldenRod daa520,Gray 808080,Green 008000,GreenYellow adff2f,HoneyDew f0fff0,HotPink ff69b4,IndianRed cd5c5c,"
        + "Indigo 4b0082,Ivory fffff0,Khaki f0e68c,Lavender e6e6fa,LavenderBlush fff0f5,LawnGreen 7cfc00,LemonChiffon fffacd,"
        + "LightBlue add8e6,LightCoral f08080,LightCyan e0ffff,LightGoldenRodYellow fafad2,LightGray d3d3d3,LightGreen 90ee90,"
        + "LightPink ffb6c1,LightSalmon ffa07a,LightSeaGreen 20b2aa,LightSkyBlue 87cefa,LightSlateGray 778899,LightSteelBlue b0c4de,"
        + "LightYellow ffffe0,Lime 00ff00,LimeGreen 32cd32,Linen faf0e6,Maroon 800000,MediumAquaMarine 66cdaa,MediumBlue 0000cd,"
        + "MediumOrchid ba55d3,MediumPurple 9370db,MediumSeaGreen 3cb371,MediumSlateBlue 7b68ee,MediumSpringGreen 00fa9a,"
        + "MediumTurquoise 48d1cc,MediumVioletRed c71585,MidnightBlue 191970,MintCream f5fffa,MistyRose ffe4e1,Moccasin ffe4b5,"
        + "NavajoWhite ffdead,Navy 000080,OldLace fdf5e6,Olive 808000,OliveDrab 6b8e23,Orange ffa500,OrangeRed ff4500,Orchid da70d6,"
        + "PaleGoldenRod eee8aa,PaleGreen 98fb98,PaleTurquoise afeeee,PaleVioletRed db7093,PapayaWhip ffefd5,PeachPuff ffdab9,"
        + "Peru cd853f,Pink ffc0cb,Plum dda0dd,PowderBlue b0e0e6,Purple 800080,RebeccaPurple 663399,Red ff0000,RosyBrown bc8f8f,"
        + "RoyalBlue 4169e1,SaddleBrown 8b4513,Salmon fa8072,SandyBrown f4a460,SeaGreen 2e8b57,SeaShell fff5ee,Sienna a0522d,"
        + "Silver c0c0c0,SkyBlue 87ceeb,SlateBlue 6a5acd,SlateGray 708090,Snow fffafa,SpringGreen 00ff7f,SteelBlue 4682b4,Tan d2b48c,"
        + "Teal 008080,Thistle d8bfd8,Tomato ff6347,Turquoise 40e0d0,Violet ee82ee,Wheat f5deb3,White ffffff,WhiteSmoke f5f5f5,"
        + "Yellow ffff00,YellowGreen 9acd32";

    private static final List<String> NAMES = new ArrayList<>();
    private static final List<Integer> VALUES = new ArrayList<>();

    static {
        for (String e : DATA.split(",")) {
            String[] p = e.split(" ");
            NAMES.add(p[0]);
            VALUES.add(Integer.parseInt(p[1], 16));
        }
    }

    /** Nearest CSS color name; prefixed with a "~" style marker when it is not an exact match. */
    static String describe(Color c) {
        int best = 0;
        long bestD = Long.MAX_VALUE;
        int rgb = c.getRGB() & 0xFFFFFF;
        for (int i = 0; i < VALUES.size(); i++) {
            int v = VALUES.get(i);
            if (v == rgb) return NAMES.get(i);
            long dr = ((v >> 16) & 255) - c.getRed(), dg = ((v >> 8) & 255) - c.getGreen(), db = (v & 255) - c.getBlue();
            long rm = (((v >> 16) & 255) + c.getRed()) / 2;
            long d = (((512 + rm) * dr * dr) >> 8) + 4 * dg * dg + (((767 - rm) * db * db) >> 8);
            if (d < bestD) { bestD = d; best = i; }
        }
        return "\u2248 " + NAMES.get(best);
    }
}
