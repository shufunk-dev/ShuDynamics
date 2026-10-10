package net.enchantedwood.screen;

import net.enchantedwood.block.entity.CokeOvenBlockEntity;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CokeOvenScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public CokeOvenScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(CokeOvenBlockEntity.INVENTORY_SIZE), new SimpleContainerData(2));
    }

    public CokeOvenScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.COKE_OVEN_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, CokeOvenBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Input Slot (Coal / Charcoal / Logs) at (56, 35)
        this.addSlot(new Slot(inventory, CokeOvenBlockEntity.INPUT_SLOT, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(ItemTags.LOGS);
            }
        });

        // Primary Output Slot (Coke Coal) at (116, 35)
        this.addSlot(new Slot(inventory, CokeOvenBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Byproduct Output Slot (Mineral Tar) at (142, 35)
        this.addSlot(new Slot(inventory, CokeOvenBlockEntity.TAR_SLOT, 142, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
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
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot == CokeOvenBlockEntity.OUTPUT_SLOT || invSlot == CokeOvenBlockEntity.TAR_SLOT) {
                if (!this.moveItemStackTo(originalStack, CokeOvenBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(originalStack, newStack);
            } else if (invSlot == CokeOvenBlockEntity.INPUT_SLOT) {
                if (!this.moveItemStackTo(originalStack, CokeOvenBlockEntity.INVENTORY_SIZE, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player Inventory
                if (originalStack.is(Items.COAL) || originalStack.is(Items.CHARCOAL) || originalStack.is(ItemTags.LOGS)) {
                    if (!this.moveItemStackTo(originalStack, CokeOvenBlockEntity.INPUT_SLOT, CokeOvenBlockEntity.INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= CokeOvenBlockEntity.INVENTORY_SIZE && invSlot < CokeOvenBlockEntity.INVENTORY_SIZE + 27) {
                    if (!this.moveItemStackTo(originalStack, CokeOvenBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= CokeOvenBlockEntity.INVENTORY_SIZE + 27 && invSlot < this.slots.size()) {
                    if (!this.moveItemStackTo(originalStack, CokeOvenBlockEntity.INVENTORY_SIZE, CokeOvenBlockEntity.INVENTORY_SIZE + 27, false)) {
                        return ItemStack.EMPTY;
                    }
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
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
