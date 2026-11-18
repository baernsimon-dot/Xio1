package dev.agra.gui;

import dev.agra.gui.core.ModuleManager;
import dev.agra.gui.module.visuals.ScreenProtect;
import dev.agra.gui.ui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AgraGuiClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        // Register through Fabric's keybinding system instead of polling GLFW.
        // This respects the active window/input backend and works after focus changes.
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("agra-gui", "main"));
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.agra-gui.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen instanceof ClickGuiScreen gui) {
                    gui.close();
                } else if (client.currentScreen == null && client.player != null
                        && !ScreenProtect.blockingGui()) {
                    client.setScreen(new ClickGuiScreen());
                }
            }
            ModuleManager.tick(client);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> ModuleManager.renderHud(ctx));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.save());
    }
}
