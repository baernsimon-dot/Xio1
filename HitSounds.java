package dev.agra.gui.module.pvp;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import dev.agra.gui.core.Sounds;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;

import java.util.Random;

public class HitSounds extends Module {
    private static final String[] IDS = {
            "block.note_block.pling", "block.amethyst_block.chime", "block.note_block.bell", "entity.experience_orb.pickup",
            "entity.arrow.hit_player", "entity.player.attack.crit", "block.note_block.bit", "entity.player.levelup"
    };

    private final Setting.Mode sound = mode("hit sound", "Sound played when you hit something.", 1, "pling", "chime", "bell", "orb", "arrow", "crit", "bit", "levelup");
    private final Setting.Num volume = num("volume", "Volume of the sound (percent).", 80, 0, 100, 5);
    private final Setting.Num pitch = num("pitch", "Base pitch of the sound.", 1.2, 0.5, 2.0, 0.1);
    private final Setting.Num variation = num("pitch variation", "Random pitch variation so it does not get boring.", 0.1, 0, 0.5, 0.05);
    private final Setting.Bool critSound = bool("crit sound", "Use a different sound for critical hits.", true);
    private final Setting.Bool killSound = bool("kill sound", "Play a special sound when your target dies.", true);
    private final Setting.Mode killSoundType = mode("kill sound type", "Sound used for kills.", 0, "levelup", "pling", "chime");

    private final Random rng = new Random();
    private LivingEntity lastHit;

    public HitSounds() {
        super("hit sounds", "Plays satisfying sounds when you hit or kill someone.", Category.PVP);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (isEnabled() && world.isClient() && player == mc.player && entity instanceof LivingEntity le) onHit(le);
            return ActionResult.PASS;
        });
    }

    private void onHit(LivingEntity e) {
        lastHit = e;
        boolean crit = mc.player.getVelocity().y < -0.1 && !mc.player.isOnGround();
        String id = (crit && critSound.get()) ? "entity.player.attack.crit" : IDS[sound.index];
        float p = pitch.f() + (float) ((rng.nextDouble() * 2 - 1) * variation.get());
        Sounds.raw(id, p, (float) (volume.get() / 100.0));
    }

    @Override
    public void onTick() {
        if (lastHit == null) return;
        if (lastHit.isDead() || lastHit.getHealth() <= 0f || lastHit.isRemoved()) {
            if (killSound.get()) {
                String id = switch (killSoundType.index) {
                    case 1 -> "block.note_block.pling";
                    case 2 -> "block.amethyst_block.chime";
                    default -> "entity.player.levelup";
                };
                Sounds.raw(id, 1.3f, (float) (volume.get() / 100.0));
            }
            lastHit = null;
        }
    }
}
