package net.enchantedwood.screen;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class RoadPaverScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public RoadPaverScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(10), new ArrayPropertyDelegate(7));
    }

    public RoadPaverScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ROAD_PAVER_SCREEN_HANDLER, syncId);
        checkSize(inventory, 10);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Slots 0-8: 3x3 Asphalt Storage Grid (x=62, y=18)
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 3; ++c) {
                this.addSlot(new Slot(inventory, c + r * 3, 62 + c * 18, 18 + r * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return stack.isOf(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.isOf(ModBlocks.ASPHALT_SLAB.asItem());
                    }
                });
            }
        }

        // Slot 9: Battery Charge Slot (x=12, y=53)
        this.addSlot(new Slot(inventory, 9, 12, 53));

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

    public boolean isPaving() {
        return propertyDelegate.get(6) == 1;
    }

    public int getScaledProgress() {
        int progress = propertyDelegate.get(0);
        int maxProgress = propertyDelegate.get(1);
        int barHeight = 36;
        return maxProgress != 0 && progress != 0 ? progress * barHeight / maxProgress : 0;
    }

    public long getEnergy() {
        long low = propertyDelegate.get(2) & 0xFFFFL;
        long high = propertyDelegate.get(3) & 0xFFFFL;
        return (high << 16) | low;
    }

    public long getMaxEnergy() {
        long low = propertyDelegate.get(4) & 0xFFFFL;
        long high = propertyDelegate.get(5) & 0xFFFFL;
        return (high << 16) | low;
    }

    public int getScaledEnergy() {
        long energy = getEnergy();
        long maxEnergy = getMaxEnergy();
        int energyBarHeight = 36;
        return maxEnergy != 0 && energy != 0 ? (int) (energy * energyBarHeight / maxEnergy) : 0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 10) {
                if (!this.insertItem(originalStack, 10, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, 10, false)) {
                return ItemStack.EMPTY;
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
