package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.LavaPumpBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.LavaPumpScreenHandler;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.enchantedwood.fluid.LavaProvider;
import org.jetbrains.annotations.Nullable;

public class LavaPumpBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider, LavaProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 25; // 25 FE/t
    public static final int MAX_LAVA = 10_000; // 10,000 mB

    public static final int BUCKET_IN_SLOT = 0;
    public static final int BUCKET_OUT_SLOT = 1;
    public static final int GEAR_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int pumpProgress = 0;
    private int totalPumpTime = 40;
    private int lavaAmount = 0;

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
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
        if (toDrain > 0) setChanged();
        return toDrain;
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enchantedwood.lava_pump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new LavaPumpScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, LavaPumpBlockEntity entity) {
        boolean dirty = false;

        entity.totalPumpTime = getTierPumpTime(entity.getActiveGearTier());

        // 1. Fill empty bucket with liquid lava from internal tank
        ItemStack bucketIn = entity.inventory.get(BUCKET_IN_SLOT);
        if (!bucketIn.isEmpty() && entity.lavaAmount >= 1000) {
            boolean isVanillaBucket = bucketIn.is(Items.BUCKET);
            boolean isCopperBucket = bucketIn.is(ModItems.COPPER_BUCKET);
            if (isVanillaBucket || isCopperBucket) {
                ItemStack filledItem = isVanillaBucket ? new ItemStack(Items.LAVA_BUCKET) : new ItemStack(ModItems.COPPER_LAVA_BUCKET);
                ItemStack bucketOut = entity.inventory.get(BUCKET_OUT_SLOT);
                if (bucketOut.isEmpty()) {
                    entity.lavaAmount -= 1000;
                    bucketIn.shrink(1);
                    entity.inventory.set(BUCKET_OUT_SLOT, filledItem);
                    dirty = true;
                } else if (ItemStack.isSameItem(bucketOut, filledItem) && bucketOut.getCount() < bucketOut.getMaxStackSize()) {
                    entity.lavaAmount -= 1000;
                    bucketIn.shrink(1);
                    bucketOut.grow(1);
                    dirty = true;
                }
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
                BlockEntity be = world.getBlockEntity(pos.relative(dir));
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

        if (state.getValue(LavaPumpBlock.LIT) != isPumping) {
            world.setBlock(pos, state.setValue(LavaPumpBlock.LIT, isPumping), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    private static boolean hasLavaBelow(Level world, BlockPos pos) {
        for (int dy = -1; dy >= -3; dy--) {
            BlockPos check = pos.offset(0, dy, 0);
            if (world.getFluidState(check).is(Fluids.LAVA) || world.getBlockState(check).is(Blocks.LAVA) || world.getBlockState(check).is(Blocks.MAGMA_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.pumpProgress = view.getIntOr("PumpProgress", 0);
        this.totalPumpTime = view.getIntOr("TotalPumpTime", 40);
        this.lavaAmount = view.getIntOr("LavaAmount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("PumpProgress", this.pumpProgress);
        view.putInt("TotalPumpTime", this.totalPumpTime);
        view.putInt("LavaAmount", this.lavaAmount);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{BUCKET_OUT_SLOT};
        if (side == Direction.UP) return new int[]{BUCKET_IN_SLOT};
        return new int[]{BUCKET_IN_SLOT, GEAR_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == BUCKET_IN_SLOT) return stack.is(Items.BUCKET);
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == BUCKET_OUT_SLOT;
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
            setChanged();
        }
        return extracted;
    }

    @Override
    public boolean canExtractLava() {
        return this.lavaAmount > 0;
    }
}
