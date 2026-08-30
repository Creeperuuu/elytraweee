package dev.limucc.elytraweee.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreenBuilder {
    private ConfigScreenBuilder() {}

    public static Screen build(Screen parent) {
        ElytraWeeeConfig cfg = ElytraWeeeConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("ElytraWEEE"))
                .setSavingRunnable(cfg::save);
        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(eb.startBooleanToggle(Component.literal("Enable ElytraWEEE"), cfg.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Master switch for auto-deploy."), Component.literal("Also toggleable via keybind."))
                .setSaveConsumer(v -> cfg.enabled = v).build());

        ConfigCategory deploy = builder.getOrCreateCategory(Component.literal("Auto-Deploy"));
        deploy.addEntry(eb.startEnumSelector(Component.literal("Jump mode"), ElytraWeeeConfig.JumpMode.class, cfg.jumpMode)
                .setDefaultValue(ElytraWeeeConfig.JumpMode.SINGLE)
                .setEnumNameProvider(e -> Component.literal(e == ElytraWeeeConfig.JumpMode.SINGLE ? "Single jump" : "Double jump"))
                .setTooltip(Component.literal("Single: one jump with a firework."), Component.literal("Double: two quick jumps."))
                .setSaveConsumer(v -> cfg.jumpMode = v).build());
        deploy.addEntry(eb.startBooleanToggle(Component.literal("Grace window"), cfg.graceWindowEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Jump first, then grab a firework shortly after."))
                .setSaveConsumer(v -> cfg.graceWindowEnabled = v).build());
        deploy.addEntry(eb.startIntSlider(Component.literal("Grace window length (ticks)"), cfg.graceWindowTicks, 0, 40)
                .setDefaultValue(10)
                .setTextGetter(v -> Component.literal(v + " ticks (" + String.format("%.2f", v / 20.0) + "s)"))
                .setSaveConsumer(v -> cfg.graceWindowTicks = v).build());

        ConfigCategory swapBack = builder.getOrCreateCategory(Component.literal("Swap-Back"));
        swapBack.addEntry(eb.startBooleanToggle(Component.literal("Swap chestplate back after landing (no firework)"), cfg.swapBackWhenNotHoldingFirework)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Restore the chestplate after landing when you stop holding a firework."))
                .setSaveConsumer(v -> cfg.swapBackWhenNotHoldingFirework = v).build());
        swapBack.addEntry(eb.startBooleanToggle(Component.literal("Always swap chestplate back the instant you land"), cfg.swapBackOnLanding)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Restore the chestplate immediately on landing, even while holding a firework."))
                .setSaveConsumer(v -> cfg.swapBackOnLanding = v).build());

        ConfigCategory fastSwap = builder.getOrCreateCategory(Component.literal("Fast Swap"));
        fastSwap.addEntry(eb.startBooleanToggle(Component.literal("Enable fast-swap keybind"), cfg.fastSwapEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Toggle elytra/chestplate instantly, including mid-air."))
                .setSaveConsumer(v -> cfg.fastSwapEnabled = v).build());
        fastSwap.addEntry(eb.startBooleanToggle(Component.literal("Auto-resume gliding after swapping back"), cfg.autoReglideOnFastSwap)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Resume gliding automatically when the elytra is swapped on while airborne."))
                .setSaveConsumer(v -> cfg.autoReglideOnFastSwap = v).build());
        fastSwap.addEntry(eb.startIntSlider(Component.literal("Auto-revert suppression after swap (ticks)"), cfg.fastSwapRevertCooldownTicks, 0, 100)
                .setDefaultValue(20)
                .setTextGetter(v -> Component.literal(v + " ticks (" + String.format("%.2f", v / 20.0) + "s)"))
                .setSaveConsumer(v -> cfg.fastSwapRevertCooldownTicks = v).build());

        ConfigCategory info = builder.getOrCreateCategory(Component.literal("Info"));
        addText(eb, info, "§l§bElytraWEEE§r — automatic elytra deploy for Minecraft 1.21.11");
        addText(eb, info, "Hold any firework rocket and jump — your elytra is equipped automatically.");
        addText(eb, info, "§7• The elytra is taken from your inventory or hotbar.");
        addText(eb, info, "§7• Your chestplate is swapped out safely and never dropped.");
        addText(eb, info, "§7• It is restored after landing according to the Swap-Back settings.");
        addText(eb, info, "§7• Fast swap can toggle the elytra/chestplate even while airborne.");
        addText(eb, info, "§8————————————————");
        addText(eb, info, "§l§bKeybinds§r — Options > Controls > ElytraWEEE");
        addText(eb, info, "§7• §fFast swap§7 — instantly toggle elytra/chestplate, even mid-air.");
        addText(eb, info, "§7• §fToggle on/off§7 — flip the master switch.");
        addText(eb, info, "§7• §fOpen settings§7 — open this screen in-game.");
        addText(eb, info, "§8————————————————");
        addText(eb, info, "Author: §bdev.limucc");
        return builder.build();
    }

    private static void addText(ConfigEntryBuilder eb, ConfigCategory category, String text) {
        category.addEntry(eb.startTextDescription(Component.literal(text)).build());
    }
}
