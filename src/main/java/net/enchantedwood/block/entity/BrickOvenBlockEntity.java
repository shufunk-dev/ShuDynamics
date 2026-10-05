package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.BrickOvenBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.BrickOvenScreenHandler;
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
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BrickOvenBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int TOTAL_SLOTS = 3;
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_RECEIVE = 500;
    public static final int POWER_PER_TICK = 20; // 20 FE/t electric baking

    private static final int[] TOP_SLOTS = new int[]{INPUT_SLOT};
    private static final int[] BOTTOM_SLOTS = new int[]{OUTPUT_SLOT, FUEL_SLOT};
    private static final int[] SIDE_SLOTS = new int[]{FUEL_SLOT};

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0, 0);

    private int burnTime = 0;
    private int fuelTime = 0;
    private int cookProgress = 0;
    private int maxCookProgress = 80; // 4 seconds (2.5x faster than furnace's 200 ticks!)
    private boolean isElectricHeating = false;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> fuelTime;
                case 2 -> cookProgress;
                case 3 -> maxCookProgress;
                case 4 -> energyStorage.getEnergy() & 0xFFFF;
                case 5 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 6 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 7 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> fuelTime = value;
                case 2 -> cookProgress = value;
                case 3 -> maxCookProgress = value;
                case 4 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0xFFFF0000) | (value & 0xFFFF));
                case 5 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
            }
        }

        @Override
        public int size() {
            return 8;
        }
    };

    public BrickOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BRICK_OVEN_BLOCK_ENTITY, pos, state);
    }

    public static int getFuelBurnTime(@Nullable World world, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (world != null) {
            int ticks = world.getFuelRegistry().getFuelTicks(stack);
            if (ticks > 0) return ticks;
        }
        Item item = stack.getItem();
        if (item == ModItems.ENCHANTED_DUST) return 8000;
        if (item == ModItems.ENCHANTED_COAL) return 10000;
        if (item == ModBlocks.ENCHANTED_COAL_BLOCK.asItem()) return 90000;
        if (item == ModItems.COKE_COAL) return 3200;
        if (item == ModBlocks.COKE_COAL_BLOCK.asItem()) return 28800;
        if (item == Items.COAL || item == Items.CHARCOAL) return 1600;
        if (item == Items.COAL_BLOCK) return 16000;
        if (item == Items.BLAZE_ROD) return 2400;
        if (item == Items.LAVA_BUCKET || item == ModItems.COPPER_LAVA_BUCKET) return 20000;
        if (item == ModItems.ENCHANTED_LAVA_BUCKET || item == ModItems.ENCHANTED_COPPER_LAVA_BUCKET) return 60000;
        if (stack.isIn(net.minecraft.registry.tag.ItemTags.LOGS) || stack.isIn(net.minecraft.registry.tag.ItemTags.PLANKS)) return 300;
        if (item == Items.STICK) return 100;
        return 0;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, BrickOvenBlockEntity entity) {
        boolean dirty = false;
        boolean wasBurning = entity.isBurning();

        if (entity.burnTime > 0) {
            entity.burnTime--;
        }

        ItemStack input = entity.inventory.get(INPUT_SLOT);
        ItemStack fuel = entity.inventory.get(FUEL_SLOT);

        ItemStack recipeResult = entity.getBakingResult(world, input);
        boolean canWork = !recipeResult.isEmpty() && entity.canAcceptOutput(recipeResult);

        entity.isElectricHeating = false;

        if (canWork) {
            // Priority 1: Electric Heating via FE power
            if (entity.energyStorage.getEnergy() >= POWER_PER_TICK) {
                entity.energyStorage.extractEnergy(POWER_PER_TICK, false);
                entity.isElectricHeating = true;
                dirty = true;
            } else if (entity.burnTime <= 0) {
                // Priority 2: Solid Fuel combustion
                int fuelVal = getFuelBurnTime(world, fuel);
                if (fuelVal > 0) {
                    entity.burnTime = fuelVal;
                    entity.fuelTime = fuelVal;
                    if (fuel.isOf(Items.LAVA_BUCKET)) {
                        entity.inventory.set(FUEL_SLOT, new ItemStack(Items.BUCKET));
                    } else if (fuel.isOf(ModItems.COPPER_LAVA_BUCKET)) {
                        entity.inventory.set(FUEL_SLOT, new ItemStack(ModItems.COPPER_BUCKET));
                    } else if (fuel.isOf(ModItems.ENCHANTED_LAVA_BUCKET)) {
                        entity.inventory.set(FUEL_SLOT, new ItemStack(Items.BUCKET));
                    } else if (fuel.isOf(ModItems.ENCHANTED_COPPER_LAVA_BUCKET)) {
                        entity.inventory.set(FUEL_SLOT, new ItemStack(ModItems.COPPER_BUCKET));
                    } else {
                        fuel.decrement(1);
                    }
                    dirty = true;
                }
            }

            if (entity.isBurning()) {
                entity.cookProgress++;
                if (entity.cookProgress >= entity.maxCookProgress) {
                    entity.craftItem(recipeResult);
                    entity.cookProgress = 0;
                    world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4f, 1.2f);
                    dirty = true;
                }
            } else {
                if (entity.cookProgress > 0) {
                    entity.cookProgress = Math.max(0, entity.cookProgress - 2);
                }
            }
        } else {
            entity.cookProgress = 0;
        }

        boolean isBurningNow = entity.isBurning();
        if (wasBurning != isBurningNow) {
            world.setBlockState(pos, state.with(BrickOvenBlock.LIT, isBurningNow), 3);
            dirty = true;
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    public boolean isBurning() {
        return this.isElectricHeating || this.burnTime > 0;
    }

    private ItemStack getBakingResult(ServerWorld world, ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;

        Item item = input.getItem();

        // 1. Salt Boiling & Dehydration
        if (item == Items.WATER_BUCKET || item == ModItems.COPPER_WATER_BUCKET) {
            return new ItemStack(ModItems.SALT, 4);
        }

        // 2. Artisanal Bakery & Dough
        if (item == ModItems.WHEAT_FLOUR) return new ItemStack(Items.BREAD, 2);
        if (item == ModItems.PIZZA_DOUGH) return new ItemStack(ModItems.BURGER_BUN);

        // 3. Artisanal Pizzas
        if (item == ModItems.RAW_MARGHERITA_PIZZA) return new ItemStack(ModItems.MARGHERITA_PIZZA);
        if (item == ModItems.RAW_MEAT_LOVERS_PIZZA) return new ItemStack(ModItems.MEAT_LOVERS_PIZZA);
        if (item == ModItems.RAW_ANCHOVY_ONION_PIZZA) return new ItemStack(ModItems.ANCHOVY_ONION_PIZZA);
        if (item == ModItems.RAW_SUPREME_PIZZA) return new ItemStack(ModItems.SUPREME_PIZZA);

        // 4. Street Foods & Gourmet Cookery
        if (item == ModItems.RAW_BURGER_PATTY) return new ItemStack(ModItems.COOKED_BURGER_PATTY);
        if (item == ModItems.CORN) return new ItemStack(ModItems.ROASTED_CORN);
        if (item == ModItems.SOY_MILK) return new ItemStack(ModItems.TOFU);

        // 5. Vanilla Smoker recipes (all roasted meats, poultry, fish, potatoes, kelp)
        var smokingMatch = world.getRecipeManager().getFirstMatch(RecipeType.SMOKING, new SingleStackRecipeInput(input), world);
        if (smokingMatch.isPresent()) {
            return smokingMatch.get().value().craft(new SingleStackRecipeInput(input), world.getRegistryManager());
        }

        // 6. Vanilla Campfire recipes
        var campfireMatch = world.getRecipeManager().getFirstMatch(RecipeType.CAMPFIRE_COOKING, new SingleStackRecipeInput(input), world);
        if (campfireMatch.isPresent()) {
            return campfireMatch.get().value().craft(new SingleStackRecipeInput(input), world.getRegistryManager());
        }

        // 7. Vanilla Smelting / Cooking fallback (stones, ores, clay, sand)
        var smeltingMatch = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(input), world);
        if (smeltingMatch.isPresent()) {
            return smeltingMatch.get().value().craft(new SingleStackRecipeInput(input), world.getRegistryManager());
        }

        return ItemStack.EMPTY;
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack currentOut = this.inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) return true;
        if (!ItemStack.areItemsAndComponentsEqual(currentOut, result)) return false;
        return currentOut.getCount() + result.getCount() <= currentOut.getMaxCount();
    }

    private void craftItem(ItemStack result) {
        ItemStack input = this.inventory.get(INPUT_SLOT);
        if (input.isOf(Items.WATER_BUCKET)) {
            this.inventory.set(INPUT_SLOT, new ItemStack(Items.BUCKET));
        } else if (input.isOf(ModItems.COPPER_WATER_BUCKET)) {
            this.inventory.set(INPUT_SLOT, new ItemStack(ModItems.COPPER_BUCKET));
        } else {
            input.decrement(1);
        }

        ItemStack currentOut = this.inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) {
            this.inventory.set(OUTPUT_SLOT, result.copy());
        } else {
            currentOut.increment(result.getCount());
        }
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.burnTime = view.getInt("BurnTime", 0);
        this.fuelTime = view.getInt("FuelTime", 0);
        this.cookProgress = view.getInt("CookProgress", 0);
        this.maxCookProgress = view.getInt("MaxCookProgress", 80);
        this.energyStorage.setEnergy(view.getInt("Energy", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("FuelTime", this.fuelTime);
        view.putInt("CookProgress", this.cookProgress);
        view.putInt("MaxCookProgress", this.maxCookProgress);
        view.putInt("Energy", this.energyStorage.getEnergy());
    }

    @Override
    public int size() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) return false;
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
        ItemStack result = Inventories.removeStack(this.inventory, slot);
        if (!result.isEmpty()) markDirty();
        return result;
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

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == OUTPUT_SLOT) return false;
        if (slot == FUEL_SLOT) return getFuelBurnTime(this.world, stack) > 0;
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        if (dir == Direction.DOWN && slot == FUEL_SLOT) {
            return stack.isOf(Items.BUCKET) || stack.isOf(ModItems.COPPER_BUCKET);
        }
        return slot == OUTPUT_SLOT;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.brick_oven");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BrickOvenScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }
}
