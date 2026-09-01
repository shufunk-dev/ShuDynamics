package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.DigitalConverterBlock;
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class DigitalConverterBlockEntity extends BlockEntity implements SidedInventory {
    public static final int BUFFER_SIZE = 4;
    private final DefaultedList<ItemStack> buffer = DefaultedList.ofSize(BUFFER_SIZE, ItemStack.EMPTY);
    private int checkTimer = 0;

    public DigitalConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_CONVERTER_BLOCK_ENTITY, pos, state);
    }

    public @Nullable EnchantedStorageTerminalBlockEntity getNetworkTerminal() {
        if (this.world == null) return null;
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageTerminalBlockEntity terminal) {
                        return terminal;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable EnchantedStorageControllerBlockEntity getNetworkController() {
        if (this.world == null) return null;
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageControllerBlockEntity controller) {
                        return controller;
                    }
                }
            }
        }
        return null;
    }

    public boolean isNetworkOnline() {
        if (this.world == null) return false;
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        if (terminal == null) return false;
        return terminal.isNetworkOnline();
    }

    public int getNetworkStoredCount() {
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        return terminal != null ? terminal.getStoredItemCount() : 0;
    }

    public int getNetworkCapacity() {
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        return terminal != null ? terminal.getNetworkCapacity() : 0;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, DigitalConverterBlockEntity entity) {
        boolean dirty = false;
        boolean wasLit = state.get(DigitalConverterBlock.LIT);
        boolean isOnline = entity.isNetworkOnline();

        if (wasLit != isOnline) {
            world.setBlockState(pos, state.with(DigitalConverterBlock.LIT, isOnline), 3);
        }

        if (isOnline) {
            EnchantedStorageTerminalBlockEntity terminal = entity.getNetworkTerminal();
            if (terminal != null) {
                int availableCapacity = terminal.getNetworkCapacity() - terminal.getStoredItemCount();
                if (availableCapacity > 0) {
                    for (int i = 0; i < BUFFER_SIZE; i++) {
                        ItemStack bufferStack = entity.buffer.get(i);
                        if (bufferStack.isEmpty()) continue;

                        int toDigitize = Math.min(bufferStack.getCount(), availableCapacity);
                        if (toDigitize <= 0) break;

                        // Insert into terminal slots (540 slots)
                        for (int slot = 0; slot < EnchantedStorageTerminalBlockEntity.STORAGE_SLOTS; slot++) {
                            ItemStack termStack = terminal.getStack(slot);
                            if (!termStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(termStack, bufferStack)) {
                                int space = termStack.getMaxCount() - termStack.getCount();
                                if (space > 0) {
                                    int move = Math.min(space, toDigitize);
                                    termStack.increment(move);
                                    bufferStack.decrement(move);
                                    toDigitize -= move;
                                    availableCapacity -= move;
                                    terminal.markDirty();
                                    dirty = true;
                                    if (bufferStack.isEmpty()) {
                                        entity.buffer.set(i, ItemStack.EMPTY);
                                        break;
                                    }
                                    if (toDigitize <= 0) break;
                                }
                            }
                        }

                        if (!bufferStack.isEmpty() && toDigitize > 0) {
                            for (int slot = 0; slot < EnchantedStorageTerminalBlockEntity.STORAGE_SLOTS; slot++) {
                                ItemStack termStack = terminal.getStack(slot);
                                if (termStack.isEmpty()) {
                                    int move = Math.min(bufferStack.getMaxCount(), toDigitize);
                                    ItemStack split = bufferStack.split(move);
                                    terminal.setStack(slot, split);
                                    toDigitize -= move;
                                    availableCapacity -= move;
                                    terminal.markDirty();
                                    dirty = true;
                                    if (bufferStack.isEmpty()) {
                                        entity.buffer.set(i, ItemStack.EMPTY);
                                        break;
                                    }
                                    if (toDigitize <= 0) break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    @Override
    public int size() {
        return BUFFER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.buffer) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.buffer.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.buffer, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(this.buffer, slot);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.buffer.set(slot, stack);
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        this.buffer.clear();
        markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        if (terminal == null || !terminal.isNetworkOnline()) return false;
        return terminal.getStoredItemCount() + stack.getCount() <= terminal.getNetworkCapacity();
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.buffer.clear();
        Inventories.readData(view, this.buffer);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.buffer);
    }
}
