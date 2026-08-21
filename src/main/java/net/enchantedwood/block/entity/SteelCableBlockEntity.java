package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SteelCableBlockEntity extends BlockEntity implements EnergyProvider {
    public static final int CABLE_TRANSFER_RATE = 12_500; // 12,500 FE/t

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CABLE_TRANSFER_RATE, CABLE_TRANSFER_RATE, CABLE_TRANSFER_RATE, 0);

    public SteelCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_CABLE_BLOCK_ENTITY, pos, state);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, SteelCableBlockEntity entity) {
        if (entity.energyStorage.getEnergy() <= 0) return;

        int availableToPush = entity.energyStorage.getEnergy();

        // 1. Categorize adjacent energy receivers
        List<EnergyStorage> machineConsumers = new ArrayList<>();
        List<EnergyStorage> cableNeighbors = new ArrayList<>();
        List<EnergyStorage> batteryNeighbors = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
            if (neighbor instanceof EnergyProvider provider && neighbor != entity) {
                // Ignore generators
                if (neighbor instanceof CopperGeneratorBlockEntity ||
                        neighbor instanceof AluminumGeneratorBlockEntity ||
                        neighbor instanceof SteelGeneratorBlockEntity) {
                    continue;
                }

                EnergyStorage storage = provider.getEnergyStorage(dir.getOpposite());
                if (storage != null && storage.canInsert() && storage.getEnergy() < storage.getMaxEnergy()) {
                    if (neighbor instanceof SteelCableBlockEntity ||
                            neighbor instanceof AluminumCableBlockEntity ||
                            neighbor instanceof CopperCableBlockEntity) {
                        cableNeighbors.add(storage);
                    } else if (neighbor instanceof SteelBatteryBlockEntity ||
                            neighbor instanceof AluminumBatteryBlockEntity ||
                            neighbor instanceof CopperBatteryBlockEntity) {
                        batteryNeighbors.add(storage);
                    } else {
                        machineConsumers.add(storage);
                    }
                }
            }
        }

        // Priority 1: Push to machine consumers (Aluminum Refiner, Steel Blast Furnace, etc.)
        if (!machineConsumers.isEmpty()) {
            int share = Math.max(1, availableToPush / machineConsumers.size());
            for (EnergyStorage target : machineConsumers) {
                if (availableToPush <= 0) break;
                int toSend = Math.min(availableToPush, share);
                int inserted = target.insertEnergy(toSend, false);
                if (inserted > 0) {
                    entity.energyStorage.extractEnergy(inserted, false);
                    availableToPush -= inserted;
                }
            }
        }

        // Priority 2: Push down cable network to reach distant machines
        if (availableToPush > 0 && !cableNeighbors.isEmpty()) {
            int share = Math.max(1, availableToPush / cableNeighbors.size());
            for (EnergyStorage target : cableNeighbors) {
                if (availableToPush <= 0) break;
                int toSend = Math.min(availableToPush, share);
                int inserted = target.insertEnergy(toSend, false);
                if (inserted > 0) {
                    entity.energyStorage.extractEnergy(inserted, false);
                    availableToPush -= inserted;
                }
            }
        }

        // Priority 3: Push surplus energy into storage batteries
        if (availableToPush > 0 && !batteryNeighbors.isEmpty()) {
            int share = Math.max(1, availableToPush / batteryNeighbors.size());
            for (EnergyStorage target : batteryNeighbors) {
                if (availableToPush <= 0) break;
                int toSend = Math.min(availableToPush, share);
                int inserted = target.insertEnergy(toSend, false);
                if (inserted > 0) {
                    entity.energyStorage.extractEnergy(inserted, false);
                    availableToPush -= inserted;
                }
            }
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
