package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.LaserQuarryBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.LaserQuarryScreenHandler;
import net.enchantedwood.util.ItemTransportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LaserQuarryBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int INVENTORY_SIZE = 12;
    public static final int OUTPUT_START = 0;
    public static final int OUTPUT_SIZE = 9;
    public static final int SPEED_SLOT = 9;
    public static final int RANGE_SLOT = 10;
    public static final int EXTRACTION_SLOT = 11;

    public static final int MAX_ENERGY = 100_000;
    public static final int MAX_RECEIVE = 2_000;
    public static final int ENERGY_PER_BLOCK = 150;

    public static final int MODE_ORE_ONLY = 0;
    public static final int MODE_EXCAVATE = 1;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int mode = MODE_ORE_ONLY;
    private boolean isPaused = true;
    private int totalMinedCount = 0;

    // Scan coordinates
    private int scanX = 0;
    private int scanY = 0;
    private int scanZ = 0;
    private boolean initializedScan = false;
    private int tickDelay = 0;
    private boolean anomalyUnearthed = false;

    // Active chunk loading
    private final Set<Long> forcedChunks = new HashSet<>();

    // Last target block for client rendering
    private @Nullable BlockPos currentTargetPos = null;

    // Remote network binding via Wrench or Wireless Crystal
    private @Nullable BlockPos boundNetworkPos = null;
    private String boundDimension = "minecraft:overworld";
    private @Nullable String remoteForcedDimension = null;
    private final Set<Long> remoteForcedChunks = new HashSet<>();

    // Client-side synced range radius
    private int clientRangeRadius = 0;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> mode;
                case 5 -> isPaused ? 1 : 0;
                case 6 -> scanY;
                case 7 -> totalMinedCount;
                case 8 -> getRangeChunkRadius();
                case 9 -> getNetworkStatusCode();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0xFFFF0000) | (value & 0xFFFF));
                case 1 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
                case 4 -> mode = value;
                case 5 -> isPaused = (value == 1);
                case 6 -> scanY = value;
                case 7 -> totalMinedCount = value;
                case 8 -> clientRangeRadius = value;
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public LaserQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_QUARRY_BLOCK_ENTITY, pos, state);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, LaserQuarryBlockEntity quarry) {
        if (!quarry.initializedScan) {
            quarry.resetScanCoordinates(state);
            quarry.initializedScan = true;
        }

        boolean wasLit = state.getValue(LaserQuarryBlock.LIT);
        boolean isMining = false;

        // Auto-eject items periodically & flush to wireless network if online
        if (world.getGameTime() % 10 == 0) {
            quarry.flushBufferToNetwork();
            quarry.ejectOutputBuffer(world);
        }

        // Wirelessly recharge internal energy hold from base network if not full (even when paused)
        if (quarry.energyStorage.getEnergy() < quarry.energyStorage.getMaxEnergy()) {
            quarry.rechargeFromNetwork();
        }

        // Keep chunk tickets synchronized
        if (world.getGameTime() % 20 == 0) {
            quarry.updateChunkLoading(world);
        }

        if (!quarry.isPaused) {
            int speedDelay = quarry.getMiningDelayTicks();
            quarry.tickDelay++;

            if (quarry.tickDelay >= speedDelay) {
                quarry.tickDelay = 0;
                isMining = quarry.performMiningStep(world);
            }
        }

        if (wasLit != isMining) {
            world.setBlock(pos, state.setValue(LaserQuarryBlock.LIT, isMining), Block.UPDATE_ALL);
        }
    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, LaserQuarryBlockEntity quarry) {
        if (world.getGameTime() % 2 != 0) return;

        int[] bounds = quarry.getMiningChunkBounds(state);
        double minX = bounds[0] * 16.0;
        double maxX = bounds[1] * 16.0 + 16.0;
        double minZ = bounds[2] * 16.0;
        double maxZ = bounds[3] * 16.0 + 16.0;
        double laserY = pos.getY() + 0.2;

        boolean isLit = state.getValue(LaserQuarryBlock.LIT);

        // Core scanning beam when actively mining
        if (isLit) {
            world.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.1, 0.0);
            world.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0.0, -0.1, 0.0);
        }

        // Perimeter neon laser boundary lines (step every 3 blocks for high continuous visibility)
        for (double x = minX; x <= maxX; x += 3.0) {
            world.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, laserY, minZ, 0.0, 0.0, 0.0);
            world.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, laserY, maxZ, 0.0, 0.0, 0.0);
        }
        for (double z = minZ; z <= maxZ; z += 3.0) {
            world.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, minX, laserY, z, 0.0, 0.0, 0.0);
            world.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, maxX, laserY, z, 0.0, 0.0, 0.0);
        }

        // 4 Corner vertical boundary beacons
        double[][] corners = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}};
        for (double[] corner : corners) {
            for (double yOff = 0; yOff <= 8.0; yOff += 2.0) {
                world.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, corner[0], laserY + yOff, corner[1], 0.0, 0.01, 0.0);
            }
        }
    }

    public int[] getMiningChunkBounds() {
        return getMiningChunkBounds(null);
    }

    public int[] getMiningChunkBounds(@org.jetbrains.annotations.Nullable BlockState state) {
        ChunkPos originChunk = ChunkPos.containing(this.worldPosition);
        int radius = getRangeChunkRadius();
        if (radius <= 0) {
            return new int[]{originChunk.x(), originChunk.x(), originChunk.z(), originChunk.z()};
        }

        int span = radius * 2; // 2 for 3x3, 4 for 5x5
        Direction facing = Direction.NORTH;
        if (state != null && state.hasProperty(LaserQuarryBlock.FACING)) {
            facing = state.getValue(LaserQuarryBlock.FACING);
        } else if (this.level != null) {
            BlockState cached = getBlockState();
            if (cached != null && cached.hasProperty(LaserQuarryBlock.FACING)) {
                facing = cached.getValue(LaserQuarryBlock.FACING);
            }
        }

        int minChunkX, maxChunkX, minChunkZ, maxChunkZ;

        switch (facing) {
            case NORTH -> {
                // Forward is -Z, Right is +X. Quarry chunk is South-West corner
                minChunkX = originChunk.x();
                maxChunkX = originChunk.x() + span;
                minChunkZ = originChunk.z() - span;
                maxChunkZ = originChunk.z();
            }
            case SOUTH -> {
                // Forward is +Z, Right is -X. Quarry chunk is North-East corner
                minChunkX = originChunk.x() - span;
                maxChunkX = originChunk.x();
                minChunkZ = originChunk.z();
                maxChunkZ = originChunk.z() + span;
            }
            case EAST -> {
                // Forward is +X, Left is -Z. Quarry chunk is South-West corner
                minChunkX = originChunk.x();
                maxChunkX = originChunk.x() + span;
                minChunkZ = originChunk.z() - span;
                maxChunkZ = originChunk.z();
            }
            case WEST -> {
                // Copy what South does for Z so it extends positive (+Z) and negative (-X)
                minChunkX = originChunk.x() - span;
                maxChunkX = originChunk.x();
                minChunkZ = originChunk.z();
                maxChunkZ = originChunk.z() + span;
            }
            default -> {
                minChunkX = originChunk.x();
                maxChunkX = originChunk.x() + span;
                minChunkZ = originChunk.z() - span;
                maxChunkZ = originChunk.z();
            }
        }

        return new int[]{minChunkX, maxChunkX, minChunkZ, maxChunkZ};
    }

    public void resetScanCoordinates() {
        resetScanCoordinates(null);
    }

    public void resetScanCoordinates(@org.jetbrains.annotations.Nullable BlockState state) {
        int[] bounds = getMiningChunkBounds(state);
        this.scanX = bounds[0] * 16;
        this.scanZ = bounds[2] * 16;
        this.scanY = this.worldPosition.getY() - 1;
        setChanged();
    }

    public int getRangeChunkRadius() {
        ItemStack rangeStack = this.inventory.get(RANGE_SLOT);
        if (!rangeStack.isEmpty()) {
            if (rangeStack.is(ModItems.RANGE_UPGRADE_T2)) return 2; // 5x5 chunks
            if (rangeStack.is(ModItems.RANGE_UPGRADE_T1)) return 1; // 3x3 chunks
        }
        return this.clientRangeRadius;
    }

    public int getMiningDelayTicks() {
        ItemStack speedStack = this.inventory.get(SPEED_SLOT);
        if (!speedStack.isEmpty()) {
            if (speedStack.is(ModItems.BLAZE_OVERCLOCK_CORE)) return 1;  // 20 blocks/s
            if (speedStack.is(ModItems.TITANIUM_GEAR) || speedStack.is(ModItems.ENCHANTED_TITANIUM_GEAR)) return 3;
            if (speedStack.is(ModItems.DIAMOND_GEAR) || speedStack.is(ModItems.ENCHANTED_DIAMOND_GEAR)) return 5;
            if (speedStack.is(ModItems.GOLD_GEAR) || speedStack.is(ModItems.ENCHANTED_GOLD_GEAR)) return 8;
            if (speedStack.is(ModItems.IRON_GEAR) || speedStack.is(ModItems.ENCHANTED_IRON_GEAR)) return 12;
            if (speedStack.is(ModItems.COPPER_GEAR) || speedStack.is(ModItems.ENCHANTED_COPPER_GEAR)) return 16;
        }
        return 20; // 1 block/s default
    }

    private boolean performMiningStep(ServerLevel world) {
        int[] bounds = getMiningChunkBounds();
        int minX = bounds[0] * 16;
        int maxX = bounds[1] * 16 + 15;
        int minZ = bounds[2] * 16;
        int maxZ = bounds[3] * 16 + 15;
        int minY = world.getMinY();

        if (this.scanY < minY) {
            return false; // Reached bedrock limit
        }

        // Unearth the Cosmic Singularity Anomaly when striking the deepest bedrock layers
        if (!this.anomalyUnearthed && this.scanY <= minY + 2) {
            ItemStack anomaly = new ItemStack(net.enchantedwood.item.ModItems.MYSTERY_KEYSTONE);
            if (canFitDrops(List.of(anomaly))) {
                depositDrops(List.of(anomaly));
                this.anomalyUnearthed = true;
                setChanged();

                if (world.getServer() != null) {
                    world.getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal("§5[ShuDynamics] §d✦ Cosmic Anomaly Extracted! The Laser Quarry has pierced deep bedrock and unearthed an unstable singularity! Check its tooltip for stabilization methods."),
                            false
                    );
                }
                world.playSound(null, this.worldPosition, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.5f, 0.6f);
            }
        }

        // Fast-scan air/unbreakables up to 48 positions per tick
        for (int attempts = 0; attempts < 48; attempts++) {
            BlockPos targetPos = new BlockPos(this.scanX, this.scanY, this.scanZ);
            BlockState targetState = world.getBlockState(targetPos);

            boolean isOre = isTargetOre(targetState);
            boolean isBreakable = targetState.getDestroySpeed(world, targetPos) >= 0 && !targetState.isAir();

            if (this.mode == MODE_ORE_ONLY) {
                if (isOre) {
                    return mineBlockAt(world, targetPos, targetState, true);
                }
            } else {
                // Safety: never mine the single block column directly beneath the quarry itself so it never floats on pure air
                if (targetPos.getX() == this.worldPosition.getX() && targetPos.getZ() == this.worldPosition.getZ()) {
                    advanceCoordinates(minX, maxX, minZ, maxZ, minY);
                    continue;
                }
                if (isBreakable) {
                    return mineBlockAt(world, targetPos, targetState, false);
                }
            }

            // Advance coordinates
            advanceCoordinates(minX, maxX, minZ, maxZ, minY);
            if (this.scanY < minY) return false;
        }

        return false;
    }

    private void advanceCoordinates(int minX, int maxX, int minZ, int maxZ, int minY) {
        this.scanX++;
        if (this.scanX > maxX) {
            this.scanX = minX;
            this.scanZ++;
            if (this.scanZ > maxZ) {
                this.scanZ = minZ;
                this.scanY--;
                setChanged();
            }
        }
    }

    private boolean mineBlockAt(ServerLevel world, BlockPos targetPos, BlockState state, boolean oreOnlyMode) {
        // 1. Check drops and capacity FIRST before extracting any energy!
        List<ItemStack> drops = calculateDrops(world, targetPos, state);
        if (!canFitDrops(drops)) {
            return false; // Output buffer and wireless network full
        }

        // 2. Energy check & deduction
        if (this.energyStorage.getEnergy() >= ENERGY_PER_BLOCK) {
            this.energyStorage.extractEnergy(ENERGY_PER_BLOCK, false);
        } else if (drawNetworkPower()) {
            // Power successfully drawn from wireless base network
        } else {
            return false; // No energy available
        }

        // 3. Insert drops into network or output buffer
        depositDrops(drops);

        // 4. Replace or destroy block
        if (oreOnlyMode) {
            BlockState filler = (targetPos.getY() <= 0) ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
            world.setBlock(targetPos, filler, Block.UPDATE_ALL);
        } else {
            world.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        this.currentTargetPos = targetPos;
        this.totalMinedCount++;
        world.playSound(null, this.worldPosition, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.4f, 1.2f);
        setChanged();

        return true;
    }

    private List<ItemStack> calculateDrops(ServerLevel world, BlockPos pos, BlockState state) {
        ItemStack extractionStack = this.inventory.get(EXTRACTION_SLOT);
        if (!extractionStack.isEmpty()) {
            if (extractionStack.is(ModItems.SILK_TOUCH_CORE)) {
                Item item = state.getBlock().asItem();
                if (item != Items.AIR) {
                    return List.of(new ItemStack(item));
                }
            } else if (extractionStack.is(ModItems.FORTUNE_CORE)) {
                // Fortune III simulation
                ItemStack fakePickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                world.registryAccess().lookup(Registries.ENCHANTMENT)
                        .flatMap(reg -> reg.get(Enchantments.FORTUNE))
                        .ifPresent(fortuneEntry -> fakePickaxe.enchant(fortuneEntry, 3));
                return Block.getDrops(state, world, pos, null, null, fakePickaxe);
            }
        }
        return Block.getDrops(state, world, pos, null);
    }

    private boolean isTargetOre(BlockState state) {
        if (state.is(BlockTags.ORES) || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES)) {
            return true;
        }

        String blockId = state.getBlock().getDescriptionId().toLowerCase();
        return blockId.contains("ore") || blockId.contains("debris") || blockId.contains("ancient");
    }

    private boolean canFitDrops(List<ItemStack> drops) {
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        if (terminal != null && terminal.isNetworkOnline()) {
            long cap = terminal.getNetworkCapacityLong();
            long current = terminal.getTotalStoredItemCountLong();
            int dropCount = 0;
            for (ItemStack d : drops) {
                if (!d.isEmpty()) dropCount += d.getCount();
            }
            if (current + dropCount <= cap) {
                return true;
            }
        }
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            int count = drop.getCount();
            for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_SIZE; i++) {
                ItemStack slotStack = this.inventory.get(i);
                if (slotStack.isEmpty()) {
                    count = 0;
                    break;
                } else if (ItemStack.isSameItemSameComponents(slotStack, drop)) {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    count -= space;
                    if (count <= 0) break;
                }
            }
            if (count > 0) return false;
        }
        return true;
    }

    public void flushBufferToNetwork() {
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        if (terminal == null || !terminal.isNetworkOnline()) return;

        boolean changed = false;
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_SIZE; i++) {
            ItemStack stack = this.inventory.get(i);
            if (!stack.isEmpty()) {
                ItemStack remainder = terminal.depositItem(stack.copy());
                if (remainder.getCount() != stack.getCount()) {
                    this.inventory.set(i, remainder);
                    changed = true;
                }
            }
        }
        if (changed) {
            setChanged();
        }
    }

    private void depositDrops(List<ItemStack> drops) {
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            ItemStack remaining = drop.copy();

            // 1. Direct to terminal if network online
            if (terminal != null && terminal.isNetworkOnline()) {
                remaining = terminal.depositItem(remaining);
            }

            // 2. Fallback to output buffer
            if (!remaining.isEmpty()) {
                for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_SIZE; i++) {
                    ItemStack slotStack = this.inventory.get(i);
                    if (slotStack.isEmpty()) {
                        this.inventory.set(i, remaining.copy());
                        remaining = ItemStack.EMPTY;
                        break;
                    } else if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                        int take = Math.min(remaining.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
                        slotStack.grow(take);
                        remaining.shrink(take);
                        if (remaining.isEmpty()) break;
                    }
                }
            }
        }
    }

    private void ejectOutputBuffer(ServerLevel world) {
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = this.worldPosition.relative(dir);
            Container targetInv = ItemTransportHelper.getInventoryAt(world, targetPos);
            if (targetInv != null && !(targetInv instanceof LaserQuarryBlockEntity)) {
                for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_SIZE; i++) {
                    ItemStack stack = this.inventory.get(i);
                    if (!stack.isEmpty()) {
                        ItemStack remainder = ItemTransportHelper.insertItem(targetInv, stack, dir.getOpposite());
                        this.inventory.set(i, remainder);
                        setChanged();
                    }
                }
            }
        }
    }

    public void handleAction(int actionId) {
        switch (actionId) {
            case 0 -> { // Toggle Mode
                this.mode = (this.mode == MODE_ORE_ONLY) ? MODE_EXCAVATE : MODE_ORE_ONLY;
                setChanged();
            }
            case 1 -> { // Toggle Pause
                this.isPaused = !this.isPaused;
                if (this.level instanceof ServerLevel sw) {
                    updateChunkLoading(sw);
                }
                setChanged();
            }
            case 2 -> { // Reset Scan
                resetScanCoordinates();
                if (this.level instanceof ServerLevel sw) {
                    updateChunkLoading(sw);
                }
            }
        }
    }

    public boolean hasLocalInterdimensionalCard() {
        ItemStack stack = this.inventory.get(EXTRACTION_SLOT);
        return stack.is(ModItems.INTERDIMENSIONAL_CARD) || stack.is(ModItems.WIRELESS_STORAGE_CRYSTAL);
    }

    public boolean hasLocalChunkLoader() {
        ItemStack stack = this.inventory.get(EXTRACTION_SLOT);
        return stack.is(ModItems.CHUNK_LOADER_MODULE) || stack.is(ModItems.WIRELESS_STORAGE_CRYSTAL) || hasLocalInterdimensionalCard();
    }

    private void ensureChunksLoaded(ServerLevel targetWorld, BlockPos center, int radiusBlocks) {
        int minCx = (center.getX() - radiusBlocks) >> 4;
        int maxCx = (center.getX() + radiusBlocks) >> 4;
        int minCz = (center.getZ() - radiusBlocks) >> 4;
        int maxCz = (center.getZ() + radiusBlocks) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!targetWorld.hasChunk(cx, cz)) {
                    targetWorld.getChunk(cx, cz);
                }
            }
        }
    }

    public int getNetworkStatusCode() {
        if (this.level == null) return 0;
        BlockPos netPos = getBoundNetworkPos();
        String netDim = getBoundDimension();
        if (netPos == null) {
            return getNetworkTerminal() != null ? 1 : 0;
        }
        boolean isCrossDim = !netDim.equals(this.level.dimension().identifier().toString());
        if (this.level.getServer() == null) return 0;
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(netDim));
        ServerLevel targetWorld = this.level.getServer().getLevel(dimKey);
        if (targetWorld == null) return 4;

        ensureChunksLoaded(targetWorld, netPos, 16);

        BlockEntity be = targetWorld.getBlockEntity(netPos);
        if (be == null) return 4;

        EnchantedStorageControllerBlockEntity ctrl = null;
        if (be instanceof EnchantedStorageControllerBlockEntity c) {
            ctrl = c;
        } else if (be instanceof EnchantedStorageTerminalBlockEntity) {
            ctrl = findControllerNear(targetWorld, netPos);
        }

        if (isCrossDim && !hasLocalInterdimensionalCard() && (ctrl == null || !ctrl.hasInterdimensionalCard())) {
            return 3; // Cross-dimension link requires Interdimensional Card
        }

        if (getNetworkTerminal() != null) {
            return isCrossDim ? 2 : 1;
        }

        return 4; // Offline / Unloaded
    }

    public void updateChunkLoading(ServerLevel world) {
        if ((!this.isPaused && this.scanY >= world.getMinY()) || hasLocalChunkLoader()) {
            int[] bounds = getMiningChunkBounds();
            Set<Long> targetChunks = new HashSet<>();
            for (int cx = bounds[0]; cx <= bounds[1]; cx++) {
                for (int cz = bounds[2]; cz <= bounds[3]; cz++) {
                    targetChunks.add(ChunkPos.pack(cx, cz));
                }
            }

            // Unload chunks no longer needed (e.g. if range upgrade removed)
            for (long chunkPosLong : new HashSet<>(this.forcedChunks)) {
                if (!targetChunks.contains(chunkPosLong)) {
                    int cx = ChunkPos.getX(chunkPosLong);
                    int cz = ChunkPos.getZ(chunkPosLong);
                    world.setChunkForced(cx, cz, false);
                    this.forcedChunks.remove(chunkPosLong);
                }
            }

            // Load new chunks
            for (long chunkPosLong : targetChunks) {
                if (!this.forcedChunks.contains(chunkPosLong)) {
                    int cx = ChunkPos.getX(chunkPosLong);
                    int cz = ChunkPos.getZ(chunkPosLong);
                    world.setChunkForced(cx, cz, true);
                    this.forcedChunks.add(chunkPosLong);
                }
            }

            // Also keep remote target base chunks (3x3 area) loaded across dimensions if mining or chunk-loaded
            BlockPos netPos = getBoundNetworkPos();
            String netDim = getBoundDimension();
            if (netPos != null && world.getServer() != null) {
                boolean isCrossDim = !netDim.equals(world.dimension().identifier().toString());
                if (isCrossDim) {
                    ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(netDim));
                    ServerLevel targetWorld = world.getServer().getLevel(dimKey);
                    if (targetWorld != null) {
                        int rcx = netPos.getX() >> 4;
                        int rcz = netPos.getZ() >> 4;
                        Set<Long> targetRemote = new HashSet<>();
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                targetRemote.add(ChunkPos.pack(rcx + dx, rcz + dz));
                            }
                        }
                        if (!targetRemote.equals(this.remoteForcedChunks) || !netDim.equals(this.remoteForcedDimension)) {
                            releaseRemoteChunkTickets(world.getServer());
                            for (long cPos : targetRemote) {
                                targetWorld.setChunkForced(ChunkPos.getX(cPos), ChunkPos.getZ(cPos), true);
                            }
                            this.remoteForcedChunks.clear();
                            this.remoteForcedChunks.addAll(targetRemote);
                            this.remoteForcedDimension = netDim;
                        }
                    }
                } else {
                    releaseRemoteChunkTickets(world.getServer());
                }
            } else if (world.getServer() != null) {
                releaseRemoteChunkTickets(world.getServer());
            }
        } else {
            releaseChunkTickets(world);
        }
    }

    public void releaseChunkTickets(ServerLevel world) {
        for (long chunkPosLong : this.forcedChunks) {
            int cx = ChunkPos.getX(chunkPosLong);
            int cz = ChunkPos.getZ(chunkPosLong);
            world.setChunkForced(cx, cz, false);
        }
        this.forcedChunks.clear();
        if (world.getServer() != null) {
            releaseRemoteChunkTickets(world.getServer());
        }
    }

    public void releaseRemoteChunkTickets(net.minecraft.server.MinecraftServer server) {
        if (this.remoteForcedDimension != null && !this.remoteForcedChunks.isEmpty()) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(this.remoteForcedDimension));
            ServerLevel targetWorld = server.getLevel(dimKey);
            if (targetWorld != null) {
                for (long cPos : this.remoteForcedChunks) {
                    int rcx = ChunkPos.getX(cPos);
                    int rcz = ChunkPos.getZ(cPos);
                    targetWorld.setChunkForced(rcx, rcz, false);
                }
            }
            this.remoteForcedDimension = null;
            this.remoteForcedChunks.clear();
        }
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel serverWorld) {
            releaseChunkTickets(serverWorld);
        }
        super.setRemoved();
    }

    public void bindNetwork(BlockPos pos, String dimension) {
        this.boundNetworkPos = pos;
        this.boundDimension = dimension;
        setChanged();
        if (this.level instanceof ServerLevel sw) {
            updateChunkLoading(sw);
            sw.getChunkSource().blockChanged(this.worldPosition);
        }
    }

    public void unbindNetwork() {
        this.boundNetworkPos = null;
        setChanged();
        if (this.level instanceof ServerLevel sw) {
            updateChunkLoading(sw);
            sw.getChunkSource().blockChanged(this.worldPosition);
        }
    }

    public @Nullable BlockPos getBoundNetworkPos() {
        if (this.boundNetworkPos != null) return this.boundNetworkPos;
        ItemStack stack = this.inventory.get(EXTRACTION_SLOT);
        if (stack.is(ModItems.WIRELESS_STORAGE_CRYSTAL)) {
            CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
            if (comp != null) {
                CompoundTag nbt = comp.copyTag();
                if (nbt.contains("boundX")) {
                    return new BlockPos(nbt.getInt("boundX").orElse(0), nbt.getInt("boundY").orElse(0), nbt.getInt("boundZ").orElse(0));
                }
            }
        }
        return null;
    }

    public String getBoundDimension() {
        if (this.boundNetworkPos != null) return this.boundDimension;
        ItemStack stack = this.inventory.get(EXTRACTION_SLOT);
        if (stack.is(ModItems.WIRELESS_STORAGE_CRYSTAL)) {
            CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
            if (comp != null) {
                CompoundTag nbt = comp.copyTag();
                if (nbt.contains("boundDimension")) {
                    return nbt.getString("boundDimension").orElse("minecraft:overworld");
                }
            }
        }
        return this.boundDimension;
    }

    public boolean isBoundToRemote() {
        return getBoundNetworkPos() != null;
    }

    public void rechargeFromNetwork() {
        int needed = Math.min(MAX_RECEIVE, this.energyStorage.getMaxEnergy() - this.energyStorage.getEnergy());
        if (needed <= 0 || this.level == null) return;

        BlockPos netPos = getBoundNetworkPos();
        String netDim = getBoundDimension();
        if (netPos != null && this.level.getServer() != null) {
            boolean isCrossDim = !netDim.equals(this.level.dimension().identifier().toString());
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(netDim));
            ServerLevel targetWorld = this.level.getServer().getLevel(dimKey);
            if (targetWorld != null) {
                ensureChunksLoaded(targetWorld, netPos, 16);
                BlockEntity be = targetWorld.getBlockEntity(netPos);
                EnchantedStorageControllerBlockEntity ctrl = null;

                if (be instanceof EnchantedStorageControllerBlockEntity c) {
                    ctrl = c;
                } else if (be instanceof EnchantedStorageTerminalBlockEntity) {
                    ctrl = findControllerNear(targetWorld, netPos);
                }

                if (ctrl != null && ctrl.isOnline()) {
                    if (isCrossDim && !hasLocalInterdimensionalCard() && !ctrl.hasInterdimensionalCard()) {
                        return;
                    }
                    // 1. Extract from controller buffer
                    EnergyStorage storage = ctrl.getEnergyStorage(null);
                    if (storage != null && storage.getEnergy() > 0) {
                        int extracted = storage.extractEnergy(needed, false);
                        if (extracted > 0) {
                            this.energyStorage.insertEnergy(extracted, false);
                            needed -= extracted;
                            setChanged();
                        }
                    }
                    // 2. If more energy needed, check adjacent energy providers (batteries, cables, generators) next to controller
                    if (needed > 0) {
                        BlockPos ctrlPos = ctrl.getBlockPos();
                        for (Direction dir : Direction.values()) {
                            if (needed <= 0) break;
                            BlockEntity neighbor = targetWorld.getBlockEntity(ctrlPos.relative(dir));
                            if (neighbor instanceof EnergyProvider ep && !(neighbor instanceof LaserQuarryBlockEntity)) {
                                EnergyStorage nStorage = ep.getEnergyStorage(dir.getOpposite());
                                if (nStorage != null && nStorage.getEnergy() > 0) {
                                    int extracted = nStorage.extractEnergy(needed, false);
                                    if (extracted > 0) {
                                        this.energyStorage.insertEnergy(extracted, false);
                                        needed -= extracted;
                                        setChanged();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Local 16-block proximity fallback
            BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
            for (int dx = -16; dx <= 16; dx++) {
                if (needed <= 0) break;
                for (int dy = -8; dy <= 8; dy++) {
                    if (needed <= 0) break;
                    for (int dz = -16; dz <= 16; dz++) {
                        if (needed <= 0) break;
                        mut.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                        BlockEntity be = this.level.getBlockEntity(mut);
                        if (be instanceof EnchantedStorageControllerBlockEntity controller) {
                            EnergyStorage storage = controller.getEnergyStorage(null);
                            if (storage != null && storage.getEnergy() > 0) {
                                int extracted = storage.extractEnergy(needed, false);
                                if (extracted > 0) {
                                    this.energyStorage.insertEnergy(extracted, false);
                                    needed -= extracted;
                                    setChanged();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean drawNetworkPower() {
        if (this.level == null) return false;

        // 1. Check bound remote network
        BlockPos netPos = getBoundNetworkPos();
        String netDim = getBoundDimension();
        if (netPos != null && this.level.getServer() != null) {
            boolean isCrossDim = !netDim.equals(this.level.dimension().identifier().toString());
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(netDim));
            ServerLevel targetWorld = this.level.getServer().getLevel(dimKey);
            if (targetWorld != null) {
                ensureChunksLoaded(targetWorld, netPos, 16);
                BlockEntity be = targetWorld.getBlockEntity(netPos);
                EnchantedStorageControllerBlockEntity ctrl = null;

                if (be instanceof EnchantedStorageControllerBlockEntity c) {
                    ctrl = c;
                } else if (be instanceof EnchantedStorageTerminalBlockEntity) {
                    ctrl = findControllerNear(targetWorld, netPos);
                }

                if (ctrl != null && ctrl.isOnline()) {
                    if (isCrossDim && !hasLocalInterdimensionalCard() && !ctrl.hasInterdimensionalCard()) {
                        return false;
                    }
                    EnergyStorage storage = ctrl.getEnergyStorage(null);
                    if (storage != null && storage.getEnergy() >= ENERGY_PER_BLOCK) {
                        storage.extractEnergy(ENERGY_PER_BLOCK, false);
                        return true;
                    }
                    // Also check adjacent energy providers next to controller (e.g. battery, cable)
                    BlockPos ctrlPos = ctrl.getBlockPos();
                    for (Direction dir : Direction.values()) {
                        BlockEntity neighbor = targetWorld.getBlockEntity(ctrlPos.relative(dir));
                        if (neighbor instanceof EnergyProvider ep && !(neighbor instanceof LaserQuarryBlockEntity)) {
                            EnergyStorage nStorage = ep.getEnergyStorage(dir.getOpposite());
                            if (nStorage != null && nStorage.getEnergy() >= ENERGY_PER_BLOCK) {
                                nStorage.extractEnergy(ENERGY_PER_BLOCK, false);
                                return true;
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
                        EnergyStorage storage = controller.getEnergyStorage(null);
                        if (storage != null && storage.getEnergy() >= ENERGY_PER_BLOCK) {
                            storage.extractEnergy(ENERGY_PER_BLOCK, false);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private @Nullable EnchantedStorageTerminalBlockEntity getNetworkTerminal() {
        if (this.level == null) return null;

        // 1. Check bound remote network
        BlockPos netPos = getBoundNetworkPos();
        String netDim = getBoundDimension();
        if (netPos != null && this.level.getServer() != null) {
            boolean isCrossDim = !netDim.equals(this.level.dimension().identifier().toString());
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(netDim));
            ServerLevel targetWorld = this.level.getServer().getLevel(dimKey);
            if (targetWorld != null) {
                ensureChunksLoaded(targetWorld, netPos, 16);
                BlockEntity be = targetWorld.getBlockEntity(netPos);
                if (be instanceof EnchantedStorageTerminalBlockEntity terminal && terminal.isNetworkOnline()) {
                    if (isCrossDim && !hasLocalInterdimensionalCard()) {
                        EnchantedStorageControllerBlockEntity ctrl = findControllerNear(targetWorld, netPos);
                        if (ctrl == null || !ctrl.hasInterdimensionalCard()) return null;
                    }
                    return terminal;
                } else if (be instanceof EnchantedStorageControllerBlockEntity ctrl && ctrl.isOnline()) {
                    if (isCrossDim && !hasLocalInterdimensionalCard() && !ctrl.hasInterdimensionalCard()) {
                        return null;
                    }
                    // Search near controller for terminal
                    BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                    for (int dx = -16; dx <= 16; dx++) {
                        for (int dy = -8; dy <= 8; dy++) {
                            for (int dz = -16; dz <= 16; dz++) {
                                mut.set(netPos.getX() + dx, netPos.getY() + dy, netPos.getZ() + dz);
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
                    if (be instanceof EnchantedStorageTerminalBlockEntity terminal && terminal.isNetworkOnline()) {
                        return terminal;
                    }
                }
            }
        }
        return null;
    }

    private @Nullable EnchantedStorageControllerBlockEntity findControllerNear(ServerLevel world, BlockPos center) {
        ensureChunksLoaded(world, center, 16);
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockEntity candidate = world.getBlockEntity(mut);
                    if (candidate instanceof EnchantedStorageControllerBlockEntity c) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable BlockPos getCurrentTargetPos() {
        return this.currentTargetPos;
    }

    public int getScanX() { return this.scanX; }
    public int getScanY() { return this.scanY; }
    public int getScanZ() { return this.scanZ; }
    public int getMode() { return this.mode; }
    public boolean isPaused() { return this.isPaused; }
    public int getTotalMinedCount() { return this.totalMinedCount; }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : this.inventory) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.inventory, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.inventory, slot);
        if (!result.isEmpty()) {
            setChanged();
            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[OUTPUT_SIZE];
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            slots[i] = OUTPUT_START + i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUT_SIZE;
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Digital Laser Quarry");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new LaserQuarryScreenHandler(syncId, playerInventory, this, this.propertyDelegate, this.worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.setEnergy(view.getIntOr("Energy", 0));
        this.mode = view.getIntOr("Mode", 0);
        this.isPaused = view.getBooleanOr("IsPaused", true);
        this.totalMinedCount = view.getIntOr("TotalMined", 0);
        this.scanX = view.getIntOr("ScanX", 0);
        this.scanY = view.getIntOr("ScanY", 0);
        this.scanZ = view.getIntOr("ScanZ", 0);
        this.initializedScan = view.getBooleanOr("InitializedScan", false);
        this.anomalyUnearthed = view.getBooleanOr("AnomalyUnearthed", false);
        this.clientRangeRadius = view.getIntOr("RangeRadius", 0);
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
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("Energy", this.energyStorage.getEnergy());
        view.putInt("Mode", this.mode);
        view.putBoolean("IsPaused", this.isPaused);
        view.putInt("TotalMined", this.totalMinedCount);
        view.putInt("ScanX", this.scanX);
        view.putInt("ScanY", this.scanY);
        view.putInt("ScanZ", this.scanZ);
        view.putBoolean("InitializedScan", this.initializedScan);
        view.putBoolean("AnomalyUnearthed", this.anomalyUnearthed);
        view.putInt("RangeRadius", getRangeChunkRadius());
        if (this.boundNetworkPos != null) {
            view.putInt("BoundX", this.boundNetworkPos.getX());
            view.putInt("BoundY", this.boundNetworkPos.getY());
            view.putInt("BoundZ", this.boundNetworkPos.getZ());
            view.putString("BoundDim", this.boundDimension);
        }
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        nbt.putInt("RangeRadius", getRangeChunkRadius());
        return nbt;
    }
}
