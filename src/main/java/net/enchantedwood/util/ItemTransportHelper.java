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
            return ChestBlock.getInventory(chestBlock, state, world, pos, true);
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

    public static ItemStack extractItem(Inventory inv, @Nullable Direction side, int maxCount) {
        if (inv == null || maxCount <= 0) return ItemStack.EMPTY;

        if (inv instanceof SidedInventory sidedInv && side != null) {
            int[] slots = sidedInv.getAvailableSlots(side);
            for (int slot : slots) {
                ItemStack current = inv.getStack(slot);
                if (!current.isEmpty() && sidedInv.canExtract(slot, current, side)) {
                    int count = Math.min(maxCount, current.getCount());
                    ItemStack extracted = current.split(count);
                    if (current.isEmpty()) {
                        inv.setStack(slot, ItemStack.EMPTY);
                    }
                    inv.markDirty();
                    return extracted;
                }
            }
        } else {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack current = inv.getStack(i);
                if (!current.isEmpty()) {
                    int count = Math.min(maxCount, current.getCount());
                    ItemStack extracted = current.split(count);
                    if (current.isEmpty()) {
                        inv.setStack(i, ItemStack.EMPTY);
                    }
                    inv.markDirty();
                    return extracted;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
