package dev.agra.gui.ui;

import dev.agra.gui.core.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Draw {
    private Draw() {}

    public static int alpha(int argb, float mul) {
        int a = (int) (((argb >>> 24) & 255) * Math.max(0f, Math.min(1f, mul)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (int) (((a >>> 24) & 255) + (((b >>> 24) & 255) - ((a >>> 24) & 255)) * t);
        int ar = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int ag = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int ab = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    public static int width(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }

    public static void text(DrawContext ctx, String s, int x, int y, int argb, boolean shadow) {
        if (((argb >>> 24) & 255) < 8) return; // vanilla renders very low alpha as opaque
        ctx.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, argb, shadow);
    }

    /** Filled rounded rectangle (row based, no overdraw so translucent colours stay clean). */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int argb) {
        if (((argb >>> 24) & 255) == 0 || w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.ceil(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, argb);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, argb);
        }
        c.fill(x, y + r, x + w, y + h - r, argb);
    }

    /** Glass look: soft border, translucent fill and an optional gloss highlight on top. */
    public static void glass(DrawContext c, int x, int y, int w, int h, int r, int fill, int edge, float a, boolean gloss) {
        rrect(c, x - 1, y - 1, w + 2, h + 2, r + 1, alpha(edge, a * 0.9f));
        rrect(c, x, y, w, h, r, alpha(fill, a));
        if (gloss && h > 8) {
            c.fillGradient(x + r, y + 1, x + w - r, y + Math.max(3, h / 2), alpha(0x26FFFFFF, a), alpha(0x00FFFFFF, a));
        }
    }

    public static void glass(DrawContext c, int x, int y, int w, int h, float a) {
        glass(c, x, y, w, h, Theme.radius(), Theme.bg(), Theme.edge(), a, true);
    }
}
