package net.enchantedwood.screen;

import net.enchantedwood.block.entity.SteelGeneratorBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SteelGeneratorScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public SteelGeneratorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(1), new SimpleContainerData(7));
    }

    public SteelGeneratorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.STEEL_GENERATOR_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 1);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Fuel Slot (center at 80, 53)
        this.addSlot(new Slot(inventory, 0, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SteelGeneratorBlockEntity.getFuelTime(playerInventory.player.level(), stack) > 0;
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

    public int getEnergy() {
        return (this.propertyDelegate.get(0) & 0xFFFF) | ((this.propertyDelegate.get(1) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
        return max > 0 ? max : 10_000_000;
    }

    public int getBurnTime() {
        return this.propertyDelegate.get(4);
    }

    public int getTotalBurnTime() {
        return this.propertyDelegate.get(5);
    }

    public int getGenerationRate() {
        return this.propertyDelegate.get(6);
    }

    public boolean isBurning() {
        return getBurnTime() > 0;
    }

    public int getScaledFuelProgress(int pixels) {
        int total = getTotalBurnTime();
        if (total == 0) total = 200;
        return getBurnTime() * pixels / total;
    }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int) (((long) getEnergy() * pixels) / max);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot == 0) {
                if (!this.moveItemStackTo(originalStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (SteelGeneratorBlockEntity.getFuelTime(player.level(), originalStack) > 0) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 1 && invSlot < 28) {
                    if (!this.moveItemStackTo(originalStack, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 28 && invSlot < 37) {
                    if (!this.moveItemStackTo(originalStack, 1, 28, false)) {
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
