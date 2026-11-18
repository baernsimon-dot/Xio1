package dev.agra.gui.module.hud;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import dev.agra.gui.core.Theme;
import dev.agra.gui.ui.Draw;
import dev.agra.gui.ui.HudAnchor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WatermarkModule extends Module {
    private final Setting.Mode title = mode("title", "Text shown at the start of the watermark.", 0, "agra", "agra gui", "agra client");
    private final Setting.Bool user = bool("username", "Show your player name.", true);
    private final Setting.Bool fps = bool("fps", "Show the current FPS.", true);
    private final Setting.Bool ping = bool("ping", "Show your latency to the server.", false);
    private final Setting.Bool time = bool("clock", "Show the current time.", false);
    private final Setting.Bool server = bool("server", "Show the server address.", false);
    private final Setting.Mode position = mode("position", "Screen corner of the watermark.", 0, HudAnchor.NAMES);
    private final Setting.Num offX = num("offset x", "Horizontal distance from the screen edge.", 4, 0, 200, 1);
    private final Setting.Num offY = num("offset y", "Vertical distance from the screen edge.", 4, 0, 200, 1);
    private final Setting.Bool background = bool("background", "Draw the glass background.", true);
    private final Setting.Bool accentLine = bool("accent line", "Draw a thin gradient line at the bottom.", false);

    public WatermarkModule() {
        super("watermark", "Small glass bar with the client name, your name, FPS and more.", Category.HUD);
        setEnabledSilently(true);
    }

    @Override
    public void onHud(DrawContext ctx) {
        if (mc.player == null) return;
        boolean sh = InterfaceModule.I.shadow.get();
        StringBuilder info = new StringBuilder();
        if (user.get()) info.append(mc.player.getName().getString());
        if (fps.get()) sep(info).append("fps ").append(mc.getCurrentFps());
        if (ping.get()) {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            sep(info).append(e == null ? 0 : e.getLatency()).append(" ms");
        }
        if (time.get()) sep(info).append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (server.get()) sep(info).append(mc.getCurrentServerEntry() == null ? "singleplayer" : mc.getCurrentServerEntry().address);

        String brand = title.get();
        String rest = info.length() == 0 ? "" : "  |  " + info;
        int w = Draw.width(brand) + Draw.width(rest) + 12;
        int h = 16;
        int[] p = HudAnchor.at(position.index, w, h, offX.i(), offY.i());

        if (background.get()) Draw.glass(ctx, p[0], p[1], w, h, 1f);
        int x = p[0] + 6, y = p[1] + 4;
        for (int i = 0; i < brand.length(); i++) {
            String ch = String.valueOf(brand.charAt(i));
            Draw.text(ctx, ch, x, y, Theme.gradient(i * 0.25f), sh);
            x += Draw.width(ch);
        }
        Draw.text(ctx, rest, x, y, Theme.text(), sh);
        if (accentLine.get()) ctx.fillGradient(p[0] + 4, p[1] + h - 2, p[0] + w - 4, p[1] + h - 1, Theme.gradient(0f), Theme.gradient(1f));
    }

    private static StringBuilder sep(StringBuilder b) {
        if (b.length() > 0) b.append(" • ");
        return b;
    }
}
