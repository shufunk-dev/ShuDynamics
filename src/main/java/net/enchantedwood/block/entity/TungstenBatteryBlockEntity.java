package net.enchantedwood.block.entity;

import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.TungstenBatteryScreenHandler;
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

public class TungstenBatteryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int CAPACITY = 100_000_000;
    public static final int MAX_TRANSFER = 25_000; // 25,000 FE/t

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
                    int low = current & 0x0000FFFF;
                    energyStorage.setEnergy(low | ((value & 0xFFFF) << 16));
                }
            }
        }

        @Override
        public int size() {
            return 5;
        }
    };

    public TungstenBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUNGSTEN_BATTERY_BE, pos, state);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.tungsten_battery");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TungstenBatteryScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, TungstenBatteryBlockEntity entity) {
        boolean dirty = false;

        // Discharge Slot 0: Item -> Battery
        ItemStack dischargeStack = entity.inventory.get(DISCHARGE_SLOT);
        if (!dischargeStack.isEmpty()) {
            EnergyStorage itemStorage = null;
            if (dischargeStack.getItem() instanceof ItemEnergyProvider itemProvider) {
                itemStorage = itemProvider.getEnergyStorage(dischargeStack);
            } else if (dischargeStack.getItem() instanceof EnergyProvider provider) {
                itemStorage = provider.getEnergyStorage(null);
            }

            if (itemStorage != null && itemStorage.canExtract() && itemStorage.getEnergy() > 0 && entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                int needed = entity.energyStorage.getMaxEnergy() - entity.energyStorage.getEnergy();
                int toExtract = Math.min(needed, MAX_TRANSFER);
                int extracted = itemStorage.extractEnergy(toExtract, false);
                if (extracted > 0) {
                    entity.energyStorage.insertEnergy(extracted, false);
                    dirty = true;
                }
            }
        }

        // Charge Slot 1: Battery -> Item
        ItemStack chargeStack = entity.inventory.get(CHARGE_SLOT);
        if (!chargeStack.isEmpty()) {
            EnergyStorage itemStorage = null;
            if (chargeStack.getItem() instanceof ItemEnergyProvider itemProvider) {
                itemStorage = itemProvider.getEnergyStorage(chargeStack);
            } else if (chargeStack.getItem() instanceof EnergyProvider provider) {
                itemStorage = provider.getEnergyStorage(null);
            }

            if (itemStorage != null && itemStorage.canInsert() && itemStorage.getEnergy() < itemStorage.getMaxEnergy() && entity.energyStorage.getEnergy() > 0) {
                int needed = itemStorage.getMaxEnergy() - itemStorage.getEnergy();
                int toSend = Math.min(needed, Math.min(entity.energyStorage.getEnergy(), MAX_TRANSFER));
                int extracted = entity.energyStorage.extractEnergy(toSend, false);
                if (extracted > 0) {
                    itemStorage.insertEnergy(extracted, false);
                    dirty = true;
                }
            }
        }

        // Push energy to adjacent machines and consumers
        if (entity.energyStorage.getEnergy() > 0) {
            int available = Math.min(entity.energyStorage.getEnergy(), MAX_TRANSFER);
            for (Direction dir : Direction.values()) {
                if (available <= 0) break;
                BlockEntity targetBe = world.getBlockEntity(pos.offset(dir));
                if (targetBe instanceof EnergyProvider provider &&
                        !(targetBe instanceof CopperBatteryBlockEntity) &&
                        !(targetBe instanceof AluminumBatteryBlockEntity) &&
                        !(targetBe instanceof SteelBatteryBlockEntity) &&
                        !(targetBe instanceof TungstenBatteryBlockEntity) &&
                        !(targetBe instanceof CopperGeneratorBlockEntity) &&
                        !(targetBe instanceof AluminumGeneratorBlockEntity) &&
                        !(targetBe instanceof SteelGeneratorBlockEntity) &&
                        !(targetBe instanceof GeothermalGeneratorBlockEntity)) {

                    // If connected to a cable, only output if the cable is currently empty (to avoid immediate ping-pong loop)
                    if (targetBe instanceof TungstenCableBlockEntity cableBe) {
                        EnergyStorage cableStorage = cableBe.getEnergyStorage(dir.getOpposite());
                        if (cableStorage != null && cableStorage.getEnergy() == 0) {
                            int inserted = cableStorage.insertEnergy(available, false);
                            if (inserted > 0) {
                                entity.energyStorage.extractEnergy(inserted, false);
                                available -= inserted;
                                dirty = true;
                            }
                        }
                    } else {
                        EnergyStorage targetStorage = provider.getEnergyStorage(dir.getOpposite());
                        if (targetStorage != null && targetStorage.canInsert()) {
                            int inserted = targetStorage.insertEnergy(available, false);
                            if (inserted > 0) {
                                entity.energyStorage.extractEnergy(inserted, false);
                                available -= inserted;
                                dirty = true;
                            }
                        }
                    }
                }
            }
        }

        if (dirty) {
            entity.markDirty();
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
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction dir) {
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
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
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
