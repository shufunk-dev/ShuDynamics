package net.enchantedwood.block.entity;

import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.SteelBatteryScreenHandler;
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

public class SteelBatteryBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 50_000_000;
    public static final int MAX_TRANSFER = 12_500; // 12,500 FE/t

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
                    int low = current & 0xFFFF;
                    energyStorage.setEnergy(low | ((value & 0xFFFF) << 16));
                }
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public SteelBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_BATTERY_BLOCK_ENTITY, pos, state);
    }

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.steel_battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new SteelBatteryScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, SteelBatteryBlockEntity entity) {
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
                int maxTransfer = Math.min(needed, Math.max(MAX_TRANSFER, itemStorage.getTransferRate()));
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

        // 3. Direct output to adjacent machine consumers (never cables/batteries/generators)
        if (entity.energyStorage.getEnergy() > 0) {
            int availableToOutput = Math.min(entity.energyStorage.getEnergy(), MAX_TRANSFER);

            for (Direction dir : Direction.values()) {
                if (availableToOutput <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
                // Do not output to cables, other batteries, or generators
                if (neighbor instanceof EnergyProvider provider &&
                        !(neighbor instanceof BaseCableBlockEntity) &&
                        !(neighbor instanceof CopperBatteryBlockEntity) &&
                        !(neighbor instanceof AluminumBatteryBlockEntity) &&
                        !(neighbor instanceof SteelBatteryBlockEntity) &&
                        !(neighbor instanceof TungstenBatteryBlockEntity) &&
                        !(neighbor instanceof CopperGeneratorBlockEntity) &&
                        !(neighbor instanceof AluminumGeneratorBlockEntity) &&
                        !(neighbor instanceof SteelGeneratorBlockEntity) &&
                        !(neighbor instanceof GeothermalGeneratorBlockEntity) &&
                        !(neighbor instanceof EnchantedLavaGeneratorBlockEntity)) {
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
            setChanged(world, pos, state);
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
        return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
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
        for (ItemStack s : inventory) {
            if (!s.isEmpty()) return false;
        }
        return true;
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
}
