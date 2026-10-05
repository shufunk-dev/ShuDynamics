package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.WaterPumpBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.fluid.WaterProvider;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.WaterPumpScreenHandler;
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
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WaterPumpBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider, WaterProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int BASE_ENERGY_DRAW = 20; // 20 FE/t
    public static final int MAX_WATER = 10_000; // 10,000 mB

    public static final int BUCKET_IN_SLOT = 0;
    public static final int BUCKET_OUT_SLOT = 1;
    public static final int GEAR_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int pumpProgress = 0;
    private int totalPumpTime = 40;
    private int waterAmount = 0;

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
                case 6 -> waterAmount;
                case 7 -> getActiveGearTier().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> pumpProgress = value;
                case 1 -> totalPumpTime = value;
                case 6 -> waterAmount = value;
            }
        }

        @Override
        public int size() {
            return 8;
        }
    };

    public WaterPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PUMP_BE, pos, state);
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
            case STEEL -> 20;
            case GOLD -> 15;
            case TITANIUM -> 12;
            case DIAMOND -> 8;
            case NETHERITE -> 4;
            case BLAZE_OVERCLOCK -> 2;
            default -> 40;
        };
    }

    public static int getTierEnergyDraw(GearTier tier) {
        return switch (tier) {
            case IRON -> 18;
            case COPPER -> 16;
            case BRONZE -> 14;
            case STEEL -> 12;
            case GOLD -> 10;
            case TITANIUM -> 8;
            case DIAMOND -> 6;
            case NETHERITE -> 4;
            case BLAZE_OVERCLOCK -> 2;
            default -> BASE_ENERGY_DRAW;
        };
    }

    @Override
    public int getWaterAmount() {
        return this.waterAmount;
    }

    @Override
    public int getMaxWater() {
        return MAX_WATER;
    }

    @Override
    public int insertWater(int amount, boolean simulate) {
        int space = MAX_WATER - this.waterAmount;
        int toInsert = Math.min(space, amount);
        if (!simulate && toInsert > 0) {
            this.waterAmount += toInsert;
            markDirty();
        }
        return toInsert;
    }

    @Override
    public int extractWater(int amount, boolean simulate) {
        int toDrain = Math.min(this.waterAmount, amount);
        if (!simulate && toDrain > 0) {
            this.waterAmount -= toDrain;
            markDirty();
        }
        return toDrain;
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.water_pump");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new WaterPumpScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, WaterPumpBlockEntity entity) {
        boolean dirty = false;

        GearTier tier = entity.getActiveGearTier();
        entity.totalPumpTime = getTierPumpTime(tier);
        int energyDraw = getTierEnergyDraw(tier);

        // 1. Fill empty buckets with water from internal tank
        ItemStack bucketIn = entity.inventory.get(BUCKET_IN_SLOT);
        if (!bucketIn.isEmpty() && entity.waterAmount >= 1000) {
            boolean isVanillaBucket = bucketIn.isOf(Items.BUCKET);
            boolean isCopperBucket = bucketIn.isOf(ModItems.COPPER_BUCKET);

            if (isVanillaBucket || isCopperBucket) {
                ItemStack filledItem = isVanillaBucket ? new ItemStack(Items.WATER_BUCKET) : new ItemStack(ModItems.COPPER_WATER_BUCKET);
                ItemStack bucketOut = entity.inventory.get(BUCKET_OUT_SLOT);

                if (bucketOut.isEmpty()) {
                    entity.waterAmount -= 1000;
                    bucketIn.decrement(1);
                    entity.inventory.set(BUCKET_OUT_SLOT, filledItem);
                    dirty = true;
                } else if (ItemStack.areItemsEqual(bucketOut, filledItem) && bucketOut.getCount() < 16) {
                    entity.waterAmount -= 1000;
                    bucketIn.decrement(1);
                    bucketOut.increment(1);
                    dirty = true;
                }
            }
        }

        // 2. Auto-eject filled buckets into adjacent container (down or sides)
        ItemStack currentOut = entity.inventory.get(BUCKET_OUT_SLOT);
        if (!currentOut.isEmpty()) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP) continue;
                BlockEntity targetBe = world.getBlockEntity(pos.offset(dir));
                if (targetBe instanceof Inventory targetInv && !(targetBe instanceof WaterPumpBlockEntity)) {
                    for (int s = 0; s < targetInv.size(); s++) {
                        if (targetInv.isValid(s, currentOut)) {
                            ItemStack existing = targetInv.getStack(s);
                            if (existing.isEmpty()) {
                                targetInv.setStack(s, currentOut.split(1));
                                targetInv.markDirty();
                                dirty = true;
                                break;
                            } else if (ItemStack.areItemsEqual(existing, currentOut) && existing.getCount() < existing.getMaxCount()) {
                                existing.increment(1);
                                currentOut.decrement(1);
                                targetInv.markDirty();
                                dirty = true;
                                break;
                            }
                        }
                    }
                    if (currentOut.isEmpty()) {
                        entity.inventory.set(BUCKET_OUT_SLOT, ItemStack.EMPTY);
                        dirty = true;
                        break;
                    }
                }
            }
        }

        // 3. Auto-pull empty buckets from container above
        if (entity.inventory.get(BUCKET_IN_SLOT).getCount() < 16) {
            BlockEntity topBe = world.getBlockEntity(pos.up());
            if (topBe instanceof Inventory topInv && !(topBe instanceof WaterPumpBlockEntity)) {
                for (int s = 0; s < topInv.size(); s++) {
                    ItemStack topStack = topInv.getStack(s);
                    if (!topStack.isEmpty() && (topStack.isOf(Items.BUCKET) || topStack.isOf(ModItems.COPPER_BUCKET))) {
                        ItemStack inSlot = entity.inventory.get(BUCKET_IN_SLOT);
                        if (inSlot.isEmpty()) {
                            entity.inventory.set(BUCKET_IN_SLOT, topStack.split(1));
                            topInv.markDirty();
                            dirty = true;
                            break;
                        } else if (ItemStack.areItemsEqual(inSlot, topStack) && inSlot.getCount() < inSlot.getMaxCount()) {
                            inSlot.increment(1);
                            topStack.decrement(1);
                            topInv.markDirty();
                            dirty = true;
                            break;
                        }
                    }
                }
            }
        }

        // 4. Pump water (rapid from water source, or steady condensation from deep groundwater aquifer)
        boolean hasWater = hasWaterNearby(world, pos);
        boolean hasSpace = entity.waterAmount < MAX_WATER;
        boolean hasEnergy = entity.energyStorage.getEnergy() >= energyDraw;

        boolean isPumping = false;
        if (hasSpace && hasEnergy) {
            entity.energyStorage.extractEnergy(energyDraw, false);
            ++entity.pumpProgress;
            isPumping = true;
            if (entity.pumpProgress >= entity.totalPumpTime) {
                entity.pumpProgress = 0;
                int yield = hasWater ? 500 : 250;
                entity.waterAmount = Math.min(MAX_WATER, entity.waterAmount + yield);
            }
            dirty = true;
        } else {
            if (entity.pumpProgress > 0) {
                entity.pumpProgress = Math.max(0, entity.pumpProgress - 1);
                dirty = true;
            }
        }

        // 3. Push water directly into adjacent Water Providers (e.g. Oxygen Generator)
        if (entity.waterAmount > 0) {
            for (Direction dir : Direction.values()) {
                BlockEntity be = world.getBlockEntity(pos.offset(dir));
                if (be instanceof WaterProvider provider && !(be instanceof WaterPumpBlockEntity)) {
                    if (provider.canInsertWater()) {
                        int toSend = Math.min(entity.waterAmount, 250);
                        int inserted = provider.insertWater(toSend, false);
                        if (inserted > 0) {
                            entity.waterAmount -= inserted;
                            dirty = true;
                        }
                    }
                }
            }
        }

        if (state.get(WaterPumpBlock.LIT) != isPumping) {
            world.setBlockState(pos, state.with(WaterPumpBlock.LIT, isPumping), 3);
            dirty = true;
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    public static boolean hasWaterNearby(World world, BlockPos pos) {
        // Check 3 blocks directly below
        for (int dy = -1; dy >= -3; dy--) {
            BlockPos check = pos.add(0, dy, 0);
            if (world.getFluidState(check).isOf(Fluids.WATER) || world.getBlockState(check).isOf(Blocks.WATER)) {
                return true;
            }
        }
        // Check horizontal neighbors
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos check = pos.offset(dir);
            if (world.getFluidState(check).isOf(Fluids.WATER) || world.getBlockState(check).isOf(Blocks.WATER)) {
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
        this.waterAmount = view.getInt("WaterAmount", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("PumpProgress", this.pumpProgress);
        view.putInt("TotalPumpTime", this.totalPumpTime);
        view.putInt("WaterAmount", this.waterAmount);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{BUCKET_OUT_SLOT};
        if (side == Direction.UP) return new int[]{BUCKET_IN_SLOT};
        return new int[]{BUCKET_IN_SLOT, GEAR_SLOT, BUCKET_OUT_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == BUCKET_IN_SLOT) {
            return stack.isOf(Items.BUCKET) || stack.isOf(ModItems.COPPER_BUCKET);
        }
        if (slot == GEAR_SLOT) {
            return stack.getItem() instanceof GearItem;
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == BUCKET_OUT_SLOT;
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : this.inventory) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.inventory, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
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
        this.inventory.clear();
    }
}
