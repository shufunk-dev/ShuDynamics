package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import net.enchantedwood.block.custom.SteelBlastFurnaceBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.gas.GasProvider;
import net.enchantedwood.gas.GasStorage;
import net.enchantedwood.gas.GasType;
import net.enchantedwood.gas.SimpleGasStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.SteelBlastFurnaceScreenHandler;
import org.jetbrains.annotations.Nullable;

public class SteelBlastFurnaceBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider, GasProvider {
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int ENERGY_DRAW = 200; // 200 FE/t
    public static final int HYDROGEN_CAPACITY = 4_000; // 4,000 mB
    public static final int TRADITIONAL_COOK_TIME = 100; // 5 seconds
    public static final int GREEN_STEEL_COOK_TIME = 60;   // 3 seconds (faster!)

    // Slots: 0=Iron Input, 1=Coke Coal Input, 2=Steel Output, 3=H2 Canister In, 4=Empty Canister Out
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 1000, 1000, 0);
    private final SimpleGasStorage hydrogenTank = new SimpleGasStorage(HYDROGEN_CAPACITY, 100) {
        @Override
        public GasType getGasType() {
            return GasType.HYDROGEN;
        }

        @Override
        public boolean canInsert(GasType type) {
            return type == GasType.HYDROGEN && getAmount() < getCapacity();
        }

        @Override
        public int insertGas(GasType type, int insertAmount, boolean simulate) {
            if (type != GasType.HYDROGEN || insertAmount <= 0) return 0;
            int space = getCapacity() - getAmount();
            int insertable = Math.min(space, insertAmount);
            if (!simulate && insertable > 0) {
                setGas(GasType.HYDROGEN, getAmount() + insertable);
            }
            return insertable;
        }

        @Override
        public int extractGas(GasType type, int extractAmount, boolean simulate) {
            if (type != GasType.NONE && type != GasType.HYDROGEN) return 0;
            int extractable = Math.min(getAmount(), extractAmount);
            if (!simulate && extractable > 0) {
                setGas(GasType.HYDROGEN, getAmount() - extractable);
            }
            return extractable;
        }
    };

    private int cookTime = 0;
    private int totalCookTime = TRADITIONAL_COOK_TIME;
    private boolean isGreenMode = false;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> hydrogenTank.getAmount();
                case 5 -> HYDROGEN_CAPACITY;
                case 6 -> cookTime;
                case 7 -> totalCookTime;
                case 8 -> isGreenMode ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 6) cookTime = value;
            if (index == 7) totalCookTime = value;
            if (index == 8) isGreenMode = (value == 1);
        }

        @Override
        public int size() {
            return 9;
        }
    };

    public SteelBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_BLAST_FURNACE_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.steel_blast_furnace");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SteelBlastFurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public @Nullable GasStorage getGasStorage(@Nullable Direction side) {
        return this.hydrogenTank;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, SteelBlastFurnaceBlockEntity entity) {
        boolean stateChanged = false;

        // 1. Process Hydrogen Canister in slot 3 -> 4
        ItemStack h2Canister = entity.inventory.get(3);
        ItemStack emptyCanister = entity.inventory.get(4);
        if (h2Canister.isOf(ModItems.HYDROGEN_CANISTER) && entity.hydrogenTank.getAmount() <= HYDROGEN_CAPACITY - 1000) {
            if (emptyCanister.isEmpty() || (emptyCanister.isOf(ModItems.EMPTY_GAS_CANISTER) && emptyCanister.getCount() < emptyCanister.getMaxCount())) {
                entity.hydrogenTank.insertGas(GasType.HYDROGEN, 1000, false);
                h2Canister.decrement(1);
                if (emptyCanister.isEmpty()) {
                    entity.inventory.set(4, new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                } else {
                    emptyCanister.increment(1);
                }
                stateChanged = true;
            }
        }

        // 2. Smelting Logic
        ItemStack ironInput = entity.inventory.get(0);
        ItemStack cokeInput = entity.inventory.get(1);
        ItemStack output = entity.inventory.get(2);

        boolean hasIron = ironInput.isOf(Items.IRON_INGOT) || ironInput.isOf(ModItems.IRON_DUST);
        boolean hasOutputSpace = output.isEmpty() || (output.isOf(ModItems.STEEL_INGOT) && output.getCount() < output.getMaxCount());
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        // Method A: Green Steel (Hydrogen)
        boolean canGreenSmelt = hasIron && hasOutputSpace && hasEnergy && entity.hydrogenTank.getAmount() >= 10;
        // Method B: Traditional (Coke Coal)
        boolean canTradSmelt = hasIron && hasOutputSpace && hasEnergy && cokeInput.isOf(ModItems.COKE_COAL);

        if (canGreenSmelt) {
            entity.isGreenMode = true;
            entity.totalCookTime = GREEN_STEEL_COOK_TIME;
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            entity.cookTime++;
            if (entity.cookTime >= GREEN_STEEL_COOK_TIME) {
                entity.cookTime = 0;
                entity.hydrogenTank.extractGas(GasType.HYDROGEN, 100, false);
                ironInput.decrement(1);
                if (output.isEmpty()) {
                    entity.inventory.set(2, new ItemStack(ModItems.STEEL_INGOT));
                } else {
                    output.increment(1);
                }
            }
            stateChanged = true;
        } else if (canTradSmelt) {
            entity.isGreenMode = false;
            entity.totalCookTime = TRADITIONAL_COOK_TIME;
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            entity.cookTime++;
            if (entity.cookTime >= TRADITIONAL_COOK_TIME) {
                entity.cookTime = 0;
                ironInput.decrement(1);
                cokeInput.decrement(1);
                if (output.isEmpty()) {
                    entity.inventory.set(2, new ItemStack(ModItems.STEEL_INGOT));
                } else {
                    output.increment(1);
                }
            }
            stateChanged = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                stateChanged = true;
            }
        }

        boolean isRunning = canGreenSmelt || canTradSmelt;
        if (state.get(SteelBlastFurnaceBlock.LIT) != isRunning) {
            world.setBlockState(pos, state.with(SteelBlastFurnaceBlock.LIT, isRunning), 3);
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
        this.hydrogenTank.readData(view, "Hydrogen");
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", TRADITIONAL_COOK_TIME);
        this.isGreenMode = view.getBoolean("IsGreenMode", false);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        this.hydrogenTank.writeData(view, "Hydrogen");
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        view.putBoolean("IsGreenMode", this.isGreenMode);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{2, 4}; // Steel Out, Empty Canister Out
        if (side == Direction.UP) return new int[]{0};       // Iron In
        return new int[]{1, 3, 2, 4};                       // Coke Coal In, H2 In, Steel Out, Empty Canister Out
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return stack.isOf(Items.IRON_INGOT) || stack.isOf(ModItems.IRON_DUST);
        if (slot == 1) return stack.isOf(ModItems.COKE_COAL);
        if (slot == 3) return stack.isOf(ModItems.HYDROGEN_CANISTER);
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == 2 || slot == 4;
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
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(inventory, slot);
        if (!result.isEmpty()) markDirty();
        return result;
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
        markDirty();
    }
}
