package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.DigitalConverterBlock;
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class DigitalConverterBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int BUFFER_SIZE = 4;
    private final NonNullList<ItemStack> buffer = NonNullList.withSize(BUFFER_SIZE, ItemStack.EMPTY);
    private int checkTimer = 0;

    // Remote network binding via Wrench
    private @Nullable BlockPos boundNetworkPos = null;
    private String boundDimension = "minecraft:overworld";

    public DigitalConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_CONVERTER_BLOCK_ENTITY, pos, state);
    }

    public void bindNetwork(BlockPos pos, String dimension) {
        this.boundNetworkPos = pos;
        this.boundDimension = dimension;
        setChanged();
    }

    public void unbindNetwork() {
        this.boundNetworkPos = null;
        setChanged();
    }

    public @Nullable BlockPos getBoundNetworkPos() {
        return this.boundNetworkPos;
    }

    public String getBoundDimension() {
        return this.boundDimension;
    }

    public boolean isBoundToRemote() {
        return this.boundNetworkPos != null;
    }

    public @Nullable EnchantedStorageTerminalBlockEntity getNetworkTerminal() {
        if (this.level == null) return null;

        // 1. Check bound remote network
        if (this.boundNetworkPos != null && this.level.getServer() != null) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(this.boundDimension));
            ServerLevel targetWorld = this.level.getServer().getLevel(dimKey);
            if (targetWorld != null && targetWorld.hasChunk(this.boundNetworkPos.getX() >> 4, this.boundNetworkPos.getZ() >> 4)) {
                BlockEntity be = targetWorld.getBlockEntity(this.boundNetworkPos);
                if (be instanceof EnchantedStorageTerminalBlockEntity terminal && terminal.isNetworkOnline()) {
                    return terminal;
                } else if (be instanceof EnchantedStorageControllerBlockEntity ctrl && ctrl.isOnline()) {
                    // Search near controller for terminal
                    BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                    for (int dx = -16; dx <= 16; dx++) {
                        for (int dy = -8; dy <= 8; dy++) {
                            for (int dz = -16; dz <= 16; dz++) {
                                mut.set(this.boundNetworkPos.getX() + dx, this.boundNetworkPos.getY() + dy, this.boundNetworkPos.getZ() + dz);
                                BlockEntity candidate = targetWorld.getBlockEntity(mut);
                                if (candidate instanceof EnchantedStorageTerminalBlockEntity t && t.isNetworkOnline()) {
                                    return t;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Fallback to local 16-block proximity
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockEntity be = this.level.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageTerminalBlockEntity terminal) {
                        return terminal;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable EnchantedStorageControllerBlockEntity getNetworkController() {
        if (this.level == null) return null;

        // 1. Check bound remote network
        if (this.boundNetworkPos != null && this.level.getServer() != null) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(this.boundDimension));
            ServerLevel targetWorld = this.level.getServer().getLevel(dimKey);
            if (targetWorld != null && targetWorld.hasChunk(this.boundNetworkPos.getX() >> 4, this.boundNetworkPos.getZ() >> 4)) {
                BlockEntity be = targetWorld.getBlockEntity(this.boundNetworkPos);
                if (be instanceof EnchantedStorageControllerBlockEntity controller) {
                    return controller;
                } else if (be instanceof EnchantedStorageTerminalBlockEntity) {
                    // Search near terminal for controller
                    BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                    for (int dx = -16; dx <= 16; dx++) {
                        for (int dy = -8; dy <= 8; dy++) {
                            for (int dz = -16; dz <= 16; dz++) {
                                mut.set(this.boundNetworkPos.getX() + dx, this.boundNetworkPos.getY() + dy, this.boundNetworkPos.getZ() + dz);
                                BlockEntity candidate = targetWorld.getBlockEntity(mut);
                                if (candidate instanceof EnchantedStorageControllerBlockEntity c) {
                                    return c;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Fallback to local 16-block proximity
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockEntity be = this.level.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageControllerBlockEntity controller) {
                        return controller;
                    }
                }
            }
        }
        return null;
    }

    public boolean isNetworkOnline() {
        if (this.level == null) return false;
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

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, DigitalConverterBlockEntity entity) {
        boolean dirty = false;
        boolean wasLit = state.getValue(DigitalConverterBlock.LIT);
        boolean isOnline = entity.isNetworkOnline();

        if (wasLit != isOnline) {
            world.setBlock(pos, state.setValue(DigitalConverterBlock.LIT, isOnline), 3);
        }

        if (isOnline) {
            EnchantedStorageTerminalBlockEntity terminal = entity.getNetworkTerminal();
            if (terminal != null && terminal.isNetworkOnline()) {
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    ItemStack bufferStack = entity.buffer.get(i);
                    if (bufferStack.isEmpty()) continue;

                    ItemStack remainder = terminal.depositItem(bufferStack);
                    if (remainder.getCount() != bufferStack.getCount()) {
                        entity.buffer.set(i, remainder);
                        dirty = true;
                    }
                    if (!remainder.isEmpty()) break; // Network capacity reached
                }
            }
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return this.buffer.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.buffer, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.buffer, slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.buffer.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.buffer.clear();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        if (terminal == null || !terminal.isNetworkOnline()) return false;
        return terminal.getStoredItemCount() + stack.getCount() <= terminal.getNetworkCapacity();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.buffer.clear();
        ContainerHelper.loadAllItems(view, this.buffer);
        if (view.contains("BoundX")) {
            this.boundNetworkPos = new BlockPos(view.getIntOr("BoundX", 0), view.getIntOr("BoundY", 0), view.getIntOr("BoundZ", 0));
            this.boundDimension = view.getStringOr("BoundDim", "minecraft:overworld");
        } else {
            this.boundNetworkPos = null;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.buffer);
        if (this.boundNetworkPos != null) {
            view.putInt("BoundX", this.boundNetworkPos.getX());
            view.putInt("BoundY", this.boundNetworkPos.getY());
            view.putInt("BoundZ", this.boundNetworkPos.getZ());
            view.putString("BoundDim", this.boundDimension);
        }
    }
}
