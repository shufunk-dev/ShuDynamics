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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class CryoFreezerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider, WaterProvider {
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

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int freezeProgress = 0;
    private int totalFreezeTime = 80;
    private int waterAmount = 0;

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
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
        return Component.translatable("block.enchantedwood.cryo_freezer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CryoFreezerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, CryoFreezerBlockEntity entity) {
        boolean dirty = false;

        GearTier tier = entity.getActiveGearTier();
        entity.totalFreezeTime = getTierFreezeTime(tier);
        int energyDraw = getTierEnergyDraw(tier);

        // 1. Process Water Buckets into internal tank
        ItemStack bucketIn = entity.inventory.get(WATER_IN_SLOT);
        if (!bucketIn.isEmpty() && entity.waterAmount <= MAX_WATER - 1000) {
            boolean isVanilla = bucketIn.is(Items.WATER_BUCKET);
            boolean isCopper = bucketIn.is(ModItems.COPPER_WATER_BUCKET);

            if (isVanilla || isCopper) {
                ItemStack emptyBucket = isVanilla ? new ItemStack(Items.BUCKET) : new ItemStack(ModItems.COPPER_BUCKET);
                ItemStack bucketOut = entity.inventory.get(BUCKET_OUT_SLOT);

                if (bucketOut.isEmpty()) {
                    entity.waterAmount += 1000;
                    bucketIn.shrink(1);
                    entity.inventory.set(BUCKET_OUT_SLOT, emptyBucket);
                    dirty = true;
                } else if (ItemStack.isSameItem(bucketOut, emptyBucket) && bucketOut.getCount() < bucketOut.getMaxStackSize()) {
                    entity.waterAmount += 1000;
                    bucketIn.shrink(1);
                    bucketOut.grow(1);
                    dirty = true;
                }
            }
        }

        // 2. Auto-siphon water from adjacent Water Providers (e.g. Water Pump or pipes)
        if (entity.waterAmount < MAX_WATER) {
            for (Direction dir : Direction.values()) {
                BlockEntity be = world.getBlockEntity(pos.relative(dir));
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
            if (solidIn.is(Items.ICE)) {
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
            } else if (solidIn.is(Items.PACKED_ICE)) {
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
            } else if (solidIn.is(Items.SNOWBALL)) {
                // 4x Snowball -> 1x Snow Block
                if (solidIn.getCount() >= 4) {
                    recipeResult = new ItemStack(Items.SNOW_BLOCK);
                    waterCost = 0;
                    solidCost = 4;
                }
            } else if (solidIn.is(ModItems.ICE_CUBES)) {
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
                (outSlot.isEmpty() || (ItemStack.isSameItem(outSlot, recipeResult) && outSlot.getCount() + recipeResult.getCount() <= outSlot.getMaxStackSize()));

        boolean hasEnergy = entity.energyStorage.getEnergy() >= energyDraw;
        boolean isFreezing = false;

        if (canFitOutput && hasEnergy) {
            entity.energyStorage.extractEnergy(energyDraw, false);
            entity.freezeProgress++;
            isFreezing = true;

            // Ambient frost and vapor particles
            if (world.getGameTime() % 5 == 0) {
                world.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.15, 0.1, 0.15, 0.02);
            }

            if (entity.freezeProgress >= entity.totalFreezeTime) {
                entity.freezeProgress = 0;
                if (waterCost > 0) entity.waterAmount -= waterCost;
                if (solidCost > 0) solidIn.shrink(solidCost);

                if (outSlot.isEmpty()) {
                    entity.inventory.set(OUTPUT_SLOT, recipeResult.copy());
                } else {
                    outSlot.grow(recipeResult.getCount());
                }

                world.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.7f, 1.6f);
                world.playSound(null, pos, SoundEvents.SNOW_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
                world.sendParticles(ParticleTypes.ITEM_SNOWBALL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.05);
            }
            dirty = true;
        } else {
            if (entity.freezeProgress > 0) {
                entity.freezeProgress = Math.max(0, entity.freezeProgress - 1);
                dirty = true;
            }
        }

        if (state.getValue(CryoFreezerBlock.LIT) != isFreezing) {
            world.setBlock(pos, state.setValue(CryoFreezerBlock.LIT, isFreezing), 3);
            dirty = true;
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
        this.freezeProgress = view.getIntOr("FreezeProgress", 0);
        this.totalFreezeTime = view.getIntOr("TotalFreezeTime", 80);
        this.waterAmount = view.getIntOr("WaterAmount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("FreezeProgress", this.freezeProgress);
        view.putInt("TotalFreezeTime", this.totalFreezeTime);
        view.putInt("WaterAmount", this.waterAmount);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{BUCKET_OUT_SLOT, OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{WATER_IN_SLOT, SOLID_IN_SLOT};
        return new int[]{SOLID_IN_SLOT, GEAR_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == WATER_IN_SLOT) {
            return stack.is(Items.WATER_BUCKET) || stack.is(ModItems.COPPER_WATER_BUCKET);
        }
        if (slot == SOLID_IN_SLOT) {
            return stack.is(Items.ICE) || stack.is(Items.PACKED_ICE) || stack.is(Items.SNOWBALL) || stack.is(ModItems.ICE_CUBES);
        }
        if (slot == GEAR_SLOT) {
            return stack.getItem() instanceof GearItem;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == BUCKET_OUT_SLOT || slot == OUTPUT_SLOT;
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
