package net.enchantedwood.screen;

import net.enchantedwood.block.entity.SteelBatteryBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SteelBatteryScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public SteelBatteryScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(SteelBatteryBlockEntity.INVENTORY_SIZE), new SimpleContainerData(5));
    }

    public SteelBatteryScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.STEEL_BATTERY_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, SteelBatteryBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Discharge Slot (x=16, y=35)
        this.addSlot(new Slot(inventory, SteelBatteryBlockEntity.DISCHARGE_SLOT, 16, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
            }
        });

        // Slot 1: Charge Slot (x=144, y=35)
        this.addSlot(new Slot(inventory, SteelBatteryBlockEntity.CHARGE_SLOT, 144, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
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
        return max > 0 ? max : SteelBatteryBlockEntity.CAPACITY;
    }

    public int getMaxTransfer() {
        return this.propertyDelegate.get(4);
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

            if (invSlot < SteelBatteryBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, SteelBatteryBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof ItemEnergyProvider || originalStack.getItem() instanceof EnergyProvider) {
                    if (!this.moveItemStackTo(originalStack, SteelBatteryBlockEntity.CHARGE_SLOT, SteelBatteryBlockEntity.CHARGE_SLOT + 1, false) &&
                        !this.moveItemStackTo(originalStack, SteelBatteryBlockEntity.DISCHARGE_SLOT, SteelBatteryBlockEntity.DISCHARGE_SLOT + 1, false)) {
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
