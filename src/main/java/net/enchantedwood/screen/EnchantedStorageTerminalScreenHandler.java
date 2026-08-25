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
import net.enchantedwood.block.entity.EnchantedStorageTerminalBlockEntity;

public class EnchantedStorageTerminalScreenHandler extends ScreenHandler {
    public static final int PAGE_SIZE = 54;
    public static final int TOTAL_PAGES = EnchantedStorageTerminalBlockEntity.TOTAL_PAGES;
    public static final int TOTAL_STORAGE_SLOTS = EnchantedStorageTerminalBlockEntity.STORAGE_SLOTS;

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    private int currentPage = 0;

    public EnchantedStorageTerminalScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(TOTAL_STORAGE_SLOTS), new ArrayPropertyDelegate(4));
    }

    public EnchantedStorageTerminalScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_STORAGE_TERMINAL_SCREEN_HANDLER, syncId);
        checkSize(inventory, TOTAL_STORAGE_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // 54 Dynamic Network Storage Slots (6 rows x 9 columns)
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                int displayIndex = col + row * 9;
                this.addSlot(new TerminalSlot(inventory, displayIndex, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player Hotbar (1 row x 9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public void setCurrentPage(int page) {
        if (page >= 0 && page < TOTAL_PAGES) {
            this.currentPage = page;
            this.sendContentUpdates();
        }
    }

    public int getTotalPages() {
        return TOTAL_PAGES;
    }

    public int getTotalCapacity() {
        return this.propertyDelegate.get(0);
    }

    public int getStoredCount() {
        return this.propertyDelegate.get(1);
    }

    public boolean isOnline() {
        return this.propertyDelegate.get(2) > 0;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == 0) {
            // Previous page
            if (this.currentPage > 0) {
                this.currentPage--;
                this.sendContentUpdates();
                return true;
            }
        } else if (id == 1) {
            // Next page
            if (this.currentPage < TOTAL_PAGES - 1) {
                this.currentPage++;
                this.sendContentUpdates();
                return true;
            }
        } else if (id >= 10 && id < 10 + TOTAL_PAGES) {
            this.currentPage = id - 10;
            this.sendContentUpdates();
            return true;
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return isOnline();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < PAGE_SIZE) {
                // Move from Terminal to Player Inventory (slots 54..89)
                if (!this.insertItem(originalStack, PAGE_SIZE, PAGE_SIZE + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from Player Inventory to Terminal
                if (!isOnline() || getTotalCapacity() <= 0 || getStoredCount() >= getTotalCapacity()) {
                    return ItemStack.EMPTY;
                }

                // Check remaining capacity in network
                int availableRoom = getTotalCapacity() - getStoredCount();
                if (availableRoom <= 0) {
                    return ItemStack.EMPTY;
                }

                // Insert into all 540 terminal slots (merging existing stacks first)
                boolean inserted = false;
                
                // 1. Try merging with identical stacks across all 540 slots
                for (int i = 0; i < TOTAL_STORAGE_SLOTS; i++) {
                    ItemStack target = this.inventory.getStack(i);
                    if (!target.isEmpty() && ItemStack.areItemsAndComponentsEqual(target, originalStack)) {
                        int maxCount = Math.min(target.getMaxCount(), this.inventory.getMaxCountPerStack());
                        int canAdd = Math.min(maxCount - target.getCount(), originalStack.getCount());
                        canAdd = Math.min(canAdd, availableRoom);
                        if (canAdd > 0) {
                            target.increment(canAdd);
                            originalStack.decrement(canAdd);
                            availableRoom -= canAdd;
                            this.inventory.markDirty();
                            inserted = true;
                            if (originalStack.isEmpty() || availableRoom <= 0) break;
                        }
                    }
                }

                // 2. If remaining, put into first empty slot
                if (!originalStack.isEmpty() && availableRoom > 0) {
                    for (int i = 0; i < TOTAL_STORAGE_SLOTS; i++) {
                        ItemStack target = this.inventory.getStack(i);
                        if (target.isEmpty()) {
                            int toMove = Math.min(originalStack.getCount(), availableRoom);
                            toMove = Math.min(toMove, this.inventory.getMaxCountPerStack());
                            ItemStack split = originalStack.split(toMove);
                            this.inventory.setStack(i, split);
                            availableRoom -= toMove;
                            inserted = true;
                            if (originalStack.isEmpty() || availableRoom <= 0) break;
                        }
                    }
                }

                if (!inserted) {
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

    public class TerminalSlot extends Slot {
        private final int displayIndex;

        public TerminalSlot(Inventory inventory, int displayIndex, int x, int y) {
            super(inventory, displayIndex, x, y);
            this.displayIndex = displayIndex;
        }

        @Override
        public int getIndex() {
            return this.displayIndex + (currentPage * PAGE_SIZE);
        }

        @Override
        public ItemStack getStack() {
            int actualIndex = getIndex();
            if (actualIndex >= 0 && actualIndex < inventory.size()) {
                return inventory.getStack(actualIndex);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setStack(ItemStack stack) {
            int actualIndex = getIndex();
            if (actualIndex >= 0 && actualIndex < inventory.size()) {
                inventory.setStack(actualIndex, stack);
                markDirty();
            }
        }

        @Override
        public ItemStack takeStack(int amount) {
            int actualIndex = getIndex();
            if (actualIndex >= 0 && actualIndex < inventory.size()) {
                return inventory.removeStack(actualIndex, amount);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (!isOnline() || getTotalCapacity() <= 0) {
                return false;
            }
            return getStoredCount() + stack.getCount() <= getTotalCapacity();
        }
    }
}
