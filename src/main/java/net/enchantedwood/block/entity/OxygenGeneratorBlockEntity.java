package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.OxygenGeneratorBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.gas.GasProvider;
import net.enchantedwood.gas.GasStorage;
import net.enchantedwood.gas.GasType;
import net.enchantedwood.gas.SimpleGasStorage;
import net.enchantedwood.fluid.WaterProvider;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.OxygenGeneratorScreenHandler;
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

public class OxygenGeneratorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider, GasProvider, WaterProvider {
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_ENERGY_DRAW = 60; // 60 FE/t
    public static final int WATER_CAPACITY = 10_000; // 10,000 mB (10 Buckets)
    public static final int OXYGEN_CAPACITY = 4_000;  // 4,000 mB
    public static final int HYDROGEN_CAPACITY = 8_000; // 8,000 mB

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(6, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);
    private final SimpleGasStorage oxygenTank = new SimpleGasStorage(OXYGEN_CAPACITY, 100) {
        @Override
        public GasType getGasType() {
            return GasType.OXYGEN;
        }

        @Override
        public boolean canExtract(GasType type) {
            return (type == GasType.NONE || type == GasType.OXYGEN) && getAmount() > 0;
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

    private final SimpleGasStorage hydrogenTank = new SimpleGasStorage(HYDROGEN_CAPACITY, 200) {
        @Override
        public GasType getGasType() {
            return GasType.HYDROGEN;
        }

        @Override
        public boolean canExtract(GasType type) {
            return (type == GasType.NONE || type == GasType.HYDROGEN) && getAmount() > 0;
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

    private int waterAmount = 0;
    private int progress = 0;
    private final int maxProgress = 10; // 10 ticks per cycle

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> waterAmount;
                case 5 -> WATER_CAPACITY;
                case 6 -> oxygenTank.getAmount();
                case 7 -> OXYGEN_CAPACITY;
                case 8 -> hydrogenTank.getAmount();
                case 9 -> HYDROGEN_CAPACITY;
                case 10 -> progress;
                case 11 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 12;
        }
    };

    public OxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_GENERATOR_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.oxygen_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new OxygenGeneratorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    private final GasStorage dualGasStorage = new GasStorage() {
        @Override
        public GasType getGasType() {
            return oxygenTank.getAmount() > 0 ? GasType.OXYGEN : (hydrogenTank.getAmount() > 0 ? GasType.HYDROGEN : GasType.NONE);
        }

        @Override
        public int getAmount() {
            return oxygenTank.getAmount() + hydrogenTank.getAmount();
        }

        @Override
        public int getCapacity() {
            return OXYGEN_CAPACITY + HYDROGEN_CAPACITY;
        }

        @Override
        public int insertGas(GasType type, int amount, boolean simulate) {
            if (type == GasType.OXYGEN) return oxygenTank.insertGas(type, amount, simulate);
            if (type == GasType.HYDROGEN) return hydrogenTank.insertGas(type, amount, simulate);
            return 0;
        }

        @Override
        public int extractGas(GasType type, int amount, boolean simulate) {
            if (type == GasType.OXYGEN) return oxygenTank.extractGas(type, amount, simulate);
            if (type == GasType.HYDROGEN) return hydrogenTank.extractGas(type, amount, simulate);
            return 0;
        }

        @Override
        public boolean canExtract(GasType type) {
            if (type == GasType.OXYGEN) return oxygenTank.getAmount() > 0;
            if (type == GasType.HYDROGEN) return hydrogenTank.getAmount() > 0;
            return oxygenTank.getAmount() > 0 || hydrogenTank.getAmount() > 0;
        }

        @Override
        public boolean canInsert(GasType type) {
            if (type == GasType.OXYGEN) return oxygenTank.getAmount() < OXYGEN_CAPACITY;
            if (type == GasType.HYDROGEN) return hydrogenTank.getAmount() < HYDROGEN_CAPACITY;
            return false;
        }
    };

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public @Nullable GasStorage getGasStorage(@Nullable Direction side) {
        return this.dualGasStorage;
    }

    public GasStorage getOxygenTank() {
        return this.oxygenTank;
    }

    public GasStorage getHydrogenTank() {
        return this.hydrogenTank;
    }

    @Override
    public int getWaterAmount() {
        return this.waterAmount;
    }

    @Override
    public int getMaxWater() {
        return WATER_CAPACITY;
    }

    @Override
    public int insertWater(int amount, boolean simulate) {
        int space = WATER_CAPACITY - this.waterAmount;
        int toInsert = Math.min(space, amount);
        if (!simulate && toInsert > 0) {
            this.waterAmount += toInsert;
            setChanged();
        }
        return toInsert;
    }

    @Override
    public int extractWater(int amount, boolean simulate) {
        int toExtract = Math.min(this.waterAmount, amount);
        if (!simulate && toExtract > 0) {
            this.waterAmount -= toExtract;
            setChanged();
        }
        return toExtract;
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, OxygenGeneratorBlockEntity entity) {
        boolean stateChanged = false;

        // 1. Process Water Buckets into internal tank
        ItemStack waterInput = entity.inventory.get(0);
        ItemStack waterOutput = entity.inventory.get(1);
        if (waterInput.is(Items.WATER_BUCKET) && entity.waterAmount <= WATER_CAPACITY - 1000) {
            if (waterOutput.isEmpty() || (waterOutput.is(Items.BUCKET) && waterOutput.getCount() < waterOutput.getMaxStackSize())) {
                entity.waterAmount += 1000;
                waterInput.shrink(1);
                if (waterOutput.isEmpty()) {
                    entity.inventory.set(1, new ItemStack(Items.BUCKET));
                } else {
                    waterOutput.grow(1);
                }
                stateChanged = true;
            }
        }

        // 2. Perform Water Electrolysis: 2 H2O -> 2 H2 + O2
        boolean canRun = entity.energyStorage.getEnergy() >= MAX_ENERGY_DRAW
                && entity.waterAmount >= 10
                && entity.oxygenTank.getAmount() <= OXYGEN_CAPACITY - 10
                && entity.hydrogenTank.getAmount() <= HYDROGEN_CAPACITY - 20;

        if (canRun) {
            entity.energyStorage.extractEnergy(MAX_ENERGY_DRAW, false);
            entity.progress++;
            if (entity.progress >= entity.maxProgress) {
                entity.progress = 0;
                entity.waterAmount -= 10;
                entity.oxygenTank.insertGas(GasType.OXYGEN, 10, false);
                entity.hydrogenTank.insertGas(GasType.HYDROGEN, 20, false);
            }
            stateChanged = true;
        } else {
            if (entity.progress > 0) {
                entity.progress = 0;
                stateChanged = true;
            }
        }

        // 3. Fill Gas Canisters in inventory
        // Oxygen Canister (slot 2 -> 3)
        ItemStack emptyO2 = entity.inventory.get(2);
        ItemStack fullO2 = entity.inventory.get(3);
        if (emptyO2.is(ModItems.EMPTY_GAS_CANISTER) && entity.oxygenTank.getAmount() >= 1000) {
            if (fullO2.isEmpty() || (fullO2.is(ModItems.OXYGEN_CANISTER) && fullO2.getCount() < fullO2.getMaxStackSize())) {
                entity.oxygenTank.extractGas(GasType.OXYGEN, 1000, false);
                emptyO2.shrink(1);
                if (fullO2.isEmpty()) {
                    entity.inventory.set(3, new ItemStack(ModItems.OXYGEN_CANISTER));
                } else {
                    fullO2.grow(1);
                }
                stateChanged = true;
            }
        }

        // Hydrogen Canister (slot 4 -> 5)
        ItemStack emptyH2 = entity.inventory.get(4);
        ItemStack fullH2 = entity.inventory.get(5);
        if (emptyH2.is(ModItems.EMPTY_GAS_CANISTER) && entity.hydrogenTank.getAmount() >= 1000) {
            if (fullH2.isEmpty() || (fullH2.is(ModItems.HYDROGEN_CANISTER) && fullH2.getCount() < fullH2.getMaxStackSize())) {
                entity.hydrogenTank.extractGas(GasType.HYDROGEN, 1000, false);
                emptyH2.shrink(1);
                if (fullH2.isEmpty()) {
                    entity.inventory.set(5, new ItemStack(ModItems.HYDROGEN_CANISTER));
                } else {
                    fullH2.grow(1);
                }
                stateChanged = true;
            }
        }

        // 4. Push Gas to adjacent Gas Pipes or consumers
        if (entity.oxygenTank.getAmount() > 0 || entity.hydrogenTank.getAmount() > 0) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
                if (neighbor instanceof GasProvider provider && neighbor != entity) {
                    GasStorage receiver = provider.getGasStorage(dir.getOpposite());
                    if (receiver != null) {
                        if (entity.oxygenTank.getAmount() > 0 && receiver.canInsert(GasType.OXYGEN)) {
                            int toPush = Math.min(entity.oxygenTank.getAmount(), 100);
                            int inserted = receiver.insertGas(GasType.OXYGEN, toPush, false);
                            if (inserted > 0) {
                                entity.oxygenTank.extractGas(GasType.OXYGEN, inserted, false);
                                stateChanged = true;
                            }
                        }
                        if (entity.hydrogenTank.getAmount() > 0 && receiver.canInsert(GasType.HYDROGEN)) {
                            int toPush = Math.min(entity.hydrogenTank.getAmount(), 200);
                            int inserted = receiver.insertGas(GasType.HYDROGEN, toPush, false);
                            if (inserted > 0) {
                                entity.hydrogenTank.extractGas(GasType.HYDROGEN, inserted, false);
                                stateChanged = true;
                            }
                        }
                    }
                }
            }
        }

        // 5. Update block LIT state
        boolean isRunningNow = canRun;
        if (state.getValue(OxygenGeneratorBlock.LIT) != isRunningNow) {
            world.setBlock(pos, state.setValue(OxygenGeneratorBlock.LIT, isRunningNow), 3);
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
        this.hydrogenTank.readData(view, "Hydrogen");
        this.waterAmount = view.getIntOr("WaterAmount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        this.oxygenTank.writeData(view, "Oxygen");
        this.hydrogenTank.writeData(view, "Hydrogen");
        view.putInt("WaterAmount", this.waterAmount);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return stack.is(Items.WATER_BUCKET);
        if (slot == 2 || slot == 4) return stack.is(ModItems.EMPTY_GAS_CANISTER);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 1 || slot == 3 || slot == 5;
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
