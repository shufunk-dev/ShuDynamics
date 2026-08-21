package net.enchantedwood.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import net.enchantedwood.event.PlayerEquipmentState;
import net.enchantedwood.event.PlayerHealthHandler;

public class PlayerEquipmentInventory implements Inventory {
    private final ServerPlayerEntity player;

    public PlayerEquipmentInventory(ServerPlayerEntity player) {
        this.player = player;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return getStack(0).isEmpty() && getStack(1).isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot == 0) return PlayerEquipmentState.getEquippedCape(player);
        if (slot == 1) return PlayerEquipmentState.getEquippedHeart(player);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack current = getStack(slot);
        if (!current.isEmpty()) {
            ItemStack result = current.split(amount);
            if (current.isEmpty()) {
                setStack(slot, ItemStack.EMPTY);
            } else {
                setStack(slot, current);
            }
            markDirty();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack current = getStack(slot);
        setStack(slot, ItemStack.EMPTY);
        markDirty();
        return current;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot == 0) {
            PlayerEquipmentState.equipCape(player, stack);
        } else if (slot == 1) {
            PlayerEquipmentState.equipHeart(player, stack);
        }
        markDirty();
    }

    @Override
    public void markDirty() {
        PlayerEquipmentState.savePlayerData(player);
        PlayerHealthHandler.applyHeartAbsorptionImmediate(player);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        setStack(0, ItemStack.EMPTY);
        setStack(1, ItemStack.EMPTY);
    }
}
