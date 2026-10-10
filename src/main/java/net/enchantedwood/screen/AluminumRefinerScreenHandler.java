package net.enchantedwood.screen;

import net.enchantedwood.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AluminumRefinerScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public AluminumRefinerScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(4), new SimpleContainerData(8));
    }

    public AluminumRefinerScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ALUMINUM_REFINER_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 4);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Slot 0: Bauxite Input (x=48, y=34)
        this.addSlot(new Slot(inventory, 0, 48, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RAW_BAUXITE) || stack.is(ModItems.BAUXITE_DUST);
            }
        });

        // Slot 1: O2 Canister in (x=80, y=17)
        this.addSlot(new Slot(inventory, 1, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.OXYGEN_CANISTER);
            }
        });

        // Slot 2: Empty Canister out (x=80, y=53)
        this.addSlot(new Slot(inventory, 2, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Slot 3: Ingot Output (x=116, y=34)
        this.addSlot(new Slot(inventory, 3, 116, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
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
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < 4) {
                if (!this.moveItemStackTo(originalStack, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.is(ModItems.RAW_BAUXITE) || originalStack.is(ModItems.BAUXITE_DUST)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(ModItems.OXYGEN_CANISTER)) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 4 && invSlot < 31) {
                    if (!this.moveItemStackTo(originalStack, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 31 && invSlot < 40) {
                    if (!this.moveItemStackTo(originalStack, 4, 31, false)) {
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
