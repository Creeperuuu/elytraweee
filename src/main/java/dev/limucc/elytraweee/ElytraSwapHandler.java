package dev.limucc.elytraweee;

import dev.limucc.elytraweee.config.ElytraWeeeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

public class ElytraSwapHandler {
    private static final int CHEST_SLOT = 6;
    private static final int FIRST_STORAGE_SLOT = 9;
    private static final int DOUBLE_JUMP_WINDOW_TICKS = 8;
    private static final int LAND_REVERT_DELAY_TICKS = 5;
    private static final int GLIDE_ATTEMPT_TICKS = 10;

    private enum State { IDLE, EQUIPPED }
    private State state = State.IDLE;
    private boolean chestWasOccupied = false;
    private int groundedTicks = 0;
    private int elytraOriginSlot = -1;
    private boolean autoDeployedForFlight = false;
    private boolean prevJumpDown = false;
    private int firstJumpTick = 0;
    private long tickCounter = 0;
    private int pendingJumpTick = 0;
    private long suppressAutoRevertUntilTick = 0;
    private long glideUntilTick = 0;

    public void tick(Minecraft client) {
        tickCounter++;
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            reset();
            glideUntilTick = 0;
            prevJumpDown = false;
            return;
        }

        ElytraWeeeConfig cfg = ElytraWeeeConfig.get();
        boolean jumpDown = client.options.keyJump.isDown();
        if (!cfg.enabled) {
            reset();
            glideUntilTick = 0;
            prevJumpDown = jumpDown;
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        boolean canClick = menu == player.inventoryMenu
                && client.screen == null
                && menu.getCarried().isEmpty()
                && menu.slots.size() > CHEST_SLOT;
        boolean wearingElytra = player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
        boolean holdingFirework = isFirework(player.getMainHandItem()) || isFirework(player.getOffhandItem());

        if (wearingElytra && player.onGround()) groundedTicks++; else groundedTicks = 0;

        boolean jumpEdge = jumpDown && !prevJumpDown && client.screen == null;
        prevJumpDown = jumpDown;
        boolean triggerEquip = false;

        if (jumpEdge && !wearingElytra && state == State.IDLE && jumpModeSatisfied(cfg)) {
            if (holdingFirework) {
                triggerEquip = true;
                pendingJumpTick = 0;
            } else if (cfg.graceWindowEnabled) {
                pendingJumpTick = (int) tickCounter;
            }
        }

        if (pendingJumpTick > 0) {
            if (tickCounter - pendingJumpTick > cfg.graceWindowTicks) {
                pendingJumpTick = 0;
            } else if (wearingElytra || state != State.IDLE) {
                pendingJumpTick = 0;
            } else if (player.onGround() && tickCounter > pendingJumpTick + 1) {
                pendingJumpTick = 0;
            } else if (holdingFirework) {
                triggerEquip = true;
                pendingJumpTick = 0;
            }
        }

        if (triggerEquip && canClick) {
            int elytraSlot = findElytraSlot(menu);
            if (elytraSlot >= 0) {
                boolean chestOccupied = menu.getSlot(CHEST_SLOT).hasItem();
                equipElytra(client, player, menu, elytraSlot, chestOccupied);
                state = State.EQUIPPED;
                chestWasOccupied = chestOccupied;
                groundedTicks = 0;
                pendingJumpTick = 0;
                autoDeployedForFlight = true;
                wearingElytra = true;
                glideUntilTick = tickCounter + GLIDE_ATTEMPT_TICKS;
            }
        }

        if (glideUntilTick > 0) {
            boolean stillElytra = player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
            if (tickCounter > glideUntilTick || player.onGround() || player.isFallFlying() || !stillElytra) {
                glideUntilTick = 0;
            } else {
                tryStartGliding(player);
                if (player.isFallFlying()) glideUntilTick = 0;
            }
        }

        if (!wearingElytra || !autoDeployedForFlight) {
            if (state == State.EQUIPPED && !wearingElytra) reset();
        } else if (!triggerEquip && canClick && tickCounter >= suppressAutoRevertUntilTick) {
            boolean landed = player.onGround() && !player.isFallFlying()
                    && groundedTicks >= LAND_REVERT_DELAY_TICKS;
            boolean revert = landed && (cfg.swapBackOnLanding
                    || (cfg.swapBackWhenNotHoldingFirework && !holdingFirework));
            if (revert) {
                boolean done = chestWasOccupied
                        ? swapBackToChestplate(client, player, menu)
                        : removeElytraToInventory(client, player, menu);
                if (!done && chestWasOccupied) done = removeElytraToInventory(client, player, menu);
                if (done) reset(); else suppressAutoRevertUntilTick = tickCounter + 10;
            }
        }
    }

    private boolean jumpModeSatisfied(ElytraWeeeConfig cfg) {
        if (cfg.jumpMode == ElytraWeeeConfig.JumpMode.SINGLE) return true;
        if (firstJumpTick > 0 && tickCounter - firstJumpTick <= DOUBLE_JUMP_WINDOW_TICKS) {
            firstJumpTick = 0;
            return true;
        }
        firstJumpTick = (int) tickCounter;
        return false;
    }

    public void fastSwap(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;
        ElytraWeeeConfig cfg = ElytraWeeeConfig.get();
        if (!cfg.fastSwapEnabled) return;
        AbstractContainerMenu menu = player.containerMenu;
        boolean canClick = menu == player.inventoryMenu
                && client.screen == null
                && menu.getCarried().isEmpty()
                && menu.slots.size() > CHEST_SLOT;
        if (!canClick) return;

        boolean wearingElytra = player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
        if (wearingElytra) {
            boolean done = swapBackToChestplate(client, player, menu);
            if (!done) done = removeElytraToInventory(client, player, menu);
            if (done) {
                reset();
                suppressAutoRevertUntilTick = tickCounter + cfg.fastSwapRevertCooldownTicks;
            }
            return;
        }

        int elytraSlot = findElytraSlot(menu);
        if (elytraSlot < 0) return;
        boolean chestOccupied = menu.getSlot(CHEST_SLOT).hasItem();
        equipElytra(client, player, menu, elytraSlot, chestOccupied);
        state = State.EQUIPPED;
        chestWasOccupied = chestOccupied;
        groundedTicks = 0;
        pendingJumpTick = 0;
        autoDeployedForFlight = false;
        suppressAutoRevertUntilTick = tickCounter + cfg.fastSwapRevertCooldownTicks;
        if (cfg.autoReglideOnFastSwap && !player.onGround()) glideUntilTick = tickCounter + GLIDE_ATTEMPT_TICKS;
    }

    private void tryStartGliding(LocalPlayer player) {
        if (player.tryToStartFallFlying()) {
            player.connection.send(new ServerboundPlayerCommandPacket(
                    player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }

    private void equipElytra(Minecraft client, Player player, AbstractContainerMenu menu, int elytraSlot, boolean chestOccupied) {
        int id = menu.containerId;
        elytraOriginSlot = elytraSlot;
        if (chestOccupied) {
            pickup(client, player, id, elytraSlot);
            pickup(client, player, id, CHEST_SLOT);
            pickup(client, player, id, elytraSlot);
        } else {
            pickup(client, player, id, elytraSlot);
            pickup(client, player, id, CHEST_SLOT);
        }
    }

    private boolean swapBackToChestplate(Minecraft client, Player player, AbstractContainerMenu menu) {
        int id = menu.containerId;
        int chestplateSlot = findChestplateSlot(menu);
        if (chestplateSlot >= 0) {
            pickup(client, player, id, CHEST_SLOT);
            pickup(client, player, id, chestplateSlot);
            pickup(client, player, id, CHEST_SLOT);
            return true;
        }
        return false;
    }

    private boolean removeElytraToInventory(Minecraft client, Player player, AbstractContainerMenu menu) {
        int id = menu.containerId;
        if (elytraOriginSlot >= FIRST_STORAGE_SLOT && elytraOriginSlot < menu.slots.size()
                && menu.getSlot(elytraOriginSlot).getItem().isEmpty()) {
            pickup(client, player, id, CHEST_SLOT);
            pickup(client, player, id, elytraOriginSlot);
            return true;
        }
        quickMove(client, player, id, CHEST_SLOT);
        return menu.getSlot(CHEST_SLOT).getItem().getItem() != Items.ELYTRA;
    }

    private int findElytraSlot(AbstractContainerMenu menu) {
        for (int i = FIRST_STORAGE_SLOT; i < menu.slots.size(); i++) {
            if (menu.getSlot(i).getItem().getItem() == Items.ELYTRA) return i;
        }
        return -1;
    }

    private int findChestplateSlot(AbstractContainerMenu menu) {
        for (int i = FIRST_STORAGE_SLOT; i < menu.slots.size(); i++) {
            if (isChestplate(menu.getSlot(i).getItem())) return i;
        }
        return -1;
    }

    private static boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() == Items.ELYTRA) return false;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    private static boolean isFirework(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET;
    }

    private void pickup(Minecraft client, Player player, int containerId, int slot) {
        client.gameMode.handleInventoryMouseClick(containerId, slot, 0, ClickType.PICKUP, player);
    }

    private void quickMove(Minecraft client, Player player, int containerId, int slot) {
        client.gameMode.handleInventoryMouseClick(containerId, slot, 0, ClickType.QUICK_MOVE, player);
    }

    private void reset() {
        state = State.IDLE;
        chestWasOccupied = false;
        groundedTicks = 0;
        firstJumpTick = 0;
        pendingJumpTick = 0;
        elytraOriginSlot = -1;
        autoDeployedForFlight = false;
    }
}
