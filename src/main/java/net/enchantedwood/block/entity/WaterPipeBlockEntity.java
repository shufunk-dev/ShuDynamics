package net.enchantedwood.block.entity;

import net.enchantedwood.fluid.WaterProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class WaterPipeBlockEntity extends BlockEntity implements WaterProvider {
    public static final int BUFFER_CAPACITY = 1000; // 1000 mB
    public static final int TRANSFER_RATE = 500;   // 500 mB/t

    private int waterAmount = 0;

    public WaterPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PIPE_BE, pos, state);
    }

    @Override
    public int getWaterAmount() {
        return this.waterAmount;
    }

    @Override
    public int getMaxWater() {
        return BUFFER_CAPACITY;
    }

    @Override
    public boolean canInsertWater() {
        return this.waterAmount < BUFFER_CAPACITY;
    }

    @Override
    public boolean canExtractWater() {
        return this.waterAmount > 0;
    }

    @Override
    public int insertWater(int amount, boolean simulate) {
        int space = BUFFER_CAPACITY - this.waterAmount;
        int toInsert = Math.min(space, amount);
        if (!simulate && toInsert > 0) {
            this.waterAmount += toInsert;
            setChanged();
        }
        return toInsert;
    }

    @Override
    public int extractWater(int amount, boolean simulate) {
        int toDrain = Math.min(this.waterAmount, amount);
        if (!simulate && toDrain > 0) {
            this.waterAmount -= toDrain;
            setChanged();
        }
        return toDrain;
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, WaterPipeBlockEntity entity) {
        boolean dirty = false;

        // 1. Pull water from adjacent non-pipe producers (e.g. Water Pump)
        if (entity.waterAmount < BUFFER_CAPACITY) {
            int needed = BUFFER_CAPACITY - entity.waterAmount;
            for (Direction dir : Direction.values()) {
                if (needed <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
                if (neighbor instanceof WaterProvider provider && !(neighbor instanceof WaterPipeBlockEntity)) {
                    if (provider.canExtractWater()) {
                        int toPull = Math.min(needed, TRANSFER_RATE);
                        int extracted = provider.extractWater(toPull, false);
                        if (extracted > 0) {
                            entity.waterAmount += extracted;
                            needed -= extracted;
                            dirty = true;
                        }
                    }
                }
            }
        }

        // 2. Push water into adjacent non-pipe consumers (e.g. Cryo Freezer, Oxygen Generator)
        if (entity.waterAmount > 0) {
            for (Direction dir : Direction.values()) {
                if (entity.waterAmount <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
                if (neighbor instanceof WaterProvider consumer && !(neighbor instanceof WaterPipeBlockEntity)) {
                    if (consumer.canInsertWater()) {
                        int toSend = Math.min(TRANSFER_RATE, entity.waterAmount);
                        int accepted = consumer.insertWater(toSend, false);
                        if (accepted > 0) {
                            entity.waterAmount -= accepted;
                            dirty = true;
                        }
                    }
                }
            }
        }

        // 3. Pressure equalization across connected Water Pipe network
        if (entity.waterAmount > 0) {
            for (Direction dir : Direction.values()) {
                if (entity.waterAmount <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
                if (neighbor instanceof WaterPipeBlockEntity otherPipe) {
                    if (otherPipe.waterAmount < entity.waterAmount) {
                        int diff = entity.waterAmount - otherPipe.waterAmount;
                        int toSend = Math.min(TRANSFER_RATE, Math.max(1, diff / 2));
                        int accepted = otherPipe.insertWater(toSend, false);
                        if (accepted > 0) {
                            entity.waterAmount -= accepted;
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
        this.waterAmount = view.getIntOr("WaterAmount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("WaterAmount", this.waterAmount);
    }
}
