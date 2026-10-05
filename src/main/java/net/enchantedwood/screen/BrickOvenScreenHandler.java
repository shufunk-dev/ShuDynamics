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

public class BrickOvenScreenHandler extends ScreenHandler {
    public static final int TOTAL_SLOTS = 3;
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    private final PlayerEntity player;

    public BrickOvenScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(TOTAL_SLOTS), new ArrayPropertyDelegate(8));
    }

    public BrickOvenScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.BRICK_OVEN_SCREEN_HANDLER, syncId);
        checkSize(inventory, TOTAL_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.player = playerInventory.player;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Machine Slots
        this.addSlot(new Slot(inventory, INPUT_SLOT, 56, 17));
        this.addSlot(new Slot(inventory, FUEL_SLOT, 56, 53));
        this.addSlot(new Slot(inventory, OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (1 row x 9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public boolean isBurning() {
        return this.propertyDelegate.get(0) > 0;
    }

    public int getBurnProgress(int pixels) {
        int fuelTime = this.propertyDelegate.get(1);
        if (fuelTime <= 0) fuelTime = 200;
        return this.propertyDelegate.get(0) * pixels / fuelTime;
    }

    public int getCookProgress(int pixels) {
        int cookTime = this.propertyDelegate.get(2);
        int maxCookTime = this.propertyDelegate.get(3);
        if (maxCookTime <= 0) maxCookTime = 80;
        return cookTime * pixels / maxCookTime;
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(5) << 16) | (this.propertyDelegate.get(4) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(7) << 16) | (this.propertyDelegate.get(6) & 0xFFFF);
    }

    public boolean hasEnergy() {
        return getEnergy() > 0;
    }

    public int getScaledEnergy(int pixels) {
        int energy = getEnergy();
        int max = getMaxEnergy();
        if (max <= 0) max = 20000;
        return (int) (((long) energy * pixels) / max);
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

            if (invSlot == OUTPUT_SLOT) {
                if (!this.insertItem(originalStack, TOTAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else if (invSlot < TOTAL_SLOTS) {
                if (!this.insertItem(originalStack, TOTAL_SLOTS, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory -> into oven slots
                if (isFuel(player, originalStack)) {
                    if (!this.insertItem(originalStack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        if (!this.insertItem(originalStack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (!this.insertItem(originalStack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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

    private static boolean isFuel(PlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (player != null && player.getEntityWorld() != null) {
            return net.enchantedwood.block.entity.BrickOvenBlockEntity.getFuelBurnTime(player.getEntityWorld(), stack) > 0;
        }
        return net.enchantedwood.block.entity.BrickOvenBlockEntity.getFuelBurnTime(null, stack) > 0;
    }
}
