package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.RoadPaverMk2Block;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.RoadPaverMk2ScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class RoadPaverMk2BlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int ENERGY_CAPACITY = 60_000;
    public static final int ENERGY_PER_STEP = 80;
    public static final int MAX_FUEL = 5_000;
    public static final int STEP_INTERVAL = 25; // Faster paving (1.25 seconds per step)
    public static final int PILLAR_INTERVAL = 5; // Support pillars built every 5 blocks

    // Slots 0-8: Road Deck (Asphalt/Slabs/Clay)
    // Slots 9-11: Pillar / Pier Materials (Stone Bricks/Cobble/Deepslate)
    // Slot 12: Battery Slot (FE)
    // Slot 13: Engine Fuel Slot (Gasoline/Biofuel)
    public static final int INVENTORY_SIZE = 14;
    public static final int ROAD_SLOTS_START = 0;
    public static final int ROAD_SLOTS_END = 9;
    public static final int PILLAR_SLOTS_START = 9;
    public static final int PILLAR_SLOTS_END = 12;
    public static final int BATTERY_SLOT = 12;
    public static final int FUEL_SLOT = 13;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 1000, 1000, 0);

    private int paveTimer = 0;
    private int fuelLevel = 0;
    private boolean isPaving = false;
    private int pavedSteps = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> paveTimer;
                case 1 -> STEP_INTERVAL;
                case 2 -> energyStorage.getEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 6 -> isPaving ? 1 : 0;
                case 7 -> fuelLevel;
                case 8 -> MAX_FUEL;
                case 9 -> pavedSteps;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> paveTimer = value;
                case 2 -> {
                    int current = energyStorage.getEnergy();
                    int high = current & 0xFFFF0000;
                    energyStorage.setEnergy(high | (value & 0xFFFF));
                }
                case 3 -> {
                    int current = energyStorage.getEnergy();
                    int low = current & 0xFFFF;
                    energyStorage.setEnergy(low | ((value & 0xFFFF) << 16));
                }
                case 6 -> isPaving = value == 1;
                case 7 -> fuelLevel = value;
                case 9 -> pavedSteps = value;
            }
        }

        @Override
        public int size() {
            return 10;
        }
    };

    public RoadPaverMk2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROAD_PAVER_MK2_BLOCK_ENTITY, pos, state);
    }

    public DefaultedList<ItemStack> getInventory() {
        return inventory;
    }

    public int getFuelLevel() {
        return fuelLevel;
    }

    public int getPavedSteps() {
        return pavedSteps;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.road_paver_mk2");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new RoadPaverMk2ScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.paveTimer = view.getInt("PaveTimer", 0);
        this.fuelLevel = view.getInt("FuelLevel", 0);
        this.isPaving = view.getBoolean("IsPaving", false);
        this.pavedSteps = view.getInt("PavedSteps", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("PaveTimer", this.paveTimer);
        view.putInt("FuelLevel", this.fuelLevel);
        view.putBoolean("IsPaving", this.isPaving);
        view.putInt("PavedSteps", this.pavedSteps);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, RoadPaverMk2BlockEntity entity) {
        boolean stateChanged = false;

        // 1. Battery charging (FE) from Slot 12
        ItemStack batteryStack = entity.inventory.get(BATTERY_SLOT);
        if (!batteryStack.isEmpty()) {
            EnergyStorage batteryStorage = null;
            if (batteryStack.getItem() instanceof ItemEnergyProvider itemProvider) {
                batteryStorage = itemProvider.getEnergyStorage(batteryStack);
            } else if (batteryStack.getItem() instanceof EnergyProvider provider) {
                batteryStorage = provider.getEnergyStorage(null);
            }

            if (batteryStorage != null && batteryStorage.getEnergy() > 0 && entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                int needed = entity.energyStorage.getMaxEnergy() - entity.energyStorage.getEnergy();
                int extracted = batteryStorage.extractEnergy(Math.min(needed, 1000), false);
                entity.energyStorage.insertEnergy(extracted, false);
                stateChanged = true;
            }
        }

        // 2. Engine Fuel processing (Gasoline, Biofuel, High-Octane, Coal) in Slot 13
        if (entity.processFuel()) {
            stateChanged = true;
        }

        // 3. Paving Validation: Must not be powered by redstone, has road materials, has FE power, has fuel
        boolean hasRedstone = world.isReceivingRedstonePower(pos);
        Direction facing = state.get(RoadPaverMk2Block.FACING);
        int availableAsphalt = entity.countAsphalt();

        boolean hasPower = entity.energyStorage.getEnergy() >= ENERGY_PER_STEP;
        boolean hasFuel = entity.fuelLevel > 0;

        if (!hasRedstone && availableAsphalt >= 3 && hasPower && hasFuel) {
            entity.isPaving = true;
            entity.paveTimer++;

            if (entity.paveTimer >= STEP_INTERVAL) {
                entity.paveTimer = 0;
                entity.energyStorage.extractEnergy(ENERGY_PER_STEP, false);
                entity.fuelLevel--;
                entity.paveRoadAhead(world, pos, facing);
            }
            stateChanged = true;
        } else {
            entity.isPaving = false;
            if (entity.paveTimer > 0) {
                entity.paveTimer = 0;
                stateChanged = true;
            }
        }

        if (state.get(RoadPaverMk2Block.PAVING) != entity.isPaving) {
            world.setBlockState(pos, state.with(RoadPaverMk2Block.PAVING, entity.isPaving), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            markDirty(world, pos, state);
        }
    }

    private boolean processFuel() {
        ItemStack fuelStack = inventory.get(FUEL_SLOT);
        if (!fuelStack.isEmpty() && this.fuelLevel <= MAX_FUEL - 200) {
            if (fuelStack.isOf(ModItems.GASOLINE_CANISTER)) {
                this.fuelLevel = Math.min(MAX_FUEL, this.fuelLevel + 1000);
                fuelStack.decrement(1);
                this.returnEmptyCanister();
                return true;
            } else if (fuelStack.isOf(ModItems.BIOFUEL_CANISTER)) {
                this.fuelLevel = Math.min(MAX_FUEL, this.fuelLevel + 600);
                fuelStack.decrement(1);
                this.returnEmptyCanister();
                return true;
            } else if (fuelStack.isOf(ModItems.HIGH_OCTANE_FUEL_CANISTER)) {
                this.fuelLevel = Math.min(MAX_FUEL, this.fuelLevel + 1500);
                fuelStack.decrement(1);
                this.returnEmptyCanister();
                return true;
            } else if (fuelStack.isOf(net.minecraft.item.Items.COAL) || fuelStack.isOf(net.minecraft.item.Items.CHARCOAL) || fuelStack.isOf(ModItems.COKE_COAL)) {
                this.fuelLevel = Math.min(MAX_FUEL, this.fuelLevel + 200);
                fuelStack.decrement(1);
                return true;
            }
        }
        return false;
    }

    private void returnEmptyCanister() {
        ItemStack fuelStack = inventory.get(FUEL_SLOT);
        if (fuelStack.isEmpty()) {
            inventory.set(FUEL_SLOT, new ItemStack(ModItems.EMPTY_GAS_CANISTER));
        } else if (this.getWorld() instanceof ServerWorld serverWorld) {
            Block.dropStack(serverWorld, this.getPos(), new ItemStack(ModItems.EMPTY_GAS_CANISTER));
        }
    }

    private int countAsphalt() {
        int count = 0;
        for (int i = ROAD_SLOTS_START; i < ROAD_SLOTS_END; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isOf(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.isOf(ModBlocks.ASPHALT_SLAB.asItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private ItemStack consumeOneAsphalt() {
        for (int i = ROAD_SLOTS_START; i < ROAD_SLOTS_END; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isOf(ModBlocks.ASPHALT_SLAB.asItem())) {
                return stack.split(1);
            }
        }
        for (int i = ROAD_SLOTS_START; i < ROAD_SLOTS_END; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isOf(ModBlocks.ASPHALT_BLOCK.asItem())) {
                return stack.split(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean consumeOneClay() {
        for (int i = ROAD_SLOTS_START; i < ROAD_SLOTS_END; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isOf(ModBlocks.CONCRETE_CURB.asItem())) {
                stack.decrement(1);
                return true;
            }
        }
        for (int i = ROAD_SLOTS_START; i < ROAD_SLOTS_END; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isOf(net.minecraft.item.Items.CLAY_BALL)) {
                stack.decrement(1);
                return true;
            }
        }
        for (int i = ROAD_SLOTS_START; i < ROAD_SLOTS_END; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isOf(net.minecraft.item.Items.CLAY)) {
                stack.decrement(1);
                ItemStack remainder = new ItemStack(net.minecraft.item.Items.CLAY_BALL, 3);
                for (int j = ROAD_SLOTS_START; j < ROAD_SLOTS_END; j++) {
                    if (remainder.isEmpty()) break;
                    ItemStack target = inventory.get(j);
                    if (target.isEmpty()) {
                        inventory.set(j, remainder);
                        break;
                    } else if (target.isOf(net.minecraft.item.Items.CLAY_BALL) && target.getCount() < 64) {
                        int toAdd = Math.min(remainder.getCount(), 64 - target.getCount());
                        target.increment(toAdd);
                        remainder.decrement(toAdd);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private BlockState getPillarBlock() {
        // 1. Check pillar inventory slots 9-11
        for (int i = PILLAR_SLOTS_START; i < PILLAR_SLOTS_END; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                stack.decrement(1);
                return block.getDefaultState();
            }
        }
        // 2. Default high-durability reinforced stone bridge pier
        return Blocks.STONE_BRICKS.getDefaultState();
    }

    private void paveRoadAhead(ServerWorld world, BlockPos pos, Direction facing) {
        this.pavedSteps++;
        Direction leftDir = facing.rotateYCounterclockwise();
        Direction rightDir = facing.rotateYClockwise();
        BlockPos aheadCenter = pos.offset(facing);

        // 5 Columns: [-2: Left Curb, -1: Left Asphalt, 0: Center Asphalt, +1: Right Asphalt, +2: Right Curb]
        BlockPos curbLeftPos = aheadCenter.offset(leftDir, 2);
        BlockPos[] asphaltPositions = new BlockPos[]{
                aheadCenter.offset(leftDir, 1),
                aheadCenter,
                aheadCenter.offset(rightDir, 1)
        };
        BlockPos curbRightPos = aheadCenter.offset(rightDir, 2);

        // 1. Clear and Pave Left Curb (-2)
        clearPath(world, curbLeftPos);
        BlockPos groundLeft = curbLeftPos.down();
        if (consumeOneClay()) {
            world.setBlockState(groundLeft, ModBlocks.CONCRETE_CURB.getDefaultState().with(net.enchantedwood.block.custom.ConcreteCurbBlock.FACING, leftDir), 3);
        }

        // 2. Clear and Pave Center Asphalt Columns (-1, 0, 1)
        for (BlockPos roadPos : asphaltPositions) {
            clearPath(world, roadPos);
            BlockPos groundPos = roadPos.down();
            ItemStack placedItem = consumeOneAsphalt();
            if (!placedItem.isEmpty()) {
                if (placedItem.isOf(ModBlocks.ASPHALT_SLAB.asItem())) {
                    world.setBlockState(groundPos, ModBlocks.ASPHALT_SLAB.getDefaultState().with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.BOTTOM), 3);
                } else {
                    world.setBlockState(groundPos, ModBlocks.ASPHALT_BLOCK.getDefaultState(), 3);
                }
            }
        }

        // 3. Clear and Pave Right Curb (+2)
        clearPath(world, curbRightPos);
        BlockPos groundRight = curbRightPos.down();
        if (consumeOneClay()) {
            world.setBlockState(groundRight, ModBlocks.CONCRETE_CURB.getDefaultState().with(net.enchantedwood.block.custom.ConcreteCurbBlock.FACING, rightDir), 3);
        }

        // 4. Structural Sub-Deck Girders (reinforces bridge deck over air/water)
        BlockPos[] allGround = new BlockPos[]{ groundLeft, asphaltPositions[0].down(), aheadCenter.down(), asphaltPositions[2].down(), groundRight };
        for (BlockPos gPos : allGround) {
            BlockPos girderPos = gPos.down();
            if (world.isAir(girderPos) || world.getBlockState(girderPos).isLiquid()) {
                world.setBlockState(girderPos, Blocks.STONE_BRICKS.getDefaultState(), 3);
            }
        }

        // 5. Automated Support Pillar Casting Every 5 Blocks
        if (this.pavedSteps % PILLAR_INTERVAL == 0) {
            castSupportPillar(world, groundLeft.down());
            castSupportPillar(world, groundRight.down());
        }

        // 6. Move Paver forward if path is clear
        BlockPos nextPaverPos = pos.offset(facing);
        if (world.isAir(nextPaverPos)) {
            BlockState currentState = world.getBlockState(pos);
            DefaultedList<ItemStack> savedInventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                savedInventory.set(i, inventory.get(i).copy());
            }
            int savedEnergy = energyStorage.getEnergy();
            int savedFuel = this.fuelLevel;
            int savedSteps = this.pavedSteps;

            inventory.clear();
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

            world.setBlockState(nextPaverPos, currentState, 3);
            BlockEntity newEntity = world.getBlockEntity(nextPaverPos);
            if (newEntity instanceof RoadPaverMk2BlockEntity paver) {
                for (int i = 0; i < INVENTORY_SIZE; i++) {
                    paver.inventory.set(i, savedInventory.get(i));
                }
                paver.energyStorage.setEnergy(savedEnergy);
                paver.fuelLevel = savedFuel;
                paver.pavedSteps = savedSteps;
                paver.markDirty();
            }
        }
    }

    private void castSupportPillar(ServerWorld world, BlockPos startPos) {
        // Only build pillar if startPos is over air or water
        BlockPos firstCheck = startPos.down();
        if (!world.isAir(firstCheck) && !world.getBlockState(firstCheck).isLiquid()) {
            return;
        }

        // Project downwards up to 48 blocks until solid foundation is reached
        for (int dy = 1; dy <= 48; dy++) {
            BlockPos pillarPos = startPos.down(dy);
            if (pillarPos.getY() <= world.getBottomY() + 1) {
                break;
            }

            BlockState current = world.getBlockState(pillarPos);
            if (world.isAir(pillarPos) || current.isLiquid()) {
                BlockState pillarState = getPillarBlock();
                world.setBlockState(pillarPos, pillarState, 3);
            } else {
                // Anchored into solid terrain
                break;
            }
        }
    }

    private void clearPath(ServerWorld world, BlockPos pos) {
        BlockPos clearPos1 = pos;
        BlockPos clearPos2 = pos.up();
        if (!world.isAir(clearPos1) && world.getBlockState(clearPos1).getBlock() != ModBlocks.ROAD_PAVER_MK2) {
            world.breakBlock(clearPos1, true);
        }
        if (!world.isAir(clearPos2)) {
            world.breakBlock(clearPos2, true);
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot < ROAD_SLOTS_END) {
            return stack.isOf(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.isOf(ModBlocks.ASPHALT_SLAB.asItem())
                    || stack.isOf(net.minecraft.item.Items.CLAY_BALL) || stack.isOf(net.minecraft.item.Items.CLAY)
                    || stack.isOf(ModBlocks.CONCRETE_CURB.asItem());
        } else if (slot < PILLAR_SLOTS_END) {
            return stack.getItem() instanceof BlockItem;
        } else if (slot == BATTERY_SLOT) {
            return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
        } else if (slot == FUEL_SLOT) {
            return stack.isOf(ModItems.GASOLINE_CANISTER) || stack.isOf(ModItems.BIOFUEL_CANISTER)
                    || stack.isOf(ModItems.HIGH_OCTANE_FUEL_CANISTER) || stack.isOf(net.minecraft.item.Items.COAL)
                    || stack.isOf(net.minecraft.item.Items.CHARCOAL) || stack.isOf(ModItems.COKE_COAL);
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == FUEL_SLOT && stack.isOf(ModItems.EMPTY_GAS_CANISTER);
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : inventory) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(inventory, slot, amount);
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
}
