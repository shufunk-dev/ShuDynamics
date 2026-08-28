package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class ItemPipeBlockEntity extends BlockEntity {
    public static final int BUFFER_SIZE = 4;
    private final DefaultedList<ItemStack> buffer = DefaultedList.ofSize(BUFFER_SIZE, ItemStack.EMPTY);
    private int cooldown = 0;
    private int disconnectedSides = 0; // Bitmask for disconnected directions

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE_BLOCK_ENTITY, pos, state);
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

    public boolean hasSpace() {
        for (ItemStack stack : this.buffer) {
            if (stack.isEmpty() || stack.getCount() < stack.getMaxCount()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack toInsert = stack.copy();

        // 1. Merge into matching slots
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

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, ItemPipeBlockEntity entity) {
        if (entity.cooldown > 0) {
            --entity.cooldown;
            return;
        }

        boolean isEmpty = true;
        for (ItemStack stack : entity.buffer) {
            if (!stack.isEmpty()) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) return;

        boolean dirty = false;

        // Discover adjacent inserters and pipe neighbors (respecting wrench disconnections)
        List<ItemInserterBlockEntity> inserters = new ArrayList<>();
        List<ItemPipeBlockEntity> pipes = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (entity.isDisconnected(dir)) continue;

            BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
            if (neighbor instanceof ItemInserterBlockEntity inserter) {
                if (!inserter.isDisconnected(dir.getOpposite())) {
                    inserters.add(inserter);
                }
            } else if (neighbor instanceof ItemPipeBlockEntity pipe && pipe != entity) {
                if (!pipe.isDisconnected(dir.getOpposite()) && pipe.hasSpace()) {
                    pipes.add(pipe);
                }
            }
        }

        // Try pushing buffered items: Priority 1 to Inserters, Priority 2 to downstream Pipes
        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack stack = entity.buffer.get(i);
            if (stack.isEmpty()) continue;

            // 1. Try Inserters
            for (ItemInserterBlockEntity inserter : inserters) {
                ItemStack remaining = inserter.receiveItemFromPipe(stack);
                if (remaining.getCount() != stack.getCount()) {
                    entity.buffer.set(i, remaining);
                    stack = remaining;
                    dirty = true;
                    if (stack.isEmpty()) break;
                }
            }

            if (stack.isEmpty()) continue;

            // 2. Try adjacent pipes
            for (ItemPipeBlockEntity pipe : pipes) {
                ItemStack remaining = pipe.insertItem(stack);
                if (remaining.getCount() != stack.getCount()) {
                    entity.buffer.set(i, remaining);
                    dirty = true;
                    break;
                }
            }
        }

        if (dirty) {
            entity.markDirty();
            entity.cooldown = 4; // Transfer step delay for smooth visual pacing
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.buffer.clear();
        Inventories.readData(view, this.buffer);
        this.cooldown = view.getInt("Cooldown", 0);
        this.disconnectedSides = view.getInt("DisconnectedSides", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.buffer);
        view.putInt("Cooldown", this.cooldown);
        view.putInt("DisconnectedSides", this.disconnectedSides);
    }
}
