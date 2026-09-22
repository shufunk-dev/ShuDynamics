package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.HarmonicRecordPressBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.HarmonicRecordPressScreenHandler;
import org.jetbrains.annotations.Nullable;

public class HarmonicRecordPressBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 5_000;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    public static final int SLOT_DISC = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_GEAR = 3;
    public static final int INVENTORY_SIZE = 4;

    private static final int[] TOP_SIDES_SLOTS = new int[]{SLOT_DISC, SLOT_CATALYST};
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_OUTPUT};

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 100; // 5 seconds base

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

    public HarmonicRecordPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HARMONIC_RECORD_PRESS_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, HarmonicRecordPressBlockEntity entity) {
        if (world.isClient()) return;

        GearTier gear = entity.getActiveGearTier();
        if (state.get(HarmonicRecordPressBlock.GEAR_TIER) != gear) {
            world.setBlockState(pos, state.with(HarmonicRecordPressBlock.GEAR_TIER, gear), 3);
        }

        ItemStack disc = entity.inventory.get(SLOT_DISC);
        ItemStack catalyst = entity.inventory.get(SLOT_CATALYST);
        ItemStack result = getPressResult(disc, catalyst);

        boolean canWork = !result.isEmpty() && entity.canOutput(result);

        if (canWork && entity.energyStorage.getEnergy() >= ENERGY_DRAW) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
            entity.cookTime++;

            // Subtle vinyl groove cutting scratch sound
            if (entity.cookTime % 25 == 0) {
                world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.35f, 1.6f);
            }

            if (!state.get(HarmonicRecordPressBlock.LIT)) {
                world.setBlockState(pos, state.with(HarmonicRecordPressBlock.LIT, true), 3);
            }

            int requiredCookTime = entity.calculateTotalCookTime(gear);
            entity.totalCookTime = requiredCookTime;

            if (entity.cookTime >= requiredCookTime) {
                entity.craftItem(result);
                entity.cookTime = 0;
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 0.7f, 1.2f);
            }
            entity.markDirty();
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                entity.markDirty();
            }
            if (state.get(HarmonicRecordPressBlock.LIT)) {
                world.setBlockState(pos, state.with(HarmonicRecordPressBlock.LIT, false), 3);
            }
        }
    }

    private int calculateTotalCookTime(GearTier gear) {
        return switch (gear) {
            case IRON, COPPER -> 80;
            case BRONZE, ALUMINUM, STEEL -> 60;
            case GOLD -> 45;
            case TITANIUM, DIAMOND -> 30;
            case NETHERITE -> 20;
            case BLAZE_OVERCLOCK -> 15;
            default -> 100;
        };
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = this.inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.areItemsEqual(out, result)) return false;
        return out.getCount() < out.getMaxCount();
    }

    private void craftItem(ItemStack result) {
        // Decrement 1 disc
        this.inventory.get(SLOT_DISC).decrement(1);
        // Decrement 1 catalyst
        this.inventory.get(SLOT_CATALYST).decrement(1);

        // Add output
        ItemStack out = this.inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) {
            this.inventory.set(SLOT_OUTPUT, result.copy());
        } else {
            out.increment(1);
        }
    }

    public static ItemStack getPressResult(ItemStack disc, ItemStack catalyst) {
        if (disc.isEmpty() || catalyst.isEmpty()) return ItemStack.EMPTY;

        Item discItem = disc.getItem();
        Item catItem = catalyst.getItem();

        // Must be Blank Vinyl Disc
        boolean isBlank = discItem == ModItems.BLANK_VINYL_DISC;
        if (!isBlank) return ItemStack.EMPTY;

        // If catalyst is already a music disc, duplicate that disc!
        if (catalyst.contains(DataComponentTypes.JUKEBOX_PLAYABLE)) {
            return new ItemStack(catItem);
        }

        // 1. Rip the Sky Wide (Convergence)
        if (catItem == Items.CRYING_OBSIDIAN || catItem == ModItems.MYSTERY_KEYSTONE) {
            return new ItemStack(ModItems.MUSIC_DISC_CONVERGENCE);
        }
        // 2. Rift of the Colossus
        if (catItem == ModItems.FIRE_CRYSTAL || catItem == ModItems.CORE_OF_AWAKENING) {
            return new ItemStack(ModItems.MUSIC_DISC_COLOSSUS);
        }
        // 3. Highway Overdrive
        if (catItem == ModItems.HIGH_OCTANE_FUEL_CANISTER || catItem == ModItems.GASOLINE_CANISTER || catItem == ModItems.BIOFUEL_CANISTER) {
            return new ItemStack(ModItems.MUSIC_DISC_OVERDRIVE);
        }
        // 4. Sterile Protocol
        if (catItem == ModItems.SILICON_WAFER || catItem == ModItems.ADRENALINE_STIM_CARTRIDGE || catItem == ModItems.STERILE_POLYMER_FABRIC) {
            return new ItemStack(ModItems.MUSIC_DISC_CLEANROOM);
        }
        // 5. Subroutine 64k
        if (catItem == ModItems.STORAGE_CRYSTAL_1K || catItem == ModItems.STORAGE_CRYSTAL_4K ||
            catItem == ModItems.STORAGE_CRYSTAL_16K || catItem == ModItems.STORAGE_CRYSTAL_64K ||
            catItem == ModItems.BASIC_COMPUTER_CHIP || catItem == ModItems.ADVANCED_COMPUTER_CHIP) {
            return new ItemStack(ModItems.MUSIC_DISC_AUTOCRAFT);
        }
        // 6. Haven Bloom
        if (catItem == ModItems.MASTER_RAINBOW_ROLL || catItem == ModItems.WASABI_ROOT ||
            catItem == ModItems.SUSHI_RICE || catItem == ModItems.NORI_SHEET || catItem == ModItems.GOLDEN_HONEY_MOCHI) {
            return new ItemStack(ModItems.MUSIC_DISC_HAVEN_BLOOM);
        }
        // 7. Heart of the Crucible
        if (catItem == ModItems.MANYULLYN_INGOT || catItem == ModItems.BASALT_FLUX_CATALYST || catItem == ModItems.TUNGSTEN_CARBIDE_INGOT) {
            return new ItemStack(ModItems.MUSIC_DISC_CRUCIBLE);
        }
        // 8. Stratosphere Break
        if (catItem == ModItems.HYDROGEN_CANISTER || catItem == ModItems.ION_REPULSOR_MODULE || catItem == ModItems.HYDROGEN_JETPACK) {
            return new ItemStack(ModItems.MUSIC_DISC_STRATOSPHERE);
        }
        // 9. Abyssal Pressure
        if (catItem == ModItems.DIVING_MASK || catItem == ModItems.SNORKEL || catItem == Items.NAUTILUS_SHELL || catItem == ModItems.SCUBA_CHESTPLATE) {
            return new ItemStack(ModItems.MUSIC_DISC_ABYSSAL);
        }
        // 10. Anoxic Echoes
        if (catItem == ModItems.SULFUR_DUST || catItem == ModItems.CUCUMBER || catItem == ModItems.DRAGON_FRUIT || catItem == ModItems.PRIMORDIAL_CATALYST) {
            return new ItemStack(ModItems.MUSIC_DISC_ANOXIC);
        }

        return ItemStack.EMPTY;
    }

    public GearTier getActiveGearTier() {
        ItemStack gear = this.inventory.get(SLOT_GEAR);
        if (gear.getItem() instanceof GearItem gi) {
            return gi.getGearTier();
        }
        return GearTier.NONE;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }
        return TOP_SIDES_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_DISC) return stack.isOf(ModItems.BLANK_VINYL_DISC);
        if (slot == SLOT_CATALYST) return true;
        if (slot == SLOT_GEAR) return stack.getItem() instanceof GearItem;
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
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        this.markDirty();
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
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.harmonic_record_press");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new HarmonicRecordPressScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public SimpleEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
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
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", 100);
    }
}
