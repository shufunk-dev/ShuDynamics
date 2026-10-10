package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.HydraulicPressBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.HydraulicPressScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class HydraulicPressBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 500, 500, 0);

    private @Nullable BlockPos boundNetworkPos = null;
    private String boundDimension = "minecraft:overworld";

    private static final int INPUT_SLOT = 0;
    private static final int GEAR_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    private int cookTime = 0;
    private int totalCookTime = 100;

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
                case 0 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0xFFFF0000) | (value & 0xFFFF));
                case 1 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
                case 4 -> cookTime = value;
                case 5 -> totalCookTime = value;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public HydraulicPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_PRESS_BLOCK_ENTITY, pos, state);
    }

    private int externalOperationTicks = 0;
    private boolean isExternalProcess = false;

    public boolean isExternalProcess() {
        return this.isExternalProcess;
    }

    public void triggerExternalOperation(int ticks) {
        this.externalOperationTicks = Math.max(this.externalOperationTicks, ticks);
    }

    public void setExternalProcess(@Nullable ItemStack input, int progressTicks, int maxTicks) {
        this.externalOperationTicks = 6;
        this.isExternalProcess = true;
        if (input != null && !input.isEmpty()) {
            if (this.inventory.get(INPUT_SLOT).isEmpty() || !this.inventory.get(INPUT_SLOT).is(input.getItem())) {
                this.inventory.set(INPUT_SLOT, input.copy());
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
        this.inventory.set(INPUT_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    public static void tick(net.minecraft.world.level.Level world, BlockPos pos, BlockState state, HydraulicPressBlockEntity entity) {
        if (world.isClientSide()) return;

        boolean isCooking = false;
        GearTier gearTier = entity.getActiveGearTier();

        if (state.getValue(HydraulicPressBlock.GEAR_TIER) != gearTier) {
            world.setBlock(pos, state.setValue(HydraulicPressBlock.GEAR_TIER, gearTier), 3);
        }

        if (entity.isExternalProcess) {
            if (entity.externalOperationTicks > 0) {
                entity.externalOperationTicks--;
                isCooking = true;
            } else {
                entity.clearExternalProcess();
            }
        } else if (entity.externalOperationTicks > 0) {
            entity.externalOperationTicks--;
            isCooking = true;
        }

        if (!entity.isExternalProcess) {
            if (entity.canProcess()) {
                if (entity.energyStorage.getEnergy() >= ENERGY_DRAW) {
                    entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
                    entity.cookTime += entity.getProcessingSpeed(gearTier);
                    isCooking = true;

                    if (entity.cookTime >= entity.totalCookTime) {
                        entity.cookTime = 0;
                        entity.processItem();
                        world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 0.7f);
                    }
                }
            } else if (entity.externalOperationTicks <= 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
            }
        }

        if (state.getValue(HydraulicPressBlock.LIT) != isCooking) {
            world.setBlock(pos, state.setValue(HydraulicPressBlock.LIT, isCooking), 3);
        }
        entity.setChanged();
    }

    private boolean canProcess() {
        ItemStack input = this.inventory.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        ItemStack result = getPlateResult(input.getItem());
        if (result.isEmpty()) return false;

        ItemStack output = this.inventory.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItem(output, result)) return false;

        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void processItem() {
        ItemStack input = this.inventory.get(INPUT_SLOT);
        ItemStack result = getPlateResult(input.getItem());

        if (!result.isEmpty()) {
            ItemStack output = this.inventory.get(OUTPUT_SLOT);
            if (output.isEmpty()) {
                this.inventory.set(OUTPUT_SLOT, result.copy());
            } else if (ItemStack.isSameItem(output, result)) {
                output.grow(result.getCount());
            }
            input.shrink(1);
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
        if (item == ModItems.SILICON) return new ItemStack(ModItems.SILICON_WAFER, 2);
        if (item == Items.IRON_INGOT) return new ItemStack(ModItems.TUNGSTEN_PLATE); // Fallback stamping
        return ItemStack.EMPTY;
    }

    public int getProcessingSpeed(GearTier tier) {
        int base = 1;
        ItemStack gear = this.inventory.get(GEAR_SLOT);
        boolean enchanted = gear.getItem() instanceof GearItem g && g.isEnchanted();

        int bonus = switch (tier) {
            case IRON -> 1;
            case ENCHANTED_IRON -> 2;
            case COPPER -> 2;
            case BRONZE -> 3;
            case ALUMINUM -> 3;
            case STEEL -> 4;
            case GOLD -> 5;
            case TITANIUM -> 7;
            case DIAMOND -> 9;
            case NETHERITE -> 14;
            case BLAZE_OVERCLOCK -> 19;
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
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 100);
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

    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : this.inventory) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack res = ContainerHelper.removeItem(this.inventory, slot, amount);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack res = ContainerHelper.takeItem(this.inventory, slot);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{INPUT_SLOT};
        return new int[]{INPUT_SLOT, GEAR_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == OUTPUT_SLOT) return false;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.hydraulic_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new HydraulicPressScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public SimpleEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }
}
