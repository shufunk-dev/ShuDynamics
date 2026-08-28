package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.GeothermalGeneratorBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.GeothermalGeneratorScreenHandler;
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
import net.enchantedwood.fluid.LavaProvider;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class GeothermalGeneratorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider, LavaProvider {
    public static final int CAPACITY = 1_000_000;
    public static final int MAX_EXTRACT = 25_000;
    public static final int BASE_GENERATION = 750; // 750 FE/t
    public static final int MAX_LAVA = 10_000; // 10,000 mB

    public static final int FUEL_SLOT = 0;
    public static final int BUCKET_OUTPUT_SLOT = 1;
    public static final int GEAR_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_EXTRACT, MAX_EXTRACT, 0);

    private int burnTime = 0;
    private int totalBurnTime = 0;
    private int lavaAmount = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
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
            case DIAMOND -> 2.2f;
            case NETHERITE -> 3.0f;
            default -> 1.0f;
        };
    }

    public int addLava(int amount) {
        int space = MAX_LAVA - this.lavaAmount;
        int toAdd = Math.min(space, amount);
        this.lavaAmount += toAdd;
        if (toAdd > 0) markDirty();
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
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.geothermal_generator");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new GeothermalGeneratorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, GeothermalGeneratorBlockEntity entity) {
        boolean dirty = false;

        // 1. Drain lava bucket or fuel in FUEL_SLOT
        ItemStack fuelStack = entity.inventory.get(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            if (fuelStack.isOf(Items.LAVA_BUCKET) && entity.lavaAmount + 1000 <= MAX_LAVA) {
                ItemStack outputStack = entity.inventory.get(BUCKET_OUTPUT_SLOT);
                if (outputStack.isEmpty() || (outputStack.isOf(Items.BUCKET) && outputStack.getCount() < outputStack.getMaxCount())) {
                    entity.lavaAmount += 1000;
                    fuelStack.decrement(1);
                    if (outputStack.isEmpty()) {
                        entity.inventory.set(BUCKET_OUTPUT_SLOT, new ItemStack(Items.BUCKET));
                    } else {
                        outputStack.increment(1);
                    }
                    dirty = true;
                }
            } else if (fuelStack.isOf(Items.MAGMA_BLOCK) && entity.lavaAmount + 250 <= MAX_LAVA) {
                entity.lavaAmount += 250;
                fuelStack.decrement(1);
                dirty = true;
            } else if (fuelStack.isOf(ModItems.FIRE_CRYSTAL) && entity.lavaAmount + 2000 <= MAX_LAVA) {
                entity.lavaAmount += 2000;
                fuelStack.decrement(1);
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
        if (state.get(GeothermalGeneratorBlock.LIT) != isGenerating) {
            world.setBlockState(pos, state.with(GeothermalGeneratorBlock.LIT, isGenerating), 3);
            dirty = true;
        }

        // 3. Push energy to adjacent blocks
        if (entity.energyStorage.getEnergy() > 0) {
            int available = Math.min(entity.energyStorage.getEnergy(), MAX_EXTRACT);
            for (Direction dir : Direction.values()) {
                if (available <= 0) break;
                BlockPos targetPos = pos.offset(dir);
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
            entity.markDirty();
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
        this.lavaAmount = view.getInt("LavaAmount", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("TotalBurnTime", this.totalBurnTime);
        view.putInt("LavaAmount", this.lavaAmount);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{BUCKET_OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{FUEL_SLOT};
        return new int[]{FUEL_SLOT, GEAR_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == FUEL_SLOT) return stack.isOf(Items.LAVA_BUCKET) || stack.isOf(Items.MAGMA_BLOCK) || stack.isOf(ModItems.FIRE_CRYSTAL);
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == BUCKET_OUTPUT_SLOT;
    }

    @Override
    public int size() {
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
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
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
            markDirty();
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
