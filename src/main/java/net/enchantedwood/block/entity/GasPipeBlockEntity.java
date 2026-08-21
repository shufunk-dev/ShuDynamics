package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.enchantedwood.block.custom.GasPipeBlock;
import net.enchantedwood.gas.GasProvider;
import net.enchantedwood.gas.GasStorage;
import net.enchantedwood.gas.GasType;
import net.enchantedwood.gas.SimpleGasStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GasPipeBlockEntity extends BlockEntity implements GasProvider {
    public static final int PIPE_TRANSFER_RATE = 100; // 100 mB/t

    private final GasType handledType;
    private final SimpleGasStorage gasStorage;

    public GasPipeBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof GasPipeBlock pipeBlock ? pipeBlock.getHandledGasType() : GasType.OXYGEN);
    }

    public GasPipeBlockEntity(BlockPos pos, BlockState state, GasType handledType) {
        super(handledType == GasType.HYDROGEN ? ModBlockEntities.HYDROGEN_PIPE_BLOCK_ENTITY : ModBlockEntities.GAS_PIPE_BLOCK_ENTITY, pos, state);
        this.handledType = handledType;
        this.gasStorage = new SimpleGasStorage(PIPE_TRANSFER_RATE, PIPE_TRANSFER_RATE) {
            @Override
            public GasType getGasType() {
                return handledType;
            }

            @Override
            public boolean canInsert(GasType type) {
                return type == handledType && getAmount() < getCapacity();
            }

            @Override
            public boolean canExtract(GasType type) {
                return (type == GasType.NONE || type == handledType) && getAmount() > 0;
            }

            @Override
            public int insertGas(GasType type, int insertAmount, boolean simulate) {
                if (type != handledType || insertAmount <= 0) return 0;
                int space = getCapacity() - getAmount();
                int insertable = Math.min(space, insertAmount);
                if (!simulate && insertable > 0) {
                    setGas(handledType, getAmount() + insertable);
                }
                return insertable;
            }

            @Override
            public int extractGas(GasType type, int extractAmount, boolean simulate) {
                if (type != GasType.NONE && type != handledType) return 0;
                int extractable = Math.min(getAmount(), extractAmount);
                if (!simulate && extractable > 0) {
                    setGas(handledType, getAmount() - extractable);
                }
                return extractable;
            }
        };
    }

    public GasType getHandledType() {
        return this.handledType;
    }

    @Override
    public @Nullable GasStorage getGasStorage(@Nullable Direction side) {
        return this.gasStorage;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, GasPipeBlockEntity entity) {
        GasType type = entity.handledType;
        boolean stateChanged = false;

        // 1. Pull gas from adjacent producers (Oxygen Generator)
        if (entity.gasStorage.getAmount() < entity.gasStorage.getCapacity()) {
            int needed = entity.gasStorage.getCapacity() - entity.gasStorage.getAmount();
            for (Direction dir : Direction.values()) {
                if (needed <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                if (neighbor instanceof OxygenGeneratorBlockEntity gen) {
                    GasStorage genStorage = gen.getGasStorage(dir.getOpposite());
                    if (genStorage != null && genStorage.canExtract(type)) {
                        int extracted = genStorage.extractGas(type, needed, false);
                        if (extracted > 0) {
                            entity.gasStorage.insertGas(type, extracted, false);
                            needed -= extracted;
                            stateChanged = true;
                        }
                    }
                }
            }
        }

        if (entity.gasStorage.getAmount() <= 0) {
            if (stateChanged) markDirty(world, pos, state);
            return;
        }

        int availableToPush = entity.gasStorage.getAmount();

        List<GasStorage> directConsumers = new ArrayList<>();
        List<GasStorage> pipeNeighbors = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
            if (neighbor instanceof GasProvider provider && neighbor != entity) {
                // Ignore generators to prevent pushing gas back into the generator
                if (neighbor instanceof OxygenGeneratorBlockEntity) {
                    continue;
                }

                GasStorage storage = provider.getGasStorage(dir.getOpposite());
                if (storage != null && storage.canInsert(type) && storage.getAmount() < storage.getCapacity()) {
                    if (neighbor instanceof GasPipeBlockEntity pipeNeighbor) {
                        if (pipeNeighbor.getHandledType() == entity.handledType) {
                            pipeNeighbors.add(storage);
                        }
                    } else {
                        directConsumers.add(storage);
                    }
                }
            }
        }

        // 2. Push to consumers first (Refiner, Steel Blast Furnace, etc.)
        if (!directConsumers.isEmpty()) {
            int share = Math.max(1, availableToPush / directConsumers.size());
            for (GasStorage target : directConsumers) {
                if (availableToPush <= 0) break;
                int toSend = Math.min(availableToPush, share);
                int inserted = target.insertGas(type, toSend, false);
                if (inserted > 0) {
                    entity.gasStorage.extractGas(type, inserted, false);
                    availableToPush -= inserted;
                    stateChanged = true;
                }
            }
        }

        // 3. Distribute through pipe network
        if (availableToPush > 0 && !pipeNeighbors.isEmpty()) {
            int share = Math.max(1, availableToPush / pipeNeighbors.size());
            for (GasStorage target : pipeNeighbors) {
                if (availableToPush <= 0) break;
                int toSend = Math.min(availableToPush, share);
                int inserted = target.insertGas(type, toSend, false);
                if (inserted > 0) {
                    entity.gasStorage.extractGas(type, inserted, false);
                    availableToPush -= inserted;
                    stateChanged = true;
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
        this.gasStorage.readData(view, "Pipe");
        this.gasStorage.setGas(this.handledType, this.gasStorage.getAmount());
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        this.gasStorage.writeData(view, "Pipe");
    }
}
