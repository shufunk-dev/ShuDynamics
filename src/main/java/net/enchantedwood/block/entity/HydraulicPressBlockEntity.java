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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.HydraulicPressBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.HydraulicPressScreenHandler;
import org.jetbrains.annotations.Nullable;

public class HydraulicPressBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);

    private static final int INPUT_SLOT = 0;
    private static final int GEAR_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    private int cookTime = 0;
    private int totalCookTime = 100;

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
                case 0 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0xFFFF0000) | (value & 0xFFFF));
                case 1 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
                case 4 -> cookTime = value;
                case 5 -> totalCookTime = value;
            }
        }

        @Override
        public int size() {
            return 7;
        }
    };

    public HydraulicPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_PRESS_BLOCK_ENTITY, pos, state);
    }

    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state, HydraulicPressBlockEntity entity) {
        if (world.isClient()) return;

        boolean isCooking = false;
        GearTier gearTier = entity.getActiveGearTier();

        if (state.get(HydraulicPressBlock.GEAR_TIER) != gearTier) {
            world.setBlockState(pos, state.with(HydraulicPressBlock.GEAR_TIER, gearTier), 3);
        }

        if (entity.canProcess()) {
            if (entity.energyStorage.getEnergy() >= ENERGY_DRAW) {
                entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
                entity.cookTime += entity.getProcessingSpeed(gearTier);
                isCooking = true;

                if (entity.cookTime >= entity.totalCookTime) {
                    entity.cookTime = 0;
                    entity.processItem();
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.6f, 0.7f);
                }
            }
        } else {
            entity.cookTime = Math.max(0, entity.cookTime - 2);
        }

        if (state.get(HydraulicPressBlock.LIT) != isCooking) {
            world.setBlockState(pos, state.with(HydraulicPressBlock.LIT, isCooking), 3);
        }
        entity.markDirty();
    }

    private boolean canProcess() {
        ItemStack input = this.inventory.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        ItemStack result = getPlateResult(input.getItem());
        if (result.isEmpty()) return false;

        ItemStack output = this.inventory.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        if (!ItemStack.areItemsEqual(output, result)) return false;

        return output.getCount() + result.getCount() <= output.getMaxCount();
    }

    private void processItem() {
        ItemStack input = this.inventory.get(INPUT_SLOT);
        ItemStack result = getPlateResult(input.getItem());

        if (!result.isEmpty()) {
            ItemStack output = this.inventory.get(OUTPUT_SLOT);
            if (output.isEmpty()) {
                this.inventory.set(OUTPUT_SLOT, result.copy());
            } else if (ItemStack.areItemsEqual(output, result)) {
                output.increment(result.getCount());
            }
            input.decrement(1);
        }
    }

    public static ItemStack getPlateResult(Item item) {
        if (item == ModItems.TUNGSTEN_INGOT) return new ItemStack(ModItems.TUNGSTEN_PLATE);
        if (item == ModItems.COBALT_INGOT) return new ItemStack(ModItems.COBALT_PLATE);
        if (item == ModItems.ARDITE_INGOT) return new ItemStack(ModItems.ARDITE_PLATE);
        if (item == ModItems.MANYULLYN_INGOT) return new ItemStack(ModItems.MANYULLYN_PLATE);
        if (item == ModItems.STEEL_INGOT) return new ItemStack(ModItems.STEEL_NUGGET, 9); // Or steel plate
        if (item == ModBlocks.TUNGSTEN_BLOCK.asItem()) return new ItemStack(ModItems.TUNGSTEN_PLATE, 9);
        if (item == ModBlocks.COBALT_BLOCK.asItem()) return new ItemStack(ModItems.COBALT_PLATE, 9);
        if (item == ModBlocks.ARDITE_BLOCK.asItem()) return new ItemStack(ModItems.ARDITE_PLATE, 9);
        if (item == ModBlocks.MANYULLYN_BLOCK.asItem()) return new ItemStack(ModItems.MANYULLYN_PLATE, 9);
        if (item == Items.IRON_INGOT) return new ItemStack(ModItems.TUNGSTEN_PLATE); // Fallback stamping
        return ItemStack.EMPTY;
    }

    private int getProcessingSpeed(GearTier tier) {
        int base = 1;
        ItemStack gear = this.inventory.get(GEAR_SLOT);
        boolean enchanted = gear.getItem() instanceof GearItem g && g.isEnchanted();

        int bonus = switch (tier) {
            case IRON -> 1;
            case COPPER -> 2;
            case BRONZE -> 3;
            case STEEL -> 4;
            case GOLD -> 5;
            case TITANIUM -> 7;
            case DIAMOND -> 9;
            case NETHERITE -> 14;
            default -> 0;
        };

        if (enchanted) bonus *= 2;
        return base + bonus;
    }

    public GearTier getActiveGearTier() {
        ItemStack gear = this.inventory.get(GEAR_SLOT);
        if (gear.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", 100);
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
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : this.inventory) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack res = Inventories.splitStack(this.inventory, slot, amount);
        if (!res.isEmpty()) markDirty();
        return res;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack res = Inventories.removeStack(this.inventory, slot);
        if (!res.isEmpty()) markDirty();
        return res;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) stack.setCount(getMaxCountPerStack());
        markDirty();
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
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{INPUT_SLOT};
        return new int[]{INPUT_SLOT, GEAR_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == OUTPUT_SLOT) return false;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.hydraulic_press");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new HydraulicPressScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public SimpleEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }
}
