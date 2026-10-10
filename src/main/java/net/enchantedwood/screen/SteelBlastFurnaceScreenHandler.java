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

public class SteelBlastFurnaceScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public SteelBlastFurnaceScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(5), new SimpleContainerData(9));
    }

    public SteelBlastFurnaceScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.STEEL_BLAST_FURNACE_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 5);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Slot 0: Iron Input (48, 24)
        this.addSlot(new Slot(inventory, 0, 48, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.IRON_INGOT) || stack.is(ModItems.IRON_DUST);
            }
        });

        // Slot 1: Coke Coal Input (48, 48)
        this.addSlot(new Slot(inventory, 1, 48, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.COKE_COAL);
            }
        });

        // Slot 2: Steel Output (116, 35)
        this.addSlot(new Slot(inventory, 2, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Slot 3: H2 Canister In (80, 17)
        this.addSlot(new Slot(inventory, 3, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.HYDROGEN_CANISTER);
            }
        });

        // Slot 4: Empty Canister Out (80, 53)
        this.addSlot(new Slot(inventory, 4, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
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

    public int getHydrogenAmount() { return this.propertyDelegate.get(4); }
    public int getMaxHydrogen() { return this.propertyDelegate.get(5); }
    public int getCookTime() { return this.propertyDelegate.get(6); }
    public int getTotalCookTime() {
        int total = this.propertyDelegate.get(7);
        return total > 0 ? total : 100;
    }
    public boolean isGreenMode() { return this.propertyDelegate.get(8) == 1; }

    public int getScaledCookProgress(int pixels) {
        return getCookTime() * pixels / getTotalCookTime();
    }

    public int getScaledHydrogen(int pixels) {
        int max = getMaxHydrogen();
        return max > 0 ? (int) (((long) getHydrogenAmount() * pixels) / max) : 0;
    }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        return max > 0 ? (int) (((long) getEnergy() * pixels) / max) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot == 2 || invSlot == 4) { // Output slots
                if (!this.moveItemStackTo(originalStack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(originalStack, newStack);
            } else if (invSlot >= 0 && invSlot <= 4) { // Machine input slots
                if (!this.moveItemStackTo(originalStack, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player Inventory
                if (originalStack.is(Items.IRON_INGOT) || originalStack.is(ModItems.IRON_DUST)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.is(ModItems.COKE_COAL)) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (originalStack.is(ModItems.HYDROGEN_CANISTER)) {
                    if (!this.moveItemStackTo(originalStack, 3, 4, false)) return ItemStack.EMPTY;
                } else if (invSlot >= 5 && invSlot < 32) {
                    if (!this.moveItemStackTo(originalStack, 32, 41, false)) return ItemStack.EMPTY;
                } else if (invSlot >= 32 && invSlot < 41) {
                    if (!this.moveItemStackTo(originalStack, 5, 32, false)) return ItemStack.EMPTY;
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
