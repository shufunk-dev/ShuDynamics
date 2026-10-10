package net.enchantedwood.block.entity;

import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.TungstenBatteryScreenHandler;
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

public class TungstenBatteryBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 100_000_000;
    public static final int MAX_TRANSFER = 25_000; // 25,000 FE/t

    public static final int INVENTORY_SIZE = 2;
    public static final int DISCHARGE_SLOT = 0;
    public static final int CHARGE_SLOT = 1;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER, 0);

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
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
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.tungsten_battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new TungstenBatteryScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, TungstenBatteryBlockEntity entity) {
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
                int toExtract = Math.min(needed, Math.max(MAX_TRANSFER, itemStorage.getTransferRate()));
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

        // Push energy out directly to adjacent consumers (machines only, never cables/batteries/generators)
        if (entity.energyStorage.getEnergy() > 0) {
            int available = Math.min(entity.energyStorage.getEnergy(), MAX_TRANSFER);
            for (Direction dir : Direction.values()) {
                if (available <= 0) break;
                BlockEntity targetBe = world.getBlockEntity(pos.relative(dir));
                if (targetBe instanceof EnergyProvider provider &&
                        !(targetBe instanceof BaseCableBlockEntity) &&
                        !(targetBe instanceof CopperBatteryBlockEntity) &&
                        !(targetBe instanceof AluminumBatteryBlockEntity) &&
                        !(targetBe instanceof SteelBatteryBlockEntity) &&
                        !(targetBe instanceof TungstenBatteryBlockEntity) &&
                        !(targetBe instanceof CopperGeneratorBlockEntity) &&
                        !(targetBe instanceof AluminumGeneratorBlockEntity) &&
                        !(targetBe instanceof SteelGeneratorBlockEntity) &&
                        !(targetBe instanceof GeothermalGeneratorBlockEntity) &&
                        !(targetBe instanceof EnchantedLavaGeneratorBlockEntity)) {

                    EnergyStorage targetStorage = provider.getEnergyStorage(dir.getOpposite());
                    if (targetStorage != null && targetStorage.canInsert() && targetStorage.getEnergy() < targetStorage.getMaxEnergy()) {
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

        if (dirty) {
            entity.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{DISCHARGE_SLOT, CHARGE_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
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
        return ContainerHelper.removeItem(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
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
