package net.enchantedwood.screen;

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
import net.enchantedwood.item.ModItems;

public class SteelBlastFurnaceScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public SteelBlastFurnaceScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5), new ArrayPropertyDelegate(9));
    }

    public SteelBlastFurnaceScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.STEEL_BLAST_FURNACE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 5);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Slot 0: Iron Input (48, 24)
        this.addSlot(new Slot(inventory, 0, 48, 24) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.IRON_INGOT) || stack.isOf(ModItems.IRON_DUST);
            }
        });

        // Slot 1: Coke Coal Input (48, 48)
        this.addSlot(new Slot(inventory, 1, 48, 48) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.COKE_COAL);
            }
        });

        // Slot 2: Steel Output (116, 35)
        this.addSlot(new Slot(inventory, 2, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
        });

        // Slot 3: H2 Canister In (80, 17)
        this.addSlot(new Slot(inventory, 3, 80, 17) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.HYDROGEN_CANISTER);
            }
        });

        // Slot 4: Empty Canister Out (80, 53)
        this.addSlot(new Slot(inventory, 4, 80, 53) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
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
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot == 2 || invSlot == 4) { // Output slots
                if (!this.insertItem(originalStack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else if (invSlot >= 0 && invSlot <= 4) { // Machine input slots
                if (!this.insertItem(originalStack, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // Player Inventory
                if (originalStack.isOf(Items.IRON_INGOT) || originalStack.isOf(ModItems.IRON_DUST)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.isOf(ModItems.COKE_COAL)) {
                    if (!this.insertItem(originalStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (originalStack.isOf(ModItems.HYDROGEN_CANISTER)) {
                    if (!this.insertItem(originalStack, 3, 4, false)) return ItemStack.EMPTY;
                } else if (invSlot >= 5 && invSlot < 32) {
                    if (!this.insertItem(originalStack, 32, 41, false)) return ItemStack.EMPTY;
                } else if (invSlot >= 32 && invSlot < 41) {
                    if (!this.insertItem(originalStack, 5, 32, false)) return ItemStack.EMPTY;
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
