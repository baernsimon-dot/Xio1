package dev.agra.gui.module.basehunt;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import dev.agra.gui.module.hud.InterfaceModule;
import dev.agra.gui.ui.HudAnchor;
import dev.agra.gui.ui.HudPanel;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SusChunks extends Module {
    private record Hit(int cx, int cz, int score) {}

    private final Setting.Num range = num("range", "How many chunks around you are scanned (only loaded chunks).", 8, 2, 32, 1);
    private final Setting.Num minScore = num("min score", "Chunks below this player-activity score are hidden.", 6, 1, 50, 1);
    private final Setting.Num interval = num("scan interval", "Ticks between scans.", 40, 10, 200, 10);
    private final Setting.Num maxEntries = num("entries", "How many chunks are listed.", 6, 1, 12, 1);
    private final Setting.Bool storage = bool("chests & barrels", "Count chests, trapped chests and barrels.", true);
    private final Setting.Bool shulkers = bool("shulkers & ender chests", "Count shulker boxes and ender chests (strong signal).", true);
    private final Setting.Bool beds = bool("beds", "Count beds.", true);
    private final Setting.Bool signs = bool("signs", "Count signs.", true);
    private final Setting.Bool furnaces = bool("furnaces", "Count furnaces, smokers and blast furnaces.", true);
    private final Setting.Bool redstone = bool("hoppers & dispensers", "Count hoppers, droppers and dispensers.", true);
    private final Setting.Bool stations = bool("beacons & stations", "Count beacons, enchanting tables and brewing stands.", true);
    private final Setting.Bool distance = bool("show distance", "Show the distance to each chunk.", true);
    private final Setting.Mode position = mode("position", "Screen corner of the list.", 3, HudAnchor.NAMES);
    private final Setting.Num offX = num("offset x", "Horizontal distance from the screen edge.", 4, 0, 200, 1);
    private final Setting.Num offY = num("offset y", "Vertical distance from the screen edge.", 4, 0, 200, 1);

    private List<Hit> hits = new ArrayList<>();
    private int timer;

    public SusChunks() {
        super("sus chunks", "Finds chunks with signs of player activity (storage, beds, furnaces...) and lists them with direction and distance.", Category.BASE_HUNT);
    }

    @Override public void onDisable() { hits = new ArrayList<>(); }

    @Override
    public void onTick() {
        if (timer-- > 0) return;
        timer = interval.i();
        List<Hit> found = new ArrayList<>();
        ChunkScan.forEach(range.i(), ch -> {
            int score = 0;
            for (BlockEntity be : ch.getBlockEntities().values()) score += weight(ChunkScan.blockId(be));
            if (score >= minScore.i()) found.add(new Hit(ch.getPos().x, ch.getPos().z, score));
        });
        found.sort(Comparator.comparingInt((Hit h) -> -h.score()));
        hits = found;
    }

    private int weight(String id) {
        if (id.contains("shulker_box") || id.equals("ender_chest")) return shulkers.get() ? 3 : 0;
        if (id.equals("chest") || id.equals("trapped_chest") || id.equals("barrel")) return storage.get() ? 1 : 0;
        if (id.endsWith("_bed")) return beds.get() ? 2 : 0;
        if (id.contains("sign")) return signs.get() ? 1 : 0;
        if (id.contains("furnace") || id.equals("smoker")) return furnaces.get() ? 1 : 0;
        if (id.equals("hopper") || id.equals("dispenser") || id.equals("dropper")) return redstone.get() ? 1 : 0;
        if (id.equals("beacon")) return stations.get() ? 5 : 0;
        if (id.equals("enchanting_table") || id.equals("brewing_stand")) return stations.get() ? 2 : 0;
        return 0;
    }

    @Override
    public void onHud(DrawContext ctx) {
        if (mc.player == null) return;
        List<String> lines = new ArrayList<>();
        double px = mc.player.getX(), pz = mc.player.getZ();
        List<Hit> sorted = new ArrayList<>(hits);
        sorted.sort(Comparator.comparingInt((Hit h) -> -h.score()));
        for (int i = 0; i < Math.min(maxEntries.i(), sorted.size()); i++) {
            Hit h = sorted.get(i);
            double cx = h.cx() * 16 + 8, cz = h.cz() * 16 + 8;
            String s = (int) cx + " " + (int) cz + "  " + ChunkScan.dir(cx - px, cz - pz);
            if (distance.get()) s += "  " + (int) Math.hypot(cx - px, cz - pz) + "m";
            lines.add(s + "  [" + h.score() + "]");
        }
        if (lines.isEmpty()) lines.add("nothing found");
        HudPanel.draw(ctx, "sus chunks", lines, position.index, offX.i(), offY.i(), InterfaceModule.I.shadow.get());
    }
}
