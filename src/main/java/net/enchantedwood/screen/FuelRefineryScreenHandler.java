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
import net.minecraft.world.item.Items;

public class FuelRefineryScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public FuelRefineryScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(5), new SimpleContainerData(6));
    }

    public FuelRefineryScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.FUEL_REFINERY_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 5);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Slot 0: Feedstock Input (x=48, y=20)
        this.addSlot(new Slot(inventory, 0, 48, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.CRUDE_OIL_SLUDGE) || stack.is(ModItems.CORN) ||
                        stack.is(Items.WHEAT) || stack.is(Items.SUGAR_CANE) ||
                        stack.is(Items.POTATO) || stack.is(ModItems.GASOLINE_CANISTER);
            }
        });

        // Slot 1: Canister / Reagent Input (x=48, y=48)
        this.addSlot(new Slot(inventory, 1, 48, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.EMPTY_GAS_CANISTER) || stack.is(ModItems.CORN);
            }
        });

        // Slot 2: Main Fuel Output (x=108, y=34)
        this.addSlot(new Slot(inventory, 2, 108, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Byproduct Mineral Tar (x=134, y=34)
        this.addSlot(new Slot(inventory, 3, 134, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
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
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < 5) {
                if (!this.moveItemStackTo(originalStack, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, 5, false)) {
                return ItemStack.EMPTY;
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
