package net.enchantedwood.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ItemTransportHelper {
    private ItemTransportHelper() {}

    public static @Nullable Container getInventoryAt(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof WorldlyContainerHolder provider) {
            return provider.getContainer(state, world, pos);
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity && block instanceof ChestBlock chestBlock) {
            Container inv = ChestBlock.getContainer(chestBlock, state, world, pos, true);
            if (inv != null) return inv;
        }
        if (be instanceof Container inventory) {
            return inventory;
        }
        return null;
    }

    public static ItemStack insertItem(Container inv, ItemStack stack, @Nullable Direction side) {
        if (stack.isEmpty() || inv == null) return stack;
        ItemStack toInsert = stack.copy();

        if (inv instanceof WorldlyContainer sidedInv && side != null) {
            int[] slots = sidedInv.getSlotsForFace(side);
            // 1. Try to merge into matching non-empty slots first
            for (int slot : slots) {
                if (!sidedInv.canPlaceItemThroughFace(slot, toInsert, side)) continue;
                ItemStack current = inv.getItem(slot);
                if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, toInsert)) {
                    int max = Math.min(inv.getMaxStackSize(), current.getMaxStackSize());
                    int space = max - current.getCount();
                    if (space > 0) {
                        int move = Math.min(space, toInsert.getCount());
                        current.grow(move);
                        toInsert.shrink(move);
                        inv.setChanged();
                        if (toInsert.isEmpty()) return ItemStack.EMPTY;
                    }
                }
            }
            // 2. Try to place into empty slots
            for (int slot : slots) {
                if (!sidedInv.canPlaceItemThroughFace(slot, toInsert, side)) continue;
                ItemStack current = inv.getItem(slot);
                if (current.isEmpty()) {
                    int max = Math.min(inv.getMaxStackSize(), toInsert.getMaxStackSize());
                    int move = Math.min(max, toInsert.getCount());
                    ItemStack split = toInsert.split(move);
                    inv.setItem(slot, split);
                    inv.setChanged();
                    if (toInsert.isEmpty()) return ItemStack.EMPTY;
                }
            }
        } else {
            // 1. Try to merge into matching non-empty slots
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack current = inv.getItem(i);
                if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, toInsert)) {
                    int max = Math.min(inv.getMaxStackSize(), current.getMaxStackSize());
                    int space = max - current.getCount();
                    if (space > 0) {
                        int move = Math.min(space, toInsert.getCount());
                        current.grow(move);
                        toInsert.shrink(move);
                        inv.setChanged();
                        if (toInsert.isEmpty()) return ItemStack.EMPTY;
                    }
                }
            }
            // 2. Try empty slots
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack current = inv.getItem(i);
                if (current.isEmpty()) {
                    int max = Math.min(inv.getMaxStackSize(), toInsert.getMaxStackSize());
                    int move = Math.min(max, toInsert.getCount());
                    ItemStack split = toInsert.split(move);
                    inv.setItem(i, split);
                    inv.setChanged();
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

    public static ItemStack extractItem(Container inv, @Nullable Direction side, int maxCount) {
        return extractItemRoundRobin(inv, side, maxCount, 0).stack;
    }

    public static ExtractResult extractItemRoundRobin(Container inv, @Nullable Direction side, int maxCount, int startIndex) {
        if (inv == null || maxCount <= 0) return new ExtractResult(ItemStack.EMPTY, startIndex);

        if (inv instanceof WorldlyContainer sidedInv && side != null) {
            int[] slots = sidedInv.getSlotsForFace(side);
            if (slots.length == 0) return new ExtractResult(ItemStack.EMPTY, 0);

            for (int i = 0; i < slots.length; i++) {
                int slotIdx = (startIndex + i) % slots.length;
                int slot = slots[slotIdx];
                ItemStack current = inv.getItem(slot);
                if (!current.isEmpty() && sidedInv.canTakeItemThroughFace(slot, current, side)) {
                    int count = Math.min(maxCount, current.getCount());
                    ItemStack extracted = current.split(count);
                    if (current.isEmpty()) {
                        inv.setItem(slot, ItemStack.EMPTY);
                    }
                    inv.setChanged();
                    return new ExtractResult(extracted, (slotIdx + 1) % slots.length);
                }
            }
            return new ExtractResult(ItemStack.EMPTY, startIndex);
        } else {
            int size = inv.getContainerSize();
            if (size == 0) return new ExtractResult(ItemStack.EMPTY, 0);

            for (int i = 0; i < size; i++) {
                int slot = (startIndex + i) % size;
                ItemStack current = inv.getItem(slot);
                if (!current.isEmpty()) {
                    int count = Math.min(maxCount, current.getCount());
                    ItemStack extracted = current.split(count);
                    if (current.isEmpty()) {
                        inv.setItem(slot, ItemStack.EMPTY);
                    }
                    inv.setChanged();
                    return new ExtractResult(extracted, (slot + 1) % size);
                }
            }
            return new ExtractResult(ItemStack.EMPTY, startIndex);
        }
    }
}
