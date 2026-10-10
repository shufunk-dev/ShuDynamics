package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.MagmaCrucibleBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.MagmaCrucibleScreenHandler;
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
import net.enchantedwood.fluid.LavaProvider;
import org.jetbrains.annotations.Nullable;

public class MagmaCrucibleBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider, LavaProvider {
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

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 140;
    private int lavaAmount = 0;

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
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
            case TITANIUM -> 50;
            case DIAMOND -> 40;
            case NETHERITE -> 18;
            case BLAZE_OVERCLOCK -> 10;
            default -> 140;
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
        return Component.translatable("block.enchantedwood.magma_crucible");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new MagmaCrucibleScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, MagmaCrucibleBlockEntity entity) {
        boolean dirty = false;

        entity.totalCookTime = getTierCookTime(entity.getActiveGearTier());

        // 1. Fill empty bucket with liquid lava from internal tank
        ItemStack bucketIn = entity.inventory.get(BUCKET_INPUT_SLOT);
        if (!bucketIn.isEmpty() && entity.lavaAmount >= 1000) {
            boolean isVanillaBucket = bucketIn.is(Items.BUCKET);
            boolean isCopperBucket = bucketIn.is(ModItems.COPPER_BUCKET);
            if (isVanillaBucket || isCopperBucket) {
                ItemStack filledItem = isVanillaBucket ? new ItemStack(Items.LAVA_BUCKET) : new ItemStack(ModItems.COPPER_LAVA_BUCKET);
                ItemStack bucketOut = entity.inventory.get(BUCKET_OUTPUT_SLOT);
                if (bucketOut.isEmpty()) {
                    entity.lavaAmount -= 1000;
                    bucketIn.shrink(1);
                    entity.inventory.set(BUCKET_OUTPUT_SLOT, filledItem);
                    dirty = true;
                } else if (ItemStack.isSameItem(bucketOut, filledItem) && bucketOut.getCount() < bucketOut.getMaxStackSize()) {
                    entity.lavaAmount -= 1000;
                    bucketIn.shrink(1);
                    bucketOut.grow(1);
                    dirty = true;
                }
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

        if (state.getValue(MagmaCrucibleBlock.LIT) != isMelting) {
            world.setBlock(pos, state.setValue(MagmaCrucibleBlock.LIT, isMelting), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    private boolean canAcceptResult(CrucibleResult result) {
        if (this.lavaAmount + result.lavaYield > MAX_LAVA) return false;
        if (result.mineralItem == null) return true;
        ItemStack out = inventory.get(MINERAL_OUTPUT_SLOT);
        if (out.isEmpty()) return true;
        if (!out.is(result.mineralItem)) return false;
        return out.getCount() + result.mineralCount <= out.getMaxStackSize();
    }

    private void processMelt(CrucibleResult result) {
        ItemStack input = inventory.get(INPUT_SLOT);
        input.shrink(1);

        this.lavaAmount = Math.min(MAX_LAVA, this.lavaAmount + result.lavaYield);

        if (result.mineralItem != null) {
            ItemStack out = inventory.get(MINERAL_OUTPUT_SLOT);
            if (out.isEmpty()) {
                inventory.set(MINERAL_OUTPUT_SLOT, new ItemStack(result.mineralItem, result.mineralCount));
            } else {
                out.grow(result.mineralCount);
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
        if (item == Items.COBBLESTONE || item == Items.STONE || item == Items.STONE_BRICKS ||
                item == Items.MOSSY_COBBLESTONE || item == Items.MOSSY_STONE_BRICKS ||
                item == Items.DEEPSLATE || item == Items.COBBLED_DEEPSLATE || item == Items.POLISHED_DEEPSLATE ||
                item == Items.DEEPSLATE_BRICKS || item == Items.DEEPSLATE_TILES ||
                item == Items.ANDESITE || item == Items.POLISHED_ANDESITE ||
                item == Items.DIORITE || item == Items.POLISHED_DIORITE ||
                item == Items.GRANITE || item == Items.POLISHED_GRANITE ||
                item == Items.TUFF || item == Items.POLISHED_TUFF ||
                item == Items.CALCITE || item == Items.DRIPSTONE_BLOCK ||
                item == Items.SANDSTONE || item == Items.RED_SANDSTONE ||
                item == Items.TERRACOTTA) {
            return new CrucibleResult(100, ModItems.VOLCANIC_ASH, 1);
        }
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 140);
        this.lavaAmount = view.getIntOr("LavaAmount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        view.putInt("LavaAmount", this.lavaAmount);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{MINERAL_OUTPUT_SLOT, BUCKET_OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{INPUT_SLOT, BUCKET_INPUT_SLOT};
        return new int[]{INPUT_SLOT, BUCKET_INPUT_SLOT, GEAR_SLOT, MINERAL_OUTPUT_SLOT, BUCKET_OUTPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT) return getCrucibleResult(stack.getItem()) != null;
        if (slot == BUCKET_INPUT_SLOT) return stack.is(Items.BUCKET);
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == MINERAL_OUTPUT_SLOT || slot == BUCKET_OUTPUT_SLOT;
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
            setChanged();
        }
        return extracted;
    }

    @Override
    public boolean canExtractLava() {
        return this.lavaAmount > 0;
    }
}
