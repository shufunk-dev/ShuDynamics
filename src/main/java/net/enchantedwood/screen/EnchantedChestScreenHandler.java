package net.enchantedwood.screen;

import net.enchantedwood.block.entity.EnchantedChestBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnchantedChestScreenHandler extends AbstractContainerMenu {
    private final Container masterInventory;
    private final SimpleContainer visibleInventory = new SimpleContainer(54) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (!EnchantedChestScreenHandler.this.isUpdating) {
                EnchantedChestScreenHandler.this.saveVisibleSlots();
            }
        }
    };
    private final ContainerData propertyDelegate;
    private int scrollRow = 0;
    private boolean isUpdating = false;

    public Container getInventory() {
        return this.masterInventory;
    }

    public EnchantedChestScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(162), new SimpleContainerData(3));
    }

    public EnchantedChestScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_CHEST_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 162);
        this.masterInventory = inventory;
        this.propertyDelegate = propertyDelegate;

        masterInventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Populate initial visible slots from master inventory
        this.updateVisibleSlots();

        // Visible slots automatically sync via visibleInventory setChanged

        // 54 Visible Chest Slots (6 rows x 9 columns: slots 0..53)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                final int slotIndex = col + row * 9;
                this.addSlot(new Slot(this.visibleInventory, slotIndex, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return (scrollRow * 9 + slotIndex) < getMaxSlots();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return (scrollRow * 9 + slotIndex) < getMaxSlots();
                    }
                });
            }
        }

        // Player Inventory (3 rows x 9 columns: slots 54..80)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player Hotbar (slots 81..89)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    public void updateVisibleSlots() {
        this.isUpdating = true;
        for (int i = 0; i < 54; i++) {
            int realIndex = this.scrollRow * 9 + i;
            if (realIndex < getMaxSlots()) {
                this.visibleInventory.setItem(i, this.masterInventory.getItem(realIndex).copy());
            } else {
                this.visibleInventory.setItem(i, ItemStack.EMPTY);
            }
        }
        this.isUpdating = false;
    }

    public void saveVisibleSlots() {
        for (int i = 0; i < 54; i++) {
            int realIndex = this.scrollRow * 9 + i;
            if (realIndex < getMaxSlots()) {
                this.masterInventory.setItem(realIndex, this.visibleInventory.getItem(i).copy());
            }
        }
        this.masterInventory.setChanged();
    }

    public void setScrollRow(int newScrollRow) {
        saveVisibleSlots();
        this.scrollRow = Math.max(0, Math.min(newScrollRow, getMaxScrollRows()));
        this.propertyDelegate.set(1, this.scrollRow);
        updateVisibleSlots();
        this.broadcastChanges();
    }

    public int getScrollRow() {
        return this.scrollRow;
    }

    public int getMaxScrollRows() {
        int totalRows = (int) Math.ceil((double) getMaxSlots() / 9.0);
        return Math.max(0, totalRows - 6);
    }

    public int getMaxSlots() {
        return propertyDelegate.get(2);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 2) { // Auto-Sort
            if (masterInventory instanceof EnchantedChestBlockEntity chestEntity) {
                saveVisibleSlots();
                chestEntity.sortInventory();
                updateVisibleSlots();
                this.broadcastChanges();
            }
            return true;
        }
        if (id >= 100) {
            int newRow = id - 100;
            this.setScrollRow(newRow);
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        saveVisibleSlots();
        super.removed(player);
        this.masterInventory.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.masterInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < 54) {
                // Moving from visible chest slot to player inventory (slots 54..89)
                if (!this.moveItemStackTo(originalStack, 54, 90, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory into the entire master chest
                saveVisibleSlots();
                boolean inserted = false;
                for (int i = 0; i < getMaxSlots(); i++) {
                    ItemStack target = masterInventory.getItem(i);
                    if (target.isEmpty()) {
                        masterInventory.setItem(i, originalStack.copy());
                        originalStack.setCount(0);
                        inserted = true;
                        break;
                    } else if (ItemStack.isSameItemSameComponents(target, originalStack) && target.getCount() < target.getMaxStackSize()) {
                        int transfer = Math.min(originalStack.getCount(), target.getMaxStackSize() - target.getCount());
                        target.grow(transfer);
                        originalStack.shrink(transfer);
                        if (originalStack.isEmpty()) {
                            inserted = true;
                            break;
                        }
                    }
                }

                if (!inserted && originalStack.getCount() == newStack.getCount()) {
                    return ItemStack.EMPTY;
                }
                updateVisibleSlots();
                this.broadcastChanges();
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
