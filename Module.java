package dev.agra.gui.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name, description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();

    private boolean enabled;
    public int key = -1;             // GLFW key code, -1 = unbound
    public boolean toggleable = true;

    // animation state (GUI + HUD)
    public float guiOn, guiHover, hudAnim;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (!toggleable && !value) return;
        if (enabled == value) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    public void toggle() { if (toggleable) setEnabled(!enabled); }

    /** Used by the config loader: no callbacks, they run once the game is ready. */
    public void setEnabledSilently(boolean value) { enabled = value; }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onHud(DrawContext ctx) {}

    protected boolean ingame() { return mc.player != null && mc.world != null; }

    // setting helpers
    protected Setting.Bool bool(String n, String d, boolean v) { Setting.Bool s = new Setting.Bool(n, d, v); settings.add(s); return s; }
    protected Setting.Num num(String n, String d, double v, double min, double max, double step) { Setting.Num s = new Setting.Num(n, d, v, min, max, step); settings.add(s); return s; }
    protected Setting.Mode mode(String n, String d, int idx, String... o) { Setting.Mode s = new Setting.Mode(n, d, idx, o); settings.add(s); return s; }
}
