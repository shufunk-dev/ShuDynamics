package net.enchantedwood.block.entity;

import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCableBlockEntity extends BlockEntity implements EnergyProvider {
    protected final int transferRate;
    protected final SimpleEnergyStorage energyStorage;

    public BaseCableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int transferRate) {
        super(type, pos, state);
        this.transferRate = transferRate;
        this.energyStorage = new SimpleEnergyStorage(transferRate, transferRate, transferRate, 0);
    }

    public int getTransferRate() {
        return this.transferRate;
    }

    public static boolean isGenerator(BlockEntity be) {
        return be instanceof GeothermalGeneratorBlockEntity ||
                be instanceof EnchantedLavaGeneratorBlockEntity ||
                be instanceof SteelGeneratorBlockEntity ||
                be instanceof AluminumGeneratorBlockEntity ||
                be instanceof CopperGeneratorBlockEntity;
    }

    public static boolean isBattery(BlockEntity be) {
        return be instanceof TungstenBatteryBlockEntity ||
                be instanceof SteelBatteryBlockEntity ||
                be instanceof AluminumBatteryBlockEntity ||
                be instanceof CopperBatteryBlockEntity;
    }

    public static boolean isCable(BlockEntity be) {
        return be instanceof BaseCableBlockEntity;
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, BaseCableBlockEntity entity) {
        boolean dirty = false;

        List<EnergyStorage> generatorSources = new ArrayList<>();
        List<EnergyStorage> machineConsumers = new ArrayList<>();
        List<EnergyStorage> batteryConsumers = new ArrayList<>();
        List<EnergyStorage> cableNeighbors = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
            if (neighbor instanceof EnergyProvider provider && neighbor != entity) {
                EnergyStorage storage = provider.getEnergyStorage(dir.getOpposite());
                if (storage == null) continue;

                if (isGenerator(neighbor)) {
                    if (storage.canExtract() && storage.getEnergy() > 0) {
                        generatorSources.add(storage);
                    }
                } else if (isBattery(neighbor)) {
                    if (storage.canInsert() && storage.getEnergy() < storage.getMaxEnergy()) {
                        batteryConsumers.add(storage);
                    }
                } else if (isCable(neighbor)) {
                    cableNeighbors.add(storage);
                } else {
                    // Machine consumer (Industrial Centrifuge, Chemical Synthesizer, Crusher, Smelter, Refiner, etc.)
                    if (storage.canInsert() && storage.getEnergy() < storage.getMaxEnergy()) {
                        machineConsumers.add(storage);
                    }
                }
            }
        }

        // 1. Pull from Generators if cable has space
        int space = entity.transferRate - entity.energyStorage.getEnergy();
        if (space > 0 && !generatorSources.isEmpty()) {
            for (EnergyStorage gen : generatorSources) {
                if (space <= 0) break;
                int extracted = gen.extractEnergy(space, false);
                if (extracted > 0) {
                    entity.energyStorage.insertEnergy(extracted, false);
                    space -= extracted;
                    dirty = true;
                }
            }
        }

        int available = entity.energyStorage.getEnergy();

        // Priority 1: Machine consumers (Centrifuge, Synthesizer, Crusher, Refiner, etc.)
        if (available > 0 && !machineConsumers.isEmpty()) {
            int share = Math.max(1, available / machineConsumers.size());
            for (EnergyStorage machine : machineConsumers) {
                if (available <= 0) break;
                int toSend = Math.min(available, share);
                int inserted = machine.insertEnergy(toSend, false);
                if (inserted > 0) {
                    entity.energyStorage.extractEnergy(inserted, false);
                    available -= inserted;
                    dirty = true;
                }
            }
        }

        // Priority 2: Cable Network (equalize forward with neighbor cables that have less energy)
        if (available > 0 && !cableNeighbors.isEmpty()) {
            for (EnergyStorage otherCable : cableNeighbors) {
                if (available <= 0) break;
                int otherEnergy = otherCable.getEnergy();
                int currentEnergy = entity.energyStorage.getEnergy();
                if (currentEnergy > otherEnergy) {
                    int diff = currentEnergy - otherEnergy;
                    int toSend = Math.min(available, (diff + 1) / 2);
                    if (toSend > 0) {
                        int inserted = otherCable.insertEnergy(toSend, false);
                        if (inserted > 0) {
                            entity.energyStorage.extractEnergy(inserted, false);
                            available -= inserted;
                            dirty = true;
                        }
                    }
                }
            }
        }

        // Priority 3: Surplus Power -> Charge Batteries!
        if (available > 0 && !batteryConsumers.isEmpty()) {
            int share = Math.max(1, available / batteryConsumers.size());
            for (EnergyStorage bat : batteryConsumers) {
                if (available <= 0) break;
                int toSend = Math.min(available, share);
                int inserted = bat.insertEnergy(toSend, false);
                if (inserted > 0) {
                    entity.energyStorage.extractEnergy(inserted, false);
                    available -= inserted;
                    dirty = true;
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
        this.energyStorage.readData(view);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        this.energyStorage.writeData(view);
    }
}
