package dev.agra.gui.module.pvp;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module {
    private final Setting.Num minCharge = num("min charge", "Weapon charge needed before hitting (percent).", 95, 50, 100, 5);
    private final Setting.Num delay = num("delay", "Extra delay after the charge is ready (ms).", 0, 0, 300, 10);
    private final Setting.Bool players = bool("players", "Hit other players.", true);
    private final Setting.Bool hostile = bool("hostile mobs", "Hit hostile mobs.", true);
    private final Setting.Bool animals = bool("animals", "Hit passive animals.", false);
    private final Setting.Bool onlyWeapon = bool("only weapons", "Only trigger while holding a weapon.", true);
    private final Setting.Bool critsOnly = bool("crits only", "Only hit while falling (critical hits).", false);
    private final Setting.Bool swing = bool("swing", "Play the swing animation.", true);

    private long readySince = -1;

    public TriggerBot() {
        super("triggerbot", "Hits automatically as soon as your crosshair is on a valid target.", Category.PVP);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        HitResult hr = mc.crosshairTarget;
        if (hr == null || hr.getType() != HitResult.Type.ENTITY || !(hr instanceof EntityHitResult ehr)) { readySince = -1; return; }
        if (!Combat.validTarget(mc, ehr.getEntity(), players.get(), hostile.get(), animals.get(), true)) { readySince = -1; return; }
        if (onlyWeapon.get() && !Combat.isWeapon(mc.player.getMainHandStack())) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < minCharge.get() / 100.0) { readySince = -1; return; }
        if (critsOnly.get() && (mc.player.isOnGround() || mc.player.getVelocity().y > -0.05)) return;

        long now = System.currentTimeMillis();
        if (readySince < 0) readySince = now;
        if (now - readySince < delay.get()) return;

        mc.interactionManager.attackEntity(mc.player, ehr.getEntity());
        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        readySince = -1;
    }
}
