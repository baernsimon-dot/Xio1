package dev.agra.gui.module.basehunt;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import dev.agra.gui.module.hud.InterfaceModule;
import dev.agra.gui.ui.HudAnchor;
import dev.agra.gui.ui.HudPanel;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlockFinder extends Module {
    private record Found(String name, BlockPos pos) {}

    private final Setting.Num range = num("range", "How many chunks around you are scanned (only loaded chunks).", 6, 1, 32, 1);
    private final Setting.Num interval = num("scan interval", "Ticks between scans.", 20, 5, 200, 5);
    private final Setting.Num maxEntries = num("entries", "How many blocks are listed (closest first).", 8, 1, 15, 1);
    private final Setting.Bool chests = bool("chests & barrels", "Find chests, trapped chests and barrels.", true);
    private final Setting.Bool ender = bool("ender chests", "Find ender chests.", false);
    private final Setting.Bool shulkers = bool("shulker boxes", "Find shulker boxes.", true);
    private final Setting.Bool spawners = bool("spawners", "Find monster spawners.", true);
    private final Setting.Bool beds = bool("beds", "Find beds.", false);
    private final Setting.Bool furnaces = bool("furnaces", "Find furnaces.", false);
    private final Setting.Bool signs = bool("signs", "Find signs.", false);
    private final Setting.Bool beacons = bool("beacons", "Find beacons.", true);
    private final Setting.Mode position = mode("position", "Screen corner of the list.", 1, HudAnchor.NAMES);
    private final Setting.Num offX = num("offset x", "Horizontal distance from the screen edge.", 4, 0, 200, 1);
    private final Setting.Num offY = num("offset y", "Vertical distance from the screen edge.", 70, 0, 200, 1);

    private List<Found> found = new ArrayList<>();
    private int timer;

    public BlockFinder() {
        super("block finder", "Lists interesting blocks (storage, spawners, beacons...) in loaded chunks with coordinates and distance.", Category.BASE_HUNT);
    }

    @Override public void onDisable() { found = new ArrayList<>(); }

    @Override
    public void onTick() {
        if (timer-- > 0) return;
        timer = interval.i();
        List<Found> out = new ArrayList<>();
        ChunkScan.forEach(range.i(), ch -> {
            for (BlockEntity be : ch.getBlockEntities().values()) {
                String id = ChunkScan.blockId(be);
                if (wanted(id)) out.add(new Found(id.replace('_', ' '), be.getPos()));
            }
        });
        out.sort(Comparator.comparingDouble(f -> f.pos().getSquaredDistance(mc.player.getPos())));
        found = out;
    }

    private boolean wanted(String id) {
        if (id.equals("chest") || id.equals("trapped_chest") || id.equals("barrel")) return chests.get();
        if (id.equals("ender_chest")) return ender.get();
        if (id.contains("shulker_box")) return shulkers.get();
        if (id.equals("spawner")) return spawners.get();
        if (id.endsWith("_bed")) return beds.get();
        if (id.contains("furnace") || id.equals("smoker")) return furnaces.get();
        if (id.contains("sign")) return signs.get();
        return id.equals("beacon") && beacons.get();
    }

    @Override
    public void onHud(DrawContext ctx) {
        if (mc.player == null) return;
        List<String> lines = new ArrayList<>();
        double px = mc.player.getX(), pz = mc.player.getZ();
        List<Found> snapshot = new ArrayList<>(found);
        for (int i = 0; i < Math.min(maxEntries.i(), snapshot.size()); i++) {
            Found f = snapshot.get(i);
            BlockPos p = f.pos();
            lines.add(f.name() + "  " + p.getX() + " " + p.getY() + " " + p.getZ()
                    + "  " + ChunkScan.dir(p.getX() + 0.5 - px, p.getZ() + 0.5 - pz)
                    + "  " + (int) Math.sqrt(p.getSquaredDistance(mc.player.getPos())) + "m");
        }
        if (lines.isEmpty()) lines.add("nothing found");
        HudPanel.draw(ctx, "block finder", lines, position.index, offX.i(), offY.i(), InterfaceModule.I.shadow.get());
    }
}
