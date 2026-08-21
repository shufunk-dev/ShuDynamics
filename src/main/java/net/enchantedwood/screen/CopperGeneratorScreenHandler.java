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
import net.enchantedwood.block.entity.CopperGeneratorBlockEntity;

public class CopperGeneratorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public CopperGeneratorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(1), new ArrayPropertyDelegate(7));
    }

    public CopperGeneratorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.COPPER_GENERATOR_SCREEN_HANDLER, syncId);
        checkSize(inventory, 1);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Fuel Slot (center)
        this.addSlot(new Slot(inventory, 0, 80, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return CopperGeneratorBlockEntity.getFuelTime(stack) > 0;
            }
        });

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
        return max > 0 ? max : 100_000;
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
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot == 0) {
                if (!this.insertItem(originalStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (CopperGeneratorBlockEntity.getFuelTime(originalStack) > 0) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 1 && invSlot < 28) {
                    if (!this.insertItem(originalStack, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 28 && invSlot < 37) {
                    if (!this.insertItem(originalStack, 1, 28, false)) {
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
