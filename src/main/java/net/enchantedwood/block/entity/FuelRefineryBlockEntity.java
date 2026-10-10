package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.FuelRefineryBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.FuelRefineryScreenHandler;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class FuelRefineryBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int ENERGY_CAPACITY = 32_000;
    public static final int ENERGY_DRAW = 20; // 20 FE/t

    public static final int FEEDSTOCK_SLOT = 0;
    public static final int CANISTER_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int BYPRODUCT_SLOT = 3;
    public static final int BATTERY_SLOT = 4;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 200, 200, 0);
    private int progress = 0;
    private int maxProgress = 100;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energyStorage.getEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
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
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public FuelRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_REFINERY_BLOCK_ENTITY, pos, state);
    }

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enchantedwood.fuel_refinery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new FuelRefineryScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
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
        this.progress = view.getIntOr("Progress", 0);
        this.maxProgress = view.getIntOr("MaxProgress", 100);
        if (this.maxProgress <= 0) this.maxProgress = 100;
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("Progress", this.progress);
        view.putInt("MaxProgress", this.maxProgress);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, FuelRefineryBlockEntity entity) {
        boolean stateChanged = false;

        // 1. Charge from battery slot if present
        ItemStack batteryStack = entity.inventory.get(BATTERY_SLOT);
        if (!batteryStack.isEmpty()) {
            EnergyStorage batteryStorage = null;
            if (batteryStack.getItem() instanceof net.enchantedwood.energy.ItemEnergyProvider itemProvider) {
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

        // 2. Refine feedstock
        RefineryRecipe currentRecipe = entity.getMatchingRecipe();
        boolean isRunningNow = false;

        if (currentRecipe != null && entity.canOutput(currentRecipe)) {
            if (entity.energyStorage.getEnergy() >= ENERGY_DRAW) {
                entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
                entity.progress++;
                entity.maxProgress = currentRecipe.processTime;
                isRunningNow = true;

                if (entity.progress >= entity.maxProgress) {
                    entity.craft(currentRecipe);
                    entity.progress = 0;
                }
                stateChanged = true;
            }
        } else {
            if (entity.progress > 0) {
                entity.progress = Math.max(0, entity.progress - 2);
                stateChanged = true;
            }
        }

        if (state.getValue(FuelRefineryBlock.LIT) != isRunningNow) {
            world.setBlock(pos, state.setValue(FuelRefineryBlock.LIT, isRunningNow), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(world, pos, state);
        }
    }

    private record RefineryRecipe(Item feedstock, int feedstockCount, Item reagent, int reagentCount, ItemStack output, ItemStack byproduct, int processTime) {}

    private RefineryRecipe getMatchingRecipe() {
        ItemStack feed = inventory.get(FEEDSTOCK_SLOT);
        ItemStack reagent = inventory.get(CANISTER_SLOT);
        if (feed.isEmpty()) return null;

        // 1. Crude Oil Sludge + Empty Canister -> Gasoline Canister + Mineral Tar
        if (feed.is(ModItems.CRUDE_OIL_SLUDGE) && feed.getCount() >= 1 && reagent.is(ModItems.EMPTY_GAS_CANISTER) && reagent.getCount() >= 1) {
            return new RefineryRecipe(ModItems.CRUDE_OIL_SLUDGE, 1, ModItems.EMPTY_GAS_CANISTER, 1, new ItemStack(ModItems.GASOLINE_CANISTER), new ItemStack(ModItems.MINERAL_TAR), 100);
        }

        // 2. Corn (High Ethanol) + Empty Canister -> Biofuel Canister (Fast: 2 corn)
        if (feed.is(ModItems.CORN) && feed.getCount() >= 2 && reagent.is(ModItems.EMPTY_GAS_CANISTER) && reagent.getCount() >= 1) {
            return new RefineryRecipe(ModItems.CORN, 2, ModItems.EMPTY_GAS_CANISTER, 1, new ItemStack(ModItems.BIOFUEL_CANISTER), ItemStack.EMPTY, 80);
        }

        // 3. Wheat (4) + Empty Canister -> Biofuel Canister
        if (feed.is(Items.WHEAT) && feed.getCount() >= 4 && reagent.is(ModItems.EMPTY_GAS_CANISTER) && reagent.getCount() >= 1) {
            return new RefineryRecipe(Items.WHEAT, 4, ModItems.EMPTY_GAS_CANISTER, 1, new ItemStack(ModItems.BIOFUEL_CANISTER), ItemStack.EMPTY, 100);
        }

        // 4. Sugar Cane (4) + Empty Canister -> Biofuel Canister
        if (feed.is(Items.SUGAR_CANE) && feed.getCount() >= 4 && reagent.is(ModItems.EMPTY_GAS_CANISTER) && reagent.getCount() >= 1) {
            return new RefineryRecipe(Items.SUGAR_CANE, 4, ModItems.EMPTY_GAS_CANISTER, 1, new ItemStack(ModItems.BIOFUEL_CANISTER), ItemStack.EMPTY, 100);
        }

        // 5. Potatoes (4) + Empty Canister -> Biofuel Canister
        if (feed.is(Items.POTATO) && feed.getCount() >= 4 && reagent.is(ModItems.EMPTY_GAS_CANISTER) && reagent.getCount() >= 1) {
            return new RefineryRecipe(Items.POTATO, 4, ModItems.EMPTY_GAS_CANISTER, 1, new ItemStack(ModItems.BIOFUEL_CANISTER), ItemStack.EMPTY, 100);
        }

        // 6. Gasoline Canister (1) + Corn (2) -> High-Octane Racing Fuel
        if (feed.is(ModItems.GASOLINE_CANISTER) && feed.getCount() >= 1 && reagent.is(ModItems.CORN) && reagent.getCount() >= 2) {
            return new RefineryRecipe(ModItems.GASOLINE_CANISTER, 1, ModItems.CORN, 2, new ItemStack(ModItems.HIGH_OCTANE_FUEL_CANISTER), ItemStack.EMPTY, 120);
        }

        return null;
    }

    private boolean canOutput(RefineryRecipe recipe) {
        ItemStack out = inventory.get(OUTPUT_SLOT);
        ItemStack by = inventory.get(BYPRODUCT_SLOT);

        // Check main output
        if (!out.isEmpty() && (!ItemStack.isSameItem(out, recipe.output) || out.getCount() + recipe.output.getCount() > out.getMaxStackSize())) {
            return false;
        }

        // Check byproduct output
        if (!recipe.byproduct.isEmpty()) {
            if (!by.isEmpty() && (!ItemStack.isSameItem(by, recipe.byproduct) || by.getCount() + recipe.byproduct.getCount() > by.getMaxStackSize())) {
                return false;
            }
        }

        return true;
    }

    private void craft(RefineryRecipe recipe) {
        inventory.get(FEEDSTOCK_SLOT).shrink(recipe.feedstockCount);
        inventory.get(CANISTER_SLOT).shrink(recipe.reagentCount);

        ItemStack out = inventory.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            inventory.set(OUTPUT_SLOT, recipe.output.copy());
        } else {
            out.grow(recipe.output.getCount());
        }

        if (!recipe.byproduct.isEmpty()) {
            ItemStack by = inventory.get(BYPRODUCT_SLOT);
            if (by.isEmpty()) {
                inventory.set(BYPRODUCT_SLOT, recipe.byproduct.copy());
            } else {
                by.grow(recipe.byproduct.getCount());
            }
        }
    }

    // SidedInventory implementation
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2, 3, 4};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == FEEDSTOCK_SLOT) {
            return stack.is(ModItems.CRUDE_OIL_SLUDGE) || stack.is(ModItems.CORN) ||
                    stack.is(Items.WHEAT) || stack.is(Items.SUGAR_CANE) ||
                    stack.is(Items.POTATO) || stack.is(ModItems.GASOLINE_CANISTER);
        }
        if (slot == CANISTER_SLOT) {
            return stack.is(ModItems.EMPTY_GAS_CANISTER) || stack.is(ModItems.CORN);
        }
        if (slot == BATTERY_SLOT) {
            return stack.getItem() instanceof net.enchantedwood.energy.ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT || slot == BYPRODUCT_SLOT;
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
