package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.CrusherMk2Block;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.CrusherMk2ScreenHandler;
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
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class CrusherMk2BlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 50; // 50 FE/t

    public static final int INPUT_SLOT = 0;
    public static final int PRIMARY_OUTPUT_SLOT = 1;
    public static final int BYPRODUCT_OUTPUT_SLOT = 2;
    public static final int GEAR_SLOT = 3;
    public static final int INVENTORY_SIZE = 4;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 100;
    private float experience = 0.0f;

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
                case 6 -> getActiveGearTier().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookTime = value;
                case 1 -> totalCookTime = value;
            }
        }

        @Override
        public int size() {
            return 7;
        }
    };

    public CrusherMk2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER_MK2_BE, pos, state);
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
            case IRON -> 80;
            case COPPER -> 70;
            case BRONZE -> 55;
            case GOLD -> 40;
            case DIAMOND -> 25;
            case NETHERITE -> 10;
            default -> 100;
        };
    }

    public int getTierYield() {
        return switch (getActiveGearTier()) {
            case IRON -> 3;
            case COPPER -> 3;
            case BRONZE -> 4;
            case GOLD -> 4;
            case DIAMOND -> 5;
            case NETHERITE -> 6;
            default -> 3;
        };
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.crusher_mk2");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CrusherMk2ScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, CrusherMk2BlockEntity entity) {
        boolean dirty = false;

        entity.totalCookTime = getTierCookTime(entity.getActiveGearTier());

        ItemStack input = entity.inventory.get(INPUT_SLOT);
        Mk2CrushRecipe recipe = getRecipe(input.getItem());

        boolean canProcess = recipe != null && entity.canAcceptOutputs(recipe);
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        boolean isCrushing = false;
        if (canProcess && hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            ++entity.cookTime;
            isCrushing = true;
            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                entity.processCrush(recipe);
            }
            dirty = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                dirty = true;
            }
        }

        if (state.get(CrusherMk2Block.LIT) != isCrushing) {
            world.setBlockState(pos, state.with(CrusherMk2Block.LIT, isCrushing), 3);
            dirty = true;
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    private boolean canAcceptOutputs(Mk2CrushRecipe recipe) {
        int primaryAmount = recipe.isBlock ? recipe.baseCount : getTierYield();
        ItemStack currentPri = inventory.get(PRIMARY_OUTPUT_SLOT);
        if (!currentPri.isEmpty() && (!currentPri.isOf(recipe.primaryOutput) || currentPri.getCount() + primaryAmount > currentPri.getMaxCount())) {
            return false;
        }

        if (recipe.byproduct != null) {
            ItemStack currentBy = inventory.get(BYPRODUCT_OUTPUT_SLOT);
            if (!currentBy.isEmpty() && (!currentBy.isOf(recipe.byproduct) || currentBy.getCount() + recipe.byproductCount > currentBy.getMaxCount())) {
                return false;
            }
        }

        return true;
    }

    private void processCrush(Mk2CrushRecipe recipe) {
        ItemStack input = inventory.get(INPUT_SLOT);
        input.decrement(1);

        int primaryAmount = recipe.isBlock ? recipe.baseCount : getTierYield();
        ItemStack currentPri = inventory.get(PRIMARY_OUTPUT_SLOT);
        if (currentPri.isEmpty()) {
            inventory.set(PRIMARY_OUTPUT_SLOT, new ItemStack(recipe.primaryOutput, primaryAmount));
        } else {
            currentPri.increment(primaryAmount);
        }

        if (recipe.byproduct != null) {
            ItemStack currentBy = inventory.get(BYPRODUCT_OUTPUT_SLOT);
            if (currentBy.isEmpty()) {
                inventory.set(BYPRODUCT_OUTPUT_SLOT, new ItemStack(recipe.byproduct, recipe.byproductCount));
            } else {
                currentBy.increment(recipe.byproductCount);
            }
        }

        this.experience += 1.0f;
    }

    public static record Mk2CrushRecipe(Item primaryOutput, int baseCount, boolean isBlock, @Nullable Item byproduct, int byproductCount) {}

    public static @Nullable Mk2CrushRecipe getRecipe(Item item) {
        // Tungsten
        if (item == ModItems.RAW_TUNGSTEN || item == ModBlocks.NETHER_TUNGSTEN_ORE.asItem() || item == ModBlocks.DEEPSLATE_TUNGSTEN_ORE.asItem()) {
            return new Mk2CrushRecipe(ModItems.TUNGSTEN_DUST, 3, false, ModItems.VOLCANIC_ASH, 1);
        }
        if (item == ModBlocks.RAW_TUNGSTEN_BLOCK.asItem() || item == ModBlocks.TUNGSTEN_BLOCK.asItem()) {
            return new Mk2CrushRecipe(ModItems.TUNGSTEN_DUST, 27, true, ModItems.VOLCANIC_ASH, 3);
        }
        if (item == ModItems.TUNGSTEN_INGOT) {
            return new Mk2CrushRecipe(ModItems.TUNGSTEN_DUST, 1, true, null, 0);
        }

        // Cobalt
        if (item == ModItems.RAW_COBALT || item == ModBlocks.COBALT_ORE.asItem()) {
            return new Mk2CrushRecipe(ModItems.COBALT_DUST, 3, false, ModItems.SULFUR_DUST, 1);
        }
        if (item == ModBlocks.RAW_COBALT_BLOCK.asItem() || item == ModBlocks.COBALT_BLOCK.asItem()) {
            return new Mk2CrushRecipe(ModItems.COBALT_DUST, 27, true, ModItems.SULFUR_DUST, 3);
        }
        if (item == ModItems.COBALT_INGOT) {
            return new Mk2CrushRecipe(ModItems.COBALT_DUST, 1, true, null, 0);
        }

        // Ardite
        if (item == ModItems.RAW_ARDITE || item == ModBlocks.ARDITE_ORE.asItem()) {
            return new Mk2CrushRecipe(ModItems.ARDITE_DUST, 3, false, ModItems.FIRE_CRYSTAL, 1);
        }
        if (item == ModBlocks.RAW_ARDITE_BLOCK.asItem() || item == ModBlocks.ARDITE_BLOCK.asItem()) {
            return new Mk2CrushRecipe(ModItems.ARDITE_DUST, 27, true, ModItems.FIRE_CRYSTAL, 3);
        }
        if (item == ModItems.ARDITE_INGOT) {
            return new Mk2CrushRecipe(ModItems.ARDITE_DUST, 1, true, null, 0);
        }

        // Manyullyn
        if (item == ModItems.MANYULLYN_INGOT) {
            return new Mk2CrushRecipe(ModItems.MANYULLYN_DUST, 1, true, null, 0);
        }
        if (item == ModBlocks.MANYULLYN_BLOCK.asItem()) {
            return new Mk2CrushRecipe(ModItems.MANYULLYN_DUST, 9, true, null, 0);
        }

        // Iron
        if (item == Items.RAW_IRON || item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE) {
            return new Mk2CrushRecipe(ModItems.IRON_DUST, 3, false, ModItems.TIN_DUST, 1);
        }
        if (item == Items.RAW_IRON_BLOCK || item == Items.IRON_BLOCK) {
            return new Mk2CrushRecipe(ModItems.IRON_DUST, 27, true, ModItems.TIN_DUST, 3);
        }

        // Copper
        if (item == Items.RAW_COPPER || item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE) {
            return new Mk2CrushRecipe(ModItems.COPPER_DUST, 4, false, ModItems.GOLD_DUST, 1);
        }
        if (item == Items.RAW_COPPER_BLOCK || item == Items.COPPER_BLOCK) {
            return new Mk2CrushRecipe(ModItems.COPPER_DUST, 36, true, ModItems.GOLD_DUST, 3);
        }

        // Gold
        if (item == Items.RAW_GOLD || item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE || item == Items.NETHER_GOLD_ORE) {
            return new Mk2CrushRecipe(ModItems.GOLD_DUST, 3, false, ModItems.COPPER_DUST, 1);
        }

        // Tin & Titanium
        if (item == ModItems.RAW_TIN || item == ModBlocks.TIN_ORE.asItem() || item == ModBlocks.DEEPSLATE_TIN_ORE.asItem()) {
            return new Mk2CrushRecipe(ModItems.TIN_DUST, 3, false, ModItems.IRON_DUST, 1);
        }
        if (item == ModItems.RAW_TITANIUM || item == ModBlocks.TITANIUM_ORE.asItem() || item == ModBlocks.DEEPSLATE_TITANIUM_ORE.asItem()) {
            return new Mk2CrushRecipe(ModItems.TITANIUM_DUST, 3, false, ModItems.VOLCANIC_ASH, 1);
        }

        // Diamonds & Emeralds
        if (item == Items.DIAMOND_ORE || item == Items.DEEPSLATE_DIAMOND_ORE) {
            return new Mk2CrushRecipe(ModItems.DIAMOND_DUST, 3, false, ModItems.EMERALD_DUST, 1);
        }
        if (item == Items.EMERALD_ORE || item == Items.DEEPSLATE_EMERALD_ORE) {
            return new Mk2CrushRecipe(ModItems.EMERALD_DUST, 3, false, ModItems.DIAMOND_DUST, 1);
        }

        // Netherite Debris
        if (item == Items.ANCIENT_DEBRIS || item == Items.NETHERITE_SCRAP) {
            return new Mk2CrushRecipe(ModItems.NETHERITE_DUST, 2, false, ModItems.GOLD_DUST, 2);
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
        this.totalCookTime = view.getInt("TotalCookTime", 100);
        this.experience = view.getFloat("Experience", 0.0f);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        view.putFloat("Experience", this.experience);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{PRIMARY_OUTPUT_SLOT, BYPRODUCT_OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{INPUT_SLOT};
        return new int[]{INPUT_SLOT, GEAR_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT) return getRecipe(stack.getItem()) != null;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == PRIMARY_OUTPUT_SLOT || slot == BYPRODUCT_OUTPUT_SLOT;
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
}
