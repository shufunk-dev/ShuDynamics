package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.DustSmelterMk2Block;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.DustSmelterMk2ScreenHandler;
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

public class DustSmelterMk2BlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 5_000;
    public static final int ENERGY_DRAW = 75; // 75 FE/t when processing

    public static final int INPUT_SLOT_A = 0;
    public static final int INPUT_SLOT_B = 1;
    public static final int OUTPUT_SLOT_A = 2;
    public static final int OUTPUT_SLOT_B = 3;
    public static final int GEAR_SLOT = 4;
    public static final int INVENTORY_SIZE = 5;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 80;
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

    public DustSmelterMk2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUST_SMELTER_MK2_BE, pos, state);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        if (gearStack.is(ModItems.BLAZE_OVERCLOCK_CORE)) {
            return GearTier.BLAZE_OVERCLOCK;
        }
        return GearTier.NONE;
    }

    public static int getTierCookTime(GearTier tier) {
        return switch (tier) {
            case IRON -> 60;
            case COPPER -> 50;
            case BRONZE -> 40;
            case GOLD -> 30;
            case DIAMOND -> 20;
            case NETHERITE -> 10;
            case BLAZE_OVERCLOCK -> 4;
            default -> 80;
        };
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enchantedwood.dust_smelter_mk2");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DustSmelterMk2ScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, DustSmelterMk2BlockEntity entity) {
        boolean dirty = false;

        entity.totalCookTime = getTierCookTime(entity.getActiveGearTier());

        int targetA = entity.getTargetOutputSlot(INPUT_SLOT_A);
        int targetB = entity.getTargetOutputSlot(INPUT_SLOT_B);

        // If both want the same empty slot, assign separate empty slots
        if (targetA != -1 && targetA == targetB && entity.inventory.get(targetA).isEmpty()) {
            targetA = OUTPUT_SLOT_A;
            targetB = OUTPUT_SLOT_B;
        }

        boolean canA = targetA != -1;
        boolean canB = targetB != -1;
        boolean canProcess = canA || canB;
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;

        boolean isSmelting = false;
        if (canProcess && hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            ++entity.cookTime;
            isSmelting = true;

            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                int finalTargetA = entity.getTargetOutputSlot(INPUT_SLOT_A);
                int finalTargetB = entity.getTargetOutputSlot(INPUT_SLOT_B);
                if (finalTargetA != -1 && finalTargetA == finalTargetB && entity.inventory.get(finalTargetA).isEmpty()) {
                    finalTargetA = OUTPUT_SLOT_A;
                    finalTargetB = OUTPUT_SLOT_B;
                }

                if (finalTargetA != -1) {
                    entity.processLane(INPUT_SLOT_A, finalTargetA);
                }
                if (finalTargetB != -1) {
                    entity.processLane(INPUT_SLOT_B, finalTargetB);
                }
            }
            dirty = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                dirty = true;
            }
        }

        if (state.getValue(DustSmelterMk2Block.LIT) != isSmelting) {
            world.setBlock(pos, state.setValue(DustSmelterMk2Block.LIT, isSmelting), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    public int getTargetOutputSlot(int inputSlot) {
        ItemStack input = inventory.get(inputSlot);
        if (input.isEmpty()) return -1;

        Item outputItem = getOutputItem(input.getItem());
        if (outputItem == null) return -1;

        int preferred = (inputSlot == INPUT_SLOT_A) ? OUTPUT_SLOT_A : OUTPUT_SLOT_B;
        int secondary = (preferred == OUTPUT_SLOT_A) ? OUTPUT_SLOT_B : OUTPUT_SLOT_A;

        // 1. Try to merge into an output slot that already has this exact ingot with space
        ItemStack prefStack = inventory.get(preferred);
        if (!prefStack.isEmpty() && prefStack.is(outputItem) && prefStack.getCount() + 1 <= prefStack.getMaxStackSize()) {
            return preferred;
        }
        ItemStack secStack = inventory.get(secondary);
        if (!secStack.isEmpty() && secStack.is(outputItem) && secStack.getCount() + 1 <= secStack.getMaxStackSize()) {
            return secondary;
        }

        // 2. Try an empty slot, favoring preferred lane
        if (prefStack.isEmpty()) {
            return preferred;
        }
        if (secStack.isEmpty()) {
            return secondary;
        }

        return -1;
    }

    private void processLane(int inputSlot, int outputSlot) {
        ItemStack input = inventory.get(inputSlot);
        if (input.isEmpty()) return;

        Item outputItem = getOutputItem(input.getItem());
        if (outputItem == null) return;

        ItemStack currentOut = inventory.get(outputSlot);
        if (currentOut.isEmpty()) {
            inventory.set(outputSlot, new ItemStack(outputItem, 1));
        } else if (currentOut.is(outputItem) && currentOut.getCount() + 1 <= currentOut.getMaxStackSize()) {
            currentOut.grow(1);
        } else {
            return;
        }

        this.experience += getExperienceAmount(input.getItem());
        input.shrink(1);
    }

    public static Item getOutputItem(Item item) {
        // Dusts
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

        // Raw Ores
        if (item == Items.RAW_IRON) return Items.IRON_INGOT;
        if (item == Items.RAW_COPPER) return Items.COPPER_INGOT;
        if (item == Items.RAW_GOLD) return Items.GOLD_INGOT;
        if (item == ModItems.RAW_TIN) return ModItems.TIN_INGOT;
        if (item == ModItems.RAW_TITANIUM) return ModItems.TITANIUM_INGOT;
        if (item == ModItems.RAW_TUNGSTEN) return ModItems.TUNGSTEN_INGOT;
        if (item == ModItems.RAW_COBALT) return ModItems.COBALT_INGOT;
        if (item == ModItems.RAW_ARDITE) return ModItems.ARDITE_INGOT;

        return null;
    }

    private static float getExperienceAmount(Item item) {
        if (item == ModItems.IRON_DUST || item == ModItems.COPPER_DUST || item == Items.RAW_IRON || item == Items.RAW_COPPER) return 0.7f;
        if (item == ModItems.GOLD_DUST || item == ModItems.DIAMOND_DUST || item == ModItems.EMERALD_DUST || item == Items.RAW_GOLD) return 1.0f;
        if (item == ModItems.NETHERITE_DUST) return 2.0f;
        if (item == ModItems.COAL_DUST) return 0.1f;
        return 0.7f;
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

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 80);
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
        if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT_A, OUTPUT_SLOT_B};
        if (side == Direction.UP) return new int[]{INPUT_SLOT_A, INPUT_SLOT_B};
        return new int[]{GEAR_SLOT, INPUT_SLOT_A, INPUT_SLOT_B, OUTPUT_SLOT_A, OUTPUT_SLOT_B};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT_A) {
            if (getOutputItem(stack.getItem()) == null) return false;
            // Anti-monopoly: reject if Slot B already holds this item
            if (dir != null) {
                ItemStack slotB = inventory.get(INPUT_SLOT_B);
                if (!slotB.isEmpty() && ItemStack.isSameItemSameComponents(slotB, stack)) {
                    return false;
                }
            }
            return true;
        }
        if (slot == INPUT_SLOT_B) {
            if (getOutputItem(stack.getItem()) == null) return false;
            // Anti-monopoly: reject if Slot A already holds this item
            if (dir != null) {
                ItemStack slotA = inventory.get(INPUT_SLOT_A);
                if (!slotA.isEmpty() && ItemStack.isSameItemSameComponents(slotA, stack)) {
                    return false;
                }
            }
            return true;
        }
        if (slot == GEAR_SLOT) {
            return stack.getItem() instanceof GearItem || stack.is(ModItems.BLAZE_OVERCLOCK_CORE);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == OUTPUT_SLOT_A || slot == OUTPUT_SLOT_B;
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
