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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public abstract class BaseCableBlockEntity extends BlockEntity implements EnergyProvider {
    protected final int transferRate;
    protected final SimpleEnergyStorage energyStorage;
    protected long lastNetworkTickTime = -1;

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
        long currentTick = world.getTime();
        if (entity.lastNetworkTickTime == currentTick) {
            return;
        }

        // BFS Network Discovery across all contiguous cables
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visitedCables = new HashSet<>();
        List<BaseCableBlockEntity> networkCables = new ArrayList<>();

        Set<BlockPos> visitedEndpoints = new HashSet<>();
        List<EnergyStorage> generatorSources = new ArrayList<>();
        List<EnergyStorage> batterySources = new ArrayList<>();
        List<EnergyStorage> machineConsumers = new ArrayList<>();
        List<EnergyStorage> batteryConsumers = new ArrayList<>();

        int networkTransferRate = entity.transferRate;

        queue.add(pos);
        visitedCables.add(pos);

        while (!queue.isEmpty() && networkCables.size() < 512) {
            BlockPos currentPos = queue.poll();
            BlockEntity be = world.getBlockEntity(currentPos);
            if (!(be instanceof BaseCableBlockEntity cableBe)) continue;

            networkCables.add(cableBe);
            cableBe.lastNetworkTickTime = currentTick;
            networkTransferRate = Math.min(networkTransferRate, cableBe.getTransferRate());

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.offset(dir);
                if (visitedCables.contains(neighborPos)) continue;

                BlockEntity neighbor = world.getBlockEntity(neighborPos);
                if (neighbor instanceof BaseCableBlockEntity) {
                    visitedCables.add(neighborPos);
                    queue.add(neighborPos);
                } else if (neighbor instanceof EnergyProvider provider) {
                    if (!visitedEndpoints.add(neighborPos)) continue;
                    EnergyStorage storage = provider.getEnergyStorage(dir.getOpposite());
                    if (storage == null) continue;

                    if (isGenerator(neighbor)) {
                        if (storage.canExtract() && storage.getEnergy() > 0) {
                            generatorSources.add(storage);
                        }
                    } else if (isBattery(neighbor)) {
                        if (storage.canExtract() && storage.getEnergy() > 0) {
                            batterySources.add(storage);
                        }
                        if (storage.canInsert() && storage.getEnergy() < storage.getMaxEnergy()) {
                            batteryConsumers.add(storage);
                        }
                    } else {
                        // Machine consumer (Centrifuge, Crusher, Smelter, Synthesizer, etc.)
                        if (storage.canInsert() && storage.getEnergy() < storage.getMaxEnergy()) {
                            machineConsumers.add(storage);
                        }
                    }
                }
            }
        }

        int transferBudget = networkTransferRate;

        // Phase 1: Power Machine Consumers
        if (!machineConsumers.isEmpty()) {
            int totalDemand = 0;
            for (EnergyStorage machine : machineConsumers) {
                totalDemand += (machine.getMaxEnergy() - machine.getEnergy());
            }
            int demandTarget = Math.min(totalDemand, transferBudget);

            if (demandTarget > 0) {
                int collected = 0;

                // 1a. First take from any energy previously pushed into cables by generators
                for (BaseCableBlockEntity cable : networkCables) {
                    if (collected >= demandTarget) break;
                    int cableEnergy = cable.energyStorage.getEnergy();
                    if (cableEnergy > 0) {
                        int needed = demandTarget - collected;
                        int toTake = Math.min(cableEnergy, needed);
                        int extracted = cable.energyStorage.extractEnergy(toTake, false);
                        if (extracted > 0) {
                            collected += extracted;
                            cable.markDirty();
                        }
                    }
                }

                // 1b. Pull from Generators
                if (collected < demandTarget && !generatorSources.isEmpty()) {
                    for (EnergyStorage gen : generatorSources) {
                        if (collected >= demandTarget) break;
                        int needed = demandTarget - collected;
                        int extracted = gen.extractEnergy(needed, false);
                        collected += extracted;
                    }
                }

                // 1c. Deficit: Pull from Batteries
                if (collected < demandTarget && !batterySources.isEmpty()) {
                    for (EnergyStorage bat : batterySources) {
                        if (collected >= demandTarget) break;
                        int needed = demandTarget - collected;
                        int extracted = bat.extractEnergy(needed, false);
                        collected += extracted;
                    }
                }

                // Distribute collected energy fairly to all machines
                if (collected > 0) {
                    transferBudget -= collected;
                    int remaining = collected;
                    for (int round = 0; round < 3 && remaining > 0; round++) {
                        int share = Math.max(1, remaining / machineConsumers.size());
                        for (EnergyStorage machine : machineConsumers) {
                            if (remaining <= 0) break;
                            int toSend = Math.min(remaining, share);
                            int inserted = machine.insertEnergy(toSend, false);
                            remaining -= inserted;
                        }
                    }
                    if (remaining > 0) {
                        entity.energyStorage.insertEnergy(remaining, false);
                    }
                }
            }
        }

        // Phase 2: Surplus Power -> Charge Batteries!
        // (Surplus ONLY comes from generators or cables, NEVER from batteries)
        if (transferBudget > 0 && !batteryConsumers.isEmpty() && !generatorSources.isEmpty()) {
            int totalBatterySpace = 0;
            for (EnergyStorage bat : batteryConsumers) {
                totalBatterySpace += (bat.getMaxEnergy() - bat.getEnergy());
            }
            int surplusTarget = Math.min(transferBudget, totalBatterySpace);
            int surplusCollected = 0;

            for (BaseCableBlockEntity cable : networkCables) {
                if (surplusCollected >= surplusTarget) break;
                int cableEnergy = cable.energyStorage.getEnergy();
                if (cableEnergy > 0) {
                    int needed = surplusTarget - surplusCollected;
                    int toTake = Math.min(cableEnergy, needed);
                    int extracted = cable.energyStorage.extractEnergy(toTake, false);
                    if (extracted > 0) {
                        surplusCollected += extracted;
                        cable.markDirty();
                    }
                }
            }

            if (surplusCollected < surplusTarget) {
                for (EnergyStorage gen : generatorSources) {
                    if (surplusCollected >= surplusTarget) break;
                    int needed = surplusTarget - surplusCollected;
                    int extracted = gen.extractEnergy(needed, false);
                    surplusCollected += extracted;
                }
            }

            if (surplusCollected > 0) {
                int remaining = surplusCollected;
                for (int round = 0; round < 3 && remaining > 0; round++) {
                    int share = Math.max(1, remaining / batteryConsumers.size());
                    for (EnergyStorage bat : batteryConsumers) {
                        if (remaining <= 0) break;
                        int toSend = Math.min(remaining, share);
                        int inserted = bat.insertEnergy(toSend, false);
                        remaining -= inserted;
                    }
                }
                if (remaining > 0) {
                    entity.energyStorage.insertEnergy(remaining, false);
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
