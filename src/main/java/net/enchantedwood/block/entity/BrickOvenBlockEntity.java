package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.BrickOvenBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.BrickOvenScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class BrickOvenBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
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

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0, 0);

    private int burnTime = 0;
    private int fuelTime = 0;
    private int cookProgress = 0;
    private int maxCookProgress = 80; // 4 seconds (2.5x faster than furnace's 200 ticks!)
    private boolean isElectricHeating = false;

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
            return 8;
        }
    };

    public BrickOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BRICK_OVEN_BLOCK_ENTITY, pos, state);
    }

    public static int getFuelBurnTime(@Nullable Level world, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (world != null) {
            int ticks = getFuelBurnTime(stack);
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
        if (stack.is(net.minecraft.tags.ItemTags.LOGS) || stack.is(net.minecraft.tags.ItemTags.PLANKS)) return 300;
        if (item == Items.STICK) return 100;
        return 0;
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, BrickOvenBlockEntity entity) {
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
                    if (fuel.is(Items.LAVA_BUCKET)) {
                        entity.inventory.set(FUEL_SLOT, new ItemStack(Items.BUCKET));
                    } else if (fuel.is(ModItems.COPPER_LAVA_BUCKET)) {
                        entity.inventory.set(FUEL_SLOT, new ItemStack(ModItems.COPPER_BUCKET));
                    } else if (fuel.is(ModItems.ENCHANTED_LAVA_BUCKET)) {
                        entity.inventory.set(FUEL_SLOT, new ItemStack(Items.BUCKET));
                    } else if (fuel.is(ModItems.ENCHANTED_COPPER_LAVA_BUCKET)) {
                        entity.inventory.set(FUEL_SLOT, new ItemStack(ModItems.COPPER_BUCKET));
                    } else {
                        fuel.shrink(1);
                    }
                    dirty = true;
                }
            }

            if (entity.isBurning()) {
                entity.cookProgress++;
                if (entity.cookProgress >= entity.maxCookProgress) {
                    entity.craftItem(recipeResult);
                    entity.cookProgress = 0;
                    world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.2f);
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
            world.setBlock(pos, state.setValue(BrickOvenBlock.LIT, isBurningNow), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    public boolean isBurning() {
        return this.isElectricHeating || this.burnTime > 0;
    }

    private ItemStack getBakingResult(ServerLevel world, ItemStack input) {
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
        var smokingMatch = world.recipeAccess().getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(input), world);
        if (smokingMatch.isPresent()) {
            return smokingMatch.get().value().assemble(new SingleRecipeInput(input));
        }

        // 6. Vanilla Campfire recipes
        var campfireMatch = world.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(input), world);
        if (campfireMatch.isPresent()) {
            return campfireMatch.get().value().assemble(new SingleRecipeInput(input));
        }

        // 7. Vanilla Smelting / Cooking fallback (stones, ores, clay, sand)
        var smeltingMatch = world.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), world);
        if (smeltingMatch.isPresent()) {
            return smeltingMatch.get().value().assemble(new SingleRecipeInput(input));
        }

        return ItemStack.EMPTY;
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack currentOut = this.inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentOut, result)) return false;
        return currentOut.getCount() + result.getCount() <= currentOut.getMaxStackSize();
    }

    private void craftItem(ItemStack result) {
        ItemStack input = this.inventory.get(INPUT_SLOT);
        if (input.is(Items.WATER_BUCKET)) {
            this.inventory.set(INPUT_SLOT, new ItemStack(Items.BUCKET));
        } else if (input.is(ModItems.COPPER_WATER_BUCKET)) {
            this.inventory.set(INPUT_SLOT, new ItemStack(ModItems.COPPER_BUCKET));
        } else {
            input.shrink(1);
        }

        ItemStack currentOut = this.inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) {
            this.inventory.set(OUTPUT_SLOT, result.copy());
        } else {
            currentOut.grow(result.getCount());
        }
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.burnTime = view.getIntOr("BurnTime", 0);
        this.fuelTime = view.getIntOr("FuelTime", 0);
        this.cookProgress = view.getIntOr("CookProgress", 0);
        this.maxCookProgress = view.getIntOr("MaxCookProgress", 80);
        this.energyStorage.setEnergy(view.getIntOr("Energy", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("FuelTime", this.fuelTime);
        view.putInt("CookProgress", this.cookProgress);
        view.putInt("MaxCookProgress", this.maxCookProgress);
        view.putInt("Energy", this.energyStorage.getEnergy());
    }

    @Override
    public int getContainerSize() {
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
        ItemStack result = ContainerHelper.takeItem(this.inventory, slot);
        if (!result.isEmpty()) setChanged();
        return result;
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

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == OUTPUT_SLOT) return false;
        if (slot == FUEL_SLOT) return getFuelBurnTime(this.level, stack) > 0;
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        if (dir == Direction.DOWN && slot == FUEL_SLOT) {
            return stack.is(Items.BUCKET) || stack.is(ModItems.COPPER_BUCKET);
        }
        return slot == OUTPUT_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.brick_oven");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new BrickOvenScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static int getFuelBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_DUST) return 8000;
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_COAL) return 10000;
        if (item == net.enchantedwood.block.ModBlocks.ENCHANTED_COAL_BLOCK.asItem()) return 90000;
        if (item == net.enchantedwood.item.ModItems.COKE_COAL) return 3200;
        if (item == net.enchantedwood.block.ModBlocks.COKE_COAL_BLOCK.asItem()) return 28800;
        if (item == net.enchantedwood.item.ModItems.COPPER_LAVA_BUCKET) return 20000;
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_LAVA_BUCKET || item == net.enchantedwood.item.ModItems.ENCHANTED_COPPER_LAVA_BUCKET) return 60000;
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) return 20000;
        if (item == net.minecraft.world.item.Items.COAL || item == net.minecraft.world.item.Items.CHARCOAL) return 1600;
        if (item == net.minecraft.world.item.Items.COAL_BLOCK) return 16000;
        if (item == net.minecraft.world.item.Items.BLAZE_ROD) return 2400;
        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(
                stack,
                net.minecraft.core.component.DataComponents.COOKING_FUEL,
                net.minecraft.world.item.component.CookingFuel::burnTime,
                null,
                0
        );
    }

    public static ItemStack getItemRemainder(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.COPPER_LAVA_BUCKET)) return new ItemStack(net.enchantedwood.item.ModItems.COPPER_BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.ENCHANTED_LAVA_BUCKET)) return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.ENCHANTED_COPPER_LAVA_BUCKET)) return new ItemStack(net.enchantedwood.item.ModItems.COPPER_BUCKET);
        var rem = stack.getItem().getCraftingRemainder();
        return rem != null ? rem.create() : ItemStack.EMPTY;
    }

}