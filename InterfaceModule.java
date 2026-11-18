package dev.agra.gui.module.hud;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import dev.agra.gui.core.Sounds;
import dev.agra.gui.core.Theme;

public class InterfaceModule extends Module {
    public static InterfaceModule I;

    public final Setting.Mode scheme = mode("color scheme", "Color scheme used by the GUI and every HUD element.", 0, Theme.names());
    public final Setting.Num gradSpeed = num("gradient speed", "How fast the accent gradient shimmers (0 = static).", 1.0, 0, 5, 0.1);
    public final Setting.Num opacity = num("glass opacity", "How see-through panels are (percent).", 90, 40, 100, 5);
    public final Setting.Num radius = num("roundness", "Corner radius of panels and buttons.", 6, 2, 10, 1);
    public final Setting.Bool gloss = bool("gloss", "Adds a soft glass highlight on top of panels.", true);
    public final Setting.Mode openStyle = mode("open animation", "Style of the open/close animation.", 0, "pop", "fade", "slide");
    public final Setting.Num animSpeed = num("animation speed", "Speed of all GUI animations.", 1.0, 0.5, 2.0, 0.1);
    public final Setting.Bool dim = bool("dim background", "Darkens the game while the GUI is open.", true);
    public final Setting.Num dimAmount = num("dim strength", "How dark the background gets (percent).", 45, 0, 80, 5);
    public final Setting.Bool sounds = bool("ui sounds", "Plays sounds when opening, closing and clicking.", true);
    public final Setting.Mode soundSet = mode("sound set", "Which sound the GUI uses.", 0, Sounds.SET_NAMES);
    public final Setting.Num volume = num("ui volume", "Volume of the GUI sounds (percent).", 70, 0, 100, 5);
    public final Setting.Bool shadow = bool("text shadow", "Draws a shadow behind HUD text.", false);

    public InterfaceModule() {
        super("interface", "Global look of the GUI and HUD: colors, glass, animations and sounds.", Category.HUD);
        I = this;
        toggleable = false;
        setEnabledSilently(true);
    }
}
