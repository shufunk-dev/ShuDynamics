package net.enchantedwood.screen;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BrickOvenScreenHandler extends AbstractContainerMenu {
    public static final int TOTAL_SLOTS = 3;
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    private final Container inventory;
    private final ContainerData propertyDelegate;
    private final Player player;

    public BrickOvenScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(TOTAL_SLOTS), new SimpleContainerData(8));
    }

    public BrickOvenScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.BRICK_OVEN_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, TOTAL_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.player = playerInventory.player;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Machine Slots
        this.addSlot(new Slot(inventory, INPUT_SLOT, 56, 17));
        this.addSlot(new Slot(inventory, FUEL_SLOT, 56, 53));
        this.addSlot(new Slot(inventory, OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
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
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot == OUTPUT_SLOT) {
                if (!this.moveItemStackTo(originalStack, TOTAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(originalStack, newStack);
            } else if (invSlot < TOTAL_SLOTS) {
                if (!this.moveItemStackTo(originalStack, TOTAL_SLOTS, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory -> into oven slots
                if (isFuel(player, originalStack)) {
                    if (!this.moveItemStackTo(originalStack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        if (!this.moveItemStackTo(originalStack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (!this.moveItemStackTo(originalStack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
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

    private static boolean isFuel(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (player != null && player.level() != null) {
            return net.enchantedwood.block.entity.BrickOvenBlockEntity.getFuelBurnTime(player.level(), stack) > 0;
        }
        return net.enchantedwood.block.entity.BrickOvenBlockEntity.getFuelBurnTime(null, stack) > 0;
    }
}
