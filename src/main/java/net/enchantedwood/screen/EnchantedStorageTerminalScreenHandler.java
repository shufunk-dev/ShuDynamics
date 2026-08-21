package net.enchantedwood.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class EnchantedStorageTerminalScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public EnchantedStorageTerminalScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(54), new ArrayPropertyDelegate(3));
    }

    public EnchantedStorageTerminalScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_STORAGE_TERMINAL_SCREEN_HANDLER, syncId);
        checkSize(inventory, 54);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // 54 Network Storage Slots (6 rows x 9 columns)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = col + row * 9;
                this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        if (!isOnline() || getTotalCapacity() <= 0) {
                            return false; // Cannot insert if offline or 0 capacity (no drives)
                        }
                        return getStoredCount() + stack.getCount() <= getTotalCapacity();
                    }
                });
            }
        }

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player Hotbar (1 row x 9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    public int getTotalCapacity() {
        return this.propertyDelegate.get(0);
    }

    public int getStoredCount() {
        return this.propertyDelegate.get(1);
    }

    public boolean isOnline() {
        return this.propertyDelegate.get(2) > 0;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return isOnline(); // Unlocks wireless range as long as network remains powered
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < 54) {
                // Move from Terminal to Player Inventory
                if (!this.insertItem(originalStack, 54, 90, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from Player Inventory to Terminal
                if (!isOnline() || getTotalCapacity() <= 0 || getStoredCount() >= getTotalCapacity()) {
                    return ItemStack.EMPTY; // Block insertion when full, offline, or no drives installed
                }

                if (!this.insertItem(originalStack, 0, 54, false)) {
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
}
