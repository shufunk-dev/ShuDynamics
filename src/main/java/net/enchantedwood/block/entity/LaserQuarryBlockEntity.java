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
                case 9 -> getNetworkTerminal() != null ? (isBoundToRemote() ? 2 : 1) : 0;
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
            quarry.resetScanCoordinates();
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

    public void resetScanCoordinates() {
        int radius = getRangeChunkRadius();
        ChunkPos originChunk = new ChunkPos(this.pos);
        int minChunkX = originChunk.x - radius;
        int minChunkZ = originChunk.z - radius;

        this.scanX = minChunkX * 16;
        this.scanZ = minChunkZ * 16;
        this.scanY = this.pos.getY() - 1;
        markDirty();
    }

    public int getRangeChunkRadius() {
        ItemStack rangeStack = this.inventory.get(RANGE_SLOT);
        if (!rangeStack.isEmpty()) {
            if (rangeStack.isOf(ModItems.RANGE_UPGRADE_T2)) return 2; // 5x5 chunks
            if (rangeStack.isOf(ModItems.RANGE_UPGRADE_T1)) return 1; // 3x3 chunks
        }
        return 0; // 1x1 chunk
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
        int radius = getRangeChunkRadius();
        ChunkPos originChunk = new ChunkPos(this.pos);
        int minChunkX = originChunk.x - radius;
        int maxChunkX = originChunk.x + radius;
        int minChunkZ = originChunk.z - radius;
        int maxChunkZ = originChunk.z + radius;

        int minX = minChunkX * 16;
        int maxX = maxChunkX * 16 + 15;
        int minZ = minChunkZ * 16;
        int maxZ = maxChunkZ * 16 + 15;
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
                remaining = ItemTransportHelper.insertItem(terminal, remaining, null);
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

    public void updateChunkLoading(ServerWorld world) {
        if (!this.isPaused && this.scanY >= world.getBottomY()) {
            int radius = getRangeChunkRadius();
            ChunkPos originChunk = new ChunkPos(this.pos);
            Set<Long> targetChunks = new HashSet<>();
            for (int cx = originChunk.x - radius; cx <= originChunk.x + radius; cx++) {
                for (int cz = originChunk.z - radius; cz <= originChunk.z + radius; cz++) {
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
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(this.boundDimension));
            ServerWorld targetWorld = this.world.getServer().getWorld(dimKey);
            if (targetWorld != null && targetWorld.isChunkLoaded(this.boundNetworkPos.getX() >> 4, this.boundNetworkPos.getZ() >> 4)) {
                BlockEntity be = targetWorld.getBlockEntity(this.boundNetworkPos);
                if (be instanceof EnchantedStorageControllerBlockEntity ctrl) {
                    EnergyStorage storage = ctrl.getEnergyStorage(null);
                    if (storage != null && storage.getEnergy() >= ENERGY_PER_BLOCK) {
                        storage.extractEnergy(ENERGY_PER_BLOCK, false);
                        return true;
                    }
                } else if (be instanceof EnchantedStorageTerminalBlockEntity) {
                    // Search near terminal for controller
                    BlockPos.Mutable mut = new BlockPos.Mutable();
                    for (int dx = -16; dx <= 16; dx++) {
                        for (int dy = -8; dy <= 8; dy++) {
                            for (int dz = -16; dz <= 16; dz++) {
                                mut.set(this.boundNetworkPos.getX() + dx, this.boundNetworkPos.getY() + dy, this.boundNetworkPos.getZ() + dz);
                                BlockEntity candidate = targetWorld.getBlockEntity(mut);
                                if (candidate instanceof EnchantedStorageControllerBlockEntity ctrl) {
                                    EnergyStorage storage = ctrl.getEnergyStorage(null);
                                    if (storage != null && storage.getEnergy() >= ENERGY_PER_BLOCK) {
                                        storage.extractEnergy(ENERGY_PER_BLOCK, false);
                                        return true;
                                    }
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
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(this.boundDimension));
            ServerWorld targetWorld = this.world.getServer().getWorld(dimKey);
            if (targetWorld != null && targetWorld.isChunkLoaded(this.boundNetworkPos.getX() >> 4, this.boundNetworkPos.getZ() >> 4)) {
                BlockEntity be = targetWorld.getBlockEntity(this.boundNetworkPos);
                if (be instanceof EnchantedStorageTerminalBlockEntity terminal && terminal.isNetworkOnline()) {
                    return terminal;
                } else if (be instanceof EnchantedStorageControllerBlockEntity ctrl && ctrl.isOnline()) {
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
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(this.inventory, slot);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        markDirty();
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
        if (this.boundNetworkPos != null) {
            view.putInt("BoundX", this.boundNetworkPos.getX());
            view.putInt("BoundY", this.boundNetworkPos.getY());
            view.putInt("BoundZ", this.boundNetworkPos.getZ());
            view.putString("BoundDim", this.boundDimension);
        }
    }
}
