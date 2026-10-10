package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.IndustrialCentrifugeBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.IndustrialCentrifugeScreenHandler;
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

public class IndustrialCentrifugeBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 25; // 25 FE/t

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int GEAR_SLOT = 3;
    public static final int INVENTORY_SIZE = 4;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 140; // 7 seconds base

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

    public IndustrialCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_CENTRIFUGE_BE, pos, state);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (!gearStack.isEmpty() && gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public static int getTierCookTime(GearTier tier) {
        return switch (tier) {
            case IRON -> 110;
            case COPPER -> 95;
            case BRONZE -> 80;
            case GOLD -> 60;
            case DIAMOND -> 40;
            case NETHERITE -> 20;
            case BLAZE_OVERCLOCK -> 15;
            default -> 140;
        };
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, IndustrialCentrifugeBlockEntity entity) {
        GearTier tier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(tier);

        CentrifugeRecipe recipe = entity.getMatchingRecipe();

        if (recipe != null && entity.canOutput(recipe)) {
            int energyRequired = ENERGY_DRAW * (1 + (tier.ordinal() * 2));
            if (entity.energyStorage.getEnergy() >= energyRequired) {
                entity.energyStorage.extractEnergy(energyRequired, false);
                entity.cookTime++;

                if (!state.getValue(IndustrialCentrifugeBlock.LIT)) {
                    world.setBlockAndUpdate(pos, state.setValue(IndustrialCentrifugeBlock.LIT, true));
                }

                if (entity.cookTime >= entity.totalCookTime) {
                    entity.craft(recipe);
                    entity.cookTime = 0;
                }
                entity.setChanged();
                return;
            }
        }

        if (entity.cookTime > 0) {
            entity.cookTime = Math.max(0, entity.cookTime - 2);
            entity.setChanged();
        }

        if (state.getValue(IndustrialCentrifugeBlock.LIT)) {
            world.setBlockAndUpdate(pos, state.setValue(IndustrialCentrifugeBlock.LIT, false));
        }
    }

    public record CentrifugeRecipe(Item input, ItemStack out1, ItemStack out2) {}

    private @Nullable CentrifugeRecipe getMatchingRecipe() {
        ItemStack input = inventory.get(INPUT_SLOT);
        if (input.isEmpty()) return null;

        Item item = input.getItem();

        // 1. Acid & Alkaline base
        if (item == Items.SLIME_BALL) {
            return new CentrifugeRecipe(item, new ItemStack(ModItems.ALKALINE_BASE_EXTRACT), new ItemStack(ModItems.SULFUR_DUST));
        }
        // 2. Cryo-Thermal
        if (item == Items.MAGMA_CREAM || item == Items.CRIMSON_FUNGUS) {
            return new CentrifugeRecipe(item, new ItemStack(ModItems.CRYO_THERMAL_EXTRACT), new ItemStack(ModItems.VOLCANIC_ASH));
        }
        // 3. Oxygenated Hemoglobin
        if (item == Items.KELP || item == Items.SEAGRASS || item == ModItems.CUCUMBER) {
            return new CentrifugeRecipe(item, new ItemStack(ModItems.OXYGENATED_EXTRACT), new ItemStack(Items.BONE_MEAL));
        }
        // 4. Cellular Nanite
        if (item == ModItems.DRAGON_FRUIT || item == Items.NETHER_WART) {
            return new CentrifugeRecipe(item, new ItemStack(ModItems.CELLULAR_NANITE_EXTRACT), new ItemStack(Items.SUGAR));
        }
        // 5. Adrenal Concentrate
        if (item == Items.GLOW_BERRIES || item == ModItems.WASABI_ROOT) {
            return new CentrifugeRecipe(item, new ItemStack(ModItems.ADRENAL_ESSENCE), new ItemStack(Items.GLOWSTONE_DUST));
        }
        // 6. Mineral Salt Extraction (Calcite / Dripstone)
        if (item == Items.CALCITE) {
            return new CentrifugeRecipe(item, new ItemStack(ModItems.SALT, 2), new ItemStack(Items.SAND));
        }
        if (item == Items.POINTED_DRIPSTONE || item == Items.DRIPSTONE_BLOCK) {
            return new CentrifugeRecipe(item, new ItemStack(ModItems.SALT, 2), new ItemStack(Items.CLAY_BALL));
        }

        return null;
    }


    private boolean canOutput(CentrifugeRecipe recipe) {
        ItemStack currentOut1 = inventory.get(OUTPUT_SLOT_1);
        ItemStack currentOut2 = inventory.get(OUTPUT_SLOT_2);

        boolean out1Ok = currentOut1.isEmpty() || (ItemStack.isSameItemSameComponents(currentOut1, recipe.out1()) &&
                currentOut1.getCount() + recipe.out1().getCount() <= currentOut1.getMaxStackSize());

        boolean out2Ok = currentOut2.isEmpty() || (ItemStack.isSameItemSameComponents(currentOut2, recipe.out2()) &&
                currentOut2.getCount() + recipe.out2().getCount() <= currentOut2.getMaxStackSize());

        return out1Ok && out2Ok;
    }

    private void craft(CentrifugeRecipe recipe) {
        inventory.get(INPUT_SLOT).shrink(1);

        ItemStack currentOut1 = inventory.get(OUTPUT_SLOT_1);
        if (currentOut1.isEmpty()) {
            inventory.set(OUTPUT_SLOT_1, recipe.out1().copy());
        } else {
            currentOut1.grow(recipe.out1().getCount());
        }

        ItemStack currentOut2 = inventory.get(OUTPUT_SLOT_2);
        if (currentOut2.isEmpty()) {
            inventory.set(OUTPUT_SLOT_2, recipe.out2().copy());
        } else {
            currentOut2.grow(recipe.out2().getCount());
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 140);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2};
        if (side == Direction.UP) return new int[]{INPUT_SLOT};
        return new int[]{INPUT_SLOT, OUTPUT_SLOT_1, OUTPUT_SLOT_2};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == OUTPUT_SLOT_1 || slot == OUTPUT_SLOT_2) return false;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT_1 || slot == OUTPUT_SLOT_2;
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
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
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
    public Component getDisplayName() {
        return Component.literal("Industrial Centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new IndustrialCentrifugeScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }
}
