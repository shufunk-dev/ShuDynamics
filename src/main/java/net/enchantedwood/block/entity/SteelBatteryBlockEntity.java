package net.enchantedwood.block.entity;

import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.SteelBatteryScreenHandler;
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
import org.jetbrains.annotations.Nullable;

public class SteelBatteryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int CAPACITY = 50_000_000;
    public static final int MAX_TRANSFER = 12_500; // 12,500 FE/t

    public static final int INVENTORY_SIZE = 2;
    public static final int DISCHARGE_SLOT = 0;
    public static final int CHARGE_SLOT = 1;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER, 0);

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> MAX_TRANSFER;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> {
                    int current = energyStorage.getEnergy();
                    int high = current & 0xFFFF0000;
                    energyStorage.setEnergy(high | (value & 0xFFFF));
                }
                case 1 -> {
                    int current = energyStorage.getEnergy();
                    int low = current & 0xFFFF;
                    energyStorage.setEnergy(low | ((value & 0xFFFF) << 16));
                }
            }
        }

        @Override
        public int size() {
            return 5;
        }
    };

    public SteelBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_BATTERY_BLOCK_ENTITY, pos, state);
    }

    public DefaultedList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.steel_battery");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SteelBatteryScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, SteelBatteryBlockEntity entity) {
        boolean stateChanged = false;

        // 1. Discharge Slot (Slot 0): transfer energy from item into battery block
        ItemStack dischargeStack = entity.inventory.get(DISCHARGE_SLOT);
        if (!dischargeStack.isEmpty()) {
            EnergyStorage itemStorage = null;
            if (dischargeStack.getItem() instanceof ItemEnergyProvider itemProvider) {
                itemStorage = itemProvider.getEnergyStorage(dischargeStack);
            } else if (dischargeStack.getItem() instanceof EnergyProvider provider) {
                itemStorage = provider.getEnergyStorage(null);
            }

            if (itemStorage != null && itemStorage.getEnergy() > 0 && entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                int needed = entity.energyStorage.getMaxEnergy() - entity.energyStorage.getEnergy();
                int maxTransfer = Math.min(needed, MAX_TRANSFER);
                int extracted = itemStorage.extractEnergy(maxTransfer, false);
                if (extracted > 0) {
                    entity.energyStorage.insertEnergy(extracted, false);
                    stateChanged = true;
                }
            }
        }

        // 2. Charge Slot (Slot 1): transfer energy from battery block into item
        ItemStack chargeStack = entity.inventory.get(CHARGE_SLOT);
        if (!chargeStack.isEmpty()) {
            EnergyStorage itemStorage = null;
            if (chargeStack.getItem() instanceof ItemEnergyProvider itemProvider) {
                itemStorage = itemProvider.getEnergyStorage(chargeStack);
            } else if (chargeStack.getItem() instanceof EnergyProvider provider) {
                itemStorage = provider.getEnergyStorage(null);
            }

            if (itemStorage != null && itemStorage.getEnergy() < itemStorage.getMaxEnergy() && entity.energyStorage.getEnergy() > 0) {
                int needed = itemStorage.getMaxEnergy() - itemStorage.getEnergy();
                int maxTransfer = Math.min(needed, Math.min(entity.energyStorage.getEnergy(), MAX_TRANSFER));
                int extracted = entity.energyStorage.extractEnergy(maxTransfer, false);
                if (extracted > 0) {
                    itemStorage.insertEnergy(extracted, false);
                    stateChanged = true;
                }
            }
        }

        // 3. Cable / grid output
        if (entity.energyStorage.getEnergy() > 0) {
            int availableToOutput = Math.min(entity.energyStorage.getEnergy(), MAX_TRANSFER);

            for (Direction dir : Direction.values()) {
                if (availableToOutput <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                // Do not output to other batteries or generators
                if (neighbor instanceof EnergyProvider provider &&
                        !(neighbor instanceof CopperBatteryBlockEntity) &&
                        !(neighbor instanceof AluminumBatteryBlockEntity) &&
                        !(neighbor instanceof SteelBatteryBlockEntity) &&
                        !(neighbor instanceof CopperGeneratorBlockEntity) &&
                        !(neighbor instanceof AluminumGeneratorBlockEntity) &&
                        !(neighbor instanceof SteelGeneratorBlockEntity)) {
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

        if (stateChanged) {
            markDirty(world, pos, state);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{DISCHARGE_SLOT, CHARGE_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
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
        for (ItemStack s : inventory) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(inventory, slot);
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
