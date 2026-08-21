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
import net.enchantedwood.block.custom.AluminumRefinerBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.gas.GasProvider;
import net.enchantedwood.gas.GasStorage;
import net.enchantedwood.gas.GasType;
import net.enchantedwood.gas.SimpleGasStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.AluminumRefinerScreenHandler;
import org.jetbrains.annotations.Nullable;

public class AluminumRefinerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider, GasProvider {
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int ENERGY_DRAW = 100; // 100 FE/t
    public static final int OXYGEN_CAPACITY = 4_000; // 4,000 mB
    public static final int TOTAL_COOK_TIME = 100; // 5 seconds per ingot

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(4, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);
    private final SimpleGasStorage oxygenTank = new SimpleGasStorage(OXYGEN_CAPACITY, 100) {
        @Override
        public GasType getGasType() {
            return GasType.OXYGEN;
        }

        @Override
        public boolean canInsert(GasType type) {
            return type == GasType.OXYGEN && getAmount() < getCapacity();
        }

        @Override
        public int insertGas(GasType type, int insertAmount, boolean simulate) {
            if (type != GasType.OXYGEN || insertAmount <= 0) return 0;
            int space = getCapacity() - getAmount();
            int insertable = Math.min(space, insertAmount);
            if (!simulate && insertable > 0) {
                setGas(GasType.OXYGEN, getAmount() + insertable);
            }
            return insertable;
        }

        @Override
        public int extractGas(GasType type, int extractAmount, boolean simulate) {
            if (type != GasType.NONE && type != GasType.OXYGEN) return 0;
            int extractable = Math.min(getAmount(), extractAmount);
            if (!simulate && extractable > 0) {
                setGas(GasType.OXYGEN, getAmount() - extractable);
            }
            return extractable;
        }
    };

    private int cookTime = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> oxygenTank.getAmount();
                case 5 -> OXYGEN_CAPACITY;
                case 6 -> cookTime;
                case 7 -> TOTAL_COOK_TIME;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 6) cookTime = value;
        }

        @Override
        public int size() {
            return 8;
        }
    };

    public AluminumRefinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALUMINUM_REFINER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.aluminum_refiner");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AluminumRefinerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public @Nullable GasStorage getGasStorage(@Nullable Direction side) {
        return this.oxygenTank;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, AluminumRefinerBlockEntity entity) {
        boolean stateChanged = false;

        // 1. Process Oxygen Canister in slot 1 -> 2
        ItemStack o2Canister = entity.inventory.get(1);
        ItemStack emptyCanister = entity.inventory.get(2);
        if (o2Canister.isOf(ModItems.OXYGEN_CANISTER) && entity.oxygenTank.getAmount() <= OXYGEN_CAPACITY - 1000) {
            if (emptyCanister.isEmpty() || (emptyCanister.isOf(ModItems.EMPTY_GAS_CANISTER) && emptyCanister.getCount() < emptyCanister.getMaxCount())) {
                entity.oxygenTank.insertGas(GasType.OXYGEN, 1000, false);
                o2Canister.decrement(1);
                if (emptyCanister.isEmpty()) {
                    entity.inventory.set(2, new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                } else {
                    emptyCanister.increment(1);
                }
                stateChanged = true;
            }
        }

        // 2. Check if can smelt Bauxite + Oxygen
        ItemStack input = entity.inventory.get(0);
        ItemStack output = entity.inventory.get(3);
        boolean hasValidInput = input.isOf(ModItems.RAW_BAUXITE) || input.isOf(ModItems.BAUXITE_DUST);
        boolean hasOutputSpace = output.isEmpty() || (output.isOf(ModItems.ALUMINUM_INGOT) && output.getCount() < output.getMaxCount());
        boolean canRefine = hasValidInput && hasOutputSpace && entity.oxygenTank.getAmount() >= 10 && entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        if (canRefine) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            entity.cookTime++;
            if (entity.cookTime >= TOTAL_COOK_TIME) {
                entity.cookTime = 0;
                entity.oxygenTank.extractGas(GasType.OXYGEN, 100, false);
                input.decrement(1);
                if (output.isEmpty()) {
                    entity.inventory.set(3, new ItemStack(ModItems.ALUMINUM_INGOT));
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

        // 3. Update block LIT state
        boolean isRunningNow = canRefine;
        if (state.get(AluminumRefinerBlock.LIT) != isRunningNow) {
            world.setBlockState(pos, state.with(AluminumRefinerBlock.LIT, isRunningNow), 3);
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
        this.oxygenTank.readData(view, "Oxygen");
        this.cookTime = view.getInt("CookTime", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        this.oxygenTank.writeData(view, "Oxygen");
        view.putInt("CookTime", this.cookTime);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return stack.isOf(ModItems.RAW_BAUXITE) || stack.isOf(ModItems.BAUXITE_DUST);
        if (slot == 1) return stack.isOf(ModItems.OXYGEN_CANISTER);
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == 2 || slot == 3;
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
