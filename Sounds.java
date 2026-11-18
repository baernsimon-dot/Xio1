package dev.agra.gui.core;

import dev.agra.gui.module.hud.InterfaceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class Sounds {
    private static final String[] SET = {
            "block.amethyst_block.chime", "block.note_block.bell", "block.note_block.pling", "ui.button.click"
    };
    public static final String[] SET_NAMES = {"chime", "bell", "pling", "click"};

    private Sounds() {}

    /** Plays a raw vanilla sound id as a non-positional UI sound. */
    public static void raw(String id, float pitch, float volume) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(Identifier.of("minecraft", id)), pitch, volume));
    }

    /** GUI sound using the sound set chosen in the interface settings. */
    public static void ui(float pitch, float volume) {
        InterfaceModule i = InterfaceModule.I;
        if (!i.sounds.get()) return;
        raw(SET[i.soundSet.index], pitch, volume * (float) (i.volume.get() / 100.0));
    }
}
