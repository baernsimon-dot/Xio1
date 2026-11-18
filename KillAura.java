package dev.agra.gui.module.pvp;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import dev.agra.gui.core.Theme;
import dev.agra.gui.module.hud.InterfaceModule;
import dev.agra.gui.ui.Draw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class KillAura extends Module {
    private final Setting.Num range = num("range", "Maximum distance to a target in blocks.", 4.2, 2.0, 6.0, 0.1);
    private final Setting.Bool useCooldown = bool("attack cooldown", "Attack when the weapon is charged (1.9+ combat). Off = use CPS.", true);
    private final Setting.Num minCharge = num("min charge", "Weapon charge needed before attacking (percent).", 90, 50, 100, 5);
    private final Setting.Num cps = num("cps", "Attacks per second when attack cooldown is off.", 10, 1, 20, 1);
    private final Setting.Bool players = bool("players", "Target other players.", true);
    private final Setting.Bool hostile = bool("hostile mobs", "Target hostile mobs.", true);
    private final Setting.Bool animals = bool("animals", "Target passive animals.", false);
    private final Setting.Bool invisible = bool("invisible", "Target invisible entities.", false);
    private final Setting.Mode priority = mode("priority", "Which target is picked first.", 0, "distance", "health", "angle");
    private final Setting.Num fov = num("fov", "Only targets inside this view angle (360 = all around).", 360, 30, 360, 10);
    private final Setting.Bool walls = bool("through walls", "Also attack targets you cannot see.", false);
    private final Setting.Mode rotate = mode("rotation", "Turn your view towards the target.", 1, "off", "smooth", "snap");
    private final Setting.Num rotSpeed = num("rotation speed", "Max degrees per tick for smooth rotation.", 40, 5, 180, 5);
    private final Setting.Bool onlyWeapon = bool("only weapons", "Only attack while holding a sword, axe, spear or mace.", true);
    private final Setting.Bool swing = bool("swing", "Play the swing animation.", true);
    private final Setting.Bool targetHud = bool("target hud", "Show name and health of the current target.", true);

    private LivingEntity target;
    private long lastHit;

    public KillAura() {
        super("killaura", "Automatically attacks entities around you with many filters and smooth aiming.", Category.PVP);
    }

    public LivingEntity target() { return target; }

    @Override public void onDisable() { target = null; }

    @Override
    public void onTick() {
        target = null;
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        target = find();
        if (target == null) return;

        if (rotate.index != 0) {
            float[] a = Combat.anglesTo(mc, target);
            float yaw = mc.player.getYaw(), pitch = mc.player.getPitch();
            if (rotate.index == 2) {
                mc.player.setYaw(a[0]);
                mc.player.setPitch(a[1]);
            } else {
                float max = rotSpeed.f();
                mc.player.setYaw(yaw + MathHelper.clamp(MathHelper.wrapDegrees(a[0] - yaw), -max, max));
                mc.player.setPitch(pitch + MathHelper.clamp(a[1] - pitch, -max, max));
            }
        }

        if (onlyWeapon.get() && !Combat.isWeapon(mc.player.getMainHandStack())) return;

        if (useCooldown.get()) {
            if (mc.player.getAttackCooldownProgress(0.5f) < minCharge.get() / 100.0) return;
        } else {
            long now = System.currentTimeMillis();
            if (now - lastHit < 1000.0 / cps.get()) return;
            lastHit = now;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
    }

    private LivingEntity find() {
        Optional<Entity> best = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(e -> Combat.validTarget(mc, e, players.get(), hostile.get(), animals.get(), invisible.get()))
                .filter(e -> mc.player.distanceTo(e) <= range.get())
                .filter(e -> walls.get() || mc.player.canSee(e))
                .filter(e -> fov.get() >= 360 || Math.abs(MathHelper.wrapDegrees(Combat.anglesTo(mc, e)[0] - mc.player.getYaw())) <= fov.get() / 2.0)
                .min(comparator());
        return best.map(e -> (LivingEntity) e).orElse(null);
    }

    private Comparator<Entity> comparator() {
        return switch (priority.index) {
            case 1 -> Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth());
            case 2 -> Comparator.comparingDouble(e -> Math.abs(MathHelper.wrapDegrees(Combat.anglesTo(mc, e)[0] - mc.player.getYaw())));
            default -> Comparator.comparingDouble(e -> mc.player.distanceTo(e));
        };
    }

    @Override
    public void onHud(DrawContext ctx) {
        if (!targetHud.get() || target == null || !target.isAlive()) return;
        String name = target.getName().getString();
        float hp = target.getHealth(), max = Math.max(1f, target.getMaxHealth());
        String hpText = String.format(java.util.Locale.ROOT, "%.1f", hp);
        int w = Math.max(90, Draw.width(name) + Draw.width(hpText) + 24);
        int h = 26;
        int x = mc.getWindow().getScaledWidth() / 2 + 20;
        int y = mc.getWindow().getScaledHeight() / 2 + 14;
        boolean sh = InterfaceModule.I.shadow.get();
        Draw.glass(ctx, x, y, w, h, 1f);
        Draw.text(ctx, name, x + 6, y + 4, Theme.text(), sh);
        Draw.text(ctx, hpText, x + w - 6 - Draw.width(hpText), y + 4, Theme.gradient(0.5f), sh);
        int bw = w - 12;
        Draw.rrect(ctx, x + 6, y + 17, bw, 4, 2, Theme.btn());
        Draw.rrect(ctx, x + 6, y + 17, Math.max(2, Math.round(bw * Math.min(1f, hp / max))), 4, 2, Theme.gradient(0f));
    }
}
