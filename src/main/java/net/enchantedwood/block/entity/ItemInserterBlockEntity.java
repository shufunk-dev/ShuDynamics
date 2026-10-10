package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.ItemInserterBlock;
import net.enchantedwood.util.ItemTransportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ItemInserterBlockEntity extends BlockEntity {
    public static final int BUFFER_SIZE = 4;
    private final NonNullList<ItemStack> buffer = NonNullList.withSize(BUFFER_SIZE, ItemStack.EMPTY);
    private int disconnectedSides = 0;

    public ItemInserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_INSERTER_BLOCK_ENTITY, pos, state);
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

    public boolean canAccept(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (ItemStack current : this.buffer) {
            if (current.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
                return true;
            }
        }
        if (this.level != null) {
            Direction facing = this.getBlockState().getValue(ItemInserterBlock.FACING);
            BlockPos targetPos = this.worldPosition.relative(facing);
            Container targetInv = ItemTransportHelper.getInventoryAt(this.level, targetPos);
            if (targetInv != null) {
                if (targetInv instanceof WorldlyContainer sidedInv) {
                    Direction targetSide = facing.getOpposite();
                    int[] slots = sidedInv.getSlotsForFace(targetSide);
                    for (int slot : slots) {
                        if (!sidedInv.canPlaceItemThroughFace(slot, stack, targetSide)) continue;
                        ItemStack invStack = targetInv.getItem(slot);
                        if (invStack.isEmpty()) return true;
                        if (ItemStack.isSameItemSameComponents(invStack, stack) && invStack.getCount() < invStack.getMaxStackSize()) {
                            return true;
                        }
                    }
                } else {
                    for (int i = 0; i < targetInv.getContainerSize(); i++) {
                        ItemStack invStack = targetInv.getItem(i);
                        if (invStack.isEmpty()) return true;
                        if (ItemStack.isSameItemSameComponents(invStack, stack) && invStack.getCount() < invStack.getMaxStackSize()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public ItemStack receiveItemFromPipe(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack toInsert = stack.copy();

        // 1. Directly insert into connected inventory for instant flow
        if (this.level instanceof ServerLevel serverWorld) {
            Direction facing = this.getBlockState().getValue(ItemInserterBlock.FACING);
            BlockPos targetPos = this.worldPosition.relative(facing);
            Container targetInv = ItemTransportHelper.getInventoryAt(serverWorld, targetPos);
            if (targetInv != null) {
                toInsert = ItemTransportHelper.insertItem(targetInv, toInsert, facing.getOpposite());
                if (toInsert.isEmpty()) {
                    setChanged();
                    return ItemStack.EMPTY;
                }
            }
        }

        // 2. Try to merge into matching buffer slots
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

        // 3. Insert into empty buffer slots
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

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, ItemInserterBlockEntity entity) {
        boolean isEmpty = true;
        for (ItemStack stack : entity.buffer) {
            if (!stack.isEmpty()) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) return;

        Direction facing = state.getValue(ItemInserterBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        Container targetInv = ItemTransportHelper.getInventoryAt(world, targetPos);
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
            entity.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.buffer.clear();
        ContainerHelper.loadAllItems(view, this.buffer);
        this.disconnectedSides = view.getIntOr("DisconnectedSides", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.buffer);
        view.putInt("DisconnectedSides", this.disconnectedSides);
    }
}
