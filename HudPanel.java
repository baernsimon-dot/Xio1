package dev.agra.gui.ui;

import dev.agra.gui.core.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Small glass box with a title and lines of text, used by several HUD modules. */
public final class HudPanel {
    private HudPanel() {}

    public static void draw(DrawContext ctx, String title, List<String> lines, int anchor, int offX, int offY, boolean shadow) {
        int w = Draw.width(title);
        for (String l : lines) w = Math.max(w, Draw.width(l));
        w += 14;
        int h = 22 + lines.size() * 11;
        int[] p = HudAnchor.at(anchor, w, h, offX, offY);
        Draw.glass(ctx, p[0], p[1], w, h, 1f);
        Draw.text(ctx, title, p[0] + 7, p[1] + 6, Theme.gradient(0f), shadow);
        int y = p[1] + 19;
        for (String l : lines) {
            Draw.text(ctx, l, p[0] + 7, y, Theme.text(), shadow);
            y += 11;
        }
    }
}
