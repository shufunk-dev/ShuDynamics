package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.ItemExtractorBlock;
import net.enchantedwood.util.ItemTransportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.*;
import org.jetbrains.annotations.Nullable;

public class ItemExtractorBlockEntity extends BlockEntity {
    public static final int BUFFER_SIZE = 4;
    public static final int EXTRACT_INTERVAL = 10; // Pulls every 10 ticks (0.5s)
    public static final int MAX_ITEMS_PER_PULL = 4;

    private final NonNullList<ItemStack> buffer = NonNullList.withSize(BUFFER_SIZE, ItemStack.EMPTY);
    private int timer = 0;
    private int extractSlotIndex = 0;
    private int disconnectedSides = 0;

    public ItemExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_EXTRACTOR_BLOCK_ENTITY, pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return this.buffer;
    }

    public boolean isDisconnected(Direction dir) {
        return (this.disconnectedSides & (1 << dir.ordinal())) != 0;
    }

    public boolean toggleConnection(Direction dir) {
        this.disconnectedSides ^= (1 << dir.ordinal());
        setChanged();
        return isDisconnected(dir);
    }

    public void setDisconnected(Direction dir, boolean disconnected) {
        if (disconnected) {
            this.disconnectedSides |= (1 << dir.ordinal());
        } else {
            this.disconnectedSides &= ~(1 << dir.ordinal());
        }
        setChanged();
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, ItemExtractorBlockEntity entity) {
        boolean dirty = false;
        Direction facing = state.getValue(ItemExtractorBlock.FACING);

        // 1. First, push any buffered items into connected pipe / inserter network
        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack stack = entity.buffer.get(i);
            if (stack.isEmpty()) continue;

            // Direct check for adjacent inserters / digital converters (distance = 0)
            boolean inserted = false;
            for (Direction dir : Direction.values()) {
                if (dir == facing) continue; // Don't push back into the source
                if (entity.isDisconnected(dir)) continue;

                BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
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
                    ItemStack remaining = ItemTransportHelper.insertItem(converter, stack, dir.getOpposite());
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

            // Use BFS to route towards nearest reachable Inserter or Digital Converter through the pipe network
            Direction bestRoute = findBestRouteFromExtractor(world, pos, entity, facing, stack);
            if (bestRoute != null) {
                BlockEntity target = world.getBlockEntity(pos.relative(bestRoute));
                if (target instanceof ItemPipeBlockEntity nextPipe) {
                    ItemStack remaining = nextPipe.insertItem(stack);
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
                    ItemStack remaining = ItemTransportHelper.insertItem(converter, stack, bestRoute.getOpposite());
                    if (remaining.getCount() != stack.getCount()) {
                        entity.buffer.set(i, remaining);
                        dirty = true;
                    }
                }
            }
        }

        // 2. Active Extraction Cycle
        ++entity.timer;
        if (entity.timer >= EXTRACT_INTERVAL) {
            entity.timer = 0;

            BlockPos sourcePos = pos.relative(facing);
            Container sourceInv = ItemTransportHelper.getInventoryAt(world, sourcePos);
            if (sourceInv != null) {
                // Pull up to 2 batches per cycle if buffer has room (allows pulling both primary and byproduct concurrently)
                for (int pull = 0; pull < 2; pull++) {
                    int freeSlot = -1;
                    for (int i = 0; i < BUFFER_SIZE; i++) {
                        if (entity.buffer.get(i).isEmpty()) {
                            freeSlot = i;
                            break;
                        }
                    }
                    if (freeSlot == -1) break;

                    ItemTransportHelper.ExtractResult result = ItemTransportHelper.extractItemRoundRobin(
                            sourceInv, facing.getOpposite(), MAX_ITEMS_PER_PULL, entity.extractSlotIndex);
                    entity.extractSlotIndex = result.nextSlotIndex;
                    if (!result.stack.isEmpty()) {
                        entity.buffer.set(freeSlot, result.stack);
                        dirty = true;
                    } else {
                        break;
                    }
                }
            }
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.buffer.clear();
        ContainerHelper.loadAllItems(view, this.buffer);
        this.timer = view.getIntOr("Timer", 0);
        this.extractSlotIndex = view.getIntOr("ExtractSlotIndex", 0);
        this.disconnectedSides = view.getIntOr("DisconnectedSides", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.buffer);
        view.putInt("Timer", this.timer);
        view.putInt("ExtractSlotIndex", this.extractSlotIndex);
        view.putInt("DisconnectedSides", this.disconnectedSides);
    }

    public boolean canAccept(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (ItemStack current : this.buffer) {
            if (current.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack toInsert = stack.copy();

        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack current = this.buffer.get(i);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, toInsert)) {
                int space = current.getMaxStackSize() - current.getCount();
                if (space > 0) {
                    int move = Math.min(space, toInsert.getCount());
                    current.grow(move);
                    toInsert.shrink(move);
                    setChanged();
                    if (toInsert.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        for (int i = 0; i < BUFFER_SIZE; i++) {
            ItemStack current = this.buffer.get(i);
            if (current.isEmpty()) {
                this.buffer.set(i, toInsert.copy());
                setChanged();
                return ItemStack.EMPTY;
            }
        }

        return toInsert;
    }

    @Nullable
    public static Direction findBestRouteFromExtractor(ServerLevel world, BlockPos startPos, ItemExtractorBlockEntity extractor, Direction facing, ItemStack stack) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Direction> firstStepMap = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();

        visited.add(startPos);

        for (Direction dir : Direction.values()) {
            if (dir == facing) continue;
            if (extractor.isDisconnected(dir)) continue;
            BlockPos neighborPos = startPos.relative(dir);
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
                Direction extFacing = world.getBlockState(neighborPos).getValue(ItemExtractorBlock.FACING);
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
                currentFacing = world.getBlockState(currentPos).getValue(ItemExtractorBlock.FACING);
            } else if (!(currentBe instanceof ItemPipeBlockEntity)) {
                continue;
            }

            for (Direction dir : Direction.values()) {
                if (currentFacing != null && dir == currentFacing) continue;
                if (currentBe instanceof ItemPipeBlockEntity p && p.isDisconnected(dir)) continue;
                if (currentBe instanceof ItemExtractorBlockEntity e && e.isDisconnected(dir)) continue;

                BlockPos nextPos = currentPos.relative(dir);
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
                    Direction extFacing = world.getBlockState(nextPos).getValue(ItemExtractorBlock.FACING);
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
}
