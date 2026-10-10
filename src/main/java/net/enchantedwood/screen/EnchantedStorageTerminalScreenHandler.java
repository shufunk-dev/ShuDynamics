package net.enchantedwood.screen;

import net.enchantedwood.block.entity.EnchantedStorageTerminalBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class EnchantedStorageTerminalScreenHandler extends AbstractContainerMenu {
    public static final int PAGE_SIZE = EnchantedStorageTerminalBlockEntity.PAGE_SIZE;
    public static final int TOTAL_STORAGE_SLOTS = EnchantedStorageTerminalBlockEntity.STORAGE_SLOTS;

    public static class UncappedInventory extends SimpleContainer {
        public UncappedInventory(int size) {
            super(size);
        }

        @Override
        public int getMaxStackSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Integer.MAX_VALUE;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.getItems().set(slot, stack);
            this.setChanged();
        }
    }

    private final Container inventory;
    private final UncappedInventory displayInventory = new UncappedInventory(PAGE_SIZE);
    private final ContainerData propertyDelegate;
    private final DataSlot pageProperty = DataSlot.standalone();
    private final DataSlot totalPagesProperty = DataSlot.standalone();

    private int currentPage = 0;
    private String searchQuery = "";
    private final List<Integer> filteredIndices = new ArrayList<>();

    public EnchantedStorageTerminalScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new UncappedInventory(TOTAL_STORAGE_SLOTS), new SimpleContainerData(4));
    }

    public EnchantedStorageTerminalScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_STORAGE_TERMINAL_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, TOTAL_STORAGE_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);
        this.addDataSlot(this.pageProperty);
        this.addDataSlot(this.totalPagesProperty);

        updateFilteredIndices();

        // 54 Dynamic Network Storage Slots (6 rows x 9 columns) backed by displayInventory
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                int displayIndex = col + row * 9;
                this.addSlot(new TerminalSlot(this.displayInventory, displayIndex, 8 + col * 18, 18 + row * 18));
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

        this.pageProperty.set(0);
        this.totalPagesProperty.set(getTotalPages());

        if (inventory instanceof EnchantedStorageTerminalBlockEntity) {
            updateDisplaySlots();
        }
    }

    public void setSearchFilter(String query) {
        this.searchQuery = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        this.currentPage = 0;
        updateFilteredIndices();
        updateDisplaySlots();
        this.broadcastChanges();
    }

    public String getSearchQuery() {
        return this.searchQuery;
    }

    public void updateFilteredIndices() {
        this.filteredIndices.clear();
        if (this.inventory instanceof EnchantedStorageTerminalBlockEntity terminal) {
            List<EnchantedStorageTerminalBlockEntity.StoredItem> items = terminal.getStoredItems();
            for (int i = 0; i < items.size(); i++) {
                EnchantedStorageTerminalBlockEntity.StoredItem item = items.get(i);
                if (item.getCount() > 0 && !item.getSample().isEmpty()) {
                    if (this.searchQuery.isEmpty()) {
                        this.filteredIndices.add(i);
                    } else {
                        ItemStack stack = item.getSample();
                        String name = stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
                        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(java.util.Locale.ROOT);
                        if (name.contains(this.searchQuery) || path.contains(this.searchQuery)) {
                            this.filteredIndices.add(i);
                        }
                    }
                }
            }
        }
        if (this.currentPage >= getTotalPages()) {
            this.currentPage = Math.max(0, getTotalPages() - 1);
        }
    }

    public void updateDisplaySlots() {
        if (!(this.inventory instanceof EnchantedStorageTerminalBlockEntity terminal)) {
            return;
        }
        for (int i = 0; i < PAGE_SIZE; i++) {
            int targetIndex = this.currentPage * PAGE_SIZE + i;
            if (targetIndex >= 0 && targetIndex < this.filteredIndices.size()) {
                int itemIndex = this.filteredIndices.get(targetIndex);
                if (itemIndex >= 0 && itemIndex < terminal.getStoredItems().size()) {
                    EnchantedStorageTerminalBlockEntity.StoredItem item = terminal.getStoredItems().get(itemIndex);
                    this.displayInventory.setItem(i, item.toItemStack());
                    continue;
                }
            }
            this.displayInventory.setItem(i, ItemStack.EMPTY);
        }
    }

    public int getCurrentPage() {
        return this.pageProperty.get();
    }

    public void setCurrentPage(int page) {
        if (page >= 0 && page < getTotalPages()) {
            this.currentPage = page;
            updateDisplaySlots();
            this.broadcastChanges();
        }
    }

    public int getTotalPages() {
        if (this.inventory instanceof EnchantedStorageTerminalBlockEntity) {
            int count = this.filteredIndices.size();
            return Math.max(1, (int) Math.ceil((double) count / PAGE_SIZE));
        }
        return Math.max(1, this.totalPagesProperty.get());
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
    public void broadcastChanges() {
        this.pageProperty.set(this.currentPage);
        if (this.inventory instanceof EnchantedStorageTerminalBlockEntity) {
            int count = this.filteredIndices.size();
            this.totalPagesProperty.set(Math.max(1, (int) Math.ceil((double) count / PAGE_SIZE)));
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Previous page
            if (this.currentPage > 0) {
                this.currentPage--;
                updateDisplaySlots();
                this.broadcastChanges();
                return true;
            }
        } else if (id == 1) {
            // Next page
            if (this.currentPage < getTotalPages() - 1) {
                this.currentPage++;
                updateDisplaySlots();
                this.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        if (player.level().isClientSide()) {
            super.clicked(slotIndex, button, actionType, player);
            return;
        }

        if (!(this.inventory instanceof EnchantedStorageTerminalBlockEntity terminal)) {
            super.clicked(slotIndex, button, actionType, player);
            return;
        }

        // 1. Interacting with Terminal Display Slots (0..53)
        if (slotIndex >= 0 && slotIndex < PAGE_SIZE) {
            int targetIndex = this.currentPage * PAGE_SIZE + slotIndex;
            ItemStack cursor = getCarried();

            if (actionType == ContainerInput.PICKUP) {
                if (cursor.isEmpty()) {
                    if (targetIndex >= 0 && targetIndex < this.filteredIndices.size()) {
                        int itemIndex = this.filteredIndices.get(targetIndex);
                        if (itemIndex >= 0 && itemIndex < terminal.getStoredItems().size()) {
                            EnchantedStorageTerminalBlockEntity.StoredItem stored = terminal.getStoredItems().get(itemIndex);
                            if (stored.getCount() > 0) {
                                int maxExtract = stored.getSample().getMaxStackSize();
                                int take = (button == 0)
                                        ? (int) Math.min((long) maxExtract, stored.getCount())
                                        : Math.max(1, (int) Math.min((long) maxExtract, stored.getCount()) / 2);

                                ItemStack extracted = terminal.extractItem(stored.getSample(), take);
                                setCarried(extracted);
                            }
                        }
                    }
                } else {
                    // Deposit from cursor into terminal
                    if (button == 0) {
                        ItemStack remainder = terminal.depositItem(cursor);
                        setCarried(remainder);
                    } else if (button == 1) {
                        ItemStack one = cursor.copyWithCount(1);
                        ItemStack remainder = terminal.depositItem(one);
                        if (remainder.isEmpty()) {
                            cursor.shrink(1);
                            setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                        }
                    }
                }
            } else if (actionType == ContainerInput.QUICK_MOVE) {
                // Shift-Click from terminal to player inventory
                if (targetIndex >= 0 && targetIndex < this.filteredIndices.size()) {
                    int itemIndex = this.filteredIndices.get(targetIndex);
                    if (itemIndex >= 0 && itemIndex < terminal.getStoredItems().size()) {
                        EnchantedStorageTerminalBlockEntity.StoredItem stored = terminal.getStoredItems().get(itemIndex);
                        if (stored.getCount() > 0) {
                            ItemStack sample = stored.getSample();
                            int maxStack = sample.getMaxStackSize();

                            // Count how much room the player has
                            int room = 0;
                            for (int i = PAGE_SIZE; i < PAGE_SIZE + 36; i++) {
                                ItemStack pStack = this.slots.get(i).getItem();
                                if (pStack.isEmpty()) {
                                    room += maxStack;
                                } else if (ItemStack.isSameItemSameComponents(pStack, sample)) {
                                    room += Math.max(0, maxStack - pStack.getCount());
                                }
                            }

                            if (room > 0) {
                                int toExtract = (int) Math.min((long) room, stored.getCount());
                                ItemStack extracted = terminal.extractItem(sample, toExtract);
                                if (!extracted.isEmpty()) {
                                    if (!this.moveItemStackTo(extracted, PAGE_SIZE, PAGE_SIZE + 36, true)) {
                                        if (!extracted.isEmpty()) {
                                            terminal.depositItem(extracted);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            updateFilteredIndices();
            updateDisplaySlots();
            this.broadcastChanges();
            return;
        }

        // 2. Shift-Clicking from Player Inventory to Terminal (slotIndex >= PAGE_SIZE)
        if (actionType == ContainerInput.QUICK_MOVE && slotIndex >= PAGE_SIZE) {
            Slot pSlot = this.slots.get(slotIndex);
            ItemStack pStack = pSlot.getItem();
            if (!pStack.isEmpty()) {
                ItemStack remainder = terminal.depositItem(pStack);
                pSlot.setByPlayer(remainder);
                pSlot.setChanged();
                updateFilteredIndices();
                updateDisplaySlots();
                this.broadcastChanges();
                return;
            }
        }

        // Standard player inventory actions
        super.clicked(slotIndex, button, actionType, player);
        updateFilteredIndices();
        updateDisplaySlots();
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        // Handled in onSlotClick
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return isOnline();
    }

    public static class TerminalSlot extends Slot {
        public TerminalSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Integer.MAX_VALUE;
        }
    }
}
