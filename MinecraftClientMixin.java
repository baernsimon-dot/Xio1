package dev.agra.gui.mixin;

import dev.agra.gui.module.exploits.Drill;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    // require = 0: if the method is ever renamed the mixin silently does nothing instead of crashing the game.
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true, require = 0)
    private void agra$drill(boolean breaking, CallbackInfo ci) {
        if (Drill.busy()) ci.cancel();
    }
}
