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

public class IceCreamMachineScreenHandler extends ScreenHandler {
    public static final int TOTAL_SLOTS = 6;
    public static final int REFRIGERANT_SLOT = 0;
    public static final int BASE_SLOT = 1;
    public static final int SWEETENER_SLOT = 2;
    public static final int FLAVOR_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int RETURN_SLOT = 5;

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public IceCreamMachineScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(TOTAL_SLOTS), new ArrayPropertyDelegate(6));
    }

    public IceCreamMachineScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ICE_CREAM_MACHINE_SCREEN_HANDLER, syncId);
        checkSize(inventory, TOTAL_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Machine Slots
        this.addSlot(new Slot(inventory, REFRIGERANT_SLOT, 34, 20));
        this.addSlot(new Slot(inventory, BASE_SLOT, 56, 20));
        this.addSlot(new Slot(inventory, SWEETENER_SLOT, 34, 48));
        this.addSlot(new Slot(inventory, FLAVOR_SLOT, 56, 48));
        this.addSlot(new Slot(inventory, OUTPUT_SLOT, 124, 34) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(inventory, RETURN_SLOT, 148, 34) {
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

    public int getEnergy() {
        return (this.propertyDelegate.get(1) << 16) | (this.propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(3) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
    }

    public int getChurnProgress() {
        return this.propertyDelegate.get(4);
    }

    public int getMaxChurnProgress() {
        int max = this.propertyDelegate.get(5);
        return max > 0 ? max : 100;
    }

    public int getScaledEnergy(int pixels) {
        int energy = getEnergy();
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int) (((long) energy * pixels) / max);
    }

    public int getScaledProgress(int pixels) {
        int progress = getChurnProgress();
        int max = getMaxChurnProgress();
        return (progress * pixels) / max;
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

            if (invSlot == OUTPUT_SLOT || invSlot == RETURN_SLOT) {
                if (!this.insertItem(originalStack, TOTAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else if (invSlot < TOTAL_SLOTS) {
                if (!this.insertItem(originalStack, TOTAL_SLOTS, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Route player items into appropriate slots
                if (isRefrigerant(originalStack)) {
                    if (!this.insertItem(originalStack, REFRIGERANT_SLOT, REFRIGERANT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isBaseLiquid(originalStack)) {
                    if (!this.insertItem(originalStack, BASE_SLOT, BASE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isSweetener(originalStack)) {
                    if (!this.insertItem(originalStack, SWEETENER_SLOT, SWEETENER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.insertItem(originalStack, FLAVOR_SLOT, FLAVOR_SLOT + 1, false)) {
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

    private static boolean isRefrigerant(ItemStack stack) {
        return stack.isOf(net.minecraft.item.Items.ICE)
                || stack.isOf(net.enchantedwood.item.ModItems.ICE_CUBES)
                || stack.isOf(net.minecraft.item.Items.PACKED_ICE)
                || stack.isOf(net.minecraft.item.Items.BLUE_ICE)
                || stack.isOf(net.minecraft.item.Items.SNOWBALL)
                || stack.isOf(net.minecraft.item.Items.SNOW_BLOCK)
                || stack.isOf(net.enchantedwood.item.ModItems.SALT);
    }

    private static boolean isBaseLiquid(ItemStack stack) {
        return stack.isOf(net.minecraft.item.Items.MILK_BUCKET)
                || stack.isOf(net.enchantedwood.item.ModItems.SOY_MILK);
    }

    private static boolean isSweetener(ItemStack stack) {
        return stack.isOf(net.minecraft.item.Items.SUGAR)
                || stack.isOf(net.minecraft.item.Items.HONEY_BOTTLE);
    }
}
