package net.enchantedwood.block.entity;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class AluminumRefinerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider, GasProvider {
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int ENERGY_DRAW = 100; // 100 FE/t
    public static final int OXYGEN_CAPACITY = 4_000; // 4,000 mB
    public static final int TOTAL_COOK_TIME = 100; // 5 seconds per ingot

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(4, ItemStack.EMPTY);
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

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
            return 8;
        }
    };

    public AluminumRefinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALUMINUM_REFINER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.aluminum_refiner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
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

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, AluminumRefinerBlockEntity entity) {
        boolean stateChanged = false;

        // 1. Process Oxygen Canister in slot 1 -> 2
        ItemStack o2Canister = entity.inventory.get(1);
        ItemStack emptyCanister = entity.inventory.get(2);
        if (o2Canister.is(ModItems.OXYGEN_CANISTER) && entity.oxygenTank.getAmount() <= OXYGEN_CAPACITY - 1000) {
            if (emptyCanister.isEmpty() || (emptyCanister.is(ModItems.EMPTY_GAS_CANISTER) && emptyCanister.getCount() < emptyCanister.getMaxStackSize())) {
                entity.oxygenTank.insertGas(GasType.OXYGEN, 1000, false);
                o2Canister.shrink(1);
                if (emptyCanister.isEmpty()) {
                    entity.inventory.set(2, new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                } else {
                    emptyCanister.grow(1);
                }
                stateChanged = true;
            }
        }

        // 2. Check if can smelt Bauxite + Oxygen
        ItemStack input = entity.inventory.get(0);
        ItemStack output = entity.inventory.get(3);
        boolean hasValidInput = input.is(ModItems.RAW_BAUXITE) || input.is(ModItems.BAUXITE_DUST);
        boolean hasOutputSpace = output.isEmpty() || (output.is(ModItems.ALUMINUM_INGOT) && output.getCount() < output.getMaxStackSize());
        boolean canRefine = hasValidInput && hasOutputSpace && entity.oxygenTank.getAmount() >= 10 && entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        if (canRefine) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            entity.cookTime++;
            if (entity.cookTime >= TOTAL_COOK_TIME) {
                entity.cookTime = 0;
                entity.oxygenTank.extractGas(GasType.OXYGEN, 100, false);
                input.shrink(1);
                if (output.isEmpty()) {
                    entity.inventory.set(3, new ItemStack(ModItems.ALUMINUM_INGOT));
                } else {
                    output.grow(1);
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
        if (state.getValue(AluminumRefinerBlock.LIT) != isRunningNow) {
            world.setBlock(pos, state.setValue(AluminumRefinerBlock.LIT, isRunningNow), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(world, pos, state);
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.oxygenTank.readData(view, "Oxygen");
        this.cookTime = view.getIntOr("CookTime", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        this.oxygenTank.writeData(view, "Oxygen");
        view.putInt("CookTime", this.cookTime);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return stack.is(ModItems.RAW_BAUXITE) || stack.is(ModItems.BAUXITE_DUST);
        if (slot == 1) return stack.is(ModItems.OXYGEN_CANISTER);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 2 || slot == 3;
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
