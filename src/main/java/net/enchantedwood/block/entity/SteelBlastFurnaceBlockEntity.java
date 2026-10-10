package net.enchantedwood.block.entity;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class SteelBlastFurnaceBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider, GasProvider {
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int ENERGY_DRAW = 200; // 200 FE/t
    public static final int HYDROGEN_CAPACITY = 4_000; // 4,000 mB
    public static final int TRADITIONAL_COOK_TIME = 100; // 5 seconds
    public static final int GREEN_STEEL_COOK_TIME = 60;   // 3 seconds (faster!)

    // Slots: 0=Iron Input, 1=Coke Coal Input, 2=Steel Output, 3=H2 Canister In, 4=Empty Canister Out
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
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

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
            return 9;
        }
    };

    public SteelBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_BLAST_FURNACE_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.steel_blast_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
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

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, SteelBlastFurnaceBlockEntity entity) {
        boolean stateChanged = false;

        // 1. Process Hydrogen Canister in slot 3 -> 4
        ItemStack h2Canister = entity.inventory.get(3);
        ItemStack emptyCanister = entity.inventory.get(4);
        if (h2Canister.is(ModItems.HYDROGEN_CANISTER) && entity.hydrogenTank.getAmount() <= HYDROGEN_CAPACITY - 1000) {
            if (emptyCanister.isEmpty() || (emptyCanister.is(ModItems.EMPTY_GAS_CANISTER) && emptyCanister.getCount() < emptyCanister.getMaxStackSize())) {
                entity.hydrogenTank.insertGas(GasType.HYDROGEN, 1000, false);
                h2Canister.shrink(1);
                if (emptyCanister.isEmpty()) {
                    entity.inventory.set(4, new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                } else {
                    emptyCanister.grow(1);
                }
                stateChanged = true;
            }
        }

        // 2. Smelting Logic
        ItemStack ironInput = entity.inventory.get(0);
        ItemStack cokeInput = entity.inventory.get(1);
        ItemStack output = entity.inventory.get(2);

        boolean hasIron = ironInput.is(Items.IRON_INGOT) || ironInput.is(ModItems.IRON_DUST);
        boolean hasOutputSpace = output.isEmpty() || (output.is(ModItems.STEEL_INGOT) && output.getCount() < output.getMaxStackSize());
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        // Method A: Green Steel (Hydrogen)
        boolean canGreenSmelt = hasIron && hasOutputSpace && hasEnergy && entity.hydrogenTank.getAmount() >= 10;
        // Method B: Traditional (Coke Coal or Basalt Flux Catalyst)
        boolean hasFlux = cokeInput.is(ModItems.BASALT_FLUX_CATALYST);
        boolean canTradSmelt = hasIron && hasOutputSpace && hasEnergy && (cokeInput.is(ModItems.COKE_COAL) || hasFlux);

        if (canGreenSmelt) {
            entity.isGreenMode = true;
            entity.totalCookTime = GREEN_STEEL_COOK_TIME;
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            entity.cookTime++;
            if (entity.cookTime >= GREEN_STEEL_COOK_TIME) {
                entity.cookTime = 0;
                entity.hydrogenTank.extractGas(GasType.HYDROGEN, 100, false);
                ironInput.shrink(1);
                if (output.isEmpty()) {
                    entity.inventory.set(2, new ItemStack(ModItems.STEEL_INGOT));
                } else {
                    output.grow(1);
                }
            }
            stateChanged = true;
        } else if (canTradSmelt) {
            entity.isGreenMode = false;
            int cookTarget = hasFlux ? TRADITIONAL_COOK_TIME / 2 : TRADITIONAL_COOK_TIME;
            entity.totalCookTime = cookTarget;
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            entity.cookTime++;
            if (entity.cookTime >= cookTarget) {
                entity.cookTime = 0;
                ironInput.shrink(1);
                cokeInput.shrink(1);
                int yield = hasFlux ? 2 : 1;
                if (output.isEmpty()) {
                    entity.inventory.set(2, new ItemStack(ModItems.STEEL_INGOT, yield));
                } else {
                    output.grow(yield);
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
        if (state.getValue(SteelBlastFurnaceBlock.LIT) != isRunning) {
            world.setBlock(pos, state.setValue(SteelBlastFurnaceBlock.LIT, isRunning), 3);
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
        this.hydrogenTank.readData(view, "Hydrogen");
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", TRADITIONAL_COOK_TIME);
        this.isGreenMode = view.getBooleanOr("IsGreenMode", false);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        this.hydrogenTank.writeData(view, "Hydrogen");
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        view.putBoolean("IsGreenMode", this.isGreenMode);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{2, 4}; // Steel Out, Empty Canister Out
        if (side == Direction.UP) return new int[]{0};       // Iron In
        return new int[]{1, 3, 2, 4};                       // Coke Coal In, H2 In, Steel Out, Empty Canister Out
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return stack.is(Items.IRON_INGOT) || stack.is(ModItems.IRON_DUST);
        if (slot == 1) return stack.is(ModItems.COKE_COAL);
        if (slot == 3) return stack.is(ModItems.HYDROGEN_CANISTER);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 2 || slot == 4;
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
        ItemStack result = ContainerHelper.removeItem(inventory, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(inventory, slot);
        if (!result.isEmpty()) setChanged();
        return result;
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
        setChanged();
    }
}
