package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.LavaPumpBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.LavaPumpScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class LavaPumpBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider, LavaProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 25; // 25 FE/t
    public static final int MAX_LAVA = 10_000; // 10,000 mB

    public static final int BUCKET_IN_SLOT = 0;
    public static final int BUCKET_OUT_SLOT = 1;
    public static final int GEAR_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int pumpProgress = 0;
    private int totalPumpTime = 40;
    private int lavaAmount = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> pumpProgress;
                case 1 -> totalPumpTime;
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
                case 0 -> pumpProgress = value;
                case 1 -> totalPumpTime = value;
                case 6 -> lavaAmount = value;
            }
        }

        @Override
        public int size() {
            return 8;
        }
    };

    public LavaPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAVA_PUMP_BE, pos, state);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public static int getTierPumpTime(GearTier tier) {
        return switch (tier) {
            case IRON -> 35;
            case COPPER -> 30;
            case BRONZE -> 25;
            case GOLD -> 20;
            case TITANIUM -> 15;
            case DIAMOND -> 12;
            case NETHERITE -> 5;
            case BLAZE_OVERCLOCK -> 3;
            default -> 40;
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
        return Text.translatable("block.enchantedwood.lava_pump");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new LavaPumpScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, LavaPumpBlockEntity entity) {
        boolean dirty = false;

        entity.totalPumpTime = getTierPumpTime(entity.getActiveGearTier());

        // 1. Fill empty bucket with liquid lava from internal tank
        ItemStack bucketIn = entity.inventory.get(BUCKET_IN_SLOT);
        if (!bucketIn.isEmpty() && bucketIn.isOf(Items.BUCKET) && entity.lavaAmount >= 1000) {
            ItemStack bucketOut = entity.inventory.get(BUCKET_OUT_SLOT);
            if (bucketOut.isEmpty()) {
                entity.lavaAmount -= 1000;
                bucketIn.decrement(1);
                entity.inventory.set(BUCKET_OUT_SLOT, new ItemStack(Items.LAVA_BUCKET));
                dirty = true;
            } else if (bucketOut.isOf(Items.LAVA_BUCKET) && bucketOut.getCount() < bucketOut.getMaxCount()) {
                entity.lavaAmount -= 1000;
                bucketIn.decrement(1);
                bucketOut.increment(1);
                dirty = true;
            }
        }

        // 2. Pump lava from below/surroundings if space in tank and has energy
        boolean hasLavaSource = hasLavaBelow(world, pos);
        boolean hasSpace = entity.lavaAmount + 250 <= MAX_LAVA;
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        boolean isPumping = false;
        if (hasLavaSource && hasSpace && hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            ++entity.pumpProgress;
            isPumping = true;
            if (entity.pumpProgress >= entity.totalPumpTime) {
                entity.pumpProgress = 0;
                entity.lavaAmount = Math.min(MAX_LAVA, entity.lavaAmount + 250);
            }
            dirty = true;
        } else {
            if (entity.pumpProgress > 0) {
                entity.pumpProgress = Math.max(0, entity.pumpProgress - 1);
                dirty = true;
            }
        }

        // 3. Push lava directly into adjacent Lava Providers (Pipes, Generators, Tanks)
        if (entity.lavaAmount > 0) {
            for (Direction dir : Direction.values()) {
                BlockEntity be = world.getBlockEntity(pos.offset(dir));
                if (be instanceof LavaProvider provider && !(be instanceof LavaPumpBlockEntity)) {
                    if (provider.canInsertLava()) {
                        int toSend = Math.min(entity.lavaAmount, 250);
                        int inserted = provider.insertLava(toSend, false);
                        if (inserted > 0) {
                            entity.lavaAmount -= inserted;
                            dirty = true;
                        }
                    }
                }
            }
        }

        if (state.get(LavaPumpBlock.LIT) != isPumping) {
            world.setBlockState(pos, state.with(LavaPumpBlock.LIT, isPumping), 3);
            dirty = true;
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    private static boolean hasLavaBelow(World world, BlockPos pos) {
        for (int dy = -1; dy >= -3; dy--) {
            BlockPos check = pos.add(0, dy, 0);
            if (world.getFluidState(check).isOf(Fluids.LAVA) || world.getBlockState(check).isOf(Blocks.LAVA) || world.getBlockState(check).isOf(Blocks.MAGMA_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.pumpProgress = view.getInt("PumpProgress", 0);
        this.totalPumpTime = view.getInt("TotalPumpTime", 40);
        this.lavaAmount = view.getInt("LavaAmount", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("PumpProgress", this.pumpProgress);
        view.putInt("TotalPumpTime", this.totalPumpTime);
        view.putInt("LavaAmount", this.lavaAmount);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{BUCKET_OUT_SLOT};
        if (side == Direction.UP) return new int[]{BUCKET_IN_SLOT};
        return new int[]{BUCKET_IN_SLOT, GEAR_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == BUCKET_IN_SLOT) return stack.isOf(Items.BUCKET);
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == BUCKET_OUT_SLOT;
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
        return 0; // Lava Pump only exports pumped lava
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
