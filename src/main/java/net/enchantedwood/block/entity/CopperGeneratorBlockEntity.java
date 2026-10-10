package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.CopperGeneratorBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.CopperGeneratorScreenHandler;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class CopperGeneratorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int BUFFER_CAPACITY = 100_000;
    public static final int GENERATION_RATE = 60; // FE/t
    public static final int MAX_OUTPUT = 500;     // FE/t

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(BUFFER_CAPACITY, GENERATION_RATE, MAX_OUTPUT, 0);

    private int burnTime = 0;
    private int totalBurnTime = 0;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> burnTime;
                case 5 -> totalBurnTime;
                case 6 -> GENERATION_RATE;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 4 -> burnTime = value;
                case 5 -> totalBurnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public CopperGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPER_GENERATOR_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.copper_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CopperGeneratorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public static int getFuelTime(ItemStack stack) {
        return getFuelTime(null, stack);
    }

    public static int getFuelTime(@Nullable Level world, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (world != null) {
            int ticks = getFuelBurnTime(stack);
            if (ticks > 0) return ticks;
        }
        Item item = stack.getItem();
        if (item == ModItems.ENCHANTED_DUST) return 8000;
        if (item == ModItems.ENCHANTED_COAL) return 10000;
        if (item == ModBlocks.ENCHANTED_COAL_BLOCK.asItem()) return 90000;
        if (item == ModItems.COKE_COAL) return 3200;
        if (item == ModBlocks.COKE_COAL_BLOCK.asItem()) return 28800;
        if (item == Items.COAL_BLOCK) return 16000;
        if (item == Items.COAL || item == Items.CHARCOAL) return 1600;
        if (item == Items.LAVA_BUCKET || item == ModItems.COPPER_LAVA_BUCKET) return 20000;
        if (item == ModItems.ENCHANTED_LAVA_BUCKET || item == ModItems.ENCHANTED_COPPER_LAVA_BUCKET) return 60000;
        if (item == Items.BLAZE_ROD) return 2400;
        if (item == Items.WOODEN_PICKAXE || item == Items.WOODEN_AXE || item == Items.WOODEN_SHOVEL || item == Items.WOODEN_HOE || item == Items.WOODEN_SWORD) return 200;
        if (item == Items.STICK) return 100;
        return 0;
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, CopperGeneratorBlockEntity entity) {
        boolean originallyBurning = entity.burnTime > 0;
        boolean stateChanged = false;

        // 1. Generation from fuel
        if (entity.burnTime > 0) {
            entity.burnTime--;
            if (entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                entity.energyStorage.insertEnergy(GENERATION_RATE, false);
                stateChanged = true;
            }
        }

        // 2. Start burning new fuel if buffer has space
        if (entity.burnTime <= 0 && entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
            ItemStack fuelStack = entity.inventory.get(0);
            if (!fuelStack.isEmpty()) {
                int fuelValue = getFuelTime(world, fuelStack);
                if (fuelValue > 0) {
                    entity.burnTime = fuelValue;
                    entity.totalBurnTime = fuelValue;
                    ItemStack remainder = getItemRemainder(fuelStack);
                    fuelStack.shrink(1);
                    if (fuelStack.isEmpty() && !remainder.isEmpty()) {
                        entity.inventory.set(0, remainder.copy());
                    }
                    stateChanged = true;
                }
            }
        }

        // 3. Push energy to adjacent EnergyProviders
        if (entity.energyStorage.getEnergy() > 0) {
            int availableToOutput = Math.min(entity.energyStorage.getEnergy(), MAX_OUTPUT);
            for (Direction dir : Direction.values()) {
                if (availableToOutput <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
                if (neighbor instanceof EnergyProvider provider) {
                    EnergyStorage receiver = provider.getEnergyStorage(dir.getOpposite());
                    if (receiver != null && receiver.canInsert()) {
                        int inserted = receiver.insertEnergy(availableToOutput, false);
                        if (inserted > 0) {
                            entity.energyStorage.extractEnergy(inserted, false);
                            availableToOutput -= inserted;
                            stateChanged = true;
                        }
                    }
                }
            }
        }

        // 4. Update block LIT state
        boolean isBurningNow = entity.burnTime > 0;
        if (originallyBurning != isBurningNow) {
            state = state.setValue(CopperGeneratorBlock.LIT, isBurningNow);
            world.setBlock(pos, state, 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(world, pos, state);
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.burnTime = view.getIntOr("BurnTime", 0);
        this.totalBurnTime = view.getIntOr("TotalBurnTime", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("TotalBurnTime", this.totalBurnTime);
    }

    // SidedInventory Implementation
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return getFuelTime(this.getLevel(), stack) > 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(inventory, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
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

    public static int getFuelBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_DUST) return 8000;
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_COAL) return 10000;
        if (item == net.enchantedwood.block.ModBlocks.ENCHANTED_COAL_BLOCK.asItem()) return 90000;
        if (item == net.enchantedwood.item.ModItems.COKE_COAL) return 3200;
        if (item == net.enchantedwood.block.ModBlocks.COKE_COAL_BLOCK.asItem()) return 28800;
        if (item == net.enchantedwood.item.ModItems.COPPER_LAVA_BUCKET) return 20000;
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_LAVA_BUCKET || item == net.enchantedwood.item.ModItems.ENCHANTED_COPPER_LAVA_BUCKET) return 60000;
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) return 20000;
        if (item == net.minecraft.world.item.Items.COAL || item == net.minecraft.world.item.Items.CHARCOAL) return 1600;
        if (item == net.minecraft.world.item.Items.COAL_BLOCK) return 16000;
        if (item == net.minecraft.world.item.Items.BLAZE_ROD) return 2400;
        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(
                stack,
                net.minecraft.core.component.DataComponents.COOKING_FUEL,
                net.minecraft.world.item.component.CookingFuel::burnTime,
                null,
                0
        );
    }

    public static ItemStack getItemRemainder(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.COPPER_LAVA_BUCKET)) return new ItemStack(net.enchantedwood.item.ModItems.COPPER_BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.ENCHANTED_LAVA_BUCKET)) return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.ENCHANTED_COPPER_LAVA_BUCKET)) return new ItemStack(net.enchantedwood.item.ModItems.COPPER_BUCKET);
        var rem = stack.getItem().getCraftingRemainder();
        return rem != null ? rem.create() : ItemStack.EMPTY;
    }

}