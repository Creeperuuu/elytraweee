package dev.limucc.elytraweee;

import dev.limucc.elytraweee.config.ConfigScreenBuilder;
import dev.limucc.elytraweee.config.ElytraWeeeConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client entrypoint. Loads the config, registers keybinds, and drives the swap logic each client tick.
 */
public class ElytraWeeeClient implements ClientModInitializer {

    public static final String MOD_ID = "elytraweee";

    private final ElytraSwapHandler handler = new ElytraSwapHandler();

    @Override
    public void onInitializeClient() {
        ElytraWeeeConfig.load();
        KeyBindings.register();
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
    }

    private void onEndClientTick(Minecraft client) {
        while (KeyBindings.fastSwap.consumeClick()) {
            handler.fastSwap(client);
        }
        while (KeyBindings.openConfig.consumeClick()) {
            if (client.player != null) {
                client.setScreen(ConfigScreenBuilder.build(client.screen));
            }
        }
        while (KeyBindings.toggleEnabled.consumeClick()) {
            toggleEnabled(client);
        }

        handler.tick(client);
    }

    private void toggleEnabled(Minecraft client) {
        ElytraWeeeConfig cfg = ElytraWeeeConfig.get();
        cfg.enabled = !cfg.enabled;
        cfg.save();
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("ElytraWEEE: " + (cfg.enabled ? "§aEnabled" : "§cDisabled")), true);
        }
    }
}
