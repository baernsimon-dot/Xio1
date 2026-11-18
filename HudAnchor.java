package dev.agra.gui.ui;

import net.minecraft.client.MinecraftClient;

public final class HudAnchor {
    public static final String[] NAMES = {"top left", "top right", "bottom left", "bottom right"};

    private HudAnchor() {}

    /** Returns {x, y} for a box of size w*h at the given corner with an offset. */
    public static int[] at(int mode, int w, int h, int offX, int offY) {
        int sw = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int sh = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int x = (mode == 1 || mode == 3) ? sw - w - offX : offX;
        int y = (mode == 2 || mode == 3) ? sh - h - offY : offY;
        return new int[]{x, y};
    }
}
