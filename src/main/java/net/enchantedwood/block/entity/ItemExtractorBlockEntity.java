package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.ItemExtractorBlock;
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

import java.util.ArrayList;
import java.util.List;

public class ItemExtractorBlockEntity extends BlockEntity {
    public static final int BUFFER_SIZE = 4;
    public static final int EXTRACT_INTERVAL = 10; // Pulls every 10 ticks (0.5s)
    public static final int MAX_ITEMS_PER_PULL = 4;

    private final DefaultedList<ItemStack> buffer = DefaultedList.ofSize(BUFFER_SIZE, ItemStack.EMPTY);
    private int timer = 0;
    private int disconnectedSides = 0;

    public ItemExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_EXTRACTOR_BLOCK_ENTITY, pos, state);
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

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, ItemExtractorBlockEntity entity) {
        boolean dirty = false;
        Direction facing = state.get(ItemExtractorBlock.FACING);

        // 1. First, push any buffered items into connected pipe / inserter network
        List<BlockEntity> pipeTargets = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (dir == facing) continue; // Don't push back into the source
            if (entity.isDisconnected(dir)) continue;

            BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
            if (neighbor instanceof ItemPipeBlockEntity pipe) {
                if (!pipe.isDisconnected(dir.getOpposite())) {
                    pipeTargets.add(pipe);
                }
            } else if (neighbor instanceof ItemInserterBlockEntity inserter) {
                if (!inserter.isDisconnected(dir.getOpposite())) {
                    pipeTargets.add(inserter);
                }
            }
        }

        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack stack = entity.buffer.get(i);
            if (stack.isEmpty()) continue;

            for (BlockEntity target : pipeTargets) {
                if (target instanceof ItemPipeBlockEntity pipe) {
                    ItemStack remaining = pipe.insertItem(stack);
                    if (remaining.getCount() != stack.getCount()) {
                        entity.buffer.set(i, remaining);
                        stack = remaining;
                        dirty = true;
                        if (stack.isEmpty()) break;
                    }
                } else if (target instanceof ItemInserterBlockEntity inserter) {
                    ItemStack remaining = inserter.receiveItemFromPipe(stack);
                    if (remaining.getCount() != stack.getCount()) {
                        entity.buffer.set(i, remaining);
                        stack = remaining;
                        dirty = true;
                        if (stack.isEmpty()) break;
                    }
                }
            }
        }

        // 2. Active Extraction Cycle
        ++entity.timer;
        if (entity.timer >= EXTRACT_INTERVAL) {
            entity.timer = 0;

            // Check if buffer has space for an extracted item
            int freeSlot = -1;
            for (int i = 0; i < BUFFER_SIZE; i++) {
                if (entity.buffer.get(i).isEmpty()) {
                    freeSlot = i;
                    break;
                }
            }

            if (freeSlot != -1) {
                BlockPos sourcePos = pos.offset(facing);
                Inventory sourceInv = ItemTransportHelper.getInventoryAt(world, sourcePos);
                if (sourceInv != null) {
                    ItemStack extracted = ItemTransportHelper.extractItem(sourceInv, facing.getOpposite(), MAX_ITEMS_PER_PULL);
                    if (!extracted.isEmpty()) {
                        entity.buffer.set(freeSlot, extracted);
                        dirty = true;
                    }
                }
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
        this.timer = view.getInt("Timer", 0);
        this.disconnectedSides = view.getInt("DisconnectedSides", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.buffer);
        view.putInt("Timer", this.timer);
        view.putInt("DisconnectedSides", this.disconnectedSides);
    }
}
