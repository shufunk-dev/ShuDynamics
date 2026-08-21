package net.enchantedwood.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.enchantedwood.item.custom.EnchantedCapeItem;
import net.enchantedwood.item.custom.EnchantedHeartItem;

public class EquipmentScreenHandler extends ScreenHandler {
    private final Inventory equipmentInventory;

    // Client-side constructor
    public EquipmentScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2));
    }

    // Server-side constructor
    public EquipmentScreenHandler(int syncId, PlayerInventory playerInventory, Inventory equipmentInventory) {
        super(ModScreenHandlers.EQUIPMENT_SCREEN_HANDLER, syncId);
        this.equipmentInventory = equipmentInventory;
        equipmentInventory.onOpen(playerInventory.player);

        // Slot 0: Back Slot (Cape)
        this.addSlot(new Slot(equipmentInventory, 0, 53, 31) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof EnchantedCapeItem;
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        // Slot 1: Heart Container Slot (Heart Lockets)
        this.addSlot(new Slot(equipmentInventory, 1, 107, 31) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof EnchantedHeartItem;
            }

            @Override
            public int getMaxItemCount() {
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
    public boolean canUse(PlayerEntity player) {
        return this.equipmentInventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < 2) {
                // Moving out of equipment slots into main inventory
                if (!this.insertItem(originalStack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from main inventory into equipment slots
                if (originalStack.getItem() instanceof EnchantedCapeItem) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.getItem() instanceof EnchantedHeartItem) {
                    if (!this.insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return newStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.equipmentInventory.onClose(player);
    }
}
