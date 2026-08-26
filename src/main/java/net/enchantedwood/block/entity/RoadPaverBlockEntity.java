package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.RoadPaverBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.RoadPaverScreenHandler;
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

public class RoadPaverBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int ENERGY_CAPACITY = 40_000;
    public static final int ENERGY_PER_STEP = 50;
    public static final int MAX_FUEL = 3_000;
    public static final int STEP_INTERVAL = 30; // 1.5 seconds per paved step

    // Slots 0-8: Asphalt supply, Slot 9: Battery slot (FE), Slot 10: Engine Fuel slot
    public static final int INVENTORY_SIZE = 11;
    public static final int BATTERY_SLOT = 9;
    public static final int FUEL_SLOT = 10;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);

    private int paveTimer = 0;
    private int fuelLevel = 0;
    private boolean isPaving = false;

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
        public int size() {
            return 9;
        }
    };

    public RoadPaverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROAD_PAVER_BLOCK_ENTITY, pos, state);
    }

    public DefaultedList<ItemStack> getInventory() {
        return inventory;
    }

    public int getFuelLevel() {
        return fuelLevel;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.road_paver");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new RoadPaverScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
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
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("PaveTimer", this.paveTimer);
        view.putInt("FuelLevel", this.fuelLevel);
        view.putBoolean("IsPaving", this.isPaving);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, RoadPaverBlockEntity entity) {
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
        boolean hasRedstone = world.isReceivingRedstonePower(pos);
        Direction facing = state.get(RoadPaverBlock.FACING);
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

        if (state.get(RoadPaverBlock.PAVING) != entity.isPaving) {
            world.setBlockState(pos, state.with(RoadPaverBlock.PAVING, entity.isPaving), 3);
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
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isOf(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.isOf(ModBlocks.ASPHALT_SLAB.asItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeAsphalt(int amount) {
        int needed = amount;
        for (int i = 0; i < 9 && needed > 0; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isOf(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.isOf(ModBlocks.ASPHALT_SLAB.asItem())) {
                int take = Math.min(stack.getCount(), needed);
                stack.decrement(take);
                needed -= take;
            }
        }
    }

    private void paveRoadAhead(ServerWorld world, BlockPos pos, Direction facing) {
        Direction leftDir = facing.rotateYCounterclockwise();
        Direction rightDir = facing.rotateYClockwise();
        BlockPos aheadCenter = pos.offset(facing);

        BlockPos[] roadPositions = new BlockPos[]{
                aheadCenter.offset(leftDir),
                aheadCenter,
                aheadCenter.offset(rightDir)
        };

        // Pave all 3 columns
        for (BlockPos roadPos : roadPositions) {
            BlockPos groundPos = roadPos.down();
            BlockPos clearPos1 = roadPos;
            BlockPos clearPos2 = roadPos.up();

            // Clear obstructions
            if (!world.isAir(clearPos1) && world.getBlockState(clearPos1).getBlock() != ModBlocks.ROAD_PAVER) {
                world.breakBlock(clearPos1, true);
            }
            if (!world.isAir(clearPos2)) {
                world.breakBlock(clearPos2, true);
            }

            // Lay asphalt foundation
            world.setBlockState(groundPos, ModBlocks.ASPHALT_BLOCK.getDefaultState(), 3);
        }

        consumeAsphalt(3);

        // Move paver forward by 1 block if path is clear
        BlockPos nextPaverPos = pos.offset(facing);
        if (world.isAir(nextPaverPos)) {
            BlockState currentState = world.getBlockState(pos);
            DefaultedList<ItemStack> savedInventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                savedInventory.set(i, inventory.get(i).copy());
            }
            int savedEnergy = energyStorage.getEnergy();
            int savedFuel = this.fuelLevel;

            // Remove old block without dropping items
            inventory.clear();
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

            // Place in new position
            world.setBlockState(nextPaverPos, currentState, 3);
            BlockEntity newEntity = world.getBlockEntity(nextPaverPos);
            if (newEntity instanceof RoadPaverBlockEntity paver) {
                for (int i = 0; i < INVENTORY_SIZE; i++) {
                    paver.inventory.set(i, savedInventory.get(i));
                }
                paver.energyStorage.setEnergy(savedEnergy);
                paver.fuelLevel = savedFuel;
                paver.markDirty();
            }
        }
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot < 9) {
            return stack.isOf(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.isOf(ModBlocks.ASPHALT_SLAB.asItem());
        }
        if (slot == BATTERY_SLOT) {
            return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
        }
        if (slot == FUEL_SLOT) {
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
