package net.enchantedwood.screen;

import net.enchantedwood.item.custom.EnchantedCapeItem;
import net.enchantedwood.item.custom.EnchantedHeartItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EquipmentScreenHandler extends AbstractContainerMenu {
    private final Container equipmentInventory;

    // Client-side constructor
    public EquipmentScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(2));
    }

    // Server-side constructor
    public EquipmentScreenHandler(int syncId, Inventory playerInventory, Container equipmentInventory) {
        super(ModScreenHandlers.EQUIPMENT_SCREEN_HANDLER, syncId);
        this.equipmentInventory = equipmentInventory;
        equipmentInventory.startOpen(playerInventory.player);

        // Slot 0: Back Slot (Cape)
        this.addSlot(new Slot(equipmentInventory, 0, 53, 31) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof EnchantedCapeItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slot 1: Heart Container Slot (Heart Lockets)
        this.addSlot(new Slot(equipmentInventory, 1, 107, 31) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof EnchantedHeartItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player Inventory (Slots 2 - 28)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (Slots 29 - 37)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.equipmentInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (slotIndex < 2) {
                // Moving out of equipment slots into main inventory
                if (!this.moveItemStackTo(originalStack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from main inventory into equipment slots
                if (originalStack.getItem() instanceof EnchantedCapeItem) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.getItem() instanceof EnchantedHeartItem) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
        }

        return newStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.equipmentInventory.stopOpen(player);
    }
}
