package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.screen.SteelBatteryScreenHandler;
import org.jetbrains.annotations.Nullable;

public class SteelBatteryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, EnergyProvider {
    public static final int CAPACITY = 50_000_000;
    public static final int MAX_TRANSFER = 12_500; // 12,500 FE/t

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
        }

        @Override
        public int size() {
            return 5;
        }
    };

    public SteelBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_BATTERY_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.steel_battery");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SteelBatteryScreenHandler(syncId, playerInventory, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, SteelBatteryBlockEntity entity) {
        if (entity.energyStorage.getEnergy() <= 0) return;

        boolean stateChanged = false;
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

        if (stateChanged) {
            markDirty(world, pos, state);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.energyStorage.readData(view);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        this.energyStorage.writeData(view);
    }
}
