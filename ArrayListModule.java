package dev.agra.gui.module.hud;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.ModuleManager;
import dev.agra.gui.core.Setting;
import dev.agra.gui.core.Theme;
import dev.agra.gui.ui.Draw;
import dev.agra.gui.ui.HudAnchor;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArrayListModule extends Module {
    private final Setting.Mode position = mode("position", "Screen corner of the list.", 1, HudAnchor.NAMES);
    private final Setting.Mode sort = mode("sort", "How entries are ordered.", 0, "length", "alphabetical");
    private final Setting.Bool gradient = bool("gradient", "Color entries along the accent gradient.", true);
    private final Setting.Bool background = bool("background", "Draw a glass background behind entries.", true);
    private final Setting.Bool bar = bool("side bar", "Draw an accent bar at the screen edge.", true);
    private final Setting.Bool showKey = bool("show keybind", "Append the keybind to each entry.", false);
    private final Setting.Bool hideHud = bool("hide hud modules", "Do not list modules from the hud tab.", true);
    private final Setting.Num spacing = num("spacing", "Gap between entries in pixels.", 1, 0, 6, 1);
    private final Setting.Num animSpeed = num("slide speed", "How fast entries slide in and out.", 12, 2, 30, 1);
    private final Setting.Num offX = num("offset x", "Horizontal distance from the screen edge.", 3, 0, 200, 1);
    private final Setting.Num offY = num("offset y", "Vertical distance from the screen edge.", 22, 0, 200, 1);

    private long last = System.nanoTime();

    public ArrayListModule() {
        super("arraylist", "Animated list of all enabled modules.", Category.HUD);
        setEnabledSilently(true);
    }

    private String label(Module m) {
        String s = m.name;
        if (showKey.get() && m.key >= 0) s += " [" + dev.agra.gui.ui.KeyNames.of(m.key) + "]";
        return s;
    }

    @Override
    public void onHud(DrawContext ctx) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        float k = 1f - (float) Math.exp(-dt * animSpeed.get());

        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.ALL) {
            if (m == this) continue;
            if (hideHud.get() && m.category == Category.HUD) { m.hudAnim = 0; continue; }
            m.hudAnim += ((m.isEnabled() ? 1f : 0f) - m.hudAnim) * k;
            if (m.hudAnim > 0.02f) list.add(m);
        }
        if (sort.index == 0) list.sort(Comparator.comparingInt((Module m) -> -Draw.width(label(m))));
        else list.sort(Comparator.comparing((Module m) -> m.name));

        boolean right = position.index == 1 || position.index == 3;
        boolean bottom = position.index >= 2;
        boolean sh = InterfaceModule.I.shadow.get();
        int rowH = 11 + spacing.i();
        int sw = mc.getWindow().getScaledWidth();
        int sh2 = mc.getWindow().getScaledHeight();

        int i = 0;
        for (Module m : list) {
            String s = label(m);
            int w = Draw.width(s) + 8;
            float slide = (1f - m.hudAnim) * (w + 6);
            int x = right ? sw - offX.i() - w + Math.round(slide) : offX.i() - Math.round(slide);
            int y = bottom ? sh2 - offY.i() - (i + 1) * rowH : offY.i() + i * rowH;
            float a = m.hudAnim;
            int c = gradient.get() ? Theme.gradient(i * 0.12f) : Theme.accent();

            if (background.get()) Draw.rrect(ctx, x, y, w, 11, 3, Draw.alpha(Theme.bg(), a));
            if (bar.get()) {
                int bx = right ? x + w - 1 : x;
                ctx.fill(bx, y + 1, bx + 1, y + 10, Draw.alpha(c, a));
            }
            Draw.text(ctx, s, x + 4, y + 2, Draw.alpha(c, a), sh);
            i++;
        }
    }
}
