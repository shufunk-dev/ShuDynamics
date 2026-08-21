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

public class OxygenGeneratorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public OxygenGeneratorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(6), new ArrayPropertyDelegate(12));
    }

    public OxygenGeneratorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.OXYGEN_GENERATOR_SCREEN_HANDLER, syncId);
        checkSize(inventory, 6);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Water Input / Output Slots
        this.addSlot(new Slot(inventory, 0, 16, 20));
        this.addSlot(new Slot(inventory, 1, 16, 56) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
        });

        // Oxygen Canister In / Out
        this.addSlot(new Slot(inventory, 2, 75, 20));
        this.addSlot(new Slot(inventory, 3, 75, 56) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
        });

        // Hydrogen Canister In / Out
        this.addSlot(new Slot(inventory, 4, 115, 20));
        this.addSlot(new Slot(inventory, 5, 115, 56) {
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

    public int getWaterAmount() { return this.propertyDelegate.get(4); }
    public int getMaxWater() { return this.propertyDelegate.get(5); }
    public int getOxygenAmount() { return this.propertyDelegate.get(6); }
    public int getMaxOxygen() { return this.propertyDelegate.get(7); }
    public int getHydrogenAmount() { return this.propertyDelegate.get(8); }
    public int getMaxHydrogen() { return this.propertyDelegate.get(9); }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        return max > 0 ? (int) (((long) getEnergy() * pixels) / max) : 0;
    }

    public int getScaledWater(int pixels) {
        int max = getMaxWater();
        return max > 0 ? (int) (((long) getWaterAmount() * pixels) / max) : 0;
    }

    public int getScaledOxygen(int pixels) {
        int max = getMaxOxygen();
        return max > 0 ? (int) (((long) getOxygenAmount() * pixels) / max) : 0;
    }

    public int getScaledHydrogen(int pixels) {
        int max = getMaxHydrogen();
        return max > 0 ? (int) (((long) getHydrogenAmount() * pixels) / max) : 0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 6) {
                if (!this.insertItem(originalStack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.insertItem(originalStack, 0, 6, false)) {
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
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
