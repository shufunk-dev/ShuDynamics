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
import net.enchantedwood.item.ModItems;

public class EnchantedDriveBayScreenHandler extends ScreenHandler {
    private static final int[] SLOT_COLS = {36, 76, 116};
    private static final int[] SLOT_ROWS = {24, 48};

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public EnchantedDriveBayScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(6), new ArrayPropertyDelegate(8));
    }

    public EnchantedDriveBayScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_DRIVE_BAY_SCREEN_HANDLER, syncId);
        checkSize(inventory, 6);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // 6 Drive Slots (2 rows x 3 columns) placed at exact socket locations
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 3; ++col) {
                final int slotIndex = col + row * 3;
                this.addSlot(new Slot(inventory, slotIndex, SLOT_COLS[col], SLOT_ROWS[row]) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return stack.isOf(ModItems.STORAGE_CRYSTAL_1K)
                                || stack.isOf(ModItems.STORAGE_CRYSTAL_4K)
                                || stack.isOf(ModItems.STORAGE_CRYSTAL_16K)
                                || stack.isOf(ModItems.STORAGE_CRYSTAL_64K);
                    }

                    @Override
                    public boolean canTakeItems(PlayerEntity playerEntity) {
                        return canTakeDrive(slotIndex);
                    }
                });
            }
        }

        // Player Inventory (3 rows x 9 columns)
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

    public int getDriveCapacity(int slot) {
        if (slot < 0 || slot >= 6) return 0;
        ItemStack stack = this.inventory.getStack(slot);
        if (stack.isOf(ModItems.STORAGE_CRYSTAL_1K)) return 1000;
        if (stack.isOf(ModItems.STORAGE_CRYSTAL_4K)) return 4000;
        if (stack.isOf(ModItems.STORAGE_CRYSTAL_16K)) return 16000;
        if (stack.isOf(ModItems.STORAGE_CRYSTAL_64K)) return 64000;
        return 0;
    }

    public boolean canTakeDrive(int slot) {
        int capWithoutThis = getTotalCapacity() - getDriveCapacity(slot);
        return getTotalStoredItems() <= capWithoutThis;
    }

    public int getTotalCapacity() {
        return this.propertyDelegate.get(0);
    }

    public int getTotalStoredItems() {
        return this.propertyDelegate.get(1);
    }

    public int getDriveState(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 6) {
            if (this.inventory.getStack(slotIndex).isEmpty()) return -1;
            return this.propertyDelegate.get(slotIndex + 2);
        }
        return -1;
    }

    public boolean hasDrive(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 6) {
            return !this.inventory.getStack(slotIndex).isEmpty();
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < 6) {
                // If drive contains active data that exceeds remaining capacity, block extraction!
                if (!canTakeDrive(slotIndex)) {
                    return ItemStack.EMPTY;
                }

                if (!this.insertItem(originalStack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.insertItem(originalStack, 0, 6, false)) {
                    return ItemStack.EMPTY;
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
