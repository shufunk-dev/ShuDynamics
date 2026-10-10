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
import org.jetbrains.annotations.Nullable;

public class WaterPumpBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider, WaterProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int BASE_ENERGY_DRAW = 20; // 20 FE/t
    public static final int MAX_WATER = 10_000; // 10,000 mB

    public static final int BUCKET_IN_SLOT = 0;
    public static final int BUCKET_OUT_SLOT = 1;
    public static final int GEAR_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int pumpProgress = 0;
    private int totalPumpTime = 40;
    private int waterAmount = 0;

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
        public int getCount() {
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
            setChanged();
        }
        return toInsert;
    }

    @Override
    public int extractWater(int amount, boolean simulate) {
        int toDrain = Math.min(this.waterAmount, amount);
        if (!simulate && toDrain > 0) {
            this.waterAmount -= toDrain;
            setChanged();
        }
        return toDrain;
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enchantedwood.water_pump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new WaterPumpScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, WaterPumpBlockEntity entity) {
        boolean dirty = false;

        GearTier tier = entity.getActiveGearTier();
        entity.totalPumpTime = getTierPumpTime(tier);
        int energyDraw = getTierEnergyDraw(tier);

        // 1. Fill empty buckets with water from internal tank
        ItemStack bucketIn = entity.inventory.get(BUCKET_IN_SLOT);
        if (!bucketIn.isEmpty() && entity.waterAmount >= 1000) {
            boolean isVanillaBucket = bucketIn.is(Items.BUCKET);
            boolean isCopperBucket = bucketIn.is(ModItems.COPPER_BUCKET);

            if (isVanillaBucket || isCopperBucket) {
                ItemStack filledItem = isVanillaBucket ? new ItemStack(Items.WATER_BUCKET) : new ItemStack(ModItems.COPPER_WATER_BUCKET);
                ItemStack bucketOut = entity.inventory.get(BUCKET_OUT_SLOT);

                if (bucketOut.isEmpty()) {
                    entity.waterAmount -= 1000;
                    bucketIn.shrink(1);
                    entity.inventory.set(BUCKET_OUT_SLOT, filledItem);
                    dirty = true;
                } else if (ItemStack.isSameItem(bucketOut, filledItem) && bucketOut.getCount() < 16) {
                    entity.waterAmount -= 1000;
                    bucketIn.shrink(1);
                    bucketOut.grow(1);
                    dirty = true;
                }
            }
        }

        // 2. Auto-eject filled buckets into adjacent container (down or sides)
        ItemStack currentOut = entity.inventory.get(BUCKET_OUT_SLOT);
        if (!currentOut.isEmpty()) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP) continue;
                BlockEntity targetBe = world.getBlockEntity(pos.relative(dir));
                if (targetBe instanceof Container targetInv && !(targetBe instanceof WaterPumpBlockEntity)) {
                    for (int s = 0; s < targetInv.getContainerSize(); s++) {
                        if (targetInv.canPlaceItem(s, currentOut)) {
                            ItemStack existing = targetInv.getItem(s);
                            if (existing.isEmpty()) {
                                targetInv.setItem(s, currentOut.split(1));
                                targetInv.setChanged();
                                dirty = true;
                                break;
                            } else if (ItemStack.isSameItem(existing, currentOut) && existing.getCount() < existing.getMaxStackSize()) {
                                existing.grow(1);
                                currentOut.shrink(1);
                                targetInv.setChanged();
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
            BlockEntity topBe = world.getBlockEntity(pos.above());
            if (topBe instanceof Container topInv && !(topBe instanceof WaterPumpBlockEntity)) {
                for (int s = 0; s < topInv.getContainerSize(); s++) {
                    ItemStack topStack = topInv.getItem(s);
                    if (!topStack.isEmpty() && (topStack.is(Items.BUCKET) || topStack.is(ModItems.COPPER_BUCKET))) {
                        ItemStack inSlot = entity.inventory.get(BUCKET_IN_SLOT);
                        if (inSlot.isEmpty()) {
                            entity.inventory.set(BUCKET_IN_SLOT, topStack.split(1));
                            topInv.setChanged();
                            dirty = true;
                            break;
                        } else if (ItemStack.isSameItem(inSlot, topStack) && inSlot.getCount() < inSlot.getMaxStackSize()) {
                            inSlot.grow(1);
                            topStack.shrink(1);
                            topInv.setChanged();
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
                BlockEntity be = world.getBlockEntity(pos.relative(dir));
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

        if (state.getValue(WaterPumpBlock.LIT) != isPumping) {
            world.setBlock(pos, state.setValue(WaterPumpBlock.LIT, isPumping), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    public static boolean hasWaterNearby(Level world, BlockPos pos) {
        // Check 3 blocks directly below
        for (int dy = -1; dy >= -3; dy--) {
            BlockPos check = pos.offset(0, dy, 0);
            if (world.getFluidState(check).is(Fluids.WATER) || world.getBlockState(check).is(Blocks.WATER)) {
                return true;
            }
        }
        // Check horizontal neighbors
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos check = pos.relative(dir);
            if (world.getFluidState(check).is(Fluids.WATER) || world.getBlockState(check).is(Blocks.WATER)) {
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
        this.waterAmount = view.getIntOr("WaterAmount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("PumpProgress", this.pumpProgress);
        view.putInt("TotalPumpTime", this.totalPumpTime);
        view.putInt("WaterAmount", this.waterAmount);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{BUCKET_OUT_SLOT};
        if (side == Direction.UP) return new int[]{BUCKET_IN_SLOT};
        return new int[]{BUCKET_IN_SLOT, GEAR_SLOT, BUCKET_OUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == BUCKET_IN_SLOT) {
            return stack.is(Items.BUCKET) || stack.is(ModItems.COPPER_BUCKET);
        }
        if (slot == GEAR_SLOT) {
            return stack.getItem() instanceof GearItem;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == BUCKET_OUT_SLOT;
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.inventory, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
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
        this.inventory.clear();
    }
}
