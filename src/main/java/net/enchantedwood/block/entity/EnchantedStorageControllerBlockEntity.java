package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
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
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.EnchantedStorageControllerScreenHandler;
import org.jetbrains.annotations.Nullable;

public class EnchantedStorageControllerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int POWER_DRAW_PER_TICK = 10; // 10 FE/t (200 FE/s)

    // Slot 0: Emergency Fuel, Slot 1: Chunk Loader Module, Slot 2: Interdimensional Card
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 1_000, 0, 0);
    private int burnTime = 0;
    private int totalBurnTime = 0;
    private boolean isChunkForceLoaded = false;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
            return 10;
        }
    };

    public EnchantedStorageControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_STORAGE_CONTROLLER_BLOCK_ENTITY, pos, state);
    }

    public boolean hasChunkLoader() {
        return inventory.get(1).isOf(ModItems.CHUNK_LOADER_MODULE) || hasInterdimensionalCard();
    }

    public boolean hasInterdimensionalCard() {
        return inventory.get(2).isOf(ModItems.INTERDIMENSIONAL_CARD);
    }

    public boolean isOnline() {
        return this.energyStorage.getEnergy() >= POWER_DRAW_PER_TICK || this.burnTime > 0;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.enchanted_storage_controller");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnchantedStorageControllerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    @Nullable
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public void markRemoved() {
        if (this.isChunkForceLoaded && this.world instanceof ServerWorld sw) {
            int centerCx = pos.getX() >> 4;
            int centerCz = pos.getZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    sw.setChunkForced(centerCx + dx, centerCz + dz, false);
                }
            }
            this.isChunkForceLoaded = false;
        }
        super.markRemoved();
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, EnchantedStorageControllerBlockEntity entity) {
        boolean wasOnline = state.get(EnchantedStorageControllerBlock.LIT);
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
            }

            ItemStack fuelStack = entity.inventory.get(0);
            if (entity.burnTime <= 0 && !fuelStack.isEmpty()) {
                if (fuelStack.isOf(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())) {
                    entity.burnTime = 90000;
                    entity.totalBurnTime = 90000;
                    fuelStack.decrement(1);
                    isOnline = true;
                    stateChanged = true;
                } else if (fuelStack.isOf(ModItems.ENCHANTED_LAVA_BUCKET)) {
                    entity.burnTime = 60000;
                    entity.totalBurnTime = 60000;
                    entity.inventory.set(0, new ItemStack(net.minecraft.item.Items.BUCKET));
                    isOnline = true;
                    stateChanged = true;
                } else if (fuelStack.isOf(ModItems.ENCHANTED_COPPER_LAVA_BUCKET)) {
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
            state = state.with(EnchantedStorageControllerBlock.LIT, isOnline);
            world.setBlockState(pos, state, 3);
            stateChanged = true;
        }

        if (stateChanged) {
            markDirty(world, pos, state);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
        this.burnTime = view.getOptionalInt("BurnTime").orElse(0);
        this.totalBurnTime = view.getOptionalInt("TotalBurnTime").orElse(0);
        int storedEnergy = view.getOptionalInt("Energy").orElse(0);
        this.energyStorage.setEnergy(storedEnergy);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("TotalBurnTime", this.totalBurnTime);
        view.putInt("Energy", this.energyStorage.getEnergy());
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0, 1, 2};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) {
            return stack.isOf(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())
                    || stack.isOf(ModItems.ENCHANTED_LAVA_BUCKET)
                    || stack.isOf(ModItems.ENCHANTED_COPPER_LAVA_BUCKET);
        }
        if (slot == 1) return stack.isOf(ModItems.CHUNK_LOADER_MODULE);
        if (slot == 2) return stack.isOf(ModItems.INTERDIMENSIONAL_CARD);
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
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
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(inventory, slot);
        markDirty();
        return result;
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
