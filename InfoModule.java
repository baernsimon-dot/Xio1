package dev.agra.gui.module.hud;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import dev.agra.gui.ui.HudAnchor;
import dev.agra.gui.ui.HudPanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InfoModule extends Module {
    private final Setting.Bool coords = bool("coordinates", "Show your XYZ position.", true);
    private final Setting.Bool otherDim = bool("nether coords", "Also show the matching coordinates in the other dimension.", true);
    private final Setting.Bool facing = bool("facing", "Show the direction you are looking.", true);
    private final Setting.Bool biome = bool("biome", "Show the current biome.", false);
    private final Setting.Bool speed = bool("speed", "Show your speed in blocks per second.", true);
    private final Setting.Bool fps = bool("fps", "Show the FPS.", false);
    private final Setting.Bool ping = bool("ping", "Show the latency.", false);
    private final Setting.Bool clock = bool("clock", "Show the time.", false);
    private final Setting.Mode position = mode("position", "Screen corner of the info box.", 2, HudAnchor.NAMES);
    private final Setting.Num offX = num("offset x", "Horizontal distance from the screen edge.", 4, 0, 200, 1);
    private final Setting.Num offY = num("offset y", "Vertical distance from the screen edge.", 4, 0, 200, 1);

    private Vec3d prev;
    private double bps;

    public InfoModule() {
        super("info", "Coordinates, speed, biome and other live info in a glass box.", Category.HUD);
    }

    @Override
    public void onTick() {
        Vec3d now = mc.player.getPos();
        if (prev != null) bps = bps * 0.8 + new Vec3d(now.x - prev.x, 0, now.z - prev.z).length() * 20.0 * 0.2;
        prev = now;
    }

    @Override
    public void onHud(DrawContext ctx) {
        if (mc.player == null || mc.world == null) return;
        List<String> l = new ArrayList<>();
        BlockPos p = mc.player.getBlockPos();
        if (coords.get()) {
            l.add("xyz  " + p.getX() + "  " + p.getY() + "  " + p.getZ());
            if (otherDim.get()) {
                String dim = mc.world.getRegistryKey().getValue().getPath();
                if (dim.equals("overworld")) l.add("nether  " + (p.getX() / 8) + "  " + (p.getZ() / 8));
                else if (dim.equals("the_nether")) l.add("overworld  " + (p.getX() * 8) + "  " + (p.getZ() * 8));
            }
        }
        if (facing.get()) l.add("facing  " + mc.player.getHorizontalFacing().asString());
        if (biome.get()) l.add("biome  " + mc.world.getBiome(p).getKey().map(k -> k.getValue().getPath()).orElse("?"));
        if (speed.get()) l.add(String.format(java.util.Locale.ROOT, "speed  %.1f b/s", bps));
        if (fps.get()) l.add("fps  " + mc.getCurrentFps());
        if (ping.get()) {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            l.add("ping  " + (e == null ? 0 : e.getLatency()) + " ms");
        }
        if (clock.get()) l.add("time  " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (l.isEmpty()) return;
        HudPanel.draw(ctx, "info", l, position.index, offX.i(), offY.i(), InterfaceModule.I.shadow.get());
    }
}
