package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.AlloyFoundryBlock;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.AlloyFoundryScreenHandler;
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

public class AlloyFoundryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    public static final int INPUT_SLOT_A = 0;
    public static final int INPUT_SLOT_B = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int GEAR_SLOT = 3;
    public static final int INVENTORY_SIZE = 4;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 160;
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
            default -> 160;
        };
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.alloy_foundry");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AlloyFoundryScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, AlloyFoundryBlockEntity entity) {
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

        if (state.get(AlloyFoundryBlock.LIT) != isCooking) {
            world.setBlockState(pos, state.with(AlloyFoundryBlock.LIT, isCooking), 3);
            dirty = true;
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    private boolean canAcceptOutput(AlloyRecipe recipe) {
        ItemStack currentOut = inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) return true;
        if (!currentOut.isOf(recipe.resultItem)) return false;
        return currentOut.getCount() + recipe.resultCount <= currentOut.getMaxCount();
    }

    private void processAlloy(AlloyRecipe recipe) {
        ItemStack stackA = inventory.get(INPUT_SLOT_A);
        ItemStack stackB = inventory.get(INPUT_SLOT_B);
        ItemStack currentOut = inventory.get(OUTPUT_SLOT);

        stackA.decrement(1);
        stackB.decrement(1);

        if (currentOut.isEmpty()) {
            inventory.set(OUTPUT_SLOT, new ItemStack(recipe.resultItem, recipe.resultCount));
        } else {
            currentOut.increment(recipe.resultCount);
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

        return null;
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

    private static boolean isIron(Item item) {
        return item == Items.IRON_INGOT || item == ModItems.IRON_DUST || item == Items.RAW_IRON;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", 160);
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
        if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{INPUT_SLOT_A, INPUT_SLOT_B};
        return new int[]{INPUT_SLOT_A, INPUT_SLOT_B, GEAR_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT_A || slot == INPUT_SLOT_B) return true;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == OUTPUT_SLOT;
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
