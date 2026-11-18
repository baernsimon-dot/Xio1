package dev.agra.gui.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.agra.gui.module.basehunt.AutoSprint;
import dev.agra.gui.module.basehunt.AutoWalk;
import dev.agra.gui.module.basehunt.BlockFinder;
import dev.agra.gui.module.basehunt.SusChunks;
import dev.agra.gui.module.exploits.Drill;
import dev.agra.gui.module.hud.ArrayListModule;
import dev.agra.gui.module.hud.InfoModule;
import dev.agra.gui.module.hud.InterfaceModule;
import dev.agra.gui.module.hud.WatermarkModule;
import dev.agra.gui.module.pvp.HitSounds;
import dev.agra.gui.module.pvp.KillAura;
import dev.agra.gui.module.pvp.SpearDash;
import dev.agra.gui.module.pvp.TriggerBot;
import dev.agra.gui.module.visuals.FpsOptimizer;
import dev.agra.gui.module.visuals.Fullbright;
import dev.agra.gui.module.visuals.ScreenProtect;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    private static final Map<Module, Boolean> keyDown = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ModuleManager() {}

    public static void init() {
        // hud
        ALL.add(new InterfaceModule());
        ALL.add(new WatermarkModule());
        ALL.add(new ArrayListModule());
        ALL.add(new InfoModule());
        // visuals
        ALL.add(new ScreenProtect());
        ALL.add(new FpsOptimizer());
        ALL.add(new Fullbright());
        // pvp
        ALL.add(new KillAura());
        ALL.add(new TriggerBot());
        ALL.add(new HitSounds());
        ALL.add(new SpearDash());
        // base hunt
        ALL.add(new SusChunks());
        ALL.add(new BlockFinder());
        ALL.add(new AutoWalk());
        ALL.add(new AutoSprint());
        // exploits
        ALL.add(new Drill());

        load();
    }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == c) out.add(m);
        return out;
    }

    public static <T extends Module> T get(Class<T> type) {
        for (Module m : ALL) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    // ---------------- per tick ----------------
    public static void tick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;
        long handle = mc.getWindow().getHandle();

        // keybinds (only when no screen is open, so typing in chat doesn't trigger anything)
        for (Module m : ALL) {
            if (m.key < 0) continue;
            boolean down = GLFW.glfwGetKey(handle, m.key) == GLFW.GLFW_PRESS;
            boolean was = keyDown.getOrDefault(m, false);
            if (down && !was && mc.currentScreen == null && mc.player != null) m.toggle();
            keyDown.put(m, down);
        }

        if (mc.player == null || mc.world == null) return;
        for (Module m : ALL) if (m.isEnabled()) m.onTick();
    }

    public static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        if (ScreenProtect.hidingHud()) return;
        for (Module m : ALL) if (m.isEnabled()) m.onHud(ctx);
    }

    // ---------------- config ----------------
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("agra-gui.json"); }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.isEnabled());
                o.addProperty("key", m.key);
                JsonObject s = new JsonObject();
                for (Setting st : m.settings) s.addProperty(st.name, st.serialize());
                o.add("settings", s);
                root.add(m.name, o);
            }
            Path config = file();
            Files.createDirectories(config.getParent());
            Files.writeString(config, GSON.toJson(root));
        } catch (Exception e) {
            System.err.println("[agra-gui] could not save config: " + e);
        }
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject root = GSON.fromJson(Files.readString(f), JsonObject.class);
            if (root == null) return;
            for (Module m : ALL) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("enabled") && m.toggleable) m.setEnabledSilently(o.get("enabled").getAsBoolean());
                if (o.has("key")) m.key = o.get("key").getAsInt();
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting st : m.settings) if (s.has(st.name)) st.deserialize(s.get(st.name).getAsString());
                }
            }
        } catch (Exception e) {
            System.err.println("[agra-gui] could not load config: " + e);
        }
    }
}
