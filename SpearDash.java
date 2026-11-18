package dev.agra.gui.module.pvp;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class SpearDash extends Module {
    private final Setting.Num speed = num("speed", "Dash speed in blocks per tick.", 1.6, 0.5, 4.0, 0.1);
    private final Setting.Bool requireSpear = bool("require spear", "Only dash while holding a spear or trident.", true);
    private final Setting.Bool vertical = bool("vertical", "Follow your look direction up and down (flying).", true);
    private final Setting.Bool autoHit = bool("auto hit", "Hit entities in front of you while dashing.", true);
    private final Setting.Num hitRange = num("hit range", "Reach of the auto hit while dashing.", 3.5, 2.0, 6.0, 0.1);
    private final Setting.Num hitDelay = num("hit delay", "Ticks between auto hits.", 4, 1, 20, 1);

    private int cooldown;

    public SpearDash() {
        super("spear kill", "Hold right click with a spear to dash through the air and hit everything on the way.", Category.PVP);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        if (!mc.options.useKey.isPressed()) { cooldown = 0; return; }
        if (requireSpear.get() && !Combat.isSpear(mc.player.getMainHandStack())) return;

        Vec3d look = mc.player.getRotationVec(1f);
        double s = speed.get();
        Vec3d v = vertical.get() ? look.multiply(s) : new Vec3d(look.x * s, mc.player.getVelocity().y, look.z * s);
        mc.player.setVelocity(v);

        if (cooldown > 0) cooldown--;
        if (autoHit.get() && cooldown == 0) {
            Entity best = null;
            double bestD = hitRange.get();
            for (Entity e : mc.world.getEntities()) {
                if (!Combat.validTarget(mc, e, true, true, false, true)) continue;
                double d = mc.player.distanceTo(e);
                if (d > bestD) continue;
                Vec3d to = e.getBoundingBox().getCenter().subtract(mc.player.getEyePos()).normalize();
                if (to.dotProduct(look) < 0.5) continue; // roughly in front
                best = e;
                bestD = d;
            }
            if (best != null) {
                mc.interactionManager.attackEntity(mc.player, best);
                mc.player.swingHand(Hand.MAIN_HAND);
                cooldown = hitDelay.i();
            }
        }
    }
}
