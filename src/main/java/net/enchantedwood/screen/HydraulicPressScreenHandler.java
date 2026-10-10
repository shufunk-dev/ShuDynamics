package net.enchantedwood.screen;

import net.enchantedwood.item.custom.GearItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class HydraulicPressScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public HydraulicPressScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(3), new SimpleContainerData(7));
    }

    public HydraulicPressScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.HYDRAULIC_PRESS_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 3);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Machine Slots
        this.addSlot(new Slot(inventory, 0, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !net.enchantedwood.block.entity.HydraulicPressBlockEntity.getPlateResult(stack.getItem()).isEmpty();
            }
        });
        this.addSlot(new Slot(inventory, 1, 74, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GearItem;
            }
        });
        this.addSlot(new Slot(inventory, 2, 116, 35) {
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

    public int getScaledProgress() {
        int progress = this.propertyDelegate.get(4);
        int maxProgress = this.propertyDelegate.get(5);
        int progressArrowSize = 24;
        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(1) << 16) | (this.propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(3) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
    }

    public int getScaledEnergy() {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        int energyBarHeight = 52;
        return maxEnergy != 0 && energy != 0 ? (int) ((long) energy * energyBarHeight / maxEnergy) : 0;
    }

    public boolean isCrafting() {
        return this.propertyDelegate.get(4) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < 3) {
                if (!this.moveItemStackTo(originalStack, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!net.enchantedwood.block.entity.HydraulicPressBlockEntity.getPlateResult(originalStack.getItem()).isEmpty()) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
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
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
