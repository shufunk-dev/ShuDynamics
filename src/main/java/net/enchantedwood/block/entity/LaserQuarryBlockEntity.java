package net.enchantedwood.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.custom.LaserQuarryBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.LaserQuarryScreenHandler;
import net.enchantedwood.util.ItemTransportHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LaserQuarryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
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

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
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

    // Remote network binding via Wrench
    private @Nullable BlockPos boundNetworkPos = null;
    private String boundDimension = "minecraft:overworld";

    // Client-side synced range radius
    private int clientRangeRadius = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
            return 10;
        }
    };

    public LaserQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_QUARRY_BLOCK_ENTITY, pos, state);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, LaserQuarryBlockEntity quarry) {
        if (!quarry.initializedScan) {
            quarry.resetScanCoordinates(state);
            quarry.initializedScan = true;
        }

        boolean wasLit = state.get(LaserQuarryBlock.LIT);
        boolean isMining = false;

        // Auto-eject items periodically
        if (world.getTime() % 10 == 0) {
            quarry.ejectOutputBuffer(world);
        }

        // Keep chunk tickets synchronized
        if (world.getTime() % 20 == 0) {
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
            world.setBlockState(pos, state.with(LaserQuarryBlock.LIT, isMining), Block.NOTIFY_ALL);
        }
    }

    public static void clientTick(World world, BlockPos pos, BlockState state, LaserQuarryBlockEntity quarry) {
        if (world.getTime() % 2 != 0) return;

        int[] bounds = quarry.getMiningChunkBounds(state);
        double minX = bounds[0] * 16.0;
        double maxX = bounds[1] * 16.0 + 16.0;
        double minZ = bounds[2] * 16.0;
        double maxZ = bounds[3] * 16.0 + 16.0;
        double laserY = pos.getY() + 0.2;

        boolean isLit = state.get(LaserQuarryBlock.LIT);

        // Core scanning beam when actively mining
        if (isLit) {
            world.addParticleClient(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.1, 0.0);
            world.addParticleClient(net.minecraft.particle.ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0.0, -0.1, 0.0);
        }

        // Perimeter neon laser boundary lines (step every 3 blocks for high continuous visibility)
        for (double x = minX; x <= maxX; x += 3.0) {
            world.addParticleClient(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, x, laserY, minZ, 0.0, 0.0, 0.0);
            world.addParticleClient(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, x, laserY, maxZ, 0.0, 0.0, 0.0);
        }
        for (double z = minZ; z <= maxZ; z += 3.0) {
            world.addParticleClient(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, minX, laserY, z, 0.0, 0.0, 0.0);
            world.addParticleClient(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, maxX, laserY, z, 0.0, 0.0, 0.0);
        }

        // 4 Corner vertical boundary beacons
        double[][] corners = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}};
        for (double[] corner : corners) {
            for (double yOff = 0; yOff <= 8.0; yOff += 2.0) {
                world.addParticleClient(net.minecraft.particle.ParticleTypes.END_ROD, corner[0], laserY + yOff, corner[1], 0.0, 0.01, 0.0);
            }
        }
    }

    public int[] getMiningChunkBounds() {
        return getMiningChunkBounds(null);
    }

    public int[] getMiningChunkBounds(@org.jetbrains.annotations.Nullable BlockState state) {
        ChunkPos originChunk = new ChunkPos(this.pos);
        int radius = getRangeChunkRadius();
        if (radius <= 0) {
            return new int[]{originChunk.x, originChunk.x, originChunk.z, originChunk.z};
        }

        int span = radius * 2; // 2 for 3x3, 4 for 5x5
        Direction facing = Direction.NORTH;
        if (state != null && state.contains(LaserQuarryBlock.FACING)) {
            facing = state.get(LaserQuarryBlock.FACING);
        } else if (this.world != null) {
            BlockState cached = getCachedState();
            if (cached != null && cached.contains(LaserQuarryBlock.FACING)) {
                facing = cached.get(LaserQuarryBlock.FACING);
            }
        }

        int minChunkX, maxChunkX, minChunkZ, maxChunkZ;

        switch (facing) {
            case NORTH -> {
                // Forward is -Z, Right is +X. Quarry chunk is South-West corner
                minChunkX = originChunk.x;
                maxChunkX = originChunk.x + span;
                minChunkZ = originChunk.z - span;
                maxChunkZ = originChunk.z;
            }
            case SOUTH -> {
                // Forward is +Z, Right is -X. Quarry chunk is North-East corner
                minChunkX = originChunk.x - span;
                maxChunkX = originChunk.x;
                minChunkZ = originChunk.z;
                maxChunkZ = originChunk.z + span;
            }
            case EAST -> {
                // Forward is +X, Left is -Z. Quarry chunk is South-West corner
                minChunkX = originChunk.x;
                maxChunkX = originChunk.x + span;
                minChunkZ = originChunk.z - span;
                maxChunkZ = originChunk.z;
            }
            case WEST -> {
                // Copy what South does for Z so it extends positive (+Z) and negative (-X)
                minChunkX = originChunk.x - span;
                maxChunkX = originChunk.x;
                minChunkZ = originChunk.z;
                maxChunkZ = originChunk.z + span;
            }
            default -> {
                minChunkX = originChunk.x;
                maxChunkX = originChunk.x + span;
                minChunkZ = originChunk.z - span;
                maxChunkZ = originChunk.z;
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
        this.scanY = this.pos.getY() - 1;
        markDirty();
    }

    public int getRangeChunkRadius() {
        ItemStack rangeStack = this.inventory.get(RANGE_SLOT);
        if (!rangeStack.isEmpty()) {
            if (rangeStack.isOf(ModItems.RANGE_UPGRADE_T2)) return 2; // 5x5 chunks
            if (rangeStack.isOf(ModItems.RANGE_UPGRADE_T1)) return 1; // 3x3 chunks
        }
        return this.clientRangeRadius;
    }

    public int getMiningDelayTicks() {
        ItemStack speedStack = this.inventory.get(SPEED_SLOT);
        if (!speedStack.isEmpty()) {
            if (speedStack.isOf(ModItems.BLAZE_OVERCLOCK_CORE)) return 1;  // 20 blocks/s
            if (speedStack.isOf(ModItems.TITANIUM_GEAR) || speedStack.isOf(ModItems.ENCHANTED_TITANIUM_GEAR)) return 3;
            if (speedStack.isOf(ModItems.DIAMOND_GEAR) || speedStack.isOf(ModItems.ENCHANTED_DIAMOND_GEAR)) return 5;
            if (speedStack.isOf(ModItems.GOLD_GEAR) || speedStack.isOf(ModItems.ENCHANTED_GOLD_GEAR)) return 8;
            if (speedStack.isOf(ModItems.IRON_GEAR) || speedStack.isOf(ModItems.ENCHANTED_IRON_GEAR)) return 12;
            if (speedStack.isOf(ModItems.COPPER_GEAR) || speedStack.isOf(ModItems.ENCHANTED_COPPER_GEAR)) return 16;
        }
        return 20; // 1 block/s default
    }

    private boolean performMiningStep(ServerWorld world) {
        int[] bounds = getMiningChunkBounds();
        int minX = bounds[0] * 16;
        int maxX = bounds[1] * 16 + 15;
        int minZ = bounds[2] * 16;
        int maxZ = bounds[3] * 16 + 15;
        int minY = world.getBottomY();

        if (this.scanY < minY) {
            return false; // Reached bedrock limit
        }

        // Unearth the Cosmic Singularity Anomaly when striking the deepest bedrock layers
        if (!this.anomalyUnearthed && this.scanY <= minY + 2) {
            ItemStack anomaly = new ItemStack(net.enchantedwood.item.ModItems.MYSTERY_KEYSTONE);
            if (canFitDrops(List.of(anomaly))) {
                depositDrops(List.of(anomaly));
                this.anomalyUnearthed = true;
                markDirty();

                if (world.getServer() != null) {
                    world.getServer().getPlayerManager().broadcast(
                            Text.literal("§5[ShuDynamics] §d✦ Cosmic Anomaly Extracted! The Laser Quarry has pierced deep bedrock and unearthed an unstable singularity! Check its tooltip for stabilization methods."),
                            false
                    );
                }
                world.playSound(null, this.pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.BLOCKS, 1.5f, 0.6f);
            }
        }

        // Fast-scan air/unbreakables up to 48 positions per tick
        for (int attempts = 0; attempts < 48; attempts++) {
            BlockPos targetPos = new BlockPos(this.scanX, this.scanY, this.scanZ);
            BlockState targetState = world.getBlockState(targetPos);

            boolean isOre = isTargetOre(targetState);
            boolean isBreakable = targetState.getHardness(world, targetPos) >= 0 && !targetState.isAir();

            if (this.mode == MODE_ORE_ONLY) {
                if (isOre) {
                    return mineBlockAt(world, targetPos, targetState, true);
                }
            } else {
                // Safety: never mine the single block column directly beneath the quarry itself so it never floats on pure air
                if (targetPos.getX() == this.pos.getX() && targetPos.getZ() == this.pos.getZ()) {
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
                markDirty();
            }
        }
    }

    private boolean mineBlockAt(ServerWorld world, BlockPos targetPos, BlockState state, boolean oreOnlyMode) {
        // Energy check
        if (this.energyStorage.getEnergy() < ENERGY_PER_BLOCK && !drawNetworkPower()) {
            return false;
        }

        // Get drops
        List<ItemStack> drops = calculateDrops(world, targetPos, state);
        if (!canFitDrops(drops)) {
            return false; // Output buffer full
        }

        // Deduct energy
        if (this.energyStorage.getEnergy() >= ENERGY_PER_BLOCK) {
            this.energyStorage.extractEnergy(ENERGY_PER_BLOCK, false);
        }

        // Insert drops
        depositDrops(drops);

        // Replace or destroy block
        if (oreOnlyMode) {
            BlockState filler = (targetPos.getY() <= 0) ? Blocks.DEEPSLATE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState();
            world.setBlockState(targetPos, filler, Block.NOTIFY_ALL);
        } else {
            world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        this.currentTargetPos = targetPos;
        this.totalMinedCount++;
        world.playSound(null, this.pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.4f, 1.2f);
        markDirty();

        return true;
    }

    private List<ItemStack> calculateDrops(ServerWorld world, BlockPos pos, BlockState state) {
        ItemStack extractionStack = this.inventory.get(EXTRACTION_SLOT);
        if (!extractionStack.isEmpty()) {
            if (extractionStack.isOf(ModItems.SILK_TOUCH_CORE)) {
                Item item = state.getBlock().asItem();
                if (item != Items.AIR) {
                    return List.of(new ItemStack(item));
                }
            } else if (extractionStack.isOf(ModItems.FORTUNE_CORE)) {
                // Fortune III simulation
                ItemStack fakePickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT)
                        .flatMap(reg -> reg.getOptional(Enchantments.FORTUNE))
                        .ifPresent(fortuneEntry -> fakePickaxe.addEnchantment(fortuneEntry, 3));
                return Block.getDroppedStacks(state, world, pos, null, null, fakePickaxe);
            }
        }
        return Block.getDroppedStacks(state, world, pos, null);
    }

    private boolean isTargetOre(BlockState state) {
        if (state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES) ||
                state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.DIAMOND_ORES) ||
                state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES) ||
                state.isIn(BlockTags.EMERALD_ORES) || state.isIn(BlockTags.COPPER_ORES)) {
            return true;
        }

        String blockId = state.getBlock().getTranslationKey().toLowerCase();
        return blockId.contains("ore") || blockId.contains("debris") || blockId.contains("ancient");
    }

    private boolean canFitDrops(List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            int count = drop.getCount();
            for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_SIZE; i++) {
                ItemStack slotStack = this.inventory.get(i);
                if (slotStack.isEmpty()) {
                    count = 0;
                    break;
                } else if (ItemStack.areItemsAndComponentsEqual(slotStack, drop)) {
                    int space = slotStack.getMaxCount() - slotStack.getCount();
                    count -= space;
                    if (count <= 0) break;
                }
            }
            if (count > 0) return false;
        }
        return true;
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
                    } else if (ItemStack.areItemsAndComponentsEqual(slotStack, remaining)) {
                        int take = Math.min(remaining.getCount(), slotStack.getMaxCount() - slotStack.getCount());
                        slotStack.increment(take);
                        remaining.decrement(take);
                        if (remaining.isEmpty()) break;
                    }
                }
            }
        }
    }

    private void ejectOutputBuffer(ServerWorld world) {
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = this.pos.offset(dir);
            Inventory targetInv = ItemTransportHelper.getInventoryAt(world, targetPos);
            if (targetInv != null && !(targetInv instanceof LaserQuarryBlockEntity)) {
                for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_SIZE; i++) {
                    ItemStack stack = this.inventory.get(i);
                    if (!stack.isEmpty()) {
                        ItemStack remainder = ItemTransportHelper.insertItem(targetInv, stack, dir.getOpposite());
                        this.inventory.set(i, remainder);
                        markDirty();
                    }
                }
            }
        }
    }

    public void handleAction(int actionId) {
        switch (actionId) {
            case 0 -> { // Toggle Mode
                this.mode = (this.mode == MODE_ORE_ONLY) ? MODE_EXCAVATE : MODE_ORE_ONLY;
                markDirty();
            }
            case 1 -> { // Toggle Pause
                this.isPaused = !this.isPaused;
                if (this.world instanceof ServerWorld sw) {
                    updateChunkLoading(sw);
                }
                markDirty();
            }
            case 2 -> { // Reset Scan
                resetScanCoordinates();
                if (this.world instanceof ServerWorld sw) {
                    updateChunkLoading(sw);
                }
            }
        }
    }

    public boolean hasLocalInterdimensionalCard() {
        return this.inventory.get(EXTRACTION_SLOT).isOf(ModItems.INTERDIMENSIONAL_CARD);
    }

    public boolean hasLocalChunkLoader() {
        return this.inventory.get(EXTRACTION_SLOT).isOf(ModItems.CHUNK_LOADER_MODULE) || hasLocalInterdimensionalCard();
    }

    public int getNetworkStatusCode() {
        if (this.world == null) return 0;
        if (!isBoundToRemote()) {
            return getNetworkTerminal() != null ? 1 : 0;
        }
        boolean isCrossDim = !this.boundDimension.equals(this.world.getRegistryKey().getValue().toString());
        if (this.world.getServer() == null) return 0;
        RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(this.boundDimension));
        ServerWorld targetWorld = this.world.getServer().getWorld(dimKey);
        if (targetWorld == null) return 4;

        BlockEntity be = targetWorld.getBlockEntity(this.boundNetworkPos);
        if (be == null) return 4;

        EnchantedStorageControllerBlockEntity ctrl = null;
        if (be instanceof EnchantedStorageControllerBlockEntity c) {
            ctrl = c;
        } else if (be instanceof EnchantedStorageTerminalBlockEntity) {
            ctrl = findControllerNear(targetWorld, this.boundNetworkPos);
        }

        if (isCrossDim && !hasLocalInterdimensionalCard() && (ctrl == null || !ctrl.hasInterdimensionalCard())) {
            return 3; // Cross-dimension link requires Interdimensional Card
        }

        if (getNetworkTerminal() != null) {
            return isCrossDim ? 2 : 1;
        }

        return 4; // Offline / Unloaded
    }

    public void updateChunkLoading(ServerWorld world) {
        if ((!this.isPaused && this.scanY >= world.getBottomY()) || hasLocalChunkLoader()) {
            int[] bounds = getMiningChunkBounds();
            Set<Long> targetChunks = new HashSet<>();
            for (int cx = bounds[0]; cx <= bounds[1]; cx++) {
                for (int cz = bounds[2]; cz <= bounds[3]; cz++) {
                    targetChunks.add(ChunkPos.toLong(cx, cz));
                }
            }

            // Unload chunks no longer needed (e.g. if range upgrade removed)
            for (long chunkPosLong : new HashSet<>(this.forcedChunks)) {
                if (!targetChunks.contains(chunkPosLong)) {
                    int cx = ChunkPos.getPackedX(chunkPosLong);
                    int cz = ChunkPos.getPackedZ(chunkPosLong);
                    world.setChunkForced(cx, cz, false);
                    this.forcedChunks.remove(chunkPosLong);
                }
            }

            // Load new chunks
            for (long chunkPosLong : targetChunks) {
                if (!this.forcedChunks.contains(chunkPosLong)) {
                    int cx = ChunkPos.getPackedX(chunkPosLong);
                    int cz = ChunkPos.getPackedZ(chunkPosLong);
                    world.setChunkForced(cx, cz, true);
                    this.forcedChunks.add(chunkPosLong);
                }
            }
        } else {
            releaseChunkTickets(world);
        }
    }

    public void releaseChunkTickets(ServerWorld world) {
        for (long chunkPosLong : this.forcedChunks) {
            int cx = ChunkPos.getPackedX(chunkPosLong);
            int cz = ChunkPos.getPackedZ(chunkPosLong);
            world.setChunkForced(cx, cz, false);
        }
        this.forcedChunks.clear();
    }

    @Override
    public void markRemoved() {
        if (this.world instanceof ServerWorld serverWorld) {
            releaseChunkTickets(serverWorld);
        }
        super.markRemoved();
    }

    public void bindNetwork(BlockPos pos, String dimension) {
        this.boundNetworkPos = pos;
        this.boundDimension = dimension;
        markDirty();
    }

    public void unbindNetwork() {
        this.boundNetworkPos = null;
        markDirty();
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

    private boolean drawNetworkPower() {
        if (this.world == null) return false;

        // 1. Check bound remote network
        if (this.boundNetworkPos != null && this.world.getServer() != null) {
            boolean isCrossDim = !this.boundDimension.equals(this.world.getRegistryKey().getValue().toString());
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(this.boundDimension));
            ServerWorld targetWorld = this.world.getServer().getWorld(dimKey);
            if (targetWorld != null) {
                BlockEntity be = targetWorld.getBlockEntity(this.boundNetworkPos);
                EnchantedStorageControllerBlockEntity ctrl = null;

                if (be instanceof EnchantedStorageControllerBlockEntity c) {
                    ctrl = c;
                } else if (be instanceof EnchantedStorageTerminalBlockEntity) {
                    ctrl = findControllerNear(targetWorld, this.boundNetworkPos);
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
                }
            }
        }

        // 2. Fallback to local 16-block proximity
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
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
        if (this.world == null) return null;

        // 1. Check bound remote network
        if (this.boundNetworkPos != null && this.world.getServer() != null) {
            boolean isCrossDim = !this.boundDimension.equals(this.world.getRegistryKey().getValue().toString());
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(this.boundDimension));
            ServerWorld targetWorld = this.world.getServer().getWorld(dimKey);
            if (targetWorld != null) {
                BlockEntity be = targetWorld.getBlockEntity(this.boundNetworkPos);
                if (be instanceof EnchantedStorageTerminalBlockEntity terminal && terminal.isNetworkOnline()) {
                    if (isCrossDim && !hasLocalInterdimensionalCard()) {
                        EnchantedStorageControllerBlockEntity ctrl = findControllerNear(targetWorld, this.boundNetworkPos);
                        if (ctrl == null || !ctrl.hasInterdimensionalCard()) return null;
                    }
                    return terminal;
                } else if (be instanceof EnchantedStorageControllerBlockEntity ctrl && ctrl.isOnline()) {
                    if (isCrossDim && !hasLocalInterdimensionalCard() && !ctrl.hasInterdimensionalCard()) {
                        return null;
                    }
                    // Search near controller for terminal
                    BlockPos.Mutable mut = new BlockPos.Mutable();
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
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageTerminalBlockEntity terminal && terminal.isNetworkOnline()) {
                        return terminal;
                    }
                }
            }
        }
        return null;
    }

    private @Nullable EnchantedStorageControllerBlockEntity findControllerNear(ServerWorld world, BlockPos center) {
        BlockPos.Mutable mut = new BlockPos.Mutable();
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
    public int size() {
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
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.inventory, slot, amount);
        if (!result.isEmpty()) {
            markDirty();
            if (this.world != null && !this.world.isClient()) {
                this.world.updateListeners(this.pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
            }
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(this.inventory, slot);
        if (!result.isEmpty()) {
            markDirty();
            if (this.world != null && !this.world.isClient()) {
                this.world.updateListeners(this.pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
            }
        }
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        markDirty();
        if (this.world != null && !this.world.isClient()) {
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        this.inventory.clear();
        markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        int[] slots = new int[OUTPUT_SIZE];
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            slots[i] = OUTPUT_START + i;
        }
        return slots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUT_SIZE;
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Digital Laser Quarry");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new LaserQuarryScreenHandler(syncId, playerInventory, this, this.propertyDelegate, this.pos);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.setEnergy(view.getInt("Energy", 0));
        this.mode = view.getInt("Mode", 0);
        this.isPaused = view.getBoolean("IsPaused", true);
        this.totalMinedCount = view.getInt("TotalMined", 0);
        this.scanX = view.getInt("ScanX", 0);
        this.scanY = view.getInt("ScanY", 0);
        this.scanZ = view.getInt("ScanZ", 0);
        this.initializedScan = view.getBoolean("InitializedScan", false);
        this.anomalyUnearthed = view.getBoolean("AnomalyUnearthed", false);
        this.clientRangeRadius = view.getInt("RangeRadius", 0);
        if (view.contains("BoundX")) {
            this.boundNetworkPos = new BlockPos(view.getInt("BoundX", 0), view.getInt("BoundY", 0), view.getInt("BoundZ", 0));
            this.boundDimension = view.getString("BoundDim", "minecraft:overworld");
        } else {
            this.boundNetworkPos = null;
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
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
    public net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.NbtCompound toInitialChunkDataNbt(net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
        nbt.putInt("RangeRadius", getRangeChunkRadius());
        return nbt;
    }
}
