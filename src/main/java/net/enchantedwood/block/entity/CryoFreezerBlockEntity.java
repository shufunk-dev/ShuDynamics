package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.CryoFreezerBlock;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.fluid.WaterProvider;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.CryoFreezerScreenHandler;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class CryoFreezerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider, WaterProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int BASE_ENERGY_DRAW = 25; // 25 FE/t
    public static final int MAX_WATER = 10_000; // 10,000 mB

    public static final int WATER_IN_SLOT = 0;
    public static final int BUCKET_OUT_SLOT = 1;
    public static final int SOLID_IN_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int GEAR_SLOT = 4;
    public static final int INVENTORY_SIZE = 5;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int freezeProgress = 0;
    private int totalFreezeTime = 80;
    private int waterAmount = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> freezeProgress;
                case 1 -> totalFreezeTime;
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
                case 0 -> freezeProgress = value;
                case 1 -> totalFreezeTime = value;
                case 6 -> waterAmount = value;
            }
        }

        @Override
        public int size() {
            return 8;
        }
    };

    public CryoFreezerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYO_FREEZER_BE, pos, state);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public static int getTierFreezeTime(GearTier tier) {
        return switch (tier) {
            case IRON -> 65;
            case COPPER -> 50;
            case BRONZE -> 40;
            case STEEL -> 30;
            case GOLD -> 20;
            case TITANIUM -> 15;
            case DIAMOND -> 10;
            case NETHERITE -> 5;
            case BLAZE_OVERCLOCK -> 2;
            default -> 80;
        };
    }

    public static int getTierEnergyDraw(GearTier tier) {
        return switch (tier) {
            case IRON -> 22;
            case COPPER -> 20;
            case BRONZE -> 18;
            case STEEL -> 16;
            case GOLD -> 14;
            case TITANIUM -> 12;
            case DIAMOND -> 10;
            case NETHERITE -> 6;
            case BLAZE_OVERCLOCK -> 4;
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
        return Text.translatable("block.enchantedwood.cryo_freezer");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CryoFreezerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, CryoFreezerBlockEntity entity) {
        boolean dirty = false;

        GearTier tier = entity.getActiveGearTier();
        entity.totalFreezeTime = getTierFreezeTime(tier);
        int energyDraw = getTierEnergyDraw(tier);

        // 1. Process Water Buckets into internal tank
        ItemStack bucketIn = entity.inventory.get(WATER_IN_SLOT);
        if (!bucketIn.isEmpty() && entity.waterAmount <= MAX_WATER - 1000) {
            boolean isVanilla = bucketIn.isOf(Items.WATER_BUCKET);
            boolean isCopper = bucketIn.isOf(ModItems.COPPER_WATER_BUCKET);

            if (isVanilla || isCopper) {
                ItemStack emptyBucket = isVanilla ? new ItemStack(Items.BUCKET) : new ItemStack(ModItems.COPPER_BUCKET);
                ItemStack bucketOut = entity.inventory.get(BUCKET_OUT_SLOT);

                if (bucketOut.isEmpty()) {
                    entity.waterAmount += 1000;
                    bucketIn.decrement(1);
                    entity.inventory.set(BUCKET_OUT_SLOT, emptyBucket);
                    dirty = true;
                } else if (ItemStack.areItemsEqual(bucketOut, emptyBucket) && bucketOut.getCount() < bucketOut.getMaxCount()) {
                    entity.waterAmount += 1000;
                    bucketIn.decrement(1);
                    bucketOut.increment(1);
                    dirty = true;
                }
            }
        }

        // 2. Auto-siphon water from adjacent Water Providers (e.g. Water Pump or pipes)
        if (entity.waterAmount < MAX_WATER) {
            for (Direction dir : Direction.values()) {
                BlockEntity be = world.getBlockEntity(pos.offset(dir));
                if (be instanceof WaterProvider provider && !(be instanceof CryoFreezerBlockEntity)) {
                    if (provider.canExtractWater()) {
                        int needed = Math.min(MAX_WATER - entity.waterAmount, 250);
                        int extracted = provider.extractWater(needed, false);
                        if (extracted > 0) {
                            entity.waterAmount += extracted;
                            dirty = true;
                            if (entity.waterAmount >= MAX_WATER) break;
                        }
                    }
                }
            }
        }

        // 3. Evaluate Freezing Recipe Operation
        ItemStack solidIn = entity.inventory.get(SOLID_IN_SLOT);
        ItemStack outSlot = entity.inventory.get(OUTPUT_SLOT);

        ItemStack recipeResult = ItemStack.EMPTY;
        int waterCost = 0;
        int solidCost = 0;

        if (!solidIn.isEmpty()) {
            if (solidIn.isOf(Items.ICE)) {
                // Ice + 500 mB Water -> 1x Packed Ice (or 4 Ice dry compression)
                if (entity.waterAmount >= 500 && solidIn.getCount() >= 1) {
                    recipeResult = new ItemStack(Items.PACKED_ICE);
                    waterCost = 500;
                    solidCost = 1;
                } else if (solidIn.getCount() >= 4) {
                    recipeResult = new ItemStack(Items.PACKED_ICE);
                    waterCost = 0;
                    solidCost = 4;
                }
            } else if (solidIn.isOf(Items.PACKED_ICE)) {
                // Packed Ice + 1000 mB Water -> 1x Blue Ice (or 4 Packed Ice dry compression)
                if (entity.waterAmount >= 1000 && solidIn.getCount() >= 1) {
                    recipeResult = new ItemStack(Items.BLUE_ICE);
                    waterCost = 1000;
                    solidCost = 1;
                } else if (solidIn.getCount() >= 4) {
                    recipeResult = new ItemStack(Items.BLUE_ICE);
                    waterCost = 0;
                    solidCost = 4;
                }
            } else if (solidIn.isOf(Items.SNOWBALL)) {
                // 4x Snowball -> 1x Snow Block
                if (solidIn.getCount() >= 4) {
                    recipeResult = new ItemStack(Items.SNOW_BLOCK);
                    waterCost = 0;
                    solidCost = 4;
                }
            } else if (solidIn.isOf(ModItems.ICE_CUBES)) {
                // 4x Ice Cubes + 250 mB Water -> 1x Ice Block
                if (entity.waterAmount >= 250 && solidIn.getCount() >= 4) {
                    recipeResult = new ItemStack(Items.ICE);
                    waterCost = 250;
                    solidCost = 4;
                }
            }
        } else {
            // Solid In is Empty: Direct Water Cryo-Freezing!
            // 1000 mB Water -> 1x Ice
            if (entity.waterAmount >= 1000) {
                recipeResult = new ItemStack(Items.ICE);
                waterCost = 1000;
                solidCost = 0;
            }
        }

        boolean canFitOutput = !recipeResult.isEmpty() &&
                (outSlot.isEmpty() || (ItemStack.areItemsEqual(outSlot, recipeResult) && outSlot.getCount() + recipeResult.getCount() <= outSlot.getMaxCount()));

        boolean hasEnergy = entity.energyStorage.getEnergy() >= energyDraw;
        boolean isFreezing = false;

        if (canFitOutput && hasEnergy) {
            entity.energyStorage.extractEnergy(energyDraw, false);
            entity.freezeProgress++;
            isFreezing = true;

            // Ambient frost and vapor particles
            if (world.getTime() % 5 == 0) {
                world.spawnParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.15, 0.1, 0.15, 0.02);
            }

            if (entity.freezeProgress >= entity.totalFreezeTime) {
                entity.freezeProgress = 0;
                if (waterCost > 0) entity.waterAmount -= waterCost;
                if (solidCost > 0) solidIn.decrement(solidCost);

                if (outSlot.isEmpty()) {
                    entity.inventory.set(OUTPUT_SLOT, recipeResult.copy());
                } else {
                    outSlot.increment(recipeResult.getCount());
                }

                world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.7f, 1.6f);
                world.playSound(null, pos, SoundEvents.BLOCK_SNOW_PLACE, SoundCategory.BLOCKS, 0.8f, 1.2f);
                world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.05);
            }
            dirty = true;
        } else {
            if (entity.freezeProgress > 0) {
                entity.freezeProgress = Math.max(0, entity.freezeProgress - 1);
                dirty = true;
            }
        }

        if (state.get(CryoFreezerBlock.LIT) != isFreezing) {
            world.setBlockState(pos, state.with(CryoFreezerBlock.LIT, isFreezing), 3);
            dirty = true;
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
        this.freezeProgress = view.getInt("FreezeProgress", 0);
        this.totalFreezeTime = view.getInt("TotalFreezeTime", 80);
        this.waterAmount = view.getInt("WaterAmount", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("FreezeProgress", this.freezeProgress);
        view.putInt("TotalFreezeTime", this.totalFreezeTime);
        view.putInt("WaterAmount", this.waterAmount);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{BUCKET_OUT_SLOT, OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{WATER_IN_SLOT, SOLID_IN_SLOT};
        return new int[]{SOLID_IN_SLOT, GEAR_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == WATER_IN_SLOT) {
            return stack.isOf(Items.WATER_BUCKET) || stack.isOf(ModItems.COPPER_WATER_BUCKET);
        }
        if (slot == SOLID_IN_SLOT) {
            return stack.isOf(Items.ICE) || stack.isOf(Items.PACKED_ICE) || stack.isOf(Items.SNOWBALL) || stack.isOf(ModItems.ICE_CUBES);
        }
        if (slot == GEAR_SLOT) {
            return stack.getItem() instanceof GearItem;
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == BUCKET_OUT_SLOT || slot == OUTPUT_SLOT;
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
