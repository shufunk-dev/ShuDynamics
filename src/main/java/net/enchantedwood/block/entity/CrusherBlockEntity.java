package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.CrusherBlock;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.CrusherScreenHandler;
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
import org.jetbrains.annotations.Nullable;

public class CrusherBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);

    private static final int INPUT_SLOT = 0;
    private static final int GEAR_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    private int cookTime = 0;
    private int totalCookTime = 160;
    private float experience = 0.0f;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> cookTime;
                case 5 -> totalCookTime;
                case 6 -> getActiveGearTier().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 4 -> cookTime = value;
                case 5 -> totalCookTime = value;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.crusher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CrusherScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(1);
        if (gearStack.getItem() instanceof net.enchantedwood.item.custom.GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public boolean isGearEnchanted() {
        ItemStack gearStack = inventory.get(1);
        if (gearStack.getItem() instanceof net.enchantedwood.item.custom.GearItem gearItem) {
            return gearItem.isEnchanted();
        }
        return false;
    }

    public static int getTierCookTime(GearTier tier) {
        return switch (tier) {
            case IRON -> 140;
            case COPPER -> 120;
            case BRONZE -> 100;
            case GOLD -> 80;
            case DIAMOND -> 50;
            case NETHERITE -> 25;
            case BLAZE_OVERCLOCK -> 15;
            default -> 160;
        };
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, CrusherBlockEntity entity) {
        boolean stateChanged = false;

        GearTier currentGearTier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(currentGearTier);

        if (state.getValue(CrusherBlock.GEAR_TIER) != currentGearTier) {
            state = state.setValue(CrusherBlock.GEAR_TIER, currentGearTier);
            world.setBlock(pos, state, 3);
            stateChanged = true;
        }

        ItemStack inputStack = entity.inventory.get(0);
        boolean canProcess = entity.canProcessInput(inputStack, currentGearTier);
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        if (canProcess && hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            ++entity.cookTime;
            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                entity.processInput(inputStack, currentGearTier);
            }
            stateChanged = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                stateChanged = true;
            }
        }

        boolean isRunningNow = canProcess && hasEnergy;
        if (state.getValue(CrusherBlock.LIT) != isRunningNow) {
            world.setBlock(pos, state.setValue(CrusherBlock.LIT, isRunningNow), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(world, pos, state);
        }
    }

    private boolean canProcessInput(ItemStack inputStack, GearTier tier) {
        if (inputStack.isEmpty()) return false;
        Item outputDust = getOutputDust(inputStack.getItem());
        if (outputDust == null) return false;

        boolean isEnchanted = isGearEnchanted();
        int yieldMultiplier = getYieldMultiplier(inputStack.getItem(), tier, isEnchanted);
        ItemStack outputSlot = inventory.get(2);
        if (outputSlot.isEmpty()) return true;
        if (!outputSlot.is(outputDust)) return false;
        return outputSlot.getCount() + yieldMultiplier <= outputSlot.getMaxStackSize();
    }

    private void processInput(ItemStack inputStack, GearTier tier) {
        Item outputDust = getOutputDust(inputStack.getItem());
        if (outputDust == null) return;

        boolean isEnchanted = isGearEnchanted();
        int yieldMultiplier = getYieldMultiplier(inputStack.getItem(), tier, isEnchanted);
        ItemStack outputSlot = inventory.get(2);

        if (outputSlot.isEmpty()) {
            inventory.set(2, new ItemStack(outputDust, yieldMultiplier));
        } else if (outputSlot.is(outputDust)) {
            outputSlot.grow(yieldMultiplier);
        }

        this.experience += getExperienceForInput(inputStack.getItem());
        inputStack.shrink(1);
    }

    private int getYieldMultiplier(Item item, GearTier tier, boolean isEnchanted) {
        // Recycling stones to Bauxite
        if (item == Items.DIORITE || item == Items.TERRACOTTA || item == Items.DYED_TERRACOTTA.red() || item == Items.GRANITE) {
            int bonus = isEnchanted ? 1 : 0;
            return switch (tier) {
                case DIAMOND -> 3 + bonus;
                case NETHERITE -> 4 + bonus;
                case GOLD, BRONZE -> 2 + bonus;
                default -> 1 + bonus;
            };
        }

        // Fixed yield items
        if (item == Items.WHEAT) {
            return 2;
        }
        if (item == Items.BLAZE_ROD) {
            return 4;
        }
        if (item == Items.MAGMA_BLOCK) {
            return 4;
        }


        // Base multiplier from gear tier (NONE=2x, IRON=2x, COPPER=3x, BRONZE=3x, GOLD=4x, DIAMOND=5x, NETHERITE=6x)
        int multiplier = tier.getBaseOreYield();
        if (isEnchanted) {
            multiplier += 1;
        }

        if (item == Items.QUARTZ_BLOCK) {
            return multiplier * 4;
        }

        // Compressed raw blocks yield 9x
        if (item == Items.RAW_IRON_BLOCK || item == Items.RAW_COPPER_BLOCK || item == Items.RAW_GOLD_BLOCK || item == ModBlocks.RAW_TITANIUM_BLOCK.asItem() || item == ModBlocks.RAW_BAUXITE_BLOCK.asItem() || item == ModBlocks.RAW_TIN_BLOCK.asItem() || item == ModBlocks.RAW_TUNGSTEN_BLOCK.asItem() || item == ModBlocks.RAW_COBALT_BLOCK.asItem() || item == ModBlocks.RAW_ARDITE_BLOCK.asItem()) {
            return multiplier * 9;
        }

        // Ingot blocks
        if (item == Items.IRON_BLOCK || Items.COPPER_BLOCK.asList().contains(item) || item == Items.GOLD_BLOCK || item == Items.DIAMOND_BLOCK || item == Items.NETHERITE_BLOCK || item == Items.EMERALD_BLOCK || item == Items.COAL_BLOCK || item == ModBlocks.ENCHANTED_COAL_BLOCK.asItem() || item == ModBlocks.TITANIUM_BLOCK.asItem() || item == ModBlocks.TIN_BLOCK.asItem() || item == ModBlocks.BRONZE_BLOCK.asItem() || item == ModBlocks.STEEL_BLOCK.asItem() || item == ModBlocks.ALUMINUM_BLOCK.asItem() || item == ModBlocks.COKE_COAL_BLOCK.asItem() || item == ModBlocks.TUNGSTEN_BLOCK.asItem() || item == ModBlocks.COBALT_BLOCK.asItem() || item == ModBlocks.ARDITE_BLOCK.asItem() || item == ModBlocks.MANYULLYN_BLOCK.asItem()) {
            return 9;
        }

        // Dense Nether Ores in MK1 Crusher (Option A - Soft Gated)
        if (item == ModItems.RAW_TUNGSTEN || item == ModBlocks.NETHER_TUNGSTEN_ORE.asItem() || item == ModBlocks.DEEPSLATE_TUNGSTEN_ORE.asItem() ||
            item == ModItems.RAW_COBALT || item == ModBlocks.COBALT_ORE.asItem() ||
            item == ModItems.RAW_ARDITE || item == ModBlocks.ARDITE_ORE.asItem()) {
            return (tier == GearTier.DIAMOND || tier == GearTier.NETHERITE) ? 2 : 1;
        }

        // Standard Ores & Raw materials
        return multiplier;
    }

    private float getExperienceForInput(Item item) {
        if (item == Items.DIORITE || item == Items.TERRACOTTA || item == Items.DYED_TERRACOTTA.red() || item == Items.GRANITE) return 0.2f;
        if (item == ModItems.RAW_BAUXITE || item == ModBlocks.BAUXITE_ORE.asItem() || item == ModBlocks.DEEPSLATE_BAUXITE_ORE.asItem() || item == ModBlocks.RAW_BAUXITE_BLOCK.asItem() || item == ModItems.ALUMINUM_INGOT || item == ModBlocks.ALUMINUM_BLOCK.asItem()) return 0.7f;
        if (item == Items.RAW_IRON || item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE || item == ModBlocks.NETHER_IRON_ORE.asItem() || item == Items.RAW_IRON_BLOCK || item == Items.IRON_INGOT || item == Items.IRON_BLOCK) return 0.7f;
        if (item == Items.RAW_COPPER || item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE || item == ModBlocks.NETHER_COPPER_ORE.asItem() || item == Items.RAW_COPPER_BLOCK || item == Items.COPPER_INGOT || Items.COPPER_BLOCK.asList().contains(item)) return 0.7f;
        if (item == ModItems.RAW_TIN || item == ModBlocks.TIN_ORE.asItem() || item == ModBlocks.DEEPSLATE_TIN_ORE.asItem() || item == ModBlocks.NETHER_TIN_ORE.asItem() || item == ModBlocks.RAW_TIN_BLOCK.asItem() || item == ModItems.TIN_INGOT || item == ModBlocks.TIN_BLOCK.asItem()) return 0.7f;
        if (item == ModItems.BRONZE_INGOT || item == ModBlocks.BRONZE_BLOCK.asItem()) return 0.8f;
        if (item == ModItems.STEEL_INGOT || item == ModBlocks.STEEL_BLOCK.asItem()) return 0.8f;
        if (item == ModItems.RAW_TITANIUM || item == ModBlocks.TITANIUM_ORE.asItem() || item == ModBlocks.DEEPSLATE_TITANIUM_ORE.asItem() || item == ModBlocks.RAW_TITANIUM_BLOCK.asItem() || item == ModItems.TITANIUM_INGOT || item == ModBlocks.TITANIUM_BLOCK.asItem()) return 0.8f;
        if (item == ModItems.RAW_TUNGSTEN || item == ModBlocks.NETHER_TUNGSTEN_ORE.asItem() || item == ModBlocks.DEEPSLATE_TUNGSTEN_ORE.asItem() || item == ModBlocks.RAW_TUNGSTEN_BLOCK.asItem() || item == ModItems.TUNGSTEN_INGOT || item == ModBlocks.TUNGSTEN_BLOCK.asItem()) return 1.0f;
        if (item == ModItems.RAW_COBALT || item == ModBlocks.COBALT_ORE.asItem() || item == ModBlocks.RAW_COBALT_BLOCK.asItem() || item == ModItems.COBALT_INGOT || item == ModBlocks.COBALT_BLOCK.asItem()) return 1.0f;
        if (item == ModItems.RAW_ARDITE || item == ModBlocks.ARDITE_ORE.asItem() || item == ModBlocks.RAW_ARDITE_BLOCK.asItem() || item == ModItems.ARDITE_INGOT || item == ModBlocks.ARDITE_BLOCK.asItem()) return 1.0f;
        if (item == ModItems.MANYULLYN_INGOT || item == ModBlocks.MANYULLYN_BLOCK.asItem()) return 1.5f;
        if (item == Items.RAW_GOLD || item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE || item == Items.NETHER_GOLD_ORE || item == Items.RAW_GOLD_BLOCK || item == Items.GOLD_INGOT || item == Items.GOLD_BLOCK) return 1.0f;
        if (item == Items.DIAMOND_ORE || item == Items.DEEPSLATE_DIAMOND_ORE || item == ModBlocks.NETHER_DIAMOND_ORE.asItem() || item == Items.DIAMOND || item == Items.DIAMOND_BLOCK) return 1.0f;
        if (item == Items.ANCIENT_DEBRIS || item == Items.NETHERITE_SCRAP || item == Items.NETHERITE_INGOT || item == Items.NETHERITE_BLOCK || item == ModItems.ENCHANTED_NETHERITE_INGOT || item == ModBlocks.ENCHANTED_NETHERITE_BLOCK.asItem()) return 2.0f;
        if (item == Items.EMERALD_ORE || item == Items.DEEPSLATE_EMERALD_ORE || item == Items.EMERALD || item == Items.EMERALD_BLOCK) return 1.0f;
        if (item == ModBlocks.NETHER_REDSTONE_ORE.asItem() || item == ModBlocks.NETHER_LAPIS_ORE.asItem()) return 0.7f;
        if (item == ModItems.ENCHANTED_COAL || item == ModBlocks.ENCHANTED_COAL_BLOCK.asItem()) return 0.8f;
        if (item == Items.BLAZE_ROD) return 0.5f;
        if (item == Items.COAL_ORE || item == Items.DEEPSLATE_COAL_ORE || item == ModBlocks.NETHER_COAL_ORE.asItem() || item == Items.COAL || item == Items.COAL_BLOCK || item == ModItems.COKE_COAL || item == ModBlocks.COKE_COAL_BLOCK.asItem()) return 0.1f;
        if (item == Items.QUARTZ || item == Items.QUARTZ_BLOCK) return 0.5f;
        return 0.7f;
    }

    private Item getOutputDust(Item item) {
        if (item == Items.QUARTZ || item == Items.QUARTZ_BLOCK) return ModItems.QUARTZ_DUST;
        if (item == ModItems.ENCHANTED_COAL || item == ModBlocks.ENCHANTED_COAL_BLOCK.asItem()) return ModItems.ENCHANTED_DUST;
        if (item == Items.DIORITE || item == Items.TERRACOTTA || item == Items.DYED_TERRACOTTA.red() || item == Items.GRANITE) return ModItems.RAW_BAUXITE;
        if (item == ModItems.RAW_BAUXITE || item == ModBlocks.BAUXITE_ORE.asItem() || item == ModBlocks.DEEPSLATE_BAUXITE_ORE.asItem() || item == ModBlocks.RAW_BAUXITE_BLOCK.asItem() || item == ModItems.ALUMINUM_INGOT || item == ModBlocks.ALUMINUM_BLOCK.asItem()) return ModItems.BAUXITE_DUST;
        if (item == Items.RAW_IRON || item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE || item == ModBlocks.NETHER_IRON_ORE.asItem() || item == Items.RAW_IRON_BLOCK || item == Items.IRON_INGOT || item == Items.IRON_BLOCK) return ModItems.IRON_DUST;
        if (item == Items.RAW_COPPER || item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE || item == ModBlocks.NETHER_COPPER_ORE.asItem() || item == Items.RAW_COPPER_BLOCK || item == Items.COPPER_INGOT || Items.COPPER_BLOCK.asList().contains(item)) return ModItems.COPPER_DUST;
        if (item == ModItems.RAW_TIN || item == ModBlocks.TIN_ORE.asItem() || item == ModBlocks.DEEPSLATE_TIN_ORE.asItem() || item == ModBlocks.NETHER_TIN_ORE.asItem() || item == ModBlocks.RAW_TIN_BLOCK.asItem() || item == ModItems.TIN_INGOT || item == ModBlocks.TIN_BLOCK.asItem()) return ModItems.TIN_DUST;
        if (item == ModBlocks.NETHER_COAL_ORE.asItem()) return Items.COAL;
        if (item == ModBlocks.NETHER_REDSTONE_ORE.asItem()) return Items.REDSTONE;
        if (item == ModBlocks.NETHER_LAPIS_ORE.asItem()) return Items.LAPIS_LAZULI;
        if (item == ModItems.BRONZE_INGOT || item == ModBlocks.BRONZE_BLOCK.asItem()) return ModItems.BRONZE_DUST;
        if (item == ModItems.STEEL_INGOT || item == ModBlocks.STEEL_BLOCK.asItem()) return ModItems.STEEL_DUST;
        if (item == ModItems.RAW_TITANIUM || item == ModBlocks.TITANIUM_ORE.asItem() || item == ModBlocks.DEEPSLATE_TITANIUM_ORE.asItem() || item == ModBlocks.RAW_TITANIUM_BLOCK.asItem() || item == ModItems.TITANIUM_INGOT || item == ModBlocks.TITANIUM_BLOCK.asItem()) return ModItems.TITANIUM_DUST;
        if (item == ModItems.RAW_TUNGSTEN || item == ModBlocks.NETHER_TUNGSTEN_ORE.asItem() || item == ModBlocks.DEEPSLATE_TUNGSTEN_ORE.asItem() || item == ModBlocks.RAW_TUNGSTEN_BLOCK.asItem() || item == ModItems.TUNGSTEN_INGOT || item == ModBlocks.TUNGSTEN_BLOCK.asItem()) return ModItems.TUNGSTEN_DUST;
        if (item == ModItems.RAW_COBALT || item == ModBlocks.COBALT_ORE.asItem() || item == ModBlocks.RAW_COBALT_BLOCK.asItem() || item == ModItems.COBALT_INGOT || item == ModBlocks.COBALT_BLOCK.asItem()) return ModItems.COBALT_DUST;
        if (item == ModItems.RAW_ARDITE || item == ModBlocks.ARDITE_ORE.asItem() || item == ModBlocks.RAW_ARDITE_BLOCK.asItem() || item == ModItems.ARDITE_INGOT || item == ModBlocks.ARDITE_BLOCK.asItem()) return ModItems.ARDITE_DUST;
        if (item == ModItems.RAW_TANTALUM || item == ModBlocks.TANTALUM_ORE.asItem() || item == ModBlocks.DEEPSLATE_TANTALUM_ORE.asItem() || item == ModItems.TANTALUM_INGOT) return ModItems.TANTALUM_DUST;
        if (item == ModItems.RAW_HAFNIUM || item == ModBlocks.HAFNIUM_ORE.asItem() || item == ModBlocks.DEEPSLATE_HAFNIUM_ORE.asItem() || item == ModItems.HAFNIUM_INGOT) return ModItems.HAFNIUM_DUST;
        if (item == ModItems.RAW_NEODYMIUM || item == ModBlocks.NEODYMIUM_ORE.asItem() || item == ModBlocks.DEEPSLATE_NEODYMIUM_ORE.asItem() || item == ModItems.NEODYMIUM_MAGNET) return ModItems.NEODYMIUM_DUST;
        if (item == ModItems.MANYULLYN_INGOT || item == ModBlocks.MANYULLYN_BLOCK.asItem()) return ModItems.MANYULLYN_DUST;
        if (item == Items.RAW_GOLD || item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE || item == Items.NETHER_GOLD_ORE || item == Items.RAW_GOLD_BLOCK || item == Items.GOLD_INGOT || item == Items.GOLD_BLOCK) return ModItems.GOLD_DUST;
        if (item == Items.DIAMOND_ORE || item == Items.DEEPSLATE_DIAMOND_ORE || item == ModBlocks.NETHER_DIAMOND_ORE.asItem() || item == Items.DIAMOND || item == Items.DIAMOND_BLOCK) return ModItems.DIAMOND_DUST;
        if (item == Items.ANCIENT_DEBRIS || item == Items.NETHERITE_SCRAP || item == Items.NETHERITE_INGOT || item == Items.NETHERITE_BLOCK || item == ModItems.ENCHANTED_NETHERITE_INGOT || item == ModBlocks.ENCHANTED_NETHERITE_BLOCK.asItem()) return ModItems.NETHERITE_DUST;
        if (item == Items.EMERALD_ORE || item == Items.DEEPSLATE_EMERALD_ORE || item == Items.EMERALD || item == Items.EMERALD_BLOCK) return ModItems.EMERALD_DUST;
        if (item == Items.COAL_ORE || item == Items.DEEPSLATE_COAL_ORE || item == Items.COAL || item == Items.COAL_BLOCK || item == ModItems.COKE_COAL || item == ModBlocks.COKE_COAL_BLOCK.asItem()) return ModItems.COAL_DUST;
        if (item == Items.BLAZE_ROD) return Items.BLAZE_POWDER;
        if (item == Items.MAGMA_BLOCK) return Items.MAGMA_CREAM;
        if (item == Items.WHEAT) return ModItems.WHEAT_FLOUR;
        return null;
    }


    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 160);
        this.experience = view.getFloatOr("Experience", 0.0f);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        view.putFloat("Experience", this.experience);
    }

    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.get(slot);
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

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{2};
        if (side == Direction.UP) return new int[]{0};
        return new int[]{0, 1, 2};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        if (slot == 0) return canProcessInput(stack, getActiveGearTier());
        if (slot == 1) return stack.getItem() instanceof net.enchantedwood.item.custom.GearItem;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 2;
    }
}
