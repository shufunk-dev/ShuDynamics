package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.ChemicalSynthesizerBlock;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.ChemicalSynthesizerScreenHandler;
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

public class ChemicalSynthesizerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 5_000;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    public static final int SLOT_CARTRIDGE = 0;
    public static final int SLOT_ESSENCE = 1;
    public static final int SLOT_CATALYST = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int GEAR_SLOT = 4;
    public static final int INVENTORY_SIZE = 5;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 160; // 8 seconds base

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

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, ChemicalSynthesizerBlockEntity entity) {
        GearTier tier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(tier);

        ItemStack output = entity.getMatchingOutput();

        if (!output.isEmpty() && entity.canOutput(output)) {
            int energyRequired = ENERGY_DRAW * (1 + (tier.ordinal() * 2));
            if (entity.energyStorage.getEnergy() >= energyRequired) {
                entity.energyStorage.extractEnergy(energyRequired, false);
                entity.cookTime++;

                if (!state.get(ChemicalSynthesizerBlock.LIT)) {
                    world.setBlockState(pos, state.with(ChemicalSynthesizerBlock.LIT, true));
                }

                if (entity.cookTime >= entity.totalCookTime) {
                    entity.craft(output);
                    entity.cookTime = 0;
                }
                entity.markDirty();
                return;
            }
        }

        if (entity.cookTime > 0) {
            entity.cookTime = Math.max(0, entity.cookTime - 2);
            entity.markDirty();
        }

        if (state.get(ChemicalSynthesizerBlock.LIT)) {
            world.setBlockState(pos, state.with(ChemicalSynthesizerBlock.LIT, false));
        }
    }

    private ItemStack getMatchingOutput() {
        ItemStack cartridge = inventory.get(SLOT_CARTRIDGE);
        ItemStack essence = inventory.get(SLOT_ESSENCE);
        ItemStack catalyst = inventory.get(SLOT_CATALYST);

        if (cartridge.isEmpty() || essence.isEmpty() || catalyst.isEmpty()) return ItemStack.EMPTY;

        // 0. Sterile Empty Cartridge Assembly: Glass Pane + Tin Ingot (or Titanium) + Quartz (or Redstone / Glowstone)
        if ((cartridge.isOf(Items.GLASS_PANE) || cartridge.isOf(Items.GLASS)) &&
            (essence.isOf(ModItems.TIN_INGOT) || essence.isOf(ModItems.TITANIUM_NUGGET) || essence.isOf(ModItems.TITANIUM_INGOT)) &&
            (catalyst.isOf(Items.QUARTZ) || catalyst.isOf(Items.REDSTONE) || catalyst.isOf(Items.GLOWSTONE_DUST))) {
            return new ItemStack(ModItems.EMPTY_CARTRIDGE, essence.isOf(ModItems.TITANIUM_INGOT) ? 8 : 4);
        }

        if (cartridge.getItem() != ModItems.EMPTY_CARTRIDGE) return ItemStack.EMPTY;

        Item essItem = essence.getItem();
        Item catItem = catalyst.getItem();

        ItemStack result = ItemStack.EMPTY;
        // 1. Acid-Neutralizing: Alkaline Essence + Redstone (or Glowstone)
        if (essItem == ModItems.ALKALINE_BASE_EXTRACT && (catItem == Items.REDSTONE || catItem == Items.GLOWSTONE_DUST || catItem == ModItems.SULFUR_DUST)) {
            result = new ItemStack(ModItems.ACID_NEUTRALIZING_CARTRIDGE);
        }

        // 2. Endothermic Heat-Buffer: Cryo-Thermal Essence + Blaze Powder (or Fire Crystal)
        else if (essItem == ModItems.CRYO_THERMAL_EXTRACT && (catItem == Items.BLAZE_POWDER || catItem == ModItems.FIRE_CRYSTAL || catItem == Items.MAGMA_CREAM)) {
            result = new ItemStack(ModItems.HEAT_BUFFER_CARTRIDGE);
        }

        // 3. Hyper-Oxygenation: Oxygenated Essence + Titanium / Aluminum
        else if (essItem == ModItems.OXYGENATED_EXTRACT && (catItem == ModItems.TITANIUM_INGOT || catItem == ModItems.ALUMINUM_INGOT || catItem == Items.IRON_INGOT)) {
            result = new ItemStack(ModItems.HYPER_OXYGENATION_CARTRIDGE);
        }

        // 4. Nanite Trauma: Cellular Nanite Essence + Golden Apple / Titanium Ingot / Ghast Tear
        else if (essItem == ModItems.CELLULAR_NANITE_EXTRACT && (catItem == Items.GOLDEN_APPLE || catItem == ModItems.TITANIUM_INGOT || catItem == Items.GHAST_TEAR)) {
            result = new ItemStack(ModItems.NANITE_TRAUMA_CARTRIDGE);
        }

        // 5. Adrenaline Combat Stim: Adrenal Essence + Sugar / Glowstone / Quartz
        else if (essItem == ModItems.ADRENAL_ESSENCE && (catItem == Items.SUGAR || catItem == Items.GLOWSTONE_DUST || catItem == Items.QUARTZ)) {
            result = new ItemStack(ModItems.ADRENALINE_STIM_CARTRIDGE);
        }

        if (!result.isEmpty() && this.world != null && net.enchantedwood.block.entity.CleanroomManager.isInsideSterileCleanroom(this.world, this.pos)) {
            net.enchantedwood.item.custom.HyposprayCartridgeItem.setPure(result, true);
        }

        return result;
    }

    private boolean canOutput(ItemStack output) {
        ItemStack currentOut = inventory.get(SLOT_OUTPUT);
        if (currentOut.isEmpty()) return true;
        if (!ItemStack.areItemsAndComponentsEqual(currentOut, output)) return false;
        return currentOut.getCount() + output.getCount() <= currentOut.getMaxCount();
    }

    private void craft(ItemStack output) {
        inventory.get(SLOT_CARTRIDGE).decrement(1);
        inventory.get(SLOT_ESSENCE).decrement(1);
        inventory.get(SLOT_CATALYST).decrement(1);

        ItemStack currentOut = inventory.get(SLOT_OUTPUT);
        if (currentOut.isEmpty()) {
            inventory.set(SLOT_OUTPUT, output.copy());
        } else {
            currentOut.increment(output.getCount());
        }

        if (this.world instanceof ServerWorld serverWorld && net.enchantedwood.item.custom.HyposprayCartridgeItem.isPure(output)) {
            serverWorld.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, net.minecraft.sound.SoundCategory.BLOCKS, 0.9f, 1.8f);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.15, 0.15, 0.15, 0.03);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", 160);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
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
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        if (slot == SLOT_CARTRIDGE) return stack.getItem() == ModItems.EMPTY_CARTRIDGE || stack.isOf(Items.GLASS_PANE) || stack.isOf(Items.GLASS);
        if (slot == SLOT_ESSENCE) return isEssence(stack) || stack.isOf(ModItems.TIN_INGOT) || stack.isOf(ModItems.TITANIUM_NUGGET) || stack.isOf(ModItems.TITANIUM_INGOT);
        if (slot == SLOT_CATALYST) return isCatalyst(stack);
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public int size() {
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
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
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

    @Override
    public Text getDisplayName() {
        return Text.literal("Chemical Synthesizer");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ChemicalSynthesizerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }
}
