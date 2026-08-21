package net.enchantedwood.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.enchantedwood.item.ModItems;

public class CokeOvenScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public CokeOvenScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2), new ArrayPropertyDelegate(2));
    }

    public CokeOvenScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.COKE_OVEN_SCREEN_HANDLER, syncId);
        checkSize(inventory, 2);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Input Slot (Coal / Charcoal) at (56, 35)
        this.addSlot(new Slot(inventory, 0, 56, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL);
            }
        });

        // Output Slot (Coke Coal) at (116, 35)
        this.addSlot(new Slot(inventory, 1, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getCookTime() {
        return this.propertyDelegate.get(0);
    }

    public int getTotalCookTime() {
        int total = this.propertyDelegate.get(1);
        return total > 0 ? total : 200;
    }

    public boolean isCooking() {
        return getCookTime() > 0;
    }

    public int getScaledCookProgress(int pixels) {
        return getCookTime() * pixels / getTotalCookTime();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot == 1) { // Output slot
                if (!this.insertItem(originalStack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else if (invSlot == 0) { // Input slot
                if (!this.insertItem(originalStack, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player Inventory
                if (originalStack.isOf(Items.COAL) || originalStack.isOf(Items.CHARCOAL)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 2 && invSlot < 29) {
                    if (!this.insertItem(originalStack, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 29 && invSlot < 38) {
                    if (!this.insertItem(originalStack, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
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
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
