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
import net.enchantedwood.block.entity.HarmonicRecordPressBlockEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;

public class HarmonicRecordPressScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public HarmonicRecordPressScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(4), new ArrayPropertyDelegate(7));
    }

    public HarmonicRecordPressScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.HARMONIC_RECORD_PRESS_SCREEN_HANDLER, syncId);
        checkSize(inventory, 4);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Slot 0: Blank Vinyl Disc
        this.addSlot(new Slot(inventory, HarmonicRecordPressBlockEntity.SLOT_DISC, 48, 22) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.BLANK_VINYL_DISC);
            }
        });

        // Slot 1: Catalyst Item
        this.addSlot(new Slot(inventory, HarmonicRecordPressBlockEntity.SLOT_CATALYST, 48, 48) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return !HarmonicRecordPressBlockEntity.getPressResult(new ItemStack(ModItems.BLANK_VINYL_DISC), stack).isEmpty();
            }
        });

        // Slot 2: Pressed Music Disc Output
        this.addSlot(new Slot(inventory, HarmonicRecordPressBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Overclocking Gear Slot
        this.addSlot(new Slot(inventory, HarmonicRecordPressBlockEntity.SLOT_GEAR, 18, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof GearItem;
            }
        });

        // Player Inventory (3 rows of 9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getScaledProgress() {
        int cookTime = this.propertyDelegate.get(0);
        int totalCookTime = this.propertyDelegate.get(1);
        int progressWidth = 24; // 24px arrow / turntable needle
        return totalCookTime != 0 && cookTime != 0 ? cookTime * progressWidth / totalCookTime : 0;
    }

    public int getScaledEnergy() {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        int barHeight = 52;
        return maxEnergy != 0 && energy != 0 ? energy * barHeight / maxEnergy : 0;
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(3) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(5) << 16) | (this.propertyDelegate.get(4) & 0xFFFF);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // From machine slots to player inventory
            if (invSlot < 4) {
                if (!this.insertItem(originalStack, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            }
            // From player inventory to machine slots
            else {
                // 1. Gear Item -> Slot 3
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.insertItem(originalStack, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 2. Blank Vinyl Disc -> Slot 0
                else if (originalStack.isOf(ModItems.BLANK_VINYL_DISC)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 3. Catalyst -> Slot 1
                else if (!HarmonicRecordPressBlockEntity.getPressResult(new ItemStack(ModItems.BLANK_VINYL_DISC), originalStack).isEmpty()) {
                    if (!this.insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Between player inv & hotbar
                else if (invSlot >= 4 && invSlot < 31) {
                    if (!this.insertItem(originalStack, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 31 && invSlot < 40) {
                    if (!this.insertItem(originalStack, 4, 31, false)) {
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
}
