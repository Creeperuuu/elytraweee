package dev.limucc.elytraweee;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * All ElytraWEEE keybinds, grouped under their own dedicated category in Options > Controls.
 */
public final class KeyBindings {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ElytraWeeeClient.MOD_ID, "main"));

    public static KeyMapping openConfig;
    public static KeyMapping fastSwap;
    public static KeyMapping toggleEnabled;

    private KeyBindings() {
    }

    public static void register() {
        openConfig = register("key.elytraweee.open_config");
        fastSwap = register("key.elytraweee.fast_swap");
        toggleEnabled = register("key.elytraweee.toggle_enabled");
    }

    private static KeyMapping register(String translationKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY));
    }
}
