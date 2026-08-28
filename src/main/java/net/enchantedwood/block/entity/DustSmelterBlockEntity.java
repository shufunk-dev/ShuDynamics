package net.enchantedwood.block.entity;

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
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.DustSmelterBlock;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.DustSmelterScreenHandler;
import org.jetbrains.annotations.Nullable;

public class DustSmelterBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_DRAW = 50; // 50 FE/t

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);

    private int cookTime = 0;
    private int totalCookTime = 160;
    private float experience = 0.0f;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
            return 7;
        }
    };

    public DustSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUST_SMELTER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.dust_smelter");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
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
            default -> 160;
        };
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, DustSmelterBlockEntity entity) {
        boolean stateChanged = false;

        GearTier currentGearTier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(currentGearTier);

        if (state.get(DustSmelterBlock.GEAR_TIER) != currentGearTier) {
            state = state.with(DustSmelterBlock.GEAR_TIER, currentGearTier);
            world.setBlockState(pos, state, 3);
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
        if (state.get(DustSmelterBlock.LIT) != isRunningNow) {
            world.setBlockState(pos, state.with(DustSmelterBlock.LIT, isRunningNow), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            markDirty(world, pos, state);
        }
    }

    private boolean canProcessInput(ItemStack input) {
        if (input.isEmpty()) return false;
        Item outputItem = getOutputItem(input.getItem());
        if (outputItem == null) return false;

        ItemStack currentOutput = inventory.get(2);
        if (currentOutput.isEmpty()) return true;
        if (!currentOutput.isOf(outputItem)) return false;
        return currentOutput.getCount() + 1 <= currentOutput.getMaxCount();
    }

    public void dropExperience(ServerWorld world, PlayerEntity player) {
        int totalXp = (int) this.experience;
        float remainder = this.experience - totalXp;
        if (remainder > 0.0f && Math.random() < remainder) {
            totalXp++;
        }
        this.experience = 0.0f;
        if (totalXp > 0) {
            net.minecraft.entity.ExperienceOrbEntity.spawn(world, net.minecraft.util.math.Vec3d.ofCenter(this.pos), totalXp);
        }
        markDirty();
    }

    private void processInput(ItemStack input) {
        if (!canProcessInput(input)) return;

        Item outputItem = getOutputItem(input.getItem());
        ItemStack currentOutput = inventory.get(2);

        if (currentOutput.isEmpty()) {
            inventory.set(2, new ItemStack(outputItem, 1));
        } else {
            currentOutput.increment(1);
        }

        this.experience += getExperienceAmount(input.getItem());
        input.decrement(1);
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
        return null;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
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

    // SidedInventory Implementation
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{2};
        if (side == Direction.UP) return new int[]{0};
        return new int[]{0, 1, 2};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return getOutputItem(stack.getItem()) != null;
        if (slot == 1) return stack.getItem() instanceof GearItem;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == 2;
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
