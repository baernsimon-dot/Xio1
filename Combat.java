package dev.agra.gui.module.pvp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Shared helpers for the pvp modules. */
final class Combat {
    private Combat() {}

    static String itemPath(ItemStack s) { return Registries.ITEM.getId(s.getItem()).getPath(); }

    static boolean isWeapon(ItemStack s) {
        String p = itemPath(s);
        return p.endsWith("_sword") || p.endsWith("_axe") || p.endsWith("_spear") || p.equals("mace") || p.equals("trident");
    }

    static boolean isSpear(ItemStack s) {
        String p = itemPath(s);
        return p.endsWith("_spear") || p.equals("trident");
    }

    static boolean validTarget(MinecraftClient mc, Entity e, boolean players, boolean hostile, boolean animals, boolean invisible) {
        if (!(e instanceof LivingEntity le) || e == mc.player) return false;
        if (!le.isAlive() || le instanceof ArmorStandEntity) return false;
        if (le.isInvisible() && !invisible) return false;
        if (le instanceof PlayerEntity pe) return players && !pe.isSpectator();
        if (le instanceof Monster) return hostile;
        if (le instanceof AnimalEntity) return animals;
        return false;
    }

    /** {yaw, pitch} needed to look at the centre of the entity. */
    static float[] anglesTo(MinecraftClient mc, Entity e) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d t = e.getBoundingBox().getCenter();
        double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }
}
