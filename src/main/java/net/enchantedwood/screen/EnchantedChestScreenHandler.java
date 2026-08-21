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
import net.enchantedwood.block.entity.EnchantedChestBlockEntity;

public class EnchantedChestScreenHandler extends ScreenHandler {
    private final Inventory masterInventory;
    private final SimpleInventory visibleInventory = new SimpleInventory(54);
    private final PropertyDelegate propertyDelegate;
    private int scrollRow = 0;
    private boolean isUpdating = false;

    public Inventory getInventory() {
        return this.masterInventory;
    }

    public EnchantedChestScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(162), new ArrayPropertyDelegate(3));
    }

    public EnchantedChestScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_CHEST_SCREEN_HANDLER, syncId);
        checkSize(inventory, 162);
        this.masterInventory = inventory;
        this.propertyDelegate = propertyDelegate;

        masterInventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Populate initial visible slots from master inventory
        this.updateVisibleSlots();

        // Listen for any changes on visible slots to save back to master inventory
        this.visibleInventory.addListener(inv -> {
            if (!this.isUpdating) {
                this.saveVisibleSlots();
            }
        });

        // 54 Visible Chest Slots (6 rows x 9 columns: slots 0..53)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                final int slotIndex = col + row * 9;
                this.addSlot(new Slot(this.visibleInventory, slotIndex, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean isEnabled() {
                        return (scrollRow * 9 + slotIndex) < getMaxSlots();
                    }

                    @Override
                    public boolean canInsert(ItemStack stack) {
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
                this.visibleInventory.setStack(i, this.masterInventory.getStack(realIndex).copy());
            } else {
                this.visibleInventory.setStack(i, ItemStack.EMPTY);
            }
        }
        this.isUpdating = false;
    }

    public void saveVisibleSlots() {
        for (int i = 0; i < 54; i++) {
            int realIndex = this.scrollRow * 9 + i;
            if (realIndex < getMaxSlots()) {
                this.masterInventory.setStack(realIndex, this.visibleInventory.getStack(i).copy());
            }
        }
        this.masterInventory.markDirty();
    }

    public void setScrollRow(int newScrollRow) {
        saveVisibleSlots();
        this.scrollRow = Math.max(0, Math.min(newScrollRow, getMaxScrollRows()));
        this.propertyDelegate.set(1, this.scrollRow);
        updateVisibleSlots();
        this.sendContentUpdates();
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
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == 2) { // Auto-Sort
            if (masterInventory instanceof EnchantedChestBlockEntity chestEntity) {
                saveVisibleSlots();
                chestEntity.sortInventory();
                updateVisibleSlots();
                this.sendContentUpdates();
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
    public void onClosed(PlayerEntity player) {
        saveVisibleSlots();
        super.onClosed(player);
        this.masterInventory.onClose(player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.masterInventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < 54) {
                // Moving from visible chest slot to player inventory (slots 54..89)
                if (!this.insertItem(originalStack, 54, 90, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory into the entire master chest
                saveVisibleSlots();
                boolean inserted = false;
                for (int i = 0; i < getMaxSlots(); i++) {
                    ItemStack target = masterInventory.getStack(i);
                    if (target.isEmpty()) {
                        masterInventory.setStack(i, originalStack.copy());
                        originalStack.setCount(0);
                        inserted = true;
                        break;
                    } else if (ItemStack.areItemsAndComponentsEqual(target, originalStack) && target.getCount() < target.getMaxCount()) {
                        int transfer = Math.min(originalStack.getCount(), target.getMaxCount() - target.getCount());
                        target.increment(transfer);
                        originalStack.decrement(transfer);
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
                this.sendContentUpdates();
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
