package net.enchantedwood.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.enchantedwood.screen.EnchantedStorageTerminalScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnchantedStorageTerminalBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
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
                ItemStack.CODEC.fieldOf("item").forGetter(StoredRecord::sample),
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

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
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
        if (this.level == null) return 0;
        long totalCap = 0;
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockEntity be = this.level.getBlockEntity(mut);
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
        if (this.level == null) return true;
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockState bs = this.level.getBlockState(mut);
                    if (bs.getBlock() instanceof EnchantedStorageControllerBlock) {
                        return bs.getValue(EnchantedStorageControllerBlock.LIT);
                    }
                }
            }
        }
        return true;
    }

    private void addLoadedItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (StoredItem item : this.storedItems) {
            if (ItemStack.isSameItemSameComponents(item.getSample(), stack)) {
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
            if (ItemStack.isSameItemSameComponents(item.getSample(), stack)) {
                item.add(toDeposit);
                stack.shrink(toDeposit);
                setChanged();
                return stack;
            }
        }

        this.storedItems.add(new StoredItem(stack, toDeposit));
        stack.shrink(toDeposit);
        setChanged();
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
            if (ItemStack.isSameItemSameComponents(item.getSample(), sample)) {
                int limit = Math.min(maxAmount, sample.getMaxStackSize());
                int take = (int) Math.min((long) limit, item.getCount());
                item.remove(take);
                if (item.getCount() <= 0) {
                    this.storedItems.remove(i);
                }
                setChanged();
                return sample.copyWithCount(take);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.enchanted_storage_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new EnchantedStorageTerminalScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.storedItems.clear();

        // 1. Modern consolidated digital storage format
        var consolidated = view.list("ConsolidatedItems", StoredRecord.CODEC);
        if (consolidated.isPresent() && !consolidated.get().isEmpty()) {
            for (StoredRecord record : consolidated.get()) {
                if (record.count() > 0 && !record.sample().isEmpty()) {
                    this.storedItems.add(new StoredItem(record.sample(), record.count()));
                }
            }
            return;
        }

        // 2. Backward compatibility: Read older TerminalItems format
        var terminalItems = view.list("TerminalItems", TerminalStack.CODEC);
        if (terminalItems.isPresent() && !terminalItems.get().isEmpty()) {
            for (TerminalStack entry : terminalItems.get()) {
                if (!entry.stack().isEmpty()) {
                    addLoadedItem(entry.stack());
                }
            }
            return;
        }

        // 3. Fallback: Standard vanilla "Items" list format (Inventories.readData)
        NonNullList<ItemStack> legacy = NonNullList.withSize(540, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(view, legacy);
        for (ItemStack s : legacy) {
            if (!s.isEmpty()) {
                addLoadedItem(s);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        var appender = view.list("ConsolidatedItems", StoredRecord.CODEC);
        for (StoredItem item : this.storedItems) {
            if (item.getCount() > 0 && !item.getSample().isEmpty()) {
                appender.add(new StoredRecord(item.getSample(), item.getCount()));
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int count = Math.min(STORAGE_SLOTS, this.storedItems.size() + 1);
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return isNetworkOnline() && getTotalStoredItemCountLong() + stack.getCount() <= getNetworkCapacityLong();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return isNetworkOnline();
    }

    @Override
    public int getContainerSize() {
        return STORAGE_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.storedItems.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < this.storedItems.size()) {
            return this.storedItems.get(slot).toItemStack();
        }
        return ItemStack.EMPTY;
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
    public ItemStack removeItem(int slot, int amount) {
        if (slot >= 0 && slot < this.storedItems.size()) {
            StoredItem item = this.storedItems.get(slot);
            int take = (int) Math.min((long) amount, Math.min((long) item.getSample().getMaxStackSize(), item.getCount()));
            item.remove(take);
            ItemStack result = item.getSample().copyWithCount(take);
            if (item.getCount() <= 0) {
                this.storedItems.remove(slot);
            }
            setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < this.storedItems.size()) {
            StoredItem item = this.storedItems.remove(slot);
            setChanged();
            return item.toItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            if (slot >= 0 && slot < this.storedItems.size()) {
                this.storedItems.remove(slot);
                setChanged();
            }
        } else {
            depositItem(stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player) && isNetworkOnline();
    }

    @Override
    public void clearContent() {
        this.storedItems.clear();
        setChanged();
    }
}
