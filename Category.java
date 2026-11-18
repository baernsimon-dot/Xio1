package dev.agra.gui.core;

public enum Category {
    HUD("hud"), VISUALS("visuals"), PVP("pvp"), BASE_HUNT("base hunt"), EXPLOITS("exploits");

    public final String label;
    Category(String label) { this.label = label; }
}
