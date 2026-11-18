package dev.agra.gui.core;

import dev.agra.gui.module.hud.InterfaceModule;
import dev.agra.gui.ui.Draw;

import java.awt.Color;

public final class Theme {
    public record Scheme(String name, int a, int b) {}

    public static final Scheme[] SCHEMES = {
            new Scheme("purple", 0xFF8B5CF6, 0xFFD946EF),
            new Scheme("ocean", 0xFF38BDF8, 0xFF6366F1),
            new Scheme("sunset", 0xFFFB923C, 0xFFEC4899),
            new Scheme("mint", 0xFF34D399, 0xFF22D3EE),
            new Scheme("rose", 0xFFFB7185, 0xFFF0ABFC),
            new Scheme("blood", 0xFFEF4444, 0xFF9F1239),
            new Scheme("mono", 0xFFE5E7EB, 0xFF9CA3AF),
            new Scheme("rainbow", 0, 0)
    };

    public static String[] names() {
        String[] n = new String[SCHEMES.length];
        for (int i = 0; i < n.length; i++) n[i] = SCHEMES[i].name();
        return n;
    }

    private Theme() {}

    private static Scheme scheme() { return SCHEMES[InterfaceModule.I.scheme.index]; }
    private static boolean rainbow() { return scheme().name().equals("rainbow"); }

    /** Colour along the accent gradient; t in [0,1] picks a position (e.g. list index). */
    public static int gradient(float t) {
        double speed = InterfaceModule.I.gradSpeed.get();
        double time = System.currentTimeMillis() / 1000.0 * speed;
        if (rainbow()) {
            float h = (float) ((time * 0.2 + t * 0.35) % 1.0);
            return 0xFF000000 | (Color.HSBtoRGB(h, 0.55f, 1f) & 0xFFFFFF);
        }
        Scheme s = scheme();
        float k = (float) (0.5 + 0.5 * Math.sin(time + t * 2.0));
        return Draw.lerp(s.a(), s.b(), k);
    }

    public static int accent() { return rainbow() ? gradient(0f) : scheme().a(); }

    public static float opacity() { return (float) (InterfaceModule.I.opacity.get() / 100.0); }
    public static int radius() { return InterfaceModule.I.radius.i(); }

    private static int tint(float amount) { return Draw.lerp(0xFF000000, rainbow() ? 0xFF8B5CF6 : scheme().a(), amount); }

    public static int bg()       { return Draw.alpha(tint(0.10f), opacity()); }
    public static int btn()      { return Draw.alpha(tint(0.22f), Math.min(1f, opacity() + 0.1f)); }
    public static int btnHover() { return tint(0.34f); }
    public static int selected() { return tint(0.62f); }
    public static int edge()     { return tint(0.30f); }
    public static int text()     { return 0xFFF3F0FF; }
    public static int dim()      { return Draw.lerp(0xFF8E8EA0, rainbow() ? 0xFF8B5CF6 : scheme().a(), 0.35f); }
}
