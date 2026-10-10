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

public class EnchantedDriveBayScreenHandler extends AbstractContainerMenu {
    private static final int[] SLOT_COLS = {36, 76, 116};
    private static final int[] SLOT_ROWS = {24, 48};

    private final Container inventory;
    private final ContainerData propertyDelegate;

    public EnchantedDriveBayScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(6), new SimpleContainerData(8));
    }

    public EnchantedDriveBayScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_DRIVE_BAY_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 6);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // 6 Drive Slots (2 rows x 3 columns) placed at exact socket locations
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 3; ++col) {
                final int slotIndex = col + row * 3;
                this.addSlot(new Slot(inventory, slotIndex, SLOT_COLS[col], SLOT_ROWS[row]) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(ModItems.STORAGE_CRYSTAL_1K)
                                || stack.is(ModItems.STORAGE_CRYSTAL_4K)
                                || stack.is(ModItems.STORAGE_CRYSTAL_16K)
                                || stack.is(ModItems.STORAGE_CRYSTAL_64K);
                    }

                    @Override
                    public boolean mayPickup(Player playerEntity) {
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
        ItemStack stack = this.inventory.getItem(slot);
        if (stack.is(ModItems.STORAGE_CRYSTAL_1K)) return 1000;
        if (stack.is(ModItems.STORAGE_CRYSTAL_4K)) return 4000;
        if (stack.is(ModItems.STORAGE_CRYSTAL_16K)) return 16000;
        if (stack.is(ModItems.STORAGE_CRYSTAL_64K)) return 64000;
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
            if (this.inventory.getItem(slotIndex).isEmpty()) return -1;
            return this.propertyDelegate.get(slotIndex + 2);
        }
        return -1;
    }

    public boolean hasDrive(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 6) {
            return !this.inventory.getItem(slotIndex).isEmpty();
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (slotIndex < 6) {
                // If drive contains active data that exceeds remaining capacity, block extraction!
                if (!canTakeDrive(slotIndex)) {
                    return ItemStack.EMPTY;
                }

                if (!this.moveItemStackTo(originalStack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(originalStack, 0, 6, false)) {
                    return ItemStack.EMPTY;
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
}
