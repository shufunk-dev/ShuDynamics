package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.ReinforcedTankGlassBlock;
import net.enchantedwood.block.custom.TitaniumTankCasingBlock;
import net.enchantedwood.block.custom.TitaniumTankInboundPortBlock;
import net.enchantedwood.fluid.LavaProvider;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.fluid.MoltenMetalProvider;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.TitaniumTankScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class TitaniumTankControllerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, LavaProvider, MoltenMetalProvider {
    public static final int CAPACITY = 500_000; // 500,000 mB = 500 buckets
    public static final int BUCKET_IN_SLOT = 0;
    public static final int BUCKET_OUT_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int lavaAmount = 0;
    private MoltenMetal currentFluid = MoltenMetal.NONE;
    private MoltenMetal filterFluid = MoltenMetal.NONE;
    private boolean isFormed = false;
    private BlockPos minPos = null; // Corner (minX, minY, minZ)

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lavaAmount & 0xFFFF;
                case 1 -> (lavaAmount >> 16) & 0xFFFF;
                case 2 -> CAPACITY & 0xFFFF;
                case 3 -> (CAPACITY >> 16) & 0xFFFF;
                case 4 -> isFormed ? 1 : 0;
                case 5 -> currentFluid.ordinal();
                case 6 -> filterFluid.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> lavaAmount = (lavaAmount & 0xFFFF0000) | (value & 0xFFFF);
                case 1 -> lavaAmount = (lavaAmount & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                case 4 -> isFormed = (value == 1);
                case 5 -> {
                    MoltenMetal[] metals = MoltenMetal.values();
                    if (value >= 0 && value < metals.length) currentFluid = metals[value];
                }
                case 6 -> {
                    MoltenMetal[] metals = MoltenMetal.values();
                    if (value >= 0 && value < metals.length) filterFluid = metals[value];
                }
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public TitaniumTankControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TITANIUM_TANK_CONTROLLER_BLOCK_ENTITY, pos, state);
    }

    public boolean isFormed() {
        return this.isFormed;
    }

    public @Nullable BlockPos getMinPos() {
        return this.minPos;
    }

    // ==========================================
    // MULTIBLOCK VALIDATION & FORMATION
    // ==========================================
    public static @Nullable TitaniumTankControllerBlockEntity findControllerForBlock(Level world, BlockPos pos) {
        if (world == null) return null;
        for (int dy = 0; dy <= 4; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockEntity be = world.getBlockEntity(checkPos);
                    if (be instanceof TitaniumTankControllerBlockEntity controller) {
                        BlockPos min = controller.isFormed() && controller.getMinPos() != null
                                ? controller.getMinPos()
                                : checkPos.offset(-2, -4, -2);
                        int rx = pos.getX() - min.getX();
                        int ry = pos.getY() - min.getY();
                        int rz = pos.getZ() - min.getZ();
                        if (rx >= 0 && rx < 5 && ry >= 0 && ry < 5 && rz >= 0 && rz < 5) {
                            return controller;
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean tryFormStructure() {
        if (this.level == null || this.level.isClientSide()) return false;
        if (this.isFormed && this.minPos != null && validateStructureAt(this.minPos)) {
            return true;
        }

        // Controller is at top center: pos is (minX + 2, minY + 4, minZ + 2)
        BlockPos origin = this.worldPosition.offset(-2, -4, -2);
        if (validateStructureAt(origin)) {
            assembleStructureAt(origin);
            return true;
        }
        return false;
    }

    private boolean validateStructureAt(BlockPos min) {
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    BlockPos p = min.offset(x, y, z);
                    BlockState bs = this.level.getBlockState(p);
                    Block b = bs.getBlock();

                    if (y == 0) {
                        // Bottom Layer: all 25 must be titanium casings
                        if (!(b instanceof TitaniumTankCasingBlock)) return false;
                    } else if (y == 4) {
                        // Top Layer: center must be inbound port, other 24 must be casings
                        if (x == 2 && z == 2) {
                            if (!(b instanceof TitaniumTankInboundPortBlock)) return false;
                        } else {
                            if (!(b instanceof TitaniumTankCasingBlock)) return false;
                        }
                    } else {
                        // Layers 1, 2, 3
                        boolean isEdgeX = (x == 0 || x == 4);
                        boolean isEdgeZ = (z == 0 || z == 4);

                        if (isEdgeX && isEdgeZ) {
                            // 4 Corner Pillars: must be titanium casings
                            if (!(b instanceof TitaniumTankCasingBlock)) return false;
                        } else if (isEdgeX || isEdgeZ) {
                            // Wall Panels: can be reinforced glass or titanium casings
                            if (!(b instanceof ReinforcedTankGlassBlock) && !(b instanceof TitaniumTankCasingBlock)) {
                                return false;
                            }
                        } else {
                            // Interior (3x3x3): must be air or existing tank fluid
                            if (!bs.isAir() && bs.getBlock() != Blocks.LAVA) return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void assembleStructureAt(BlockPos min) {
        this.minPos = min;
        this.isFormed = true;

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    BlockPos p = min.offset(x, y, z);
                    BlockState bs = this.level.getBlockState(p);

                    if (bs.hasProperty(TitaniumTankCasingBlock.FORMED)) {
                        this.level.setBlock(p, bs.setValue(TitaniumTankCasingBlock.FORMED, true), Block.UPDATE_ALL);
                    } else if (bs.hasProperty(ReinforcedTankGlassBlock.FORMED)) {
                        this.level.setBlock(p, bs.setValue(ReinforcedTankGlassBlock.FORMED, true), Block.UPDATE_ALL);
                    } else if (bs.hasProperty(TitaniumTankInboundPortBlock.FORMED)) {
                        this.level.setBlock(p, bs.setValue(TitaniumTankInboundPortBlock.FORMED, true), Block.UPDATE_ALL);
                    }

                    BlockEntity be = this.level.getBlockEntity(p);
                    if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                        casingBE.setMasterPos(this.worldPosition);
                    }
                }
            }
        }

        // Formation Sound & Particles
        this.level.playSound(null, this.worldPosition, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.2f, 0.8f);
        if (this.level instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.worldPosition.getX() + 0.5, this.worldPosition.getY() - 1.5, this.worldPosition.getZ() + 0.5, 40, 1.5, 1.5, 1.5, 0.1);
        }

        updateInteriorLavaBlocks();
        setChanged();
    }

    // ==========================================
    // ANTI-GRIEF DECONSTRUCTION & STEAM PURGE
    // ==========================================
    public void dismantleStructure() {
        if (!this.isFormed || this.level == null || this.minPos == null) return;

        // Emergency Steam Purge: vaporize all interior fluid safely to air
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    BlockPos p = this.minPos.offset(x, y, z);
                    BlockState bs = this.level.getBlockState(p);
                    if (bs.getBlock() == Blocks.LAVA) {
                        this.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }

        // Steam Hiss Sound & Smoke Particles
        this.level.playSound(null, this.worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.2f);
        if (this.level instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.worldPosition.getX() + 0.5, this.worldPosition.getY() - 2.0, this.worldPosition.getZ() + 0.5, 50, 1.5, 1.5, 1.5, 0.05);
            sw.sendParticles(ParticleTypes.SMOKE, this.worldPosition.getX() + 0.5, this.worldPosition.getY() - 2.0, this.worldPosition.getZ() + 0.5, 60, 1.5, 1.5, 1.5, 0.08);
        }

        // Unlink all member blocks
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    BlockPos p = this.minPos.offset(x, y, z);
                    BlockState bs = this.level.getBlockState(p);

                    if (bs.hasProperty(TitaniumTankCasingBlock.FORMED)) {
                        this.level.setBlock(p, bs.setValue(TitaniumTankCasingBlock.FORMED, false), Block.UPDATE_ALL);
                    } else if (bs.hasProperty(ReinforcedTankGlassBlock.FORMED)) {
                        this.level.setBlock(p, bs.setValue(ReinforcedTankGlassBlock.FORMED, false), Block.UPDATE_ALL);
                    } else if (bs.hasProperty(TitaniumTankInboundPortBlock.FORMED)) {
                        this.level.setBlock(p, bs.setValue(TitaniumTankInboundPortBlock.FORMED, false), Block.UPDATE_ALL);
                    }

                    BlockEntity be = this.level.getBlockEntity(p);
                    if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                        casingBE.setMasterPos(null);
                    }
                }
            }
        }

        this.isFormed = false;
        this.lavaAmount = 0; // Voided safely by the steam purge
        this.currentFluid = MoltenMetal.NONE;
        this.minPos = null;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ==========================================
    // INTERIOR FLUID LEVEL & CLIENT SYNC
    // ==========================================
    public void updateInteriorLavaBlocks() {
        if (!this.isFormed || this.level == null || this.minPos == null || this.level.isClientSide()) return;

        // Safely clear any legacy physical lava blocks to air so fluid is smoothly rendered by BER
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    BlockPos p = this.minPos.offset(x, y, z);
                    BlockState current = this.level.getBlockState(p);
                    if (current.getBlock() == Blocks.LAVA) {
                        this.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }

        this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ==========================================
    // TICK LOGIC: BUCKET HANDLING & INTEGRITY
    // ==========================================
    public static void tick(ServerLevel world, BlockPos pos, BlockState state, TitaniumTankControllerBlockEntity entity) {
        boolean dirty = false;

        if (entity.isFormed) {
            // Periodic structure integrity check every 40 ticks
            if (world.getGameTime() % 40 == 0) {
                if (entity.minPos == null || !entity.validateStructureAt(entity.minPos)) {
                    entity.dismantleStructure();
                    return;
                }
            }

            // 1. Manual Bucket In (Fill tank from Lava Bucket)
            ItemStack input = entity.inventory.get(BUCKET_IN_SLOT);
            ItemStack output = entity.inventory.get(BUCKET_OUT_SLOT);

            if (!input.isEmpty() && input.is(Items.LAVA_BUCKET)) {
                if (entity.lavaAmount + 1000 <= CAPACITY && (entity.currentFluid == MoltenMetal.LAVA || entity.lavaAmount == 0) && (output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < output.getMaxStackSize()))) {
                    entity.currentFluid = MoltenMetal.LAVA;
                    entity.lavaAmount += 1000;
                    input.shrink(1);
                    if (output.isEmpty()) {
                        entity.inventory.set(BUCKET_OUT_SLOT, new ItemStack(Items.BUCKET));
                    } else {
                        output.grow(1);
                    }
                    entity.updateInteriorLavaBlocks();
                    dirty = true;
                }
            }
            // 2. Manual Bucket Out (Drain tank into Empty Bucket - Lava only)
            else if (!input.isEmpty() && input.is(Items.BUCKET)) {
                if (entity.lavaAmount >= 1000 && entity.currentFluid == MoltenMetal.LAVA && (output.isEmpty() || (output.is(Items.LAVA_BUCKET) && output.getCount() < output.getMaxStackSize()))) {
                    entity.lavaAmount -= 1000;
                    if (entity.lavaAmount <= 0) {
                        entity.currentFluid = MoltenMetal.NONE;
                    }
                    input.shrink(1);
                    if (output.isEmpty()) {
                        entity.inventory.set(BUCKET_OUT_SLOT, new ItemStack(Items.LAVA_BUCKET));
                    } else {
                        output.grow(1);
                    }
                    entity.updateInteriorLavaBlocks();
                    dirty = true;
                }
            }
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    // ==========================================
    // LAVA & MOLTEN METAL PROVIDER IMPLEMENTATIONS
    // ==========================================
    @Override
    public int getLavaAmount() {
        return (this.isFormed && this.currentFluid == MoltenMetal.LAVA) ? this.lavaAmount : 0;
    }

    @Override
    public int getMaxLava() {
        return (this.isFormed && (this.currentFluid == MoltenMetal.LAVA || this.lavaAmount == 0)) ? CAPACITY : 0;
    }

    @Override
    public boolean canInsertLava() {
        if (!this.isFormed) return false;
        if (this.filterFluid != MoltenMetal.NONE && this.filterFluid != MoltenMetal.LAVA) {
            return false;
        }
        if (this.lavaAmount > 0 && this.currentFluid != MoltenMetal.NONE && this.currentFluid != MoltenMetal.LAVA) {
            return false;
        }
        return this.lavaAmount < CAPACITY;
    }

    @Override
    public boolean canInsertFluid(MoltenMetal metal) {
        if (!this.isFormed || metal == MoltenMetal.NONE) return false;
        if (this.filterFluid != MoltenMetal.NONE && metal != this.filterFluid) {
            return false;
        }
        if (this.lavaAmount > 0 && this.currentFluid != MoltenMetal.NONE && this.currentFluid != metal) {
            return false;
        }
        return this.lavaAmount < CAPACITY;
    }

    @Override
    public boolean isDedicatedTo(MoltenMetal metal) {
        if (!this.isFormed || metal == MoltenMetal.NONE) return false;
        if (this.filterFluid == metal) return true;
        return this.currentFluid == metal && this.lavaAmount > 0;
    }

    @Override
    public int insertLava(int amount, boolean simulate) {
        if (!this.isFormed || amount <= 0) return 0;
        if (this.filterFluid != MoltenMetal.NONE && this.filterFluid != MoltenMetal.LAVA) return 0;
        if (this.currentFluid != MoltenMetal.NONE && this.currentFluid != MoltenMetal.LAVA && this.lavaAmount > 0) return 0;
        int space = CAPACITY - this.lavaAmount;
        int inserted = Math.min(space, amount);
        if (!simulate && inserted > 0) {
            this.currentFluid = MoltenMetal.LAVA;
            this.lavaAmount += inserted;
            updateInteriorLavaBlocks();
            setChanged();
        }
        return inserted;
    }

    @Override
    public boolean canExtractLava() {
        return false; // Inbound Port is strictly inbound; extract from outer casings
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        return 0; // Inbound Port is strictly inbound; extract from outer casings
    }

    public int extractLavaInternal(int amount, boolean simulate) {
        if (!this.isFormed || this.currentFluid != MoltenMetal.LAVA || amount <= 0) return 0;
        int extracted = Math.min(this.lavaAmount, amount);
        if (!simulate && extracted > 0) {
            this.lavaAmount -= extracted;
            if (this.lavaAmount <= 0) {
                this.currentFluid = MoltenMetal.NONE;
            }
            updateInteriorLavaBlocks();
            setChanged();
        }
        return extracted;
    }

    @Override
    public MoltenMetal getFluidType() {
        return this.currentFluid;
    }

    @Override
    public int getFluidAmount(MoltenMetal metal) {
        return (this.isFormed && metal == this.currentFluid) ? this.lavaAmount : 0;
    }

    @Override
    public int getMaxFluid() {
        return this.isFormed ? CAPACITY : 0;
    }

    @Override
    public int insertFluid(MoltenMetal metal, int amount, boolean simulate) {
        if (!this.isFormed || metal == MoltenMetal.NONE || amount <= 0) return 0;
        if (this.filterFluid != MoltenMetal.NONE && this.filterFluid != metal) return 0;
        if (this.currentFluid != MoltenMetal.NONE && this.currentFluid != metal && this.lavaAmount > 0) return 0;
        int space = CAPACITY - this.lavaAmount;
        int inserted = Math.min(space, amount);
        if (!simulate && inserted > 0) {
            this.currentFluid = metal;
            this.lavaAmount += inserted;
            updateInteriorLavaBlocks();
            setChanged();
        }
        return inserted;
    }

    @Override
    public boolean canExtractFluid(MoltenMetal metal) {
        return false; // Inbound Port is strictly inbound; extract from outer casings
    }

    @Override
    public int extractFluid(MoltenMetal metal, int amount, boolean simulate) {
        return 0; // Inbound Port is strictly inbound; extract from outer casings
    }

    public int extractFluidInternal(MoltenMetal metal, int amount, boolean simulate) {
        if (!this.isFormed || metal == MoltenMetal.NONE || metal != this.currentFluid || amount <= 0) return 0;
        int extracted = Math.min(this.lavaAmount, amount);
        if (!simulate && extracted > 0) {
            this.lavaAmount -= extracted;
            if (this.lavaAmount <= 0) {
                this.currentFluid = MoltenMetal.NONE;
            }
            updateInteriorLavaBlocks();
            setChanged();
        }
        return extracted;
    }

    @Override
    public List<MoltenMetal> getContainedFluids() {
        return (this.isFormed && this.currentFluid != MoltenMetal.NONE && this.lavaAmount > 0) ? List.of(this.currentFluid) : List.of();
    }

    // ==========================================
    // INVENTORY & SCREEN HANDLER
    // ==========================================
    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack res = ContainerHelper.removeItem(inventory, slot, amount);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{BUCKET_IN_SLOT, BUCKET_OUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == BUCKET_IN_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == BUCKET_OUT_SLOT;
    }

    @Override
    public Component getDisplayName() {
        if (this.currentFluid != null && this.currentFluid != MoltenMetal.NONE) {
            return Component.literal("5x5 " + this.currentFluid.getDisplayName() + " Tank");
        }
        return Component.literal("5x5 Titanium Multi-Fluid Tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new TitaniumTankScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("LavaAmount", this.lavaAmount);
        view.putString("FluidType", this.currentFluid.getId());
        view.putString("FilterFluid", this.filterFluid.getId());
        view.putBoolean("IsFormed", this.isFormed);
        if (this.minPos != null) {
            view.putInt("MinX", this.minPos.getX());
            view.putInt("MinY", this.minPos.getY());
            view.putInt("MinZ", this.minPos.getZ());
        }
        ContainerHelper.saveAllItems(view, this.inventory);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.lavaAmount = view.getIntOr("LavaAmount", 0);
        if (view.contains("FluidType")) {
            this.currentFluid = MoltenMetal.fromId(view.getStringOr("FluidType", "none"));
        } else {
            this.currentFluid = (this.lavaAmount > 0) ? MoltenMetal.LAVA : MoltenMetal.NONE;
        }
        if (this.lavaAmount <= 0) {
            this.currentFluid = MoltenMetal.NONE;
        }
        if (view.contains("FilterFluid")) {
            this.filterFluid = MoltenMetal.fromId(view.getStringOr("FilterFluid", "none"));
        } else {
            this.filterFluid = MoltenMetal.NONE;
        }
        this.isFormed = view.getBooleanOr("IsFormed", false);
        if (view.contains("MinX") && view.contains("MinY") && view.contains("MinZ")) {
            this.minPos = new BlockPos(view.getIntOr("MinX", 0), view.getIntOr("MinY", 0), view.getIntOr("MinZ", 0));
        } else {
            this.minPos = null;
        }
        ContainerHelper.loadAllItems(view, this.inventory);
    }

    public int getStoredFluidAmount() {
        return this.lavaAmount;
    }

    public MoltenMetal getFilterFluid() {
        return this.filterFluid;
    }

    public void setFilterFluid(MoltenMetal filter) {
        this.filterFluid = filter != null ? filter : MoltenMetal.NONE;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
