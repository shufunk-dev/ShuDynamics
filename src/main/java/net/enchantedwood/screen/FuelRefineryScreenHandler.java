package net.enchantedwood.screen;

import net.enchantedwood.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class FuelRefineryScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public FuelRefineryScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5), new ArrayPropertyDelegate(6));
    }

    public FuelRefineryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.FUEL_REFINERY_SCREEN_HANDLER, syncId);
        checkSize(inventory, 5);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Slot 0: Feedstock Input (x=48, y=20)
        this.addSlot(new Slot(inventory, 0, 48, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.CRUDE_OIL_SLUDGE) || stack.isOf(ModItems.CORN) ||
                        stack.isOf(Items.WHEAT) || stack.isOf(Items.SUGAR_CANE) ||
                        stack.isOf(Items.POTATO) || stack.isOf(ModItems.GASOLINE_CANISTER);
            }
        });

        // Slot 1: Canister / Reagent Input (x=48, y=48)
        this.addSlot(new Slot(inventory, 1, 48, 48) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.EMPTY_GAS_CANISTER) || stack.isOf(ModItems.CORN);
            }
        });

        // Slot 2: Main Fuel Output (x=108, y=34)
        this.addSlot(new Slot(inventory, 2, 108, 34) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Byproduct Mineral Tar (x=134, y=34)
        this.addSlot(new Slot(inventory, 3, 134, 34) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 4: Battery Charge Slot (x=12, y=53)
        this.addSlot(new Slot(inventory, 4, 12, 53));

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

    public boolean isRefining() {
        return propertyDelegate.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = propertyDelegate.get(0);
        int maxProgress = propertyDelegate.get(1);
        int progressArrowSize = 24;
        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
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
            if (invSlot < 5) {
                if (!this.insertItem(originalStack, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, 5, false)) {
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
