package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.ChemicalSynthesizerBlock;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.ChemicalSynthesizerScreenHandler;
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

public class ChemicalSynthesizerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 5_000;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    public static final int SLOT_CARTRIDGE = 0;
    public static final int SLOT_ESSENCE = 1;
    public static final int SLOT_CATALYST = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int GEAR_SLOT = 4;
    public static final int INVENTORY_SIZE = 5;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 160; // 8 seconds base

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

    public ChemicalSynthesizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_SYNTHESIZER_BE, pos, state);
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

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, ChemicalSynthesizerBlockEntity entity) {
        GearTier tier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(tier);

        ItemStack output = entity.getMatchingOutput();

        if (!output.isEmpty() && entity.canOutput(output)) {
            int energyRequired = ENERGY_DRAW * (1 + (tier.ordinal() * 2));
            if (entity.energyStorage.getEnergy() >= energyRequired) {
                entity.energyStorage.extractEnergy(energyRequired, false);
                entity.cookTime++;

                if (!state.getValue(ChemicalSynthesizerBlock.LIT)) {
                    world.setBlockAndUpdate(pos, state.setValue(ChemicalSynthesizerBlock.LIT, true));
                }

                if (entity.cookTime >= entity.totalCookTime) {
                    entity.craft(output);
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

        if (state.getValue(ChemicalSynthesizerBlock.LIT)) {
            world.setBlockAndUpdate(pos, state.setValue(ChemicalSynthesizerBlock.LIT, false));
        }
    }

    private ItemStack getMatchingOutput() {
        ItemStack cartridge = inventory.get(SLOT_CARTRIDGE);
        ItemStack essence = inventory.get(SLOT_ESSENCE);
        ItemStack catalyst = inventory.get(SLOT_CATALYST);

        if (cartridge.isEmpty() || essence.isEmpty() || catalyst.isEmpty()) return ItemStack.EMPTY;

        // 0. Sterile Empty Cartridge Assembly: Glass Pane + Tin Ingot (or Titanium) + Quartz (or Redstone / Glowstone)
        if ((cartridge.is(Items.GLASS_PANE) || cartridge.is(Items.GLASS)) &&
            (essence.is(ModItems.TIN_INGOT) || essence.is(ModItems.TITANIUM_NUGGET) || essence.is(ModItems.TITANIUM_INGOT)) &&
            (catalyst.is(Items.QUARTZ) || catalyst.is(Items.REDSTONE) || catalyst.is(Items.GLOWSTONE_DUST))) {
            return new ItemStack(ModItems.EMPTY_CARTRIDGE, essence.is(ModItems.TITANIUM_INGOT) ? 8 : 4);
        }

        if (cartridge.getItem() != ModItems.EMPTY_CARTRIDGE) return ItemStack.EMPTY;

        Item essItem = essence.getItem();
        Item catItem = catalyst.getItem();

        ItemStack result = ItemStack.EMPTY;
        // 1. Acid-Neutralizing: Alkaline Essence + Redstone (or Glowstone)
        if (essItem == ModItems.ALKALINE_BASE_EXTRACT && (catItem == Items.REDSTONE || catItem == Items.GLOWSTONE_DUST || catItem == ModItems.SULFUR_DUST)) {
            result = new ItemStack(ModItems.ACID_NEUTRALIZING_CARTRIDGE);
        }

        // 2. Endothermic Heat-Buffer: Cryo-Thermal Essence + Blaze Powder (or Fire Crystal, Magma Cream, Volcanic Ash)
        else if (essItem == ModItems.CRYO_THERMAL_EXTRACT && (catItem == Items.BLAZE_POWDER || catItem == ModItems.FIRE_CRYSTAL || catItem == Items.MAGMA_CREAM || catItem == ModItems.VOLCANIC_ASH)) {
            result = new ItemStack(ModItems.HEAT_BUFFER_CARTRIDGE);
        }

        // 3. Hyper-Oxygenation: Oxygenated Essence + Titanium / Aluminum / Iron / Bone Meal
        else if (essItem == ModItems.OXYGENATED_EXTRACT && (catItem == ModItems.TITANIUM_INGOT || catItem == ModItems.ALUMINUM_INGOT || catItem == Items.IRON_INGOT || catItem == Items.BONE_MEAL)) {
            result = new ItemStack(ModItems.HYPER_OXYGENATION_CARTRIDGE);
        }

        // 4. Nanite Trauma: Cellular Nanite Essence + Golden Apple / Titanium Ingot / Ghast Tear / Bone Meal
        else if (essItem == ModItems.CELLULAR_NANITE_EXTRACT && (catItem == Items.GOLDEN_APPLE || catItem == ModItems.TITANIUM_INGOT || catItem == Items.GHAST_TEAR || catItem == Items.BONE_MEAL)) {
            result = new ItemStack(ModItems.NANITE_TRAUMA_CARTRIDGE);
        }

        // 5. Adrenaline Combat Stim: Adrenal Essence + Sugar / Glowstone / Quartz
        else if (essItem == ModItems.ADRENAL_ESSENCE && (catItem == Items.SUGAR || catItem == Items.GLOWSTONE_DUST || catItem == Items.QUARTZ)) {
            result = new ItemStack(ModItems.ADRENALINE_STIM_CARTRIDGE);
        }

        if (!result.isEmpty() && this.level != null && net.enchantedwood.block.entity.CleanroomManager.isInsideSterileCleanroom(this.level, this.worldPosition)) {
            net.enchantedwood.item.custom.HyposprayCartridgeItem.setPure(result, true);
        }

        return result;
    }

    private boolean canOutput(ItemStack output) {
        ItemStack currentOut = inventory.get(SLOT_OUTPUT);
        if (currentOut.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentOut, output)) return false;
        return currentOut.getCount() + output.getCount() <= currentOut.getMaxStackSize();
    }

    private void craft(ItemStack output) {
        inventory.get(SLOT_CARTRIDGE).shrink(1);
        inventory.get(SLOT_ESSENCE).shrink(1);
        inventory.get(SLOT_CATALYST).shrink(1);

        ItemStack currentOut = inventory.get(SLOT_OUTPUT);
        if (currentOut.isEmpty()) {
            inventory.set(SLOT_OUTPUT, output.copy());
        } else {
            currentOut.grow(output.getCount());
        }

        if (this.level instanceof ServerLevel serverWorld && net.enchantedwood.item.custom.HyposprayCartridgeItem.isPure(output)) {
            serverWorld.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 0.9f, 1.8f);
            serverWorld.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5, 6, 0.15, 0.15, 0.15, 0.03);
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 160);
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
        if (side == Direction.DOWN) return new int[]{SLOT_OUTPUT};
        if (side == Direction.UP) return new int[]{SLOT_CARTRIDGE, SLOT_ESSENCE, SLOT_CATALYST};
        return new int[]{SLOT_CARTRIDGE, SLOT_ESSENCE, SLOT_CATALYST, SLOT_OUTPUT};
    }

    public static boolean isEssence(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.ALKALINE_BASE_EXTRACT ||
               item == ModItems.CRYO_THERMAL_EXTRACT ||
               item == ModItems.OXYGENATED_EXTRACT ||
               item == ModItems.CELLULAR_NANITE_EXTRACT ||
               item == ModItems.ADRENAL_ESSENCE;
    }

    public static boolean isCatalyst(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.REDSTONE ||
               item == Items.GLOWSTONE_DUST ||
               item == ModItems.SULFUR_DUST ||
               item == Items.BLAZE_POWDER ||
               item == ModItems.FIRE_CRYSTAL ||
               item == Items.MAGMA_CREAM ||
               item == ModItems.VOLCANIC_ASH ||
               item == Items.BONE_MEAL ||
               item == ModItems.TITANIUM_INGOT ||
               item == ModItems.ALUMINUM_INGOT ||
               item == Items.IRON_INGOT ||
               item == Items.GOLDEN_APPLE ||
               item == Items.ENCHANTED_GOLDEN_APPLE ||
               item == Items.GHAST_TEAR ||
               item == Items.SUGAR ||
               item == Items.QUARTZ;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        if (slot == SLOT_CARTRIDGE) return stack.getItem() == ModItems.EMPTY_CARTRIDGE || stack.is(Items.GLASS_PANE) || stack.is(Items.GLASS);
        if (slot == SLOT_ESSENCE) return isEssence(stack) || stack.is(ModItems.TIN_INGOT) || stack.is(ModItems.TITANIUM_NUGGET) || stack.is(ModItems.TITANIUM_INGOT);
        if (slot == SLOT_CATALYST) return isCatalyst(stack);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
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
        return Component.literal("Chemical Synthesizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ChemicalSynthesizerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }
}
