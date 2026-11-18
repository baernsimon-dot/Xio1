package dev.agra.gui.module.basehunt;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;

public class AutoSprint extends Module {
    private final Setting.Bool hunger = bool("respect hunger", "Stop sprinting when your food level is too low.", true);

    public AutoSprint() {
        super("auto sprint", "Always sprints while you move forward.", Category.BASE_HUNT);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null || !mc.options.forwardKey.isPressed() || mc.player.isSneaking()) return;
        if (hunger.get() && mc.player.getHungerManager().getFoodLevel() <= 6) return;
        mc.player.setSprinting(true);
    }
}
