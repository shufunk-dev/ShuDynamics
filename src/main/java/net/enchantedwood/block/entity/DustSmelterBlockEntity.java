package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.DustSmelterBlock;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.DustSmelterScreenHandler;
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

public class DustSmelterBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_DRAW = 50; // 50 FE/t

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);

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

    public DustSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUST_SMELTER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.dust_smelter");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DustSmelterScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(1);
        if (gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
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

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, DustSmelterBlockEntity entity) {
        boolean stateChanged = false;

        GearTier currentGearTier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(currentGearTier);

        if (state.getValue(DustSmelterBlock.GEAR_TIER) != currentGearTier) {
            state = state.setValue(DustSmelterBlock.GEAR_TIER, currentGearTier);
            world.setBlock(pos, state, 3);
            stateChanged = true;
        }

        ItemStack inputStack = entity.inventory.get(0);
        boolean canProcess = entity.canProcessInput(inputStack);
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        if (canProcess && hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            ++entity.cookTime;
            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                entity.processInput(inputStack);
            }
            stateChanged = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                stateChanged = true;
            }
        }

        boolean isRunningNow = canProcess && hasEnergy;
        if (state.getValue(DustSmelterBlock.LIT) != isRunningNow) {
            world.setBlock(pos, state.setValue(DustSmelterBlock.LIT, isRunningNow), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(world, pos, state);
        }
    }

    private boolean canProcessInput(ItemStack input) {
        if (input.isEmpty()) return false;
        Item outputItem = getOutputItem(input.getItem());
        if (outputItem == null) return false;

        ItemStack currentOutput = inventory.get(2);
        if (currentOutput.isEmpty()) return true;
        if (!currentOutput.is(outputItem)) return false;
        return currentOutput.getCount() + 1 <= currentOutput.getMaxStackSize();
    }

    public void dropExperience(ServerLevel world, Player player) {
        int totalXp = (int) this.experience;
        float remainder = this.experience - totalXp;
        if (remainder > 0.0f && Math.random() < remainder) {
            totalXp++;
        }
        this.experience = 0.0f;
        if (totalXp > 0) {
            net.minecraft.world.entity.ExperienceOrb.award(world, net.minecraft.world.phys.Vec3.atCenterOf(this.worldPosition), totalXp);
        }
        setChanged();
    }

    private void processInput(ItemStack input) {
        if (!canProcessInput(input)) return;

        Item outputItem = getOutputItem(input.getItem());
        ItemStack currentOutput = inventory.get(2);

        if (currentOutput.isEmpty()) {
            inventory.set(2, new ItemStack(outputItem, 1));
        } else {
            currentOutput.grow(1);
        }

        this.experience += getExperienceAmount(input.getItem());
        input.shrink(1);
    }

    private float getExperienceAmount(Item item) {
        if (item == ModItems.IRON_DUST || item == ModItems.COPPER_DUST) return 0.7f;
        if (item == ModItems.GOLD_DUST || item == ModItems.DIAMOND_DUST || item == ModItems.EMERALD_DUST) return 1.0f;
        if (item == ModItems.NETHERITE_DUST) return 2.0f;
        if (item == ModItems.COAL_DUST) return 0.1f;
        return 0.7f;
    }

    private Item getOutputItem(Item item) {
        if (item == ModItems.IRON_DUST) return Items.IRON_INGOT;
        if (item == ModItems.COPPER_DUST) return Items.COPPER_INGOT;
        if (item == ModItems.TIN_DUST) return ModItems.TIN_INGOT;
        if (item == ModItems.BRONZE_DUST) return ModItems.BRONZE_INGOT;
        if (item == ModItems.TITANIUM_DUST) return ModItems.TITANIUM_INGOT;
        if (item == ModItems.STEEL_DUST) return ModItems.STEEL_INGOT;
        if (item == ModItems.TUNGSTEN_DUST) return ModItems.TUNGSTEN_INGOT;
        if (item == ModItems.COBALT_DUST) return ModItems.COBALT_INGOT;
        if (item == ModItems.ARDITE_DUST) return ModItems.ARDITE_INGOT;
        if (item == ModItems.MANYULLYN_DUST) return ModItems.MANYULLYN_INGOT;
        if (item == ModItems.GOLD_DUST) return Items.GOLD_INGOT;
        if (item == ModItems.DIAMOND_DUST) return Items.DIAMOND;
        if (item == ModItems.NETHERITE_DUST) return Items.NETHERITE_INGOT;
        if (item == ModItems.EMERALD_DUST) return Items.EMERALD;
        if (item == ModItems.COAL_DUST) return Items.COAL;
        if (item == ModItems.QUARTZ_DUST) return ModItems.SILICON;
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

    // SidedInventory Implementation
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{2};
        if (side == Direction.UP) return new int[]{0};
        return new int[]{0, 1, 2};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return getOutputItem(stack.getItem()) != null;
        if (slot == 1) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == 2;
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
