package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.enchantedwood.screen.EnchantedStorageTerminalScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnchantedStorageTerminalBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory {
    public static final int PAGE_SIZE = 54;
    public static final int TOTAL_PAGES = 50; // Dynamic capacity for up to 2,700 unique items
    public static final int STORAGE_SLOTS = TOTAL_PAGES * PAGE_SIZE;

    public static class StoredItem {
        private final ItemStack sample;
        private long count;

        public StoredItem(ItemStack sample, long count) {
            this.sample = sample.copyWithCount(1);
            this.count = count;
        }

        public ItemStack getSample() {
            return this.sample;
        }

        public long getCount() {
            return this.count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public void add(long amount) {
            this.count += amount;
        }

        public long remove(long amount) {
            long taken = Math.min(this.count, amount);
            this.count -= taken;
            return taken;
        }

        public ItemStack toItemStack() {
            return this.sample.copyWithCount((int) Math.min(this.count, (long) Integer.MAX_VALUE));
        }
    }

    public record StoredRecord(ItemStack sample, long count) {
        public static final Codec<StoredRecord> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ItemStack.UNCOUNTED_CODEC.fieldOf("item").forGetter(StoredRecord::sample),
                Codec.LONG.fieldOf("count").forGetter(StoredRecord::count)
            ).apply(instance, StoredRecord::new)
        );
    }

    public record TerminalStack(int slot, ItemStack stack) {
        public static final Codec<TerminalStack> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.fieldOf("Slot").forGetter(TerminalStack::slot),
                ItemStack.MAP_CODEC.forGetter(TerminalStack::stack)
            ).apply(instance, TerminalStack::new)
        );
    }

    private final List<StoredItem> storedItems = new ArrayList<>();

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getNetworkCapacity();
                case 1 -> getStoredItemCount();
                case 2 -> isNetworkOnline() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int size() {
            return 3;
        }
    };

    public EnchantedStorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_STORAGE_TERMINAL_BLOCK_ENTITY, pos, state);
    }

    public List<StoredItem> getStoredItems() {
        return this.storedItems;
    }

    public int getStoredItemCount() {
        long total = 0;
        for (StoredItem item : this.storedItems) {
            total += item.getCount();
        }
        return (int) Math.min(total, (long) Integer.MAX_VALUE);
    }

    public long getTotalStoredItemCountLong() {
        long total = 0;
        for (StoredItem item : this.storedItems) {
            total += item.getCount();
        }
        return total;
    }

    public long getNetworkCapacityLong() {
        if (this.world == null) return 0;
        long totalCap = 0;
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
                    if (be instanceof EnchantedDriveBayBlockEntity driveBay) {
                        totalCap += driveBay.getTotalNetworkCapacity();
                    }
                }
            }
        }
        return totalCap;
    }

    public int getNetworkCapacity() {
        return (int) Math.min(getNetworkCapacityLong(), (long) Integer.MAX_VALUE);
    }

    public boolean isNetworkOnline() {
        if (this.world == null) return true;
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockState bs = this.world.getBlockState(mut);
                    if (bs.getBlock() instanceof EnchantedStorageControllerBlock) {
                        return bs.get(EnchantedStorageControllerBlock.LIT);
                    }
                }
            }
        }
        return true;
    }

    private void addLoadedItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (StoredItem item : this.storedItems) {
            if (ItemStack.areItemsAndComponentsEqual(item.getSample(), stack)) {
                item.add(stack.getCount());
                return;
            }
        }
        this.storedItems.add(new StoredItem(stack, stack.getCount()));
    }

    /**
     * Deposits an ItemStack into the digital network.
     * Consolidates duplicate items into existing StoredItems.
     * @return Any remainder that could not fit due to network capacity.
     */
    public ItemStack depositItem(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (!isNetworkOnline()) return stack;

        long cap = getNetworkCapacityLong();
        long current = getTotalStoredItemCountLong();
        long available = cap - current;
        if (available <= 0) return stack;

        int toDeposit = (int) Math.min((long) stack.getCount(), available);
        if (toDeposit <= 0) return stack;

        for (StoredItem item : this.storedItems) {
            if (ItemStack.areItemsAndComponentsEqual(item.getSample(), stack)) {
                item.add(toDeposit);
                stack.decrement(toDeposit);
                markDirty();
                return stack;
            }
        }

        this.storedItems.add(new StoredItem(stack, toDeposit));
        stack.decrement(toDeposit);
        markDirty();
        return stack;
    }

    /**
     * Extracts an item from the digital network matching the sample.
     * @param sample The sample ItemStack to match.
     * @param maxAmount The maximum count to extract (capped by sample max stack count).
     * @return Extracted ItemStack, or EMPTY if not found or network offline.
     */
    public ItemStack extractItem(ItemStack sample, int maxAmount) {
        if (sample.isEmpty() || maxAmount <= 0) return ItemStack.EMPTY;
        if (!isNetworkOnline()) return ItemStack.EMPTY;

        for (int i = 0; i < this.storedItems.size(); i++) {
            StoredItem item = this.storedItems.get(i);
            if (ItemStack.areItemsAndComponentsEqual(item.getSample(), sample)) {
                int limit = Math.min(maxAmount, sample.getMaxCount());
                int take = (int) Math.min((long) limit, item.getCount());
                item.remove(take);
                if (item.getCount() <= 0) {
                    this.storedItems.remove(i);
                }
                markDirty();
                return sample.copyWithCount(take);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.enchanted_storage_terminal");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnchantedStorageTerminalScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.storedItems.clear();

        // 1. Modern consolidated digital storage format
        var consolidated = view.getOptionalTypedListView("ConsolidatedItems", StoredRecord.CODEC);
        if (consolidated.isPresent() && !consolidated.get().isEmpty()) {
            for (StoredRecord record : consolidated.get()) {
                if (record.count() > 0 && !record.sample().isEmpty()) {
                    this.storedItems.add(new StoredItem(record.sample(), record.count()));
                }
            }
            return;
        }

        // 2. Backward compatibility: Read older TerminalItems format
        var terminalItems = view.getOptionalTypedListView("TerminalItems", TerminalStack.CODEC);
        if (terminalItems.isPresent() && !terminalItems.get().isEmpty()) {
            for (TerminalStack entry : terminalItems.get()) {
                if (!entry.stack().isEmpty()) {
                    addLoadedItem(entry.stack());
                }
            }
            return;
        }

        // 3. Fallback: Standard vanilla "Items" list format (Inventories.readData)
        DefaultedList<ItemStack> legacy = DefaultedList.ofSize(540, ItemStack.EMPTY);
        Inventories.readData(view, legacy);
        for (ItemStack s : legacy) {
            if (!s.isEmpty()) {
                addLoadedItem(s);
            }
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        var appender = view.getListAppender("ConsolidatedItems", StoredRecord.CODEC);
        for (StoredItem item : this.storedItems) {
            if (item.getCount() > 0 && !item.getSample().isEmpty()) {
                appender.add(new StoredRecord(item.getSample(), item.getCount()));
            }
        }

        // Also write legacy TerminalItems for full compatibility
        var termAppender = view.getListAppender("TerminalItems", TerminalStack.CODEC);
        for (int i = 0; i < this.storedItems.size(); i++) {
            StoredItem item = this.storedItems.get(i);
            if (item.getCount() > 0 && !item.getSample().isEmpty()) {
                termAppender.add(new TerminalStack(i, item.toItemStack()));
            }
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        int count = Math.min(STORAGE_SLOTS, this.storedItems.size() + 1);
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isNetworkOnline() && getTotalStoredItemCountLong() + stack.getCount() <= getNetworkCapacityLong();
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return isNetworkOnline();
    }

    @Override
    public int size() {
        return STORAGE_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.storedItems.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < this.storedItems.size()) {
            return this.storedItems.get(slot).toItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getMaxCountPerStack() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxCount(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot >= 0 && slot < this.storedItems.size()) {
            StoredItem item = this.storedItems.get(slot);
            int take = (int) Math.min((long) amount, Math.min((long) item.getSample().getMaxCount(), item.getCount()));
            item.remove(take);
            ItemStack result = item.getSample().copyWithCount(take);
            if (item.getCount() <= 0) {
                this.storedItems.remove(slot);
            }
            markDirty();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot >= 0 && slot < this.storedItems.size()) {
            StoredItem item = this.storedItems.remove(slot);
            markDirty();
            return item.toItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            if (slot >= 0 && slot < this.storedItems.size()) {
                this.storedItems.remove(slot);
                markDirty();
            }
        } else {
            depositItem(stack);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player) && isNetworkOnline();
    }

    @Override
    public void clear() {
        this.storedItems.clear();
        markDirty();
    }
}
