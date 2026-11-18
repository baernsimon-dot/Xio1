package dev.agra.gui.module.visuals;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.ModuleManager;
import dev.agra.gui.core.Setting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Panic switch for streaming / screen sharing. One key press hides every overlay of this client
 * and optionally switches modules off. It cannot detect a screen share by itself.
 */
public class ScreenProtect extends Module {
    private static ScreenProtect I;

    private final Setting.Bool hideHud = bool("hide hud", "Hides watermark, lists, info boxes and every other overlay.", true);
    private final Setting.Bool blockGui = bool("block gui", "The GUI cannot be opened while protection is on.", true);
    private final Setting.Bool pauseVisuals = bool("pause visuals", "Switches visual modules off while protection is on and restores them afterwards.", true);
    private final Setting.Bool pausePvp = bool("pause pvp", "Switches pvp modules off while protection is on and restores them afterwards.", false);

    private final List<Module> paused = new ArrayList<>();

    public ScreenProtect() {
        super("screen protect", "Panic key: hides all overlays and the GUI at once so nothing shows up on a screen share. Bind a key to it.", Category.VISUALS);
        I = this;
        key = GLFW.GLFW_KEY_END;
    }

    public static boolean hidingHud() { return I != null && I.isEnabled() && I.hideHud.get(); }
    public static boolean blockingGui() { return I != null && I.isEnabled() && I.blockGui.get(); }

    @Override
    public void onEnable() {
        paused.clear();
        for (Module m : ModuleManager.ALL) {
            if (m == this || !m.isEnabled() || !m.toggleable) continue;
            boolean hit = (pauseVisuals.get() && m.category == Category.VISUALS) || (pausePvp.get() && m.category == Category.PVP);
            if (hit) { paused.add(m); m.setEnabled(false); }
        }
    }

    @Override
    public void onDisable() {
        for (Module m : paused) m.setEnabled(true);
        paused.clear();
    }
}
