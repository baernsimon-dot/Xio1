package dev.agra.gui.module.basehunt;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;

public class AutoWalk extends Module {
    private final Setting.Bool sprint = bool("sprint", "Sprint while walking.", true);
    private final Setting.Bool jump = bool("auto jump", "Jump when you run into a block.", true);

    public AutoWalk() {
        super("auto walk", "Walks forward on its own, ideal for long trips while you look around.", Category.BASE_HUNT);
    }

    @Override
    public void onTick() {
        boolean open = mc.currentScreen != null;
        mc.options.forwardKey.setPressed(!open);
        if (open) return;
        if (sprint.get()) mc.player.setSprinting(true);
        if (jump.get()) mc.options.jumpKey.setPressed(mc.player.horizontalCollision);
    }

    @Override
    public void onDisable() {
        if (mc.options == null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
    }
}
