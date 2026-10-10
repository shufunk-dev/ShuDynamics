package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.EnchantedStorageControllerScreenHandler;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class EnchantedStorageControllerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int POWER_DRAW_PER_TICK = 10; // 10 FE/t (200 FE/s)

    // Slot 0: Emergency Fuel, Slot 1: Chunk Loader Module, Slot 2: Interdimensional Card
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 1_000, 5_000, 0);
    private int burnTime = 0;
    private int totalBurnTime = 0;
    private boolean isChunkForceLoaded = false;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime & 0xFFFF;
                case 1 -> (burnTime >> 16) & 0xFFFF;
                case 2 -> totalBurnTime & 0xFFFF;
                case 3 -> (totalBurnTime >> 16) & 0xFFFF;
                case 4 -> energyStorage.getEnergy() & 0xFFFF;
                case 5 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 6 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 7 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 8 -> hasChunkLoader() ? 1 : 0;
                case 9 -> hasInterdimensionalCard() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = (burnTime & 0xFFFF0000) | (value & 0xFFFF);
                case 1 -> burnTime = (burnTime & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                case 2 -> totalBurnTime = (totalBurnTime & 0xFFFF0000) | (value & 0xFFFF);
                case 3 -> totalBurnTime = (totalBurnTime & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                case 4 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0xFFFF0000) | (value & 0xFFFF));
                case 5 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public EnchantedStorageControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_STORAGE_CONTROLLER_BLOCK_ENTITY, pos, state);
    }

    public boolean hasChunkLoader() {
        return inventory.get(1).is(ModItems.CHUNK_LOADER_MODULE) || hasInterdimensionalCard();
    }

    public boolean hasInterdimensionalCard() {
        return inventory.get(2).is(ModItems.INTERDIMENSIONAL_CARD);
    }

    public boolean isOnline() {
        return this.energyStorage.getEnergy() >= POWER_DRAW_PER_TICK || this.burnTime > 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.enchanted_storage_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new EnchantedStorageControllerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    @Nullable
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public void setRemoved() {
        if (this.isChunkForceLoaded && this.level instanceof ServerLevel sw) {
            int centerCx = worldPosition.getX() >> 4;
            int centerCz = worldPosition.getZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    sw.setChunkForced(centerCx + dx, centerCz + dz, false);
                }
            }
            this.isChunkForceLoaded = false;
        }
        super.setRemoved();
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, EnchantedStorageControllerBlockEntity entity) {
        boolean wasOnline = state.getValue(EnchantedStorageControllerBlock.LIT);
        boolean stateChanged = false;

        boolean isOnline = false;

        // 1. Primary Source: Electrical Grid (Generator / Battery / Cable power)
        if (entity.energyStorage.getEnergy() >= POWER_DRAW_PER_TICK) {
            entity.energyStorage.extractEnergy(POWER_DRAW_PER_TICK, false);
            isOnline = true;
        } else {
            // 2. Backup Source: Magical Fuel (Enchanted Coal Block / Enchanted Lava Bucket)
            if (entity.burnTime > 0) {
                --entity.burnTime;
                isOnline = true;
                if (entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                    entity.energyStorage.insertEnergy(500, false);
                }
            }

            ItemStack fuelStack = entity.inventory.get(0);
            if (entity.burnTime <= 0 && !fuelStack.isEmpty()) {
                if (fuelStack.is(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())) {
                    entity.burnTime = 90000;
                    entity.totalBurnTime = 90000;
                    fuelStack.shrink(1);
                    isOnline = true;
                    stateChanged = true;
                } else if (fuelStack.is(ModItems.ENCHANTED_LAVA_BUCKET)) {
                    entity.burnTime = 60000;
                    entity.totalBurnTime = 60000;
                    entity.inventory.set(0, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                    isOnline = true;
                    stateChanged = true;
                } else if (fuelStack.is(ModItems.ENCHANTED_COPPER_LAVA_BUCKET)) {
                    entity.burnTime = 60000;
                    entity.totalBurnTime = 60000;
                    entity.inventory.set(0, new ItemStack(ModItems.COPPER_BUCKET));
                    isOnline = true;
                    stateChanged = true;
                }
            }
        }

        // Handle chunk loading (force load 3x3 chunks centered on controller)
        boolean shouldLoadChunk = isOnline && entity.hasChunkLoader();
        if (shouldLoadChunk != entity.isChunkForceLoaded) {
            int centerCx = pos.getX() >> 4;
            int centerCz = pos.getZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.setChunkForced(centerCx + dx, centerCz + dz, shouldLoadChunk);
                }
            }
            entity.isChunkForceLoaded = shouldLoadChunk;
        }

        if (wasOnline != isOnline) {
            state = state.setValue(EnchantedStorageControllerBlock.LIT, isOnline);
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
        ContainerHelper.loadAllItems(view, this.inventory);
        this.burnTime = view.getInt("BurnTime").orElse(0);
        this.totalBurnTime = view.getInt("TotalBurnTime").orElse(0);
        int storedEnergy = view.getInt("Energy").orElse(0);
        this.energyStorage.setEnergy(storedEnergy);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("TotalBurnTime", this.totalBurnTime);
        view.putInt("Energy", this.energyStorage.getEnergy());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) {
            return stack.is(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())
                    || stack.is(ModItems.ENCHANTED_LAVA_BUCKET)
                    || stack.is(ModItems.ENCHANTED_COPPER_LAVA_BUCKET);
        }
        if (slot == 1) return stack.is(ModItems.CHUNK_LOADER_MODULE);
        if (slot == 2) return stack.is(ModItems.INTERDIMENSIONAL_CARD);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
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
        ItemStack result = ContainerHelper.removeItem(inventory, slot, amount);
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(inventory, slot);
        setChanged();
        return result;
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
