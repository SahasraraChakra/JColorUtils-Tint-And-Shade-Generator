package com.jsoftbharath.palette;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Palette import / export in text, JSON and CSS-variable flavours. (c) JSoft Bharath */
final class PaletteIO {
    private PaletteIO() {}

    static final class Entry {
        String kind = "Base", source; Color base; int tints = 10, shades = 10;
    }

    static String write(List<PaletteCard> cards, CopyFormat fmt, String ext) {
        switch (ext) {
            case "json": return json(cards, fmt);
            case "css": return css(cards);
            default: return text(cards, fmt);
        }
    }

    private static String text(List<PaletteCard> cards, CopyFormat fmt) {
        StringBuilder sb = new StringBuilder("# Tint & Shade Palette Generator - JSoft Bharath\n\n");
        for (PaletteCard c : cards) {
            sb.append("[PALETTE]\n");
            sb.append("Title: ").append(c.titleText()).append('\n');
            sb.append("Kind: ").append(c.kind).append('\n');
            if (c.source != null) sb.append("Source: ").append(c.source).append('\n');
            sb.append("BaseColor: ").append(ColorUtils.toHex(c.getBase(), true)).append('\n');
            sb.append("Tints: ").append(c.getTints()).append('\n');
            sb.append("Shades: ").append(c.getShades()).append("\n\n");
            sb.append("Base: ").append(ColorUtils.format(c.getBase(), fmt)).append('\n');
            for (SwatchRow.Item i : c.tintItems()) sb.append("Tint ").append(i.label).append(": ").append(ColorUtils.format(i.color, fmt)).append('\n');
            for (SwatchRow.Item i : c.shadeItems()) sb.append("Shade ").append(i.label).append(": ").append(ColorUtils.format(i.color, fmt)).append('\n');
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String q(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

    private static String json(List<PaletteCard> cards, CopyFormat fmt) {
        StringBuilder sb = new StringBuilder("{\n  \"app\": " + q(Main.APP_NAME + " - " + Main.COMPANY) + ",\n  \"palettes\": [\n");
        for (int p = 0; p < cards.size(); p++) {
            PaletteCard c = cards.get(p);
            sb.append("    {\n      \"title\": ").append(q(c.titleText())).append(",\n");
            sb.append("      \"kind\": ").append(q(c.kind)).append(",\n");
            sb.append("      \"source\": ").append(q(c.source == null ? "" : c.source)).append(",\n");
            sb.append("      \"base\": ").append(q(ColorUtils.toHex(c.getBase(), true))).append(",\n");
            sb.append("      \"tints\": ").append(c.getTints()).append(",\n      \"shades\": ").append(c.getShades()).append(",\n");
            sb.append("      \"colors\": {\n");
            List<String> lines = new ArrayList<>();
            lines.add("        \"base\": " + q(ColorUtils.format(c.getBase(), fmt)));
            for (SwatchRow.Item i : c.tintItems()) lines.add("        " + q("tint" + i.label) + ": " + q(ColorUtils.format(i.color, fmt)));
            for (SwatchRow.Item i : c.shadeItems()) lines.add("        " + q("shade" + i.label) + ": " + q(ColorUtils.format(i.color, fmt)));
            sb.append(String.join(",\n", lines)).append("\n      }\n    }").append(p < cards.size() - 1 ? "," : "").append('\n');
        }
        return sb.append("  ]\n}\n").toString();
    }

    private static String css(List<PaletteCard> cards) {
        StringBuilder sb = new StringBuilder("/* Tint & Shade Palette Generator - JSoft Bharath */\n:root {\n");
        int n = 1;
        for (PaletteCard c : cards) {
            String p = "--c" + n++;
            sb.append("  /* ").append(c.titleText()).append(" */\n");
            sb.append("  ").append(p).append(": ").append(ColorUtils.toHex(c.getBase(), true)).append(";\n");
            for (SwatchRow.Item i : c.tintItems()) sb.append("  ").append(p).append("-tint-").append(i.label.replace("+", "").replace("%", "")).append(": ").append(ColorUtils.toHex(i.color, true)).append(";\n");
            for (SwatchRow.Item i : c.shadeItems()) sb.append("  ").append(p).append("-shade-").append(i.label.replace("-", "").replace("%", "")).append(": ").append(ColorUtils.toHex(i.color, true)).append(";\n");
        }
        return sb.append("}\n").toString();
    }

    // ------------------------------------------------------------------ reading
    private static final Pattern JSON_P = Pattern.compile(
        "\"kind\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"source\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"base\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"tints\"\\s*:\\s*(\\d+)\\s*,\\s*\"shades\"\\s*:\\s*(\\d+)");

    static List<Entry> read(String text) {
        List<Entry> out = new ArrayList<>();
        Matcher m = JSON_P.matcher(text);
        while (m.find()) {
            Color c = ColorUtils.parseToken(m.group(3));
            if (c == null) continue;
            Entry e = new Entry();
            e.kind = m.group(1); e.source = m.group(2).isEmpty() ? null : m.group(2); e.base = c;
            e.tints = Math.min(30, Integer.parseInt(m.group(4))); e.shades = Math.min(30, Integer.parseInt(m.group(5)));
            out.add(e);
        }
        if (!out.isEmpty()) return out;
        if (text.contains("[PALETTE]")) {
            for (String block : text.split("\\[PALETTE\\]")) {
                Entry e = new Entry();
                for (String line : block.split("\\R")) {
                    int i = line.indexOf(':');
                    if (i < 0) continue;
                    String k = line.substring(0, i).trim(), v = line.substring(i + 1).trim();
                    try {
                        if (k.equals("BaseColor")) e.base = ColorUtils.parseToken(v);
                        else if (k.equals("Kind")) e.kind = v;
                        else if (k.equals("Source")) e.source = v;
                        else if (k.equals("Tints")) e.tints = Math.min(30, Integer.parseInt(v));
                        else if (k.equals("Shades")) e.shades = Math.min(30, Integer.parseInt(v));
                        else if (k.equals("Count")) { e.tints = Math.min(30, Integer.parseInt(v)); e.shades = e.tints; }
                    } catch (NumberFormatException ignored) {}
                }
                if (e.base != null) out.add(e);
            }
            if (!out.isEmpty()) return out;
        }
        // fallback: every hex color found becomes a base palette
        for (Color c : ColorUtils.extractHex(text)) {
            boolean dup = false;
            for (Entry e : out) if (e.base.equals(c)) dup = true;
            if (!dup) { Entry e = new Entry(); e.base = c; out.add(e); }
        }
        return out;
    }
}
