package dev.agra.gui.module.visuals;

import dev.agra.gui.core.Category;
import dev.agra.gui.core.Module;
import dev.agra.gui.core.Setting;
import net.minecraft.client.option.ParticlesMode;

public class FpsOptimizer extends Module {
    private final Setting.Num renderDistance = num("render distance", "Render distance while the optimizer is on (chunks).", 6, 2, 32, 1);
    private final Setting.Bool particles = bool("minimal particles", "Reduces particles to the minimum.", true);
    private final Setting.Num entityDist = num("entity distance", "How far entities are rendered (percent). Great for mob farms.", 50, 50, 100, 5);
    private final Setting.Bool shadows = bool("no entity shadows", "Disables entity shadows.", true);
    private final Setting.Bool blend = bool("no biome blend", "Disables biome color blending.", true);
    private final Setting.Bool afkCap = bool("afk fps cap", "Caps the FPS while you stand still, e.g. at a farm.", true);
    private final Setting.Num afkFps = num("afk fps", "FPS limit while standing still.", 15, 10, 60, 5);
    private final Setting.Num afkSeconds = num("afk after", "Seconds without moving before the cap applies.", 5, 1, 60, 1);

    private Integer oView, oBlend, oMaxFps;
    private Double oEntity;
    private Boolean oShadows;
    private ParticlesMode oParticles;
    private int idle;

    public FpsOptimizer() {
        super("fps optimizer", "Lowers render distance, particles and entity rendering for AFK farms. Restores your settings when turned off.", Category.VISUALS);
    }

    @Override
    public void onEnable() {
        if (mc.options == null) return;
        oView = mc.options.getViewDistance().getValue();
        oEntity = mc.options.getEntityDistanceScaling().getValue();
        oShadows = mc.options.getEntityShadows().getValue();
        oBlend = mc.options.getBiomeBlendRadius().getValue();
        oParticles = mc.options.getParticles().getValue();
        oMaxFps = mc.options.getMaxFps().getValue();
    }

    @Override
    public void onDisable() {
        if (mc.options == null || oView == null) return;
        mc.options.getViewDistance().setValue(oView);
        mc.options.getEntityDistanceScaling().setValue(oEntity);
        mc.options.getEntityShadows().setValue(oShadows);
        mc.options.getBiomeBlendRadius().setValue(oBlend);
        mc.options.getParticles().setValue(oParticles);
        mc.options.getMaxFps().setValue(oMaxFps);
        oView = null;
    }

    @Override
    public void onTick() {
        if (oView == null) onEnable(); // enabled from the saved config before the game was ready
        if (oView == null) return;

        if (mc.options.getViewDistance().getValue() != renderDistance.i()) mc.options.getViewDistance().setValue(renderDistance.i());
        double ed = entityDist.get() / 100.0;
        if (Math.abs(mc.options.getEntityDistanceScaling().getValue() - ed) > 0.001) mc.options.getEntityDistanceScaling().setValue(ed);
        if (particles.get() && mc.options.getParticles().getValue() != ParticlesMode.MINIMAL) mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
        if (shadows.get() && mc.options.getEntityShadows().getValue()) mc.options.getEntityShadows().setValue(false);
        if (blend.get() && mc.options.getBiomeBlendRadius().getValue() != 0) mc.options.getBiomeBlendRadius().setValue(0);

        boolean still = mc.player.getVelocity().horizontalLengthSquared() < 1e-5
                && !mc.options.attackKey.isPressed() && !mc.options.useKey.isPressed();
        idle = still ? idle + 1 : 0;
        int target = (afkCap.get() && idle > afkSeconds.i() * 20) ? afkFps.i() : oMaxFps;
        if (mc.options.getMaxFps().getValue() != target) mc.options.getMaxFps().setValue(target);
    }
}
