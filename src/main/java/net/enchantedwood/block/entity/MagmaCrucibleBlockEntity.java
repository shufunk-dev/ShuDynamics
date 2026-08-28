package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.MagmaCrucibleBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.MagmaCrucibleScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
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

public class MagmaCrucibleBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider, LavaProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 35; // 35 FE/t
    public static final int MAX_LAVA = 10_000; // 10,000 mB

    public static final int INPUT_SLOT = 0;
    public static final int MINERAL_OUTPUT_SLOT = 1;
    public static final int BUCKET_INPUT_SLOT = 2;
    public static final int BUCKET_OUTPUT_SLOT = 3;
    public static final int GEAR_SLOT = 4;
    public static final int INVENTORY_SIZE = 5;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 140;
    private int lavaAmount = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> totalCookTime;
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
                case 0 -> cookTime = value;
                case 1 -> totalCookTime = value;
                case 6 -> lavaAmount = value;
            }
        }

        @Override
        public int size() {
            return 8;
        }
    };

    public MagmaCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGMA_CRUCIBLE_BE, pos, state);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public static int getTierCookTime(GearTier tier) {
        return switch (tier) {
            case IRON -> 120;
            case COPPER -> 100;
            case BRONZE -> 80;
            case GOLD -> 60;
            case DIAMOND -> 40;
            case NETHERITE -> 18;
            default -> 140;
        };
    }

    public int getLavaAmount() {
        return this.lavaAmount;
    }

    public int drainLava(int amount) {
        int toDrain = Math.min(this.lavaAmount, amount);
        this.lavaAmount -= toDrain;
        if (toDrain > 0) markDirty();
        return toDrain;
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.magma_crucible");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MagmaCrucibleScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, MagmaCrucibleBlockEntity entity) {
        boolean dirty = false;

        entity.totalCookTime = getTierCookTime(entity.getActiveGearTier());

        // 1. Fill empty bucket with liquid lava from internal tank
        ItemStack bucketIn = entity.inventory.get(BUCKET_INPUT_SLOT);
        if (!bucketIn.isEmpty() && bucketIn.isOf(Items.BUCKET) && entity.lavaAmount >= 1000) {
            ItemStack bucketOut = entity.inventory.get(BUCKET_OUTPUT_SLOT);
            if (bucketOut.isEmpty()) {
                entity.lavaAmount -= 1000;
                bucketIn.decrement(1);
                entity.inventory.set(BUCKET_OUTPUT_SLOT, new ItemStack(Items.LAVA_BUCKET));
                dirty = true;
            } else if (bucketOut.isOf(Items.LAVA_BUCKET) && bucketOut.getCount() < bucketOut.getMaxCount()) {
                entity.lavaAmount -= 1000;
                bucketIn.decrement(1);
                bucketOut.increment(1);
                dirty = true;
            }
        }

        // 2. Melt and distill input block into minerals + lava
        ItemStack input = entity.inventory.get(INPUT_SLOT);
        CrucibleResult result = getCrucibleResult(input.getItem());

        boolean canMelt = result != null && entity.canAcceptResult(result);
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        boolean isMelting = false;
        if (canMelt && hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            ++entity.cookTime;
            isMelting = true;
            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                entity.processMelt(result);
            }
            dirty = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                dirty = true;
            }
        }

        if (state.get(MagmaCrucibleBlock.LIT) != isMelting) {
            world.setBlockState(pos, state.with(MagmaCrucibleBlock.LIT, isMelting), 3);
            dirty = true;
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    private boolean canAcceptResult(CrucibleResult result) {
        if (this.lavaAmount + result.lavaYield > MAX_LAVA) return false;
        if (result.mineralItem == null) return true;
        ItemStack out = inventory.get(MINERAL_OUTPUT_SLOT);
        if (out.isEmpty()) return true;
        if (!out.isOf(result.mineralItem)) return false;
        return out.getCount() + result.mineralCount <= out.getMaxCount();
    }

    private void processMelt(CrucibleResult result) {
        ItemStack input = inventory.get(INPUT_SLOT);
        input.decrement(1);

        this.lavaAmount = Math.min(MAX_LAVA, this.lavaAmount + result.lavaYield);

        if (result.mineralItem != null) {
            ItemStack out = inventory.get(MINERAL_OUTPUT_SLOT);
            if (out.isEmpty()) {
                inventory.set(MINERAL_OUTPUT_SLOT, new ItemStack(result.mineralItem, result.mineralCount));
            } else {
                out.increment(result.mineralCount);
            }
        }
    }

    public static record CrucibleResult(int lavaYield, @Nullable Item mineralItem, int mineralCount) {}

    public static @Nullable CrucibleResult getCrucibleResult(Item item) {
        if (item == Items.BASALT || item == Items.SMOOTH_BASALT || item == Items.POLISHED_BASALT) {
            return new CrucibleResult(250, ModItems.VOLCANIC_ASH, 2);
        }
        if (item == Items.BLACKSTONE || item == Items.POLISHED_BLACKSTONE) {
            return new CrucibleResult(250, ModItems.SULFUR_DUST, 1);
        }
        if (item == Items.MAGMA_BLOCK) {
            return new CrucibleResult(500, ModItems.SULFUR_DUST, 2);
        }
        if (item == Items.NETHERRACK) {
            return new CrucibleResult(100, ModItems.VOLCANIC_ASH, 1);
        }
        if (item == ModItems.FIRE_CRYSTAL) {
            return new CrucibleResult(1000, ModItems.SULFUR_DUST, 4);
        }
        return null;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", 140);
        this.lavaAmount = view.getInt("LavaAmount", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        view.putInt("LavaAmount", this.lavaAmount);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{MINERAL_OUTPUT_SLOT, BUCKET_OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{INPUT_SLOT, BUCKET_INPUT_SLOT};
        return new int[]{INPUT_SLOT, BUCKET_INPUT_SLOT, GEAR_SLOT, MINERAL_OUTPUT_SLOT, BUCKET_OUTPUT_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT) return getCrucibleResult(stack.getItem()) != null;
        if (slot == BUCKET_INPUT_SLOT) return stack.isOf(Items.BUCKET);
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == MINERAL_OUTPUT_SLOT || slot == BUCKET_OUTPUT_SLOT;
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
        return 0; // Magma Crucible only exports melted lava
    }

    @Override
    public boolean canInsertLava() {
        return false;
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        int extracted = Math.min(this.lavaAmount, amount);
        if (!simulate && extracted > 0) {
            this.lavaAmount -= extracted;
            markDirty();
        }
        return extracted;
    }

    @Override
    public boolean canExtractLava() {
        return this.lavaAmount > 0;
    }
}
