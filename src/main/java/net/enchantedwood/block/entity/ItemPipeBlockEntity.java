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

import java.util.*;
import org.jetbrains.annotations.Nullable;

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

    public boolean canAccept(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (ItemStack current : this.buffer) {
            if (current.isEmpty()) return true;
            if (ItemStack.areItemsAndComponentsEqual(current, stack) && current.getCount() < current.getMaxCount()) {
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

        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack stack = entity.buffer.get(i);
            if (stack.isEmpty()) continue;

            // 1. Direct check for adjacent inserters / digital converters (distance = 0)
            boolean inserted = false;
            for (Direction dir : Direction.values()) {
                if (entity.isDisconnected(dir)) continue;
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                if (neighbor instanceof ItemInserterBlockEntity inserter) {
                    if (!inserter.isDisconnected(dir.getOpposite()) && inserter.canAccept(stack)) {
                        ItemStack remaining = inserter.receiveItemFromPipe(stack);
                        if (remaining.getCount() != stack.getCount()) {
                            entity.buffer.set(i, remaining);
                            stack = remaining;
                            dirty = true;
                            if (stack.isEmpty()) {
                                inserted = true;
                                break;
                            }
                        }
                    }
                } else if (neighbor instanceof DigitalConverterBlockEntity converter && converter.isNetworkOnline()) {
                    ItemStack remaining = net.enchantedwood.util.ItemTransportHelper.insertItem(converter, stack, dir.getOpposite());
                    if (remaining.getCount() != stack.getCount()) {
                        entity.buffer.set(i, remaining);
                        stack = remaining;
                        dirty = true;
                        if (stack.isEmpty()) {
                            inserted = true;
                            break;
                        }
                    }
                }
            }

            if (inserted || stack.isEmpty()) continue;

            // 2. Use BFS network pathfinding to route towards the nearest reachable Inserter or Digital Converter
            Direction bestRoute = findBestRoute(world, pos, entity, stack);
            if (bestRoute != null) {
                BlockEntity target = world.getBlockEntity(pos.offset(bestRoute));
                if (target instanceof ItemPipeBlockEntity nextPipe) {
                    ItemStack remaining = nextPipe.insertItem(stack);
                    if (remaining.getCount() != stack.getCount()) {
                        entity.buffer.set(i, remaining);
                        dirty = true;
                    }
                } else if (target instanceof ItemExtractorBlockEntity nextExt) {
                    ItemStack remaining = nextExt.insertItem(stack);
                    if (remaining.getCount() != stack.getCount()) {
                        entity.buffer.set(i, remaining);
                        dirty = true;
                    }
                } else if (target instanceof ItemInserterBlockEntity inserter) {
                    ItemStack remaining = inserter.receiveItemFromPipe(stack);
                    if (remaining.getCount() != stack.getCount()) {
                        entity.buffer.set(i, remaining);
                        dirty = true;
                    }
                } else if (target instanceof DigitalConverterBlockEntity converter) {
                    ItemStack remaining = net.enchantedwood.util.ItemTransportHelper.insertItem(converter, stack, bestRoute.getOpposite());
                    if (remaining.getCount() != stack.getCount()) {
                        entity.buffer.set(i, remaining);
                        dirty = true;
                    }
                }
            }
        }

        if (dirty) {
            entity.markDirty();
            entity.cooldown = 2; // Smooth 2-tick transfer step
        }
    }

    @Nullable
    public static Direction findBestRoute(ServerWorld world, BlockPos startPos, ItemPipeBlockEntity startPipe, ItemStack stack) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Direction> firstStepMap = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();

        visited.add(startPos);

        for (Direction dir : Direction.values()) {
            if (startPipe.isDisconnected(dir)) continue;
            BlockPos neighborPos = startPos.offset(dir);
            BlockEntity neighbor = world.getBlockEntity(neighborPos);

            if (neighbor instanceof ItemInserterBlockEntity inserter) {
                if (!inserter.isDisconnected(dir.getOpposite()) && inserter.canAccept(stack)) {
                    return dir;
                }
            } else if (neighbor instanceof DigitalConverterBlockEntity converter) {
                if (converter.isNetworkOnline()) {
                    return dir;
                }
            } else if (neighbor instanceof ItemPipeBlockEntity nextPipe) {
                if (!nextPipe.isDisconnected(dir.getOpposite()) && nextPipe.canAccept(stack)) {
                    queue.add(neighborPos);
                    firstStepMap.put(neighborPos, dir);
                    visited.add(neighborPos);
                }
            } else if (neighbor instanceof ItemExtractorBlockEntity nextExt) {
                Direction extFacing = world.getBlockState(neighborPos).get(net.enchantedwood.block.custom.ItemExtractorBlock.FACING);
                if (dir.getOpposite() != extFacing && !nextExt.isDisconnected(dir.getOpposite()) && nextExt.canAccept(stack)) {
                    queue.add(neighborPos);
                    firstStepMap.put(neighborPos, dir);
                    visited.add(neighborPos);
                }
            }
        }

        int searchLimit = 256;
        while (!queue.isEmpty() && searchLimit-- > 0) {
            BlockPos currentPos = queue.poll();
            Direction firstStep = firstStepMap.get(currentPos);

            BlockEntity currentBe = world.getBlockEntity(currentPos);
            Direction currentFacing = null;
            if (currentBe instanceof ItemExtractorBlockEntity ext) {
                currentFacing = world.getBlockState(currentPos).get(net.enchantedwood.block.custom.ItemExtractorBlock.FACING);
            } else if (!(currentBe instanceof ItemPipeBlockEntity)) {
                continue;
            }

            for (Direction dir : Direction.values()) {
                if (currentFacing != null && dir == currentFacing) continue;
                if (currentBe instanceof ItemPipeBlockEntity p && p.isDisconnected(dir)) continue;
                if (currentBe instanceof ItemExtractorBlockEntity e && e.isDisconnected(dir)) continue;

                BlockPos nextPos = currentPos.offset(dir);
                if (visited.contains(nextPos)) continue;

                BlockEntity nextBe = world.getBlockEntity(nextPos);
                if (nextBe instanceof ItemInserterBlockEntity inserter) {
                    if (!inserter.isDisconnected(dir.getOpposite()) && inserter.canAccept(stack)) {
                        return firstStep;
                    }
                } else if (nextBe instanceof DigitalConverterBlockEntity converter) {
                    if (converter.isNetworkOnline()) {
                        return firstStep;
                    }
                } else if (nextBe instanceof ItemPipeBlockEntity nextPipe) {
                    if (!nextPipe.isDisconnected(dir.getOpposite())) {
                        visited.add(nextPos);
                        firstStepMap.put(nextPos, firstStep);
                        queue.add(nextPos);
                    }
                } else if (nextBe instanceof ItemExtractorBlockEntity nextExt) {
                    Direction extFacing = world.getBlockState(nextPos).get(net.enchantedwood.block.custom.ItemExtractorBlock.FACING);
                    if (dir.getOpposite() != extFacing && !nextExt.isDisconnected(dir.getOpposite())) {
                        visited.add(nextPos);
                        firstStepMap.put(nextPos, firstStep);
                        queue.add(nextPos);
                    }
                }
            }
        }

        return null;
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
