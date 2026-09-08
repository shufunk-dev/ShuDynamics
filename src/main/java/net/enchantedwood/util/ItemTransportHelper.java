package net.enchantedwood.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ItemTransportHelper {
    private ItemTransportHelper() {}

    public static @Nullable Inventory getInventoryAt(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof InventoryProvider provider) {
            return provider.getInventory(state, world, pos);
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity && block instanceof ChestBlock chestBlock) {
            Inventory inv = ChestBlock.getInventory(chestBlock, state, world, pos, true);
            if (inv != null) return inv;
        }
        if (be instanceof Inventory inventory) {
            return inventory;
        }
        return null;
    }

    public static ItemStack insertItem(Inventory inv, ItemStack stack, @Nullable Direction side) {
        if (stack.isEmpty() || inv == null) return stack;
        ItemStack toInsert = stack.copy();

        if (inv instanceof SidedInventory sidedInv && side != null) {
            int[] slots = sidedInv.getAvailableSlots(side);
            // 1. Try to merge into matching non-empty slots first
            for (int slot : slots) {
                if (!sidedInv.canInsert(slot, toInsert, side)) continue;
                ItemStack current = inv.getStack(slot);
                if (!current.isEmpty() && ItemStack.areItemsAndComponentsEqual(current, toInsert)) {
                    int max = Math.min(inv.getMaxCountPerStack(), current.getMaxCount());
                    int space = max - current.getCount();
                    if (space > 0) {
                        int move = Math.min(space, toInsert.getCount());
                        current.increment(move);
                        toInsert.decrement(move);
                        inv.markDirty();
                        if (toInsert.isEmpty()) return ItemStack.EMPTY;
                    }
                }
            }
            // 2. Try to place into empty slots
            for (int slot : slots) {
                if (!sidedInv.canInsert(slot, toInsert, side)) continue;
                ItemStack current = inv.getStack(slot);
                if (current.isEmpty()) {
                    int max = Math.min(inv.getMaxCountPerStack(), toInsert.getMaxCount());
                    int move = Math.min(max, toInsert.getCount());
                    ItemStack split = toInsert.split(move);
                    inv.setStack(slot, split);
                    inv.markDirty();
                    if (toInsert.isEmpty()) return ItemStack.EMPTY;
                }
            }
        } else {
            // 1. Try to merge into matching non-empty slots
            for (int i = 0; i < inv.size(); i++) {
                ItemStack current = inv.getStack(i);
                if (!current.isEmpty() && ItemStack.areItemsAndComponentsEqual(current, toInsert)) {
                    int max = Math.min(inv.getMaxCountPerStack(), current.getMaxCount());
                    int space = max - current.getCount();
                    if (space > 0) {
                        int move = Math.min(space, toInsert.getCount());
                        current.increment(move);
                        toInsert.decrement(move);
                        inv.markDirty();
                        if (toInsert.isEmpty()) return ItemStack.EMPTY;
                    }
                }
            }
            // 2. Try empty slots
            for (int i = 0; i < inv.size(); i++) {
                ItemStack current = inv.getStack(i);
                if (current.isEmpty()) {
                    int max = Math.min(inv.getMaxCountPerStack(), toInsert.getMaxCount());
                    int move = Math.min(max, toInsert.getCount());
                    ItemStack split = toInsert.split(move);
                    inv.setStack(i, split);
                    inv.markDirty();
                    if (toInsert.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        return toInsert;
    }

    public static class ExtractResult {
        public final ItemStack stack;
        public final int nextSlotIndex;

        public ExtractResult(ItemStack stack, int nextSlotIndex) {
            this.stack = stack;
            this.nextSlotIndex = nextSlotIndex;
        }
    }

    public static ItemStack extractItem(Inventory inv, @Nullable Direction side, int maxCount) {
        return extractItemRoundRobin(inv, side, maxCount, 0).stack;
    }

    public static ExtractResult extractItemRoundRobin(Inventory inv, @Nullable Direction side, int maxCount, int startIndex) {
        if (inv == null || maxCount <= 0) return new ExtractResult(ItemStack.EMPTY, startIndex);

        if (inv instanceof SidedInventory sidedInv && side != null) {
            int[] slots = sidedInv.getAvailableSlots(side);
            if (slots.length == 0) return new ExtractResult(ItemStack.EMPTY, 0);

            for (int i = 0; i < slots.length; i++) {
                int slotIdx = (startIndex + i) % slots.length;
                int slot = slots[slotIdx];
                ItemStack current = inv.getStack(slot);
                if (!current.isEmpty() && sidedInv.canExtract(slot, current, side)) {
                    int count = Math.min(maxCount, current.getCount());
                    ItemStack extracted = current.split(count);
                    if (current.isEmpty()) {
                        inv.setStack(slot, ItemStack.EMPTY);
                    }
                    inv.markDirty();
                    return new ExtractResult(extracted, (slotIdx + 1) % slots.length);
                }
            }
            return new ExtractResult(ItemStack.EMPTY, startIndex);
        } else {
            int size = inv.size();
            if (size == 0) return new ExtractResult(ItemStack.EMPTY, 0);

            for (int i = 0; i < size; i++) {
                int slot = (startIndex + i) % size;
                ItemStack current = inv.getStack(slot);
                if (!current.isEmpty()) {
                    int count = Math.min(maxCount, current.getCount());
                    ItemStack extracted = current.split(count);
                    if (current.isEmpty()) {
                        inv.setStack(slot, ItemStack.EMPTY);
                    }
                    inv.markDirty();
                    return new ExtractResult(extracted, (slot + 1) % size);
                }
            }
            return new ExtractResult(ItemStack.EMPTY, startIndex);
        }
    }
}
