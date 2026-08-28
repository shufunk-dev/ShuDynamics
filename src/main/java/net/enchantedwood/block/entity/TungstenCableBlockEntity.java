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

public class TungstenCableBlockEntity extends BlockEntity implements EnergyProvider {
    public static final int CABLE_TRANSFER_RATE = 25_000; // 25,000 FE/t

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CABLE_TRANSFER_RATE, CABLE_TRANSFER_RATE, CABLE_TRANSFER_RATE, 0);

    public TungstenCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUNGSTEN_CABLE_BE, pos, state);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, TungstenCableBlockEntity entity) {
        boolean dirty = false;

        // Discover all adjacent blocks
        List<EnergyStorage> generatorSources = new ArrayList<>();
        List<EnergyStorage> batterySources = new ArrayList<>();
        List<EnergyStorage> machineConsumers = new ArrayList<>();
        List<EnergyStorage> batteryConsumers = new ArrayList<>();
        List<TungstenCableBlockEntity> otherCables = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
            if (neighbor instanceof EnergyProvider provider && neighbor != entity) {
                EnergyStorage storage = provider.getEnergyStorage(dir.getOpposite());
                if (storage == null) continue;

                if (neighbor instanceof CopperGeneratorBlockEntity ||
                        neighbor instanceof AluminumGeneratorBlockEntity ||
                        neighbor instanceof SteelGeneratorBlockEntity ||
                        neighbor instanceof GeothermalGeneratorBlockEntity ||
                        neighbor instanceof EnchantedLavaGeneratorBlockEntity) {
                    if (storage.canExtract() && storage.getEnergy() > 0) {
                        generatorSources.add(storage);
                    }
                } else if (neighbor instanceof TungstenBatteryBlockEntity ||
                        neighbor instanceof SteelBatteryBlockEntity ||
                        neighbor instanceof AluminumBatteryBlockEntity ||
                        neighbor instanceof CopperBatteryBlockEntity) {
                    if (storage.canExtract() && storage.getEnergy() > 0) {
                        batterySources.add(storage);
                    }
                    if (storage.canInsert() && storage.getEnergy() < storage.getMaxEnergy()) {
                        batteryConsumers.add(storage);
                    }
                } else if (neighbor instanceof TungstenCableBlockEntity otherCable) {
                    otherCables.add(otherCable);
                } else {
                    // Machine consumers (Magma Crucible, Crusher, Lava Pump, Alloy Foundry, etc.)
                    if (storage.canInsert() && storage.getEnergy() < storage.getMaxEnergy()) {
                        machineConsumers.add(storage);
                    }
                }
            }
        }

        // 1. Pull from Generators if cable has space
        if (entity.energyStorage.getEnergy() < CABLE_TRANSFER_RATE && !generatorSources.isEmpty()) {
            int needed = CABLE_TRANSFER_RATE - entity.energyStorage.getEnergy();
            for (EnergyStorage gen : generatorSources) {
                if (needed <= 0) break;
                int extracted = gen.extractEnergy(needed, false);
                if (extracted > 0) {
                    entity.energyStorage.insertEnergy(extracted, false);
                    needed -= extracted;
                    dirty = true;
                }
            }
        }

        // 2. If machines need power (or cable has space) and no generator provided enough, pull from Batteries
        if (entity.energyStorage.getEnergy() < CABLE_TRANSFER_RATE && !batterySources.isEmpty()) {
            int needed = CABLE_TRANSFER_RATE - entity.energyStorage.getEnergy();
            for (EnergyStorage bat : batterySources) {
                if (needed <= 0) break;
                int extracted = bat.extractEnergy(needed, false);
                if (extracted > 0) {
                    entity.energyStorage.insertEnergy(extracted, false);
                    needed -= extracted;
                    dirty = true;
                }
            }
        }

        // 3. Distribute energy: Priority 1 to Machines, Priority 2 to Batteries (with generator surplus), Priority 3 to other cables
        if (entity.energyStorage.getEnergy() > 0) {
            int available = entity.energyStorage.getEnergy();

            // Priority 1: Power machine consumers
            if (!machineConsumers.isEmpty()) {
                int share = Math.max(1, available / machineConsumers.size());
                for (EnergyStorage target : machineConsumers) {
                    if (available <= 0) break;
                    int toSend = Math.min(available, share);
                    int inserted = target.insertEnergy(toSend, false);
                    if (inserted > 0) {
                        entity.energyStorage.extractEnergy(inserted, false);
                        available -= inserted;
                        dirty = true;
                    }
                }
            }

            // Priority 2: Charge Batteries with surplus (only when supplied by generators)
            if (available > 0 && !generatorSources.isEmpty() && !batteryConsumers.isEmpty()) {
                int share = Math.max(1, available / batteryConsumers.size());
                for (EnergyStorage target : batteryConsumers) {
                    if (available <= 0) break;
                    int toSend = Math.min(available, share);
                    int inserted = target.insertEnergy(toSend, false);
                    if (inserted > 0) {
                        entity.energyStorage.extractEnergy(inserted, false);
                        available -= inserted;
                        dirty = true;
                    }
                }
            }

            // Priority 3: Forward / equalize across Cable Network
            if (available > 0 && !otherCables.isEmpty()) {
                for (TungstenCableBlockEntity otherCable : otherCables) {
                    if (available <= 0) break;
                    int otherEnergy = otherCable.energyStorage.getEnergy();
                    if (entity.energyStorage.getEnergy() > otherEnergy) {
                        int diff = entity.energyStorage.getEnergy() - otherEnergy;
                        int toSend = Math.min(available, diff / 2);
                        if (toSend > 0) {
                            int inserted = otherCable.energyStorage.insertEnergy(toSend, false);
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
        this.energyStorage.readData(view);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        this.energyStorage.writeData(view);
    }
}
