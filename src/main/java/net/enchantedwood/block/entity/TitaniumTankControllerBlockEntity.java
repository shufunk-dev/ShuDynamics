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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import java.util.List;

public class TitaniumTankControllerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, LavaProvider, MoltenMetalProvider {
    public static final int CAPACITY = 500_000; // 500,000 mB = 500 buckets
    public static final int BUCKET_IN_SLOT = 0;
    public static final int BUCKET_OUT_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int lavaAmount = 0;
    private MoltenMetal currentFluid = MoltenMetal.NONE;
    private boolean isFormed = false;
    private BlockPos minPos = null; // Corner (minX, minY, minZ)

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lavaAmount & 0xFFFF;
                case 1 -> (lavaAmount >> 16) & 0xFFFF;
                case 2 -> CAPACITY & 0xFFFF;
                case 3 -> (CAPACITY >> 16) & 0xFFFF;
                case 4 -> isFormed ? 1 : 0;
                case 5 -> currentFluid.ordinal();
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
            }
        }

        @Override
        public int size() {
            return 6;
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
    public static @Nullable TitaniumTankControllerBlockEntity findControllerForBlock(World world, BlockPos pos) {
        if (world == null) return null;
        for (int dy = 0; dy <= 4; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    BlockEntity be = world.getBlockEntity(checkPos);
                    if (be instanceof TitaniumTankControllerBlockEntity controller) {
                        BlockPos min = controller.isFormed() && controller.getMinPos() != null
                                ? controller.getMinPos()
                                : checkPos.add(-2, -4, -2);
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
        if (this.world == null || this.world.isClient()) return false;
        if (this.isFormed && this.minPos != null && validateStructureAt(this.minPos)) {
            return true;
        }

        // Controller is at top center: pos is (minX + 2, minY + 4, minZ + 2)
        BlockPos origin = this.pos.add(-2, -4, -2);
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
                    BlockPos p = min.add(x, y, z);
                    BlockState bs = this.world.getBlockState(p);
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
                    BlockPos p = min.add(x, y, z);
                    BlockState bs = this.world.getBlockState(p);

                    if (bs.contains(TitaniumTankCasingBlock.FORMED)) {
                        this.world.setBlockState(p, bs.with(TitaniumTankCasingBlock.FORMED, true), Block.NOTIFY_ALL);
                    } else if (bs.contains(ReinforcedTankGlassBlock.FORMED)) {
                        this.world.setBlockState(p, bs.with(ReinforcedTankGlassBlock.FORMED, true), Block.NOTIFY_ALL);
                    } else if (bs.contains(TitaniumTankInboundPortBlock.FORMED)) {
                        this.world.setBlockState(p, bs.with(TitaniumTankInboundPortBlock.FORMED, true), Block.NOTIFY_ALL);
                    }

                    BlockEntity be = this.world.getBlockEntity(p);
                    if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                        casingBE.setMasterPos(this.pos);
                    }
                }
            }
        }

        // Formation Sound & Particles
        this.world.playSound(null, this.pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.BLOCKS, 1.2f, 0.8f);
        if (this.world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, this.pos.getX() + 0.5, this.pos.getY() - 1.5, this.pos.getZ() + 0.5, 40, 1.5, 1.5, 1.5, 0.1);
        }

        updateInteriorLavaBlocks();
        markDirty();
    }

    // ==========================================
    // ANTI-GRIEF DECONSTRUCTION & STEAM PURGE
    // ==========================================
    public void dismantleStructure() {
        if (!this.isFormed || this.world == null || this.minPos == null) return;

        // Emergency Steam Purge: vaporize all interior fluid safely to air
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    BlockPos p = this.minPos.add(x, y, z);
                    BlockState bs = this.world.getBlockState(p);
                    if (bs.getBlock() == Blocks.LAVA) {
                        this.world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
            }
        }

        // Steam Hiss Sound & Smoke Particles
        this.world.playSound(null, this.pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.2f);
        if (this.world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.pos.getX() + 0.5, this.pos.getY() - 2.0, this.pos.getZ() + 0.5, 50, 1.5, 1.5, 1.5, 0.05);
            sw.spawnParticles(ParticleTypes.SMOKE, this.pos.getX() + 0.5, this.pos.getY() - 2.0, this.pos.getZ() + 0.5, 60, 1.5, 1.5, 1.5, 0.08);
        }

        // Unlink all member blocks
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    BlockPos p = this.minPos.add(x, y, z);
                    BlockState bs = this.world.getBlockState(p);

                    if (bs.contains(TitaniumTankCasingBlock.FORMED)) {
                        this.world.setBlockState(p, bs.with(TitaniumTankCasingBlock.FORMED, false), Block.NOTIFY_ALL);
                    } else if (bs.contains(ReinforcedTankGlassBlock.FORMED)) {
                        this.world.setBlockState(p, bs.with(ReinforcedTankGlassBlock.FORMED, false), Block.NOTIFY_ALL);
                    } else if (bs.contains(TitaniumTankInboundPortBlock.FORMED)) {
                        this.world.setBlockState(p, bs.with(TitaniumTankInboundPortBlock.FORMED, false), Block.NOTIFY_ALL);
                    }

                    BlockEntity be = this.world.getBlockEntity(p);
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
        markDirty();
        if (this.world != null) {
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    // ==========================================
    // INTERIOR FLUID LEVEL & CLIENT SYNC
    // ==========================================
    public void updateInteriorLavaBlocks() {
        if (!this.isFormed || this.world == null || this.minPos == null || this.world.isClient()) return;

        // Safely clear any legacy physical lava blocks to air so fluid is smoothly rendered by BER
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    BlockPos p = this.minPos.add(x, y, z);
                    BlockState current = this.world.getBlockState(p);
                    if (current.getBlock() == Blocks.LAVA) {
                        this.world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
            }
        }

        this.world.updateListeners(this.pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
    }

    // ==========================================
    // TICK LOGIC: BUCKET HANDLING & INTEGRITY
    // ==========================================
    public static void tick(ServerWorld world, BlockPos pos, BlockState state, TitaniumTankControllerBlockEntity entity) {
        boolean dirty = false;

        if (entity.isFormed) {
            // Periodic structure integrity check every 40 ticks
            if (world.getTime() % 40 == 0) {
                if (entity.minPos == null || !entity.validateStructureAt(entity.minPos)) {
                    entity.dismantleStructure();
                    return;
                }
            }

            // 1. Manual Bucket In (Fill tank from Lava Bucket)
            ItemStack input = entity.inventory.get(BUCKET_IN_SLOT);
            ItemStack output = entity.inventory.get(BUCKET_OUT_SLOT);

            if (!input.isEmpty() && input.isOf(Items.LAVA_BUCKET)) {
                if (entity.lavaAmount + 1000 <= CAPACITY && (entity.currentFluid == MoltenMetal.LAVA || entity.lavaAmount == 0) && (output.isEmpty() || (output.isOf(Items.BUCKET) && output.getCount() < output.getMaxCount()))) {
                    entity.currentFluid = MoltenMetal.LAVA;
                    entity.lavaAmount += 1000;
                    input.decrement(1);
                    if (output.isEmpty()) {
                        entity.inventory.set(BUCKET_OUT_SLOT, new ItemStack(Items.BUCKET));
                    } else {
                        output.increment(1);
                    }
                    entity.updateInteriorLavaBlocks();
                    dirty = true;
                }
            }
            // 2. Manual Bucket Out (Drain tank into Empty Bucket - Lava only)
            else if (!input.isEmpty() && input.isOf(Items.BUCKET)) {
                if (entity.lavaAmount >= 1000 && entity.currentFluid == MoltenMetal.LAVA && (output.isEmpty() || (output.isOf(Items.LAVA_BUCKET) && output.getCount() < output.getMaxCount()))) {
                    entity.lavaAmount -= 1000;
                    if (entity.lavaAmount <= 0) {
                        entity.currentFluid = MoltenMetal.NONE;
                    }
                    input.decrement(1);
                    if (output.isEmpty()) {
                        entity.inventory.set(BUCKET_OUT_SLOT, new ItemStack(Items.LAVA_BUCKET));
                    } else {
                        output.increment(1);
                    }
                    entity.updateInteriorLavaBlocks();
                    dirty = true;
                }
            }
        }

        if (dirty) {
            entity.markDirty();
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
        if (this.lavaAmount > 0 && this.currentFluid != MoltenMetal.NONE && this.currentFluid != MoltenMetal.LAVA) {
            return false;
        }
        return this.lavaAmount < CAPACITY;
    }

    @Override
    public boolean canInsertFluid(MoltenMetal metal) {
        if (!this.isFormed || metal == MoltenMetal.NONE) return false;
        if (this.lavaAmount > 0 && this.currentFluid != MoltenMetal.NONE && this.currentFluid != metal) {
            return false;
        }
        return this.lavaAmount < CAPACITY;
    }

    @Override
    public int insertLava(int amount, boolean simulate) {
        if (!this.isFormed || amount <= 0) return 0;
        if (this.currentFluid != MoltenMetal.NONE && this.currentFluid != MoltenMetal.LAVA && this.lavaAmount > 0) return 0;
        int space = CAPACITY - this.lavaAmount;
        int inserted = Math.min(space, amount);
        if (!simulate && inserted > 0) {
            this.currentFluid = MoltenMetal.LAVA;
            this.lavaAmount += inserted;
            updateInteriorLavaBlocks();
            markDirty();
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
            markDirty();
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
        if (this.currentFluid != MoltenMetal.NONE && this.currentFluid != metal && this.lavaAmount > 0) return 0;
        int space = CAPACITY - this.lavaAmount;
        int inserted = Math.min(space, amount);
        if (!simulate && inserted > 0) {
            this.currentFluid = metal;
            this.lavaAmount += inserted;
            updateInteriorLavaBlocks();
            markDirty();
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
            markDirty();
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
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack res = Inventories.splitStack(inventory, slot, amount);
        if (!res.isEmpty()) markDirty();
        return res;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{BUCKET_IN_SLOT, BUCKET_OUT_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == BUCKET_IN_SLOT;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == BUCKET_OUT_SLOT;
    }

    @Override
    public Text getDisplayName() {
        if (this.currentFluid != null && this.currentFluid != MoltenMetal.NONE) {
            return Text.literal("5x5 " + this.currentFluid.getDisplayName() + " Tank");
        }
        return Text.literal("5x5 Titanium Multi-Fluid Tank");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TitaniumTankScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("LavaAmount", this.lavaAmount);
        view.putString("FluidType", this.currentFluid.getId());
        view.putBoolean("IsFormed", this.isFormed);
        if (this.minPos != null) {
            view.putInt("MinX", this.minPos.getX());
            view.putInt("MinY", this.minPos.getY());
            view.putInt("MinZ", this.minPos.getZ());
        }
        Inventories.writeData(view, this.inventory);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.lavaAmount = view.getInt("LavaAmount", 0);
        if (view.contains("FluidType")) {
            this.currentFluid = MoltenMetal.fromId(view.getString("FluidType", "none"));
        } else {
            this.currentFluid = (this.lavaAmount > 0) ? MoltenMetal.LAVA : MoltenMetal.NONE;
        }
        if (this.lavaAmount <= 0) {
            this.currentFluid = MoltenMetal.NONE;
        }
        this.isFormed = view.getBoolean("IsFormed", false);
        if (view.contains("MinX") && view.contains("MinY") && view.contains("MinZ")) {
            this.minPos = new BlockPos(view.getInt("MinX", 0), view.getInt("MinY", 0), view.getInt("MinZ", 0));
        } else {
            this.minPos = null;
        }
        Inventories.readData(view, this.inventory);
    }

    public int getStoredFluidAmount() {
        return this.lavaAmount;
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }
}
