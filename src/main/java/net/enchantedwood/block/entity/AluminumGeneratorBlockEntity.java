package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
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
import net.enchantedwood.block.custom.AluminumGeneratorBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.AluminumGeneratorScreenHandler;
import org.jetbrains.annotations.Nullable;

public class AluminumGeneratorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int BUFFER_CAPACITY = 1_000_000;
    public static final int GENERATION_RATE = 300; // 300 FE/t
    public static final int MAX_OUTPUT = 2_500;    // 2,500 FE/t

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(BUFFER_CAPACITY, GENERATION_RATE, MAX_OUTPUT, 0);

    private int burnTime = 0;
    private int totalBurnTime = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> burnTime;
                case 5 -> totalBurnTime;
                case 6 -> GENERATION_RATE;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 4 -> burnTime = value;
                case 5 -> totalBurnTime = value;
            }
        }

        @Override
        public int size() {
            return 7;
        }
    };

    public AluminumGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALUMINUM_GENERATOR_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.aluminum_generator");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AluminumGeneratorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, AluminumGeneratorBlockEntity entity) {
        boolean originallyBurning = entity.burnTime > 0;
        boolean stateChanged = false;

        // 1. Generation from fuel
        if (entity.burnTime > 0) {
            entity.burnTime--;
            if (entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                entity.energyStorage.insertEnergy(GENERATION_RATE, false);
                stateChanged = true;
            }
        }

        // 2. Start burning new fuel if buffer has space
        if (entity.burnTime <= 0 && entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
            ItemStack fuelStack = entity.inventory.get(0);
            if (!fuelStack.isEmpty()) {
                int fuelValue = CopperGeneratorBlockEntity.getFuelTime(world, fuelStack);
                if (fuelValue > 0) {
                    entity.burnTime = fuelValue;
                    entity.totalBurnTime = fuelValue;
                    ItemStack remainder = fuelStack.getRecipeRemainder();
                    fuelStack.decrement(1);
                    if (fuelStack.isEmpty() && !remainder.isEmpty()) {
                        entity.inventory.set(0, remainder.copy());
                    }
                    stateChanged = true;
                }
            }
        }

        // 3. Push energy to adjacent EnergyProviders
        if (entity.energyStorage.getEnergy() > 0) {
            int availableToOutput = Math.min(entity.energyStorage.getEnergy(), MAX_OUTPUT);
            for (Direction dir : Direction.values()) {
                if (availableToOutput <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                if (neighbor instanceof EnergyProvider provider) {
                    EnergyStorage receiver = provider.getEnergyStorage(dir.getOpposite());
                    if (receiver != null && receiver.canInsert()) {
                        int inserted = receiver.insertEnergy(availableToOutput, false);
                        if (inserted > 0) {
                            entity.energyStorage.extractEnergy(inserted, false);
                            availableToOutput -= inserted;
                            stateChanged = true;
                        }
                    }
                }
            }
        }

        // 4. Update block LIT state
        boolean isBurningNow = entity.burnTime > 0;
        if (originallyBurning != isBurningNow) {
            state = state.with(AluminumGeneratorBlock.LIT, isBurningNow);
            world.setBlockState(pos, state, 3);
            stateChanged = true;
        }

        if (stateChanged) {
            markDirty(world, pos, state);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.burnTime = view.getInt("BurnTime", 0);
        this.totalBurnTime = view.getInt("TotalBurnTime", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("TotalBurnTime", this.totalBurnTime);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) { return new int[]{0}; }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return CopperGeneratorBlockEntity.getFuelTime(this.getWorld(), stack) > 0;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) { return false; }

    @Override
    public int size() { return inventory.size(); }

    @Override
    public boolean isEmpty() { return inventory.get(0).isEmpty(); }

    @Override
    public ItemStack getStack(int slot) { return inventory.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) { return Inventories.splitStack(inventory, slot, amount); }

    @Override
    public ItemStack removeStack(int slot) { return Inventories.removeStack(inventory, slot); }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }

    @Override
    public void clear() { inventory.clear(); }
}
