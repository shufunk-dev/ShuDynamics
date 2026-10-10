package net.enchantedwood.screen;

import net.enchantedwood.event.PlayerEquipmentState;
import net.enchantedwood.event.PlayerHealthHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerEquipmentInventory implements Container {
    private final ServerPlayer player;

    public PlayerEquipmentInventory(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return getItem(0).isEmpty() && getItem(1).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == 0) return PlayerEquipmentState.getEquippedCape(player);
        if (slot == 1) return PlayerEquipmentState.getEquippedHeart(player);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (!current.isEmpty()) {
            ItemStack result = current.split(amount);
            if (current.isEmpty()) {
                setItem(slot, ItemStack.EMPTY);
            } else {
                setItem(slot, current);
            }
            setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        setChanged();
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            PlayerEquipmentState.equipCape(player, stack);
        } else if (slot == 1) {
            PlayerEquipmentState.equipHeart(player, stack);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        PlayerEquipmentState.savePlayerData(player);
        PlayerHealthHandler.applyHeartAbsorptionImmediate(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        setItem(0, ItemStack.EMPTY);
        setItem(1, ItemStack.EMPTY);
    }
}
