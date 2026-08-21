package net.enchantedwood.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CopperBatteryScreenHandler extends ScreenHandler {
    private final PropertyDelegate propertyDelegate;

    public CopperBatteryScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null, new ArrayPropertyDelegate(5));
    }

    public CopperBatteryScreenHandler(int syncId, PlayerInventory playerInventory, Object unused, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.COPPER_BATTERY_SCREEN_HANDLER, syncId);
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);

        // Player Inventory (3 rows of 9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (1 row of 9)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(0) & 0xFFFF) | ((this.propertyDelegate.get(1) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
        return max > 0 ? max : 500_000;
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
            if (invSlot < 27) {
                if (!this.insertItem(originalStack, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot < 36) {
                if (!this.insertItem(originalStack, 0, 27, false)) {
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
        return true;
    }
}
