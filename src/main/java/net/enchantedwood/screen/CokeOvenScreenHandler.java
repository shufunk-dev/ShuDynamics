package net.enchantedwood.screen;

import net.enchantedwood.block.entity.CokeOvenBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CokeOvenScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public CokeOvenScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CokeOvenBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(2));
    }

    public CokeOvenScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.COKE_OVEN_SCREEN_HANDLER, syncId);
        checkSize(inventory, CokeOvenBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Input Slot (Coal / Charcoal / Logs) at (56, 35)
        this.addSlot(new Slot(inventory, CokeOvenBlockEntity.INPUT_SLOT, 56, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL) || stack.isIn(ItemTags.LOGS);
            }
        });

        // Primary Output Slot (Coke Coal) at (116, 35)
        this.addSlot(new Slot(inventory, CokeOvenBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Byproduct Output Slot (Mineral Tar) at (142, 35)
        this.addSlot(new Slot(inventory, CokeOvenBlockEntity.TAR_SLOT, 142, 35) {
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
        return total > 0 ? total : CokeOvenBlockEntity.TOTAL_COOK_TIME;
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

            if (invSlot == CokeOvenBlockEntity.OUTPUT_SLOT || invSlot == CokeOvenBlockEntity.TAR_SLOT) {
                if (!this.insertItem(originalStack, CokeOvenBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else if (invSlot == CokeOvenBlockEntity.INPUT_SLOT) {
                if (!this.insertItem(originalStack, CokeOvenBlockEntity.INVENTORY_SIZE, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player Inventory
                if (originalStack.isOf(Items.COAL) || originalStack.isOf(Items.CHARCOAL) || originalStack.isIn(ItemTags.LOGS)) {
                    if (!this.insertItem(originalStack, CokeOvenBlockEntity.INPUT_SLOT, CokeOvenBlockEntity.INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= CokeOvenBlockEntity.INVENTORY_SIZE && invSlot < CokeOvenBlockEntity.INVENTORY_SIZE + 27) {
                    if (!this.insertItem(originalStack, CokeOvenBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= CokeOvenBlockEntity.INVENTORY_SIZE + 27 && invSlot < this.slots.size()) {
                    if (!this.insertItem(originalStack, CokeOvenBlockEntity.INVENTORY_SIZE, CokeOvenBlockEntity.INVENTORY_SIZE + 27, false)) {
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
