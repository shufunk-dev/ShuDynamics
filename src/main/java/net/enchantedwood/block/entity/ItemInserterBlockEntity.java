package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.ItemInserterBlock;
import net.enchantedwood.util.ItemTransportHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ItemInserterBlockEntity extends BlockEntity {
    public static final int BUFFER_SIZE = 4;
    private final DefaultedList<ItemStack> buffer = DefaultedList.ofSize(BUFFER_SIZE, ItemStack.EMPTY);
    private int disconnectedSides = 0;

    public ItemInserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_INSERTER_BLOCK_ENTITY, pos, state);
    }

    public DefaultedList<ItemStack> getItems() {
        return this.buffer;
    }

    public boolean isDisconnected(Direction dir) {
        return (this.disconnectedSides & (1 << dir.ordinal())) != 0;
    }

    public boolean toggleConnection(Direction dir) {
        this.disconnectedSides ^= (1 << dir.ordinal());
        markDirty();
        return isDisconnected(dir);
    }

    public void setDisconnected(Direction dir, boolean disconnected) {
        if (disconnected) {
            this.disconnectedSides |= (1 << dir.ordinal());
        } else {
            this.disconnectedSides &= ~(1 << dir.ordinal());
        }
        markDirty();
    }

    public ItemStack receiveItemFromPipe(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack toInsert = stack.copy();

        // 1. Try to merge into matching slots
        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack current = this.buffer.get(i);
            if (!current.isEmpty() && ItemStack.areItemsAndComponentsEqual(current, toInsert)) {
                int space = current.getMaxCount() - current.getCount();
                if (space > 0) {
                    int move = Math.min(space, toInsert.getCount());
                    current.increment(move);
                    toInsert.decrement(move);
                    markDirty();
                    if (toInsert.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // 2. Insert into empty slots
        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack current = this.buffer.get(i);
            if (current.isEmpty()) {
                this.buffer.set(i, toInsert.copy());
                markDirty();
                return ItemStack.EMPTY;
            }
        }

        return toInsert;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, ItemInserterBlockEntity entity) {
        boolean isEmpty = true;
        for (ItemStack stack : entity.buffer) {
            if (!stack.isEmpty()) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) return;

        Direction facing = state.get(ItemInserterBlock.FACING);
        BlockPos targetPos = pos.offset(facing);
        Inventory targetInv = ItemTransportHelper.getInventoryAt(world, targetPos);
        if (targetInv == null) return;

        boolean dirty = false;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack stack = entity.buffer.get(i);
            if (stack.isEmpty()) continue;

            ItemStack remaining = ItemTransportHelper.insertItem(targetInv, stack, facing.getOpposite());
            if (remaining.getCount() != stack.getCount()) {
                entity.buffer.set(i, remaining);
                dirty = true;
            }
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.buffer.clear();
        Inventories.readData(view, this.buffer);
        this.disconnectedSides = view.getInt("DisconnectedSides", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.buffer);
        view.putInt("DisconnectedSides", this.disconnectedSides);
    }
}
