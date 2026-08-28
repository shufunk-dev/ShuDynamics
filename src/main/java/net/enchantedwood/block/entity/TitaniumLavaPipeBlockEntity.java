package net.enchantedwood.block.entity;

import net.enchantedwood.fluid.LavaProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class TitaniumLavaPipeBlockEntity extends BlockEntity implements LavaProvider {
    public static final int BUFFER_CAPACITY = 1000; // 1000 mB (1 bucket)
    public static final int TRANSFER_RATE = 500;   // 500 mB/t (10 buckets/sec)

    private int lavaAmount = 0;

    public TitaniumLavaPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TITANIUM_LAVA_PIPE_BLOCK_ENTITY, pos, state);
    }

    @Override
    public int getLavaAmount() {
        return this.lavaAmount;
    }

    @Override
    public int getMaxLava() {
        return BUFFER_CAPACITY;
    }

    @Override
    public int insertLava(int amount, boolean simulate) {
        if (amount <= 0) return 0;
        int space = BUFFER_CAPACITY - this.lavaAmount;
        int insertable = Math.min(space, amount);
        if (!simulate && insertable > 0) {
            this.lavaAmount += insertable;
            markDirty();
        }
        return insertable;
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        if (amount <= 0) return 0;
        int extractable = Math.min(this.lavaAmount, amount);
        if (!simulate && extractable > 0) {
            this.lavaAmount -= extractable;
            markDirty();
        }
        return extractable;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, TitaniumLavaPipeBlockEntity entity) {
        boolean dirty = false;

        // 1. Pull lava from adjacent producers (Pumps, Crucibles, Tank Outbounds) if pipe has space
        if (entity.lavaAmount < BUFFER_CAPACITY) {
            int needed = Math.min(TRANSFER_RATE, BUFFER_CAPACITY - entity.lavaAmount);
            for (Direction dir : Direction.values()) {
                if (needed <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                // Do not pull from another pipe in this step (distribution handles pipe-to-pipe)
                if (neighbor instanceof LavaProvider provider && !(neighbor instanceof TitaniumLavaPipeBlockEntity)) {
                    if (provider.canExtractLava()) {
                        int extracted = provider.extractLava(needed, false);
                        if (extracted > 0) {
                            entity.lavaAmount += extracted;
                            needed -= extracted;
                            dirty = true;
                        }
                    }
                }
            }
        }

        // 2. Push lava to adjacent consumers (Generators, Inbound Ports, Tanks) and adjacent pipes
        if (entity.lavaAmount > 0) {
            List<LavaProvider> consumers = new ArrayList<>();
            List<TitaniumLavaPipeBlockEntity> pipeNeighbors = new ArrayList<>();

            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                if (neighbor instanceof LavaProvider provider) {
                    if (neighbor instanceof TitaniumLavaPipeBlockEntity otherPipe) {
                        if (otherPipe.getLavaAmount() < entity.lavaAmount) {
                            pipeNeighbors.add(otherPipe);
                        }
                    } else if (neighbor instanceof GeothermalGeneratorBlockEntity) {
                        // Only insert into Geothermal Generator from above (dir == Direction.DOWN)
                        if (dir == Direction.DOWN && provider.canInsertLava()) {
                            consumers.add(provider);
                        }
                    } else if (neighbor instanceof TitaniumTankControllerBlockEntity) {
                        // Only insert into Tank Inbound Port from above (dir == Direction.DOWN)
                        if (dir == Direction.DOWN && provider.canInsertLava()) {
                            consumers.add(provider);
                        }
                    } else if (provider.canInsertLava()) {
                        consumers.add(provider);
                    }
                }
            }

            // Prioritize primary machine consumers first
            if (!consumers.isEmpty()) {
                int perConsumer = Math.max(1, Math.min(TRANSFER_RATE, entity.lavaAmount) / consumers.size());
                for (LavaProvider consumer : consumers) {
                    if (entity.lavaAmount <= 0) break;
                    int toSend = Math.min(perConsumer, entity.lavaAmount);
                    int accepted = consumer.insertLava(toSend, false);
                    if (accepted > 0) {
                        entity.lavaAmount -= accepted;
                        dirty = true;
                    }
                }
            }

            // Distribute remaining fluid evenly among pipe network
            if (entity.lavaAmount > 0 && !pipeNeighbors.isEmpty()) {
                for (TitaniumLavaPipeBlockEntity otherPipe : pipeNeighbors) {
                    if (entity.lavaAmount <= otherPipe.getLavaAmount()) continue;
                    int diff = entity.lavaAmount - otherPipe.getLavaAmount();
                    int toEqualize = Math.min(TRANSFER_RATE, diff / 2);
                    if (toEqualize > 0) {
                        int accepted = otherPipe.insertLava(toEqualize, false);
                        if (accepted > 0) {
                            entity.lavaAmount -= accepted;
                            dirty = true;
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
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("LavaAmount", this.lavaAmount);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.lavaAmount = view.getInt("LavaAmount", 0);
    }
}
