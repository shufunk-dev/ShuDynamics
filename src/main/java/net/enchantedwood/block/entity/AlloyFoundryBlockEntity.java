package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.AlloyFoundryBlock;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.AlloyFoundryScreenHandler;
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

public class AlloyFoundryBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    public static final int INPUT_SLOT_A = 0;
    public static final int INPUT_SLOT_B = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int GEAR_SLOT = 3;
    public static final int INVENTORY_SIZE = 4;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 160;
    private float experience = 0.0f;

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
        public int getCount() {
            return 7;
        }
    };

    public AlloyFoundryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_FOUNDRY_BE, pos, state);
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
            case IRON -> 130;
            case COPPER -> 110;
            case BRONZE -> 90;
            case GOLD -> 70;
            case DIAMOND -> 45;
            case NETHERITE -> 20;
            case BLAZE_OVERCLOCK -> 15;
            default -> 160;
        };
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enchantedwood.alloy_foundry");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new AlloyFoundryScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, AlloyFoundryBlockEntity entity) {
        boolean dirty = false;

        entity.totalCookTime = getTierCookTime(entity.getActiveGearTier());

        ItemStack stackA = entity.inventory.get(INPUT_SLOT_A);
        ItemStack stackB = entity.inventory.get(INPUT_SLOT_B);

        AlloyRecipe recipe = getMatchingRecipe(stackA, stackB);
        boolean canProcess = recipe != null && entity.canAcceptOutput(recipe);
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        boolean isCooking = false;
        if (canProcess && hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            ++entity.cookTime;
            isCooking = true;
            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                entity.processAlloy(recipe);
            }
            dirty = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                dirty = true;
            }
        }

        if (state.getValue(AlloyFoundryBlock.LIT) != isCooking) {
            world.setBlock(pos, state.setValue(AlloyFoundryBlock.LIT, isCooking), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    private boolean canAcceptOutput(AlloyRecipe recipe) {
        ItemStack currentOut = inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) return true;
        if (!currentOut.is(recipe.resultItem)) return false;
        return currentOut.getCount() + recipe.resultCount <= currentOut.getMaxStackSize();
    }

    private void processAlloy(AlloyRecipe recipe) {
        ItemStack stackA = inventory.get(INPUT_SLOT_A);
        ItemStack stackB = inventory.get(INPUT_SLOT_B);
        ItemStack currentOut = inventory.get(OUTPUT_SLOT);

        stackA.shrink(1);
        stackB.shrink(1);

        if (currentOut.isEmpty()) {
            inventory.set(OUTPUT_SLOT, new ItemStack(recipe.resultItem, recipe.resultCount));
        } else {
            currentOut.grow(recipe.resultCount);
        }

        this.experience += 1.5f;
    }

    public static record AlloyRecipe(Item itemA, Item itemB, Item resultItem, int resultCount) {}

    public static @Nullable AlloyRecipe getMatchingRecipe(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return null;
        Item itemA = a.getItem();
        Item itemB = b.getItem();

        if (isCobalt(itemA) && isArdite(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.MANYULLYN_INGOT, 2);
        if (isArdite(itemA) && isCobalt(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.MANYULLYN_INGOT, 2);

        if (isTungsten(itemA) && isCarbon(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.TUNGSTEN_CARBIDE_INGOT, 2);
        if (isCarbon(itemA) && isTungsten(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.TUNGSTEN_CARBIDE_INGOT, 2);

        if (isCopper(itemA) && isTin(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.BRONZE_INGOT, 2);
        if (isTin(itemA) && isCopper(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.BRONZE_INGOT, 2);

        if (isIron(itemA) && isCarbon(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.STEEL_INGOT, 1);
        if (isCarbon(itemA) && isIron(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.STEEL_INGOT, 1);

        // Basalt Flux Catalyst Recipes
        if (isIron(itemA) && isFlux(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.STEEL_INGOT, 2);
        if (isFlux(itemA) && isIron(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.STEEL_INGOT, 2);

        // Reinforced Obsidian & Volcanic Glass Casting
        if (isObsidian(itemA) && isTitanium(itemB)) return new AlloyRecipe(itemA, itemB, net.enchantedwood.block.ModBlocks.REINFORCED_OBSIDIAN.asItem(), 2);
        if (isTitanium(itemA) && isObsidian(itemB)) return new AlloyRecipe(itemA, itemB, net.enchantedwood.block.ModBlocks.REINFORCED_OBSIDIAN.asItem(), 2);

        if (isGlass(itemA) && isVolcanicOrFire(itemB)) return new AlloyRecipe(itemA, itemB, net.enchantedwood.block.ModBlocks.VOLCANIC_GLASS.asItem(), 2);
        if (isVolcanicOrFire(itemA) && isGlass(itemB)) return new AlloyRecipe(itemA, itemB, net.enchantedwood.block.ModBlocks.VOLCANIC_GLASS.asItem(), 2);

        // Convergence Superalloys & Advanced Composites
        if (isTantalum(itemA) && isTitanium(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.TAN_TI_INGOT, 2);
        if (isTitanium(itemA) && isTantalum(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.TAN_TI_INGOT, 2);

        if (isHafnium(itemA) && isTungsten(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.HAFNIUM_TUNGSTEN_CARBIDE_INGOT, 2);
        if (isTungsten(itemA) && isHafnium(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.HAFNIUM_TUNGSTEN_CARBIDE_INGOT, 2);

        if (isNeodymium(itemA) && isTitanium(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.NEO_TITANIUM_INGOT, 2);
        if (isTitanium(itemA) && isNeodymium(itemB)) return new AlloyRecipe(itemA, itemB, ModItems.NEO_TITANIUM_INGOT, 2);

        if (isAerogel(itemA) && isGlass(itemB)) return new AlloyRecipe(itemA, itemB, net.enchantedwood.block.ModBlocks.AEROGEL_GLASS.asItem(), 2);
        if (isGlass(itemA) && isAerogel(itemB)) return new AlloyRecipe(itemA, itemB, net.enchantedwood.block.ModBlocks.AEROGEL_GLASS.asItem(), 2);

        return null;
    }

    private static boolean isTantalum(Item item) {
        return item == ModItems.TANTALUM_INGOT || item == ModItems.TANTALUM_DUST || item == ModItems.RAW_TANTALUM;
    }

    private static boolean isHafnium(Item item) {
        return item == ModItems.HAFNIUM_INGOT || item == ModItems.HAFNIUM_DUST || item == ModItems.RAW_HAFNIUM;
    }

    private static boolean isNeodymium(Item item) {
        return item == ModItems.NEODYMIUM_MAGNET || item == ModItems.NEODYMIUM_DUST || item == ModItems.RAW_NEODYMIUM;
    }

    private static boolean isAerogel(Item item) {
        return item == ModItems.AEROGEL_SHARD || item == ModItems.RAW_AEROGEL;
    }

    private static boolean isCobalt(Item item) {
        return item == ModItems.COBALT_INGOT || item == ModItems.COBALT_DUST || item == ModItems.RAW_COBALT;
    }

    private static boolean isArdite(Item item) {
        return item == ModItems.ARDITE_INGOT || item == ModItems.ARDITE_DUST || item == ModItems.RAW_ARDITE;
    }

    private static boolean isTungsten(Item item) {
        return item == ModItems.TUNGSTEN_INGOT || item == ModItems.TUNGSTEN_DUST || item == ModItems.RAW_TUNGSTEN;
    }

    private static boolean isCarbon(Item item) {
        return item == ModItems.COKE_COAL;
    }

    private static boolean isCopper(Item item) {
        return item == Items.COPPER_INGOT || item == ModItems.COPPER_DUST || item == Items.RAW_COPPER;
    }

    private static boolean isTin(Item item) {
        return item == ModItems.TIN_INGOT || item == ModItems.TIN_DUST || item == ModItems.RAW_TIN;
    }

    private static boolean isTitanium(Item item) {
        return item == ModItems.TITANIUM_INGOT || item == ModItems.TITANIUM_DUST || item == ModItems.RAW_TITANIUM;
    }

    private static boolean isIron(Item item) {
        return item == Items.IRON_INGOT || item == ModItems.IRON_DUST || item == Items.RAW_IRON;
    }

    private static boolean isFlux(Item item) {
        return item == ModItems.BASALT_FLUX_CATALYST;
    }

    private static boolean isObsidian(Item item) {
        return item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN;
    }

    private static boolean isGlass(Item item) {
        return item == Items.GLASS || item == Items.GLASS_PANE;
    }

    private static boolean isVolcanicOrFire(Item item) {
        return item == ModItems.FIRE_CRYSTAL || item == ModItems.VOLCANIC_ASH || item == ModItems.SULFUR_DUST;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
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

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{INPUT_SLOT_A, INPUT_SLOT_B};
        return new int[]{INPUT_SLOT_A, INPUT_SLOT_B, GEAR_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT_A || slot == INPUT_SLOT_B) return true;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == OUTPUT_SLOT;
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
}
