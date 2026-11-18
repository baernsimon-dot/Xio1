package dev.agra.gui.module.basehunt;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.function.Consumer;

/** Helpers for scanning the chunks the client has already loaded (no extra packets, nothing server-side). */
final class ChunkScan {
    private ChunkScan() {}

    static void forEach(int radius, Consumer<WorldChunk> c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        ChunkPos cp = mc.player.getChunkPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                WorldChunk ch = mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
                if (ch != null) c.accept(ch);
            }
        }
    }

    static String blockId(BlockEntity be) {
        return Registries.BLOCK.getId(be.getCachedState().getBlock()).getPath();
    }

    /** Compass direction from the player to an offset (north = -z). */
    static String dir(double dx, double dz) {
        String[] n = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        double ang = Math.toDegrees(Math.atan2(dx, -dz));
        int i = (int) Math.round(ang / 45.0);
        return n[((i % 8) + 8) % 8];
    }
}
