package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.CircuitFabricatorBlock;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.CircuitFabricatorScreenHandler;
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

import java.util.*;

public class CircuitFabricatorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 35; // 35 FE/t

    public static final int SUBSTRATE_SLOT = 0;
    public static final int COMPONENT_SLOT_1 = 1;
    public static final int COMPONENT_SLOT_2 = 2;
    public static final int COMPONENT_SLOT_3 = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int GEAR_SLOT = 5;
    public static final int INVENTORY_SIZE = 6;

    public record FabricatorRecipe(Item substrate, List<Item> components, ItemStack output, int baseCookTime) {}

    private static final List<FabricatorRecipe> RECIPES = new ArrayList<>();

    public static void registerRecipe(Item substrate, int baseCookTime, ItemStack output, Item... components) {
        RECIPES.add(new FabricatorRecipe(substrate, List.of(components), output, baseCookTime));
    }

    static {
        // 1. Basic Computer Chip (Tier 1 Micro-Controller)
        registerRecipe(ModItems.SILICON_WAFER, 120, new ItemStack(ModItems.BASIC_COMPUTER_CHIP),
                Items.COPPER_INGOT, Items.GOLD_NUGGET, Items.REDSTONE);

        // 2. Advanced Computer Chip (Tier 2 Environmental Processor)
        registerRecipe(ModItems.BASIC_COMPUTER_CHIP, 160, new ItemStack(ModItems.ADVANCED_COMPUTER_CHIP),
                Items.DIAMOND, Items.GLOWSTONE_DUST, Items.LAPIS_LAZULI);

        // 3. Quantum Computer Chip (Tier 3 Quantum Core)
        registerRecipe(ModItems.ADVANCED_COMPUTER_CHIP, 240, new ItemStack(ModItems.QUANTUM_COMPUTER_CHIP),
                ModItems.NETHERITE_DUST, Items.BLAZE_POWDER, ModItems.ENCHANTED_DUST);

        // 4. Metallurgy Controller Chip (Industrial Logic Module for Induction Smelter)
        registerRecipe(ModItems.SILICON_WAFER, 180, new ItemStack(ModItems.METALLURGY_CONTROLLER_CHIP),
                Items.GOLD_INGOT, Items.REDSTONE, ModItems.ZIRCONIA_NODULE);
    }

    public static List<FabricatorRecipe> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, 0, 0);

    private @Nullable BlockPos boundNetworkPos = null;
    private String boundDimension = "minecraft:overworld";

    private int cookTime = 0;
    private int totalCookTime = 140;

    public void bindNetwork(BlockPos pos, String dimension) {
        this.boundNetworkPos = pos;
        this.boundDimension = dimension != null ? dimension : "minecraft:overworld";
        setChanged();
    }

    public @Nullable BlockPos getBoundNetworkPos() {
        return this.boundNetworkPos;
    }

    public boolean isPowered() {
        return this.energyStorage.getEnergy() >= ENERGY_DRAW;
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (gearStack.is(ModItems.BLAZE_OVERCLOCK_CORE)) {
            return GearTier.BLAZE_OVERCLOCK;
        }
        if (gearStack.getItem() instanceof net.enchantedwood.item.custom.GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public float getSpeedMultiplier() {
        return switch (getActiveGearTier()) {
            case IRON, ENCHANTED_IRON -> 1.5f;
            case COPPER, BRONZE -> 2.0f;
            case ALUMINUM, STEEL, GOLD -> 2.5f;
            case TITANIUM, DIAMOND -> 3.0f;
            case NETHERITE -> 4.0f;
            case BLAZE_OVERCLOCK -> 6.0f;
            default -> 1.0f;
        };
    }

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

    public CircuitFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CIRCUIT_FABRICATOR_BE, pos, state);
    }

    private int externalOperationTicks = 0;
    private boolean isExternalProcess = false;

    public boolean isExternalProcess() {
        return this.isExternalProcess;
    }

    public void triggerExternalOperation(int ticks) {
        this.externalOperationTicks = Math.max(this.externalOperationTicks, ticks);
    }

    public void setExternalProcess(@Nullable ItemStack substrate, int progressTicks, int maxTicks) {
        this.externalOperationTicks = 6;
        this.isExternalProcess = true;
        if (substrate != null && !substrate.isEmpty()) {
            if (this.inventory.get(SUBSTRATE_SLOT).isEmpty() || !this.inventory.get(SUBSTRATE_SLOT).is(substrate.getItem())) {
                this.inventory.set(SUBSTRATE_SLOT, substrate.copy());
            }
        }
        this.totalCookTime = Math.max(1, maxTicks);
        this.cookTime = Math.min(this.totalCookTime, progressTicks);
        setChanged();
    }

    public void clearExternalProcess() {
        this.externalOperationTicks = 0;
        this.isExternalProcess = false;
        this.cookTime = 0;
        this.inventory.set(SUBSTRATE_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, CircuitFabricatorBlockEntity entity) {
        boolean dirty = false;

        if (entity.isExternalProcess) {
            if (entity.externalOperationTicks > 0) {
                entity.externalOperationTicks--;
                dirty = true;
            } else {
                entity.clearExternalProcess();
            }
        } else {
            // 1. Determine Gear speed multiplier
            float speedMultiplier = entity.getSpeedMultiplier();

            // 2. Check for active matching recipe
            FabricatorRecipe activeRecipe = entity.findMatchingRecipe();

            if (activeRecipe != null && entity.canAcceptOutput(activeRecipe.output())) {
                entity.totalCookTime = Math.max(20, (int) (activeRecipe.baseCookTime() / speedMultiplier));
                boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

                if (hasEnergy) {
                    entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
                    entity.cookTime++;
                    dirty = true;

                    if (entity.cookTime >= entity.totalCookTime) {
                        entity.cookTime = 0;
                        entity.craftRecipe(activeRecipe);
                        dirty = true;
                    }
                } else {
                    // Decay progress slowly if power cut
                    if (entity.cookTime > 0) {
                        entity.cookTime = Math.max(0, entity.cookTime - 1);
                        dirty = true;
                    }
                }
            } else {
                if (entity.cookTime > 0) {
                    entity.cookTime = 0;
                    dirty = true;
                }
            }
        }

        FabricatorRecipe activeRecipe = entity.findMatchingRecipe();
        boolean isLit = (activeRecipe != null && entity.energyStorage.getEnergy() >= ENERGY_DRAW && entity.cookTime > 0) || entity.externalOperationTicks > 0;
        if (entity.externalOperationTicks > 0 && !entity.isExternalProcess) {
            entity.externalOperationTicks--;
            dirty = true;
        }

        if (state.getValue(CircuitFabricatorBlock.LIT) != isLit) {
            world.setBlock(pos, state.setValue(CircuitFabricatorBlock.LIT, isLit), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    private @Nullable FabricatorRecipe findMatchingRecipe() {
        ItemStack substrate = inventory.get(SUBSTRATE_SLOT);
        if (substrate.isEmpty()) return null;

        List<Item> currentComponents = new ArrayList<>();
        for (int i = COMPONENT_SLOT_1; i <= COMPONENT_SLOT_3; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                currentComponents.add(stack.getItem());
            }
        }

        for (FabricatorRecipe recipe : RECIPES) {
            if (!substrate.is(recipe.substrate())) continue;
            if (recipe.components().size() != currentComponents.size()) continue;

            List<Item> required = new ArrayList<>(recipe.components());
            boolean matches = true;
            for (Item item : currentComponents) {
                if (!required.remove(item)) {
                    matches = false;
                    break;
                }
            }
            if (matches && required.isEmpty()) {
                return recipe;
            }
        }
        return null;
    }

    private boolean canAcceptOutput(ItemStack recipeOutput) {
        ItemStack currentOut = inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) return true;
        if (!ItemStack.isSameItem(currentOut, recipeOutput)) return false;
        return currentOut.getCount() + recipeOutput.getCount() <= currentOut.getMaxStackSize();
    }

    private void craftRecipe(FabricatorRecipe recipe) {
        inventory.get(SUBSTRATE_SLOT).shrink(1);

        // Decrement one of each component
        List<Item> toConsume = new ArrayList<>(recipe.components());
        for (int i = COMPONENT_SLOT_1; i <= COMPONENT_SLOT_3; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty() && toConsume.remove(stack.getItem())) {
                stack.shrink(1);
            }
        }

        ItemStack out = inventory.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            inventory.set(OUTPUT_SLOT, recipe.output().copy());
        } else {
            out.grow(recipe.output().getCount());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enchantedwood.circuit_fabricator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CircuitFabricatorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return new int[]{ SUBSTRATE_SLOT };
        } else if (side == Direction.DOWN) {
            return new int[]{ OUTPUT_SLOT };
        } else {
            return new int[]{ COMPONENT_SLOT_1, COMPONENT_SLOT_2, COMPONENT_SLOT_3, GEAR_SLOT };
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == OUTPUT_SLOT) return false;
        if (slot == GEAR_SLOT) {
            return stack.getItem() instanceof GearItem || stack.is(ModItems.BLAZE_OVERCLOCK_CORE);
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
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
        ItemStack result = ContainerHelper.removeItem(inventory, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(inventory, slot);
        if (!result.isEmpty()) setChanged();
        return result;
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
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 140);
        if (view.contains("BoundX") && view.contains("BoundY") && view.contains("BoundZ")) {
            this.boundNetworkPos = new BlockPos(view.getIntOr("BoundX", 0), view.getIntOr("BoundY", 0), view.getIntOr("BoundZ", 0));
            this.boundDimension = view.getStringOr("BoundDim", "minecraft:overworld");
        } else {
            this.boundNetworkPos = null;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        if (this.boundNetworkPos != null) {
            view.putInt("BoundX", this.boundNetworkPos.getX());
            view.putInt("BoundY", this.boundNetworkPos.getY());
            view.putInt("BoundZ", this.boundNetworkPos.getZ());
            view.putString("BoundDim", this.boundDimension != null ? this.boundDimension : "minecraft:overworld");
        }
    }
}
