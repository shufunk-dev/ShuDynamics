package net.enchantedwood.screen;

import net.enchantedwood.block.entity.TungstenBatteryBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class TungstenBatteryScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public TungstenBatteryScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(TungstenBatteryBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(5));
    }

    public TungstenBatteryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.TUNGSTEN_BATTERY_SCREEN_HANDLER, syncId);
        checkSize(inventory, TungstenBatteryBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Discharge Slot (x=16, y=35)
        this.addSlot(new Slot(inventory, TungstenBatteryBlockEntity.DISCHARGE_SLOT, 16, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
            }
        });

        // Slot 1: Charge Slot (x=144, y=35)
        this.addSlot(new Slot(inventory, TungstenBatteryBlockEntity.CHARGE_SLOT, 144, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
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
        return max > 0 ? max : TungstenBatteryBlockEntity.CAPACITY;
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
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < TungstenBatteryBlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, TungstenBatteryBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof ItemEnergyProvider || originalStack.getItem() instanceof EnergyProvider) {
                    if (!this.insertItem(originalStack, TungstenBatteryBlockEntity.CHARGE_SLOT, TungstenBatteryBlockEntity.CHARGE_SLOT + 1, false) &&
                        !this.insertItem(originalStack, TungstenBatteryBlockEntity.DISCHARGE_SLOT, TungstenBatteryBlockEntity.DISCHARGE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot < 2 + 27) {
                    if (!this.insertItem(originalStack, 2 + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(originalStack, 2, 2 + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
