package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.GeothermalGeneratorBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.GeothermalGeneratorScreenHandler;
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
import net.enchantedwood.fluid.LavaProvider;
import org.jetbrains.annotations.Nullable;

public class GeothermalGeneratorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider, LavaProvider {
    public static final int CAPACITY = 1_000_000;
    public static final int MAX_EXTRACT = 25_000;
    public static final int BASE_GENERATION = 750; // 750 FE/t
    public static final int MAX_LAVA = 10_000; // 10,000 mB

    public static final int FUEL_SLOT = 0;
    public static final int BUCKET_OUTPUT_SLOT = 1;
    public static final int GEAR_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_EXTRACT, MAX_EXTRACT, 0);

    private int burnTime = 0;
    private int totalBurnTime = 0;
    private int lavaAmount = 0;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> totalBurnTime;
                case 2 -> energyStorage.getEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 6 -> lavaAmount;
                case 7 -> getActiveGearTier().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> totalBurnTime = value;
                case 6 -> lavaAmount = value;
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEOTHERMAL_GENERATOR_BE, pos, state);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public float getGearMultiplier() {
        return switch (getActiveGearTier()) {
            case IRON -> 1.25f;
            case COPPER -> 1.4f;
            case BRONZE -> 1.6f;
            case GOLD -> 1.8f;
            case TITANIUM -> 2.0f;
            case DIAMOND -> 2.2f;
            case NETHERITE -> 3.0f;
            default -> 1.0f;
        };
    }

    public int addLava(int amount) {
        int space = MAX_LAVA - this.lavaAmount;
        int toAdd = Math.min(space, amount);
        this.lavaAmount += toAdd;
        if (toAdd > 0) setChanged();
        return toAdd;
    }

    public int getLavaAmount() {
        return this.lavaAmount;
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.geothermal_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new GeothermalGeneratorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, GeothermalGeneratorBlockEntity entity) {
        boolean dirty = false;

        // 1. Drain lava bucket or fuel in FUEL_SLOT
        ItemStack fuelStack = entity.inventory.get(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            if (fuelStack.is(Items.LAVA_BUCKET) && entity.lavaAmount + 1000 <= MAX_LAVA) {
                ItemStack outputStack = entity.inventory.get(BUCKET_OUTPUT_SLOT);
                if (outputStack.isEmpty() || (outputStack.is(Items.BUCKET) && outputStack.getCount() < outputStack.getMaxStackSize())) {
                    entity.lavaAmount += 1000;
                    fuelStack.shrink(1);
                    if (outputStack.isEmpty()) {
                        entity.inventory.set(BUCKET_OUTPUT_SLOT, new ItemStack(Items.BUCKET));
                    } else {
                        outputStack.grow(1);
                    }
                    dirty = true;
                }
            } else if (fuelStack.is(Items.MAGMA_BLOCK) && entity.lavaAmount + 250 <= MAX_LAVA) {
                entity.lavaAmount += 250;
                fuelStack.shrink(1);
                dirty = true;
            } else if (fuelStack.is(ModItems.FIRE_CRYSTAL) && entity.lavaAmount + 2000 <= MAX_LAVA) {
                entity.lavaAmount += 2000;
                fuelStack.shrink(1);
                dirty = true;
            }
        }

        // 2. Burn lava from internal tank to generate high power
        boolean isGenerating = false;
        if (entity.burnTime <= 0) {
            if (entity.lavaAmount >= 100 && entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                entity.lavaAmount -= 100;
                entity.burnTime = 40;
                entity.totalBurnTime = 40;
                dirty = true;
            }
        }

        if (entity.burnTime > 0) {
            --entity.burnTime;
            isGenerating = true;
            int toGen = Math.round(BASE_GENERATION * entity.getGearMultiplier());
            entity.energyStorage.insertEnergy(toGen, false);
            dirty = true;
        }

        // Update block LIT state
        if (state.getValue(GeothermalGeneratorBlock.LIT) != isGenerating) {
            world.setBlock(pos, state.setValue(GeothermalGeneratorBlock.LIT, isGenerating), 3);
            dirty = true;
        }

        // 3. Push energy to adjacent blocks
        if (entity.energyStorage.getEnergy() > 0) {
            int available = Math.min(entity.energyStorage.getEnergy(), MAX_EXTRACT);
            for (Direction dir : Direction.values()) {
                if (available <= 0) break;
                BlockPos targetPos = pos.relative(dir);
                BlockEntity targetBe = world.getBlockEntity(targetPos);
                if (targetBe instanceof EnergyProvider provider) {
                    EnergyStorage targetStorage = provider.getEnergyStorage(dir.getOpposite());
                    if (targetStorage != null && targetStorage.canInsert()) {
                        int inserted = targetStorage.insertEnergy(available, false);
                        if (inserted > 0) {
                            entity.energyStorage.extractEnergy(inserted, false);
                            available -= inserted;
                            dirty = true;
                        }
                    }
                }
            }
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.burnTime = view.getIntOr("BurnTime", 0);
        this.totalBurnTime = view.getIntOr("TotalBurnTime", 0);
        this.lavaAmount = view.getIntOr("LavaAmount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("TotalBurnTime", this.totalBurnTime);
        view.putInt("LavaAmount", this.lavaAmount);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{BUCKET_OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{FUEL_SLOT};
        return new int[]{FUEL_SLOT, GEAR_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == FUEL_SLOT) return stack.is(Items.LAVA_BUCKET) || stack.is(Items.MAGMA_BLOCK) || stack.is(ModItems.FIRE_CRYSTAL);
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == BUCKET_OUTPUT_SLOT;
    }

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
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

    @Override
    public int getMaxLava() {
        return MAX_LAVA;
    }

    @Override
    public int insertLava(int amount, boolean simulate) {
        int space = MAX_LAVA - this.lavaAmount;
        int inserted = Math.min(space, amount);
        if (!simulate && inserted > 0) {
            this.lavaAmount += inserted;
            setChanged();
        }
        return inserted;
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        return 0; // Geothermal Generator is strictly a consumer
    }

    @Override
    public boolean canExtractLava() {
        return false;
    }
}
