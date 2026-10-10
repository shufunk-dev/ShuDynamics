package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.RoadPaverBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.RoadPaverScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class RoadPaverBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int ENERGY_CAPACITY = 40_000;
    public static final int ENERGY_PER_STEP = 50;
    public static final int MAX_FUEL = 3_000;
    public static final int STEP_INTERVAL = 30; // 1.5 seconds per paved step

    // Slots 0-8: Asphalt supply, Slot 9: Battery slot (FE), Slot 10: Engine Fuel slot
    public static final int INVENTORY_SIZE = 11;
    public static final int BATTERY_SLOT = 9;
    public static final int FUEL_SLOT = 10;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);

    private int paveTimer = 0;
    private int fuelLevel = 0;
    private boolean isPaving = false;

    protected final ContainerData propertyDelegate = new ContainerData() {
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
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public RoadPaverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROAD_PAVER_BLOCK_ENTITY, pos, state);
    }

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    public int getFuelLevel() {
        return fuelLevel;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enchantedwood.road_paver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new RoadPaverScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.paveTimer = view.getIntOr("PaveTimer", 0);
        this.fuelLevel = view.getIntOr("FuelLevel", 0);
        this.isPaving = view.getBooleanOr("IsPaving", false);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("PaveTimer", this.paveTimer);
        view.putInt("FuelLevel", this.fuelLevel);
        view.putBoolean("IsPaving", this.isPaving);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, RoadPaverBlockEntity entity) {
        boolean stateChanged = false;

        // 1. Battery charging (FE)
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
                int extracted = batteryStorage.extractEnergy(Math.min(needed, 500), false);
                entity.energyStorage.insertEnergy(extracted, false);
                stateChanged = true;
            }
        }

        // 2. Engine Fuel processing (Gasoline, Biofuel, High-Octane, Coal)
        if (entity.processFuel()) {
            stateChanged = true;
        }

        // 3. Check if active and unpowered by redstone (redstone signal pauses paver)
        boolean hasRedstone = world.hasNeighborSignal(pos);
        Direction facing = state.getValue(RoadPaverBlock.FACING);
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

        if (state.getValue(RoadPaverBlock.PAVING) != entity.isPaving) {
            world.setBlock(pos, state.setValue(RoadPaverBlock.PAVING, entity.isPaving), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(world, pos, state);
        }
    }

    private boolean processFuel() {
        ItemStack fuelStack = inventory.get(FUEL_SLOT);
        if (!fuelStack.isEmpty() && this.fuelLevel <= MAX_FUEL - 200) {
            if (fuelStack.is(ModItems.GASOLINE_CANISTER)) {
                this.fuelLevel = Math.min(MAX_FUEL, this.fuelLevel + 1000);
                fuelStack.shrink(1);
                this.returnEmptyCanister();
                return true;
            } else if (fuelStack.is(ModItems.BIOFUEL_CANISTER)) {
                this.fuelLevel = Math.min(MAX_FUEL, this.fuelLevel + 600);
                fuelStack.shrink(1);
                this.returnEmptyCanister();
                return true;
            } else if (fuelStack.is(ModItems.HIGH_OCTANE_FUEL_CANISTER)) {
                this.fuelLevel = Math.min(MAX_FUEL, this.fuelLevel + 1500);
                fuelStack.shrink(1);
                this.returnEmptyCanister();
                return true;
            } else if (fuelStack.is(net.minecraft.world.item.Items.COAL) || fuelStack.is(net.minecraft.world.item.Items.CHARCOAL) || fuelStack.is(ModItems.COKE_COAL)) {
                this.fuelLevel = Math.min(MAX_FUEL, this.fuelLevel + 200);
                fuelStack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private void returnEmptyCanister() {
        ItemStack fuelStack = inventory.get(FUEL_SLOT);
        if (fuelStack.isEmpty()) {
            inventory.set(FUEL_SLOT, new ItemStack(ModItems.EMPTY_GAS_CANISTER));
        } else if (this.getLevel() instanceof ServerLevel serverWorld) {
            Block.popResource(serverWorld, this.getBlockPos(), new ItemStack(ModItems.EMPTY_GAS_CANISTER));
        }
    }

    private int countAsphalt() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.is(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.is(ModBlocks.ASPHALT_SLAB.asItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countClay() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.is(net.minecraft.world.item.Items.CLAY_BALL) || stack.is(ModBlocks.CONCRETE_CURB.asItem())) {
                count += stack.getCount();
            } else if (stack.is(net.minecraft.world.item.Items.CLAY)) {
                count += stack.getCount() * 4;
            }
        }
        return count;
    }

    private ItemStack consumeOneAsphalt() {
        // Prioritize slabs if available, otherwise full blocks
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.is(ModBlocks.ASPHALT_SLAB.asItem())) {
                return stack.split(1);
            }
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.is(ModBlocks.ASPHALT_BLOCK.asItem())) {
                return stack.split(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean consumeOneClay() {
        // 1. Direct concrete curb
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.is(ModBlocks.CONCRETE_CURB.asItem())) {
                stack.shrink(1);
                return true;
            }
        }
        // 2. Raw clay ball
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.is(net.minecraft.world.item.Items.CLAY_BALL)) {
                stack.shrink(1);
                return true;
            }
        }
        // 3. Raw clay block
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.is(net.minecraft.world.item.Items.CLAY)) {
                stack.shrink(1);
                // Return 3 clay balls to remaining slots if possible
                ItemStack remainder = new ItemStack(net.minecraft.world.item.Items.CLAY_BALL, 3);
                for (int j = 0; j < 9; j++) {
                    if (remainder.isEmpty()) break;
                    ItemStack target = inventory.get(j);
                    if (target.isEmpty()) {
                        inventory.set(j, remainder);
                        break;
                    } else if (target.is(net.minecraft.world.item.Items.CLAY_BALL) && target.getCount() < 64) {
                        int toAdd = Math.min(remainder.getCount(), 64 - target.getCount());
                        target.grow(toAdd);
                        remainder.shrink(toAdd);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void paveRoadAhead(ServerLevel world, BlockPos pos, Direction facing) {
        Direction leftDir = facing.getCounterClockWise();
        Direction rightDir = facing.getClockWise();
        BlockPos aheadCenter = pos.relative(facing);

        // 5 Columns: [-2: Left Curb, -1: Left Asphalt, 0: Center Asphalt, +1: Right Asphalt, +2: Right Curb]
        BlockPos[] curbLeftPositions = new BlockPos[]{ aheadCenter.relative(leftDir, 2) };
        BlockPos[] asphaltPositions = new BlockPos[]{
                aheadCenter.relative(leftDir, 1),
                aheadCenter,
                aheadCenter.relative(rightDir, 1)
        };
        BlockPos[] curbRightPositions = new BlockPos[]{ aheadCenter.relative(rightDir, 2) };

        // 1. Clear and Pave Left Curb (-2)
        for (BlockPos curbPos : curbLeftPositions) {
            clearPath(world, curbPos);
            BlockPos groundPos = curbPos.below();
            if (consumeOneClay()) {
                world.setBlock(groundPos, ModBlocks.CONCRETE_CURB.defaultBlockState().setValue(net.enchantedwood.block.custom.ConcreteCurbBlock.FACING, leftDir), 3);
            }
        }

        // 2. Clear and Pave Center Asphalt Columns (-1, 0, 1)
        for (BlockPos roadPos : asphaltPositions) {
            clearPath(world, roadPos);
            BlockPos groundPos = roadPos.below();
            ItemStack placedItem = consumeOneAsphalt();
            if (!placedItem.isEmpty()) {
                if (placedItem.is(ModBlocks.ASPHALT_SLAB.asItem())) {
                    world.setBlock(groundPos, ModBlocks.ASPHALT_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.BOTTOM), 3);
                } else {
                    world.setBlock(groundPos, ModBlocks.ASPHALT_BLOCK.defaultBlockState(), 3);
                }
            }
        }

        // 3. Clear and Pave Right Curb (+2)
        for (BlockPos curbPos : curbRightPositions) {
            clearPath(world, curbPos);
            BlockPos groundPos = curbPos.below();
            if (consumeOneClay()) {
                world.setBlock(groundPos, ModBlocks.CONCRETE_CURB.defaultBlockState().setValue(net.enchantedwood.block.custom.ConcreteCurbBlock.FACING, rightDir), 3);
            }
        }

        // Move paver forward by 1 block if path is clear
        BlockPos nextPaverPos = pos.relative(facing);
        if (world.isEmptyBlock(nextPaverPos)) {
            BlockState currentState = world.getBlockState(pos);
            NonNullList<ItemStack> savedInventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                savedInventory.set(i, inventory.get(i).copy());
            }
            int savedEnergy = energyStorage.getEnergy();
            int savedFuel = this.fuelLevel;

            // Remove old block without dropping items
            inventory.clear();
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            // Place in new position
            world.setBlock(nextPaverPos, currentState, 3);
            BlockEntity newEntity = world.getBlockEntity(nextPaverPos);
            if (newEntity instanceof RoadPaverBlockEntity paver) {
                for (int i = 0; i < INVENTORY_SIZE; i++) {
                    paver.inventory.set(i, savedInventory.get(i));
                }
                paver.energyStorage.setEnergy(savedEnergy);
                paver.fuelLevel = savedFuel;
                paver.setChanged();
            }
        }
    }

    private void clearPath(ServerLevel world, BlockPos pos) {
        BlockPos clearPos1 = pos;
        BlockPos clearPos2 = pos.above();
        if (!world.isEmptyBlock(clearPos1) && world.getBlockState(clearPos1).getBlock() != ModBlocks.ROAD_PAVER) {
            world.destroyBlock(clearPos1, true);
        }
        if (!world.isEmptyBlock(clearPos2)) {
            world.destroyBlock(clearPos2, true);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot < 9) {
            return stack.is(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.is(ModBlocks.ASPHALT_SLAB.asItem())
                    || stack.is(net.minecraft.world.item.Items.CLAY_BALL) || stack.is(net.minecraft.world.item.Items.CLAY)
                    || stack.is(ModBlocks.CONCRETE_CURB.asItem());
        }
        if (slot == BATTERY_SLOT) {
            return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
        }
        if (slot == FUEL_SLOT) {
            return stack.is(ModItems.GASOLINE_CANISTER) || stack.is(ModItems.BIOFUEL_CANISTER)
                    || stack.is(ModItems.HIGH_OCTANE_FUEL_CANISTER) || stack.is(net.minecraft.world.item.Items.COAL)
                    || stack.is(net.minecraft.world.item.Items.CHARCOAL) || stack.is(ModItems.COKE_COAL);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == FUEL_SLOT && stack.is(ModItems.EMPTY_GAS_CANISTER);
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(inventory, slot, amount);
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
}
