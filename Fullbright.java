package dev.agra.gui.module.visuals;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {
    private boolean applied;

    public Fullbright() {
        super("fullbright", "Everything is bright, also in caves and at night (client-side night vision).", Category.VISUALS);
    }

    @Override
    public void onTick() {
        if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
            applied = true;
        }
    }

    @Override
    public void onDisable() {
        if (applied && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        applied = false;
    }
}
