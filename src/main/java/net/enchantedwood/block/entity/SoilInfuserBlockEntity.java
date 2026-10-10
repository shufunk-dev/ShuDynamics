package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.SoilInfuserBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.SoilInfuserScreenHandler;
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

public class SoilInfuserBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 30; // 30 FE/t

    public static final int INPUT_SLOT_DIRT = 0;
    public static final int INPUT_SLOT_MINERAL = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int GEAR_SLOT = 3;
    public static final int INVENTORY_SIZE = 4;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 120;
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

    public SoilInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOIL_INFUSER_BE, pos, state);
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
            case IRON -> 95;
            case COPPER -> 80;
            case BRONZE -> 65;
            case GOLD -> 50;
            case TITANIUM -> 40;
            case DIAMOND -> 30;
            case NETHERITE -> 12;
            case BLAZE_OVERCLOCK -> 8;
            default -> 120;
        };
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.soil_infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new SoilInfuserScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, SoilInfuserBlockEntity entity) {
        boolean dirty = false;

        entity.totalCookTime = getTierCookTime(entity.getActiveGearTier());

        ItemStack dirtStack = entity.inventory.get(INPUT_SLOT_DIRT);
        ItemStack minStack = entity.inventory.get(INPUT_SLOT_MINERAL);

        boolean canInfuse = canProcess(dirtStack, minStack, entity.inventory.get(OUTPUT_SLOT));
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        boolean isInfusing = false;
        if (canInfuse && hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            ++entity.cookTime;
            isInfusing = true;
            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                entity.processInfuse();
            }
            dirty = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                dirty = true;
            }
        }

        if (state.getValue(SoilInfuserBlock.LIT) != isInfusing) {
            world.setBlock(pos, state.setValue(SoilInfuserBlock.LIT, isInfusing), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    private static boolean canProcess(ItemStack dirt, ItemStack mineral, ItemStack output) {
        if (dirt.isEmpty() || mineral.isEmpty()) return false;
        if (!isDirtMaterial(dirt.getItem())) return false;
        if (!isMineralMaterial(mineral.getItem())) return false;

        if (output.isEmpty()) return true;
        if (!output.is(ModBlocks.VOLCANIC_SOIL.asItem())) return false;
        return output.getCount() + 2 <= output.getMaxStackSize();
    }

    private void processInfuse() {
        ItemStack dirt = inventory.get(INPUT_SLOT_DIRT);
        ItemStack mineral = inventory.get(INPUT_SLOT_MINERAL);
        ItemStack out = inventory.get(OUTPUT_SLOT);

        boolean isFlux = mineral.is(ModItems.BASALT_FLUX_CATALYST);
        int yield = isFlux ? 4 : 2;

        dirt.shrink(1);
        mineral.shrink(1);

        if (out.isEmpty()) {
            inventory.set(OUTPUT_SLOT, new ItemStack(ModBlocks.VOLCANIC_SOIL, yield));
        } else {
            out.grow(yield);
        }

        this.experience += 1.0f;
    }

    public static boolean isDirtMaterial(Item item) {
        return item == Items.DIRT || item == Items.COARSE_DIRT || item == Items.ROOTED_DIRT || item == Items.MUD || item == Items.PODZOL;
    }

    public static boolean isMineralMaterial(Item item) {
        return item == ModItems.VOLCANIC_ASH || item == ModItems.VOLCANIC_FERTILIZER || item == ModItems.SULFUR_DUST || item == ModItems.BASALT_FLUX_CATALYST;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 120);
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
        if (side == Direction.UP) return new int[]{INPUT_SLOT_DIRT, INPUT_SLOT_MINERAL};
        return new int[]{INPUT_SLOT_DIRT, INPUT_SLOT_MINERAL, GEAR_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT_DIRT) return isDirtMaterial(stack.getItem());
        if (slot == INPUT_SLOT_MINERAL) return isMineralMaterial(stack.getItem());
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
