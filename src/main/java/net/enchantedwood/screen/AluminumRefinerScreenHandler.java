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
import net.enchantedwood.item.ModItems;

public class AluminumRefinerScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public AluminumRefinerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(4), new ArrayPropertyDelegate(8));
    }

    public AluminumRefinerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ALUMINUM_REFINER_SCREEN_HANDLER, syncId);
        checkSize(inventory, 4);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Slot 0: Bauxite Input (x=48, y=34)
        this.addSlot(new Slot(inventory, 0, 48, 34) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.RAW_BAUXITE) || stack.isOf(ModItems.BAUXITE_DUST);
            }
        });

        // Slot 1: O2 Canister in (x=80, y=17)
        this.addSlot(new Slot(inventory, 1, 80, 17) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.OXYGEN_CANISTER);
            }
        });

        // Slot 2: Empty Canister out (x=80, y=53)
        this.addSlot(new Slot(inventory, 2, 80, 53) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
        });

        // Slot 3: Ingot Output (x=116, y=34)
        this.addSlot(new Slot(inventory, 3, 116, 34) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
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

    public int getEnergy() {
        return (this.propertyDelegate.get(0) & 0xFFFF) | ((this.propertyDelegate.get(1) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
        return max > 0 ? max : 100_000;
    }

    public int getOxygenAmount() { return this.propertyDelegate.get(4); }
    public int getMaxOxygen() { return this.propertyDelegate.get(5); }
    public int getCookTime() { return this.propertyDelegate.get(6); }
    public int getTotalCookTime() { return this.propertyDelegate.get(7); }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        return max > 0 ? (int) (((long) getEnergy() * pixels) / max) : 0;
    }

    public int getScaledOxygen(int pixels) {
        int max = getMaxOxygen();
        return max > 0 ? (int) (((long) getOxygenAmount() * pixels) / max) : 0;
    }

    public int getScaledCookProgress(int pixels) {
        int total = getTotalCookTime();
        return total > 0 ? getCookTime() * pixels / total : 0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 4) {
                if (!this.insertItem(originalStack, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.isOf(ModItems.RAW_BAUXITE) || originalStack.isOf(ModItems.BAUXITE_DUST)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(ModItems.OXYGEN_CANISTER)) {
                    if (!this.insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 4 && invSlot < 31) {
                    if (!this.insertItem(originalStack, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 31 && invSlot < 40) {
                    if (!this.insertItem(originalStack, 4, 31, false)) {
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
