package net.enchantedwood.block.entity;

import net.enchantedwood.fluid.LavaProvider;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.fluid.MoltenMetalProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TitaniumLavaPipeBlockEntity extends BlockEntity implements LavaProvider, MoltenMetalProvider {
    public static final int BUFFER_CAPACITY = 1000; // 1000 mB (1 bucket)
    public static final int TRANSFER_RATE = 500;   // 500 mB/t (10 buckets/sec)

    private int lavaAmount = 0;
    private MoltenMetal fluidType = MoltenMetal.NONE;

    public TitaniumLavaPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TITANIUM_LAVA_PIPE_BLOCK_ENTITY, pos, state);
    }

    // ==========================================
    // LAVA PROVIDER (Pure Lava compatibility)
    // ==========================================
    @Override
    public int getLavaAmount() {
        return (this.fluidType == MoltenMetal.LAVA) ? this.lavaAmount : 0;
    }

    @Override
    public int getMaxLava() {
        return (this.fluidType == MoltenMetal.LAVA || this.lavaAmount == 0) ? BUFFER_CAPACITY : 0;
    }

    @Override
    public boolean canInsertLava() {
        return this.lavaAmount < BUFFER_CAPACITY && (this.fluidType == MoltenMetal.LAVA || this.lavaAmount == 0);
    }

    @Override
    public boolean canExtractLava() {
        return this.fluidType == MoltenMetal.LAVA && this.lavaAmount > 0;
    }

    @Override
    public int insertLava(int amount, boolean simulate) {
        return insertFluid(MoltenMetal.LAVA, amount, simulate);
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        return extractFluid(MoltenMetal.LAVA, amount, simulate);
    }

    // ==========================================
    // MOLTEN METAL PROVIDER (Multi-Fluid support)
    // ==========================================
    @Override
    public MoltenMetal getFluidType() {
        return this.fluidType;
    }

    @Override
    public int getFluidAmount(MoltenMetal metal) {
        return (metal == this.fluidType) ? this.lavaAmount : 0;
    }

    @Override
    public int getMaxFluid() {
        return BUFFER_CAPACITY;
    }

    @Override
    public boolean canInsertFluid(MoltenMetal metal) {
        if (metal == MoltenMetal.NONE) return false;
        if (this.lavaAmount <= 0 || this.fluidType == MoltenMetal.NONE) return true;
        return this.fluidType == metal && this.lavaAmount < BUFFER_CAPACITY;
    }

    @Override
    public boolean canExtractFluid(MoltenMetal metal) {
        if (metal == MoltenMetal.NONE) return false;
        return this.fluidType == metal && this.lavaAmount > 0;
    }

    @Override
    public int insertFluid(MoltenMetal metal, int amount, boolean simulate) {
        if (metal == MoltenMetal.NONE || amount <= 0) return 0;
        if (this.fluidType != MoltenMetal.NONE && this.fluidType != metal && this.lavaAmount > 0) return 0;

        int space = BUFFER_CAPACITY - this.lavaAmount;
        int insertable = Math.min(space, amount);
        if (!simulate && insertable > 0) {
            this.fluidType = metal;
            this.lavaAmount += insertable;
            markDirty();
        }
        return insertable;
    }

    @Override
    public int extractFluid(MoltenMetal metal, int amount, boolean simulate) {
        if (metal == MoltenMetal.NONE || metal != this.fluidType || amount <= 0) return 0;
        int extractable = Math.min(this.lavaAmount, amount);
        if (!simulate && extractable > 0) {
            this.lavaAmount -= extractable;
            if (this.lavaAmount <= 0) {
                this.lavaAmount = 0;
                this.fluidType = MoltenMetal.NONE;
            }
            markDirty();
        }
        return extractable;
    }

    @Override
    public List<MoltenMetal> getContainedFluids() {
        return (this.fluidType != MoltenMetal.NONE && this.lavaAmount > 0) ? List.of(this.fluidType) : List.of();
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, TitaniumLavaPipeBlockEntity entity) {
        boolean dirty = false;

        // Auto-sanitize empty state
        if (entity.lavaAmount <= 0) {
            if (entity.fluidType != MoltenMetal.NONE || entity.lavaAmount != 0) {
                entity.lavaAmount = 0;
                entity.fluidType = MoltenMetal.NONE;
                dirty = true;
            }
        }

        // 1. Pull fluid from adjacent producers (Smelters, Lava Pumps, Tank Outbound Casings) if pipe has space
        if (entity.lavaAmount < BUFFER_CAPACITY) {
            int needed = Math.min(TRANSFER_RATE, BUFFER_CAPACITY - entity.lavaAmount);
            for (Direction dir : Direction.values()) {
                if (needed <= 0) break;
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                if (neighbor == null || neighbor instanceof TitaniumLavaPipeBlockEntity) continue;

                if (neighbor instanceof MoltenMetalProvider metalProvider) {
                    if (entity.fluidType != MoltenMetal.NONE && entity.lavaAmount > 0) {
                        int extracted = metalProvider.extractFluid(entity.fluidType, needed, false);
                        if (extracted > 0) {
                            entity.lavaAmount += extracted;
                            needed -= extracted;
                            dirty = true;
                        }
                    } else {
                        // Empty pipe: adopt first available fluid from producer
                        for (MoltenMetal metal : metalProvider.getContainedFluids()) {
                            if (metalProvider.getFluidAmount(metal) > 0) {
                                int extracted = metalProvider.extractFluid(metal, needed, false);
                                if (extracted > 0) {
                                    entity.fluidType = metal;
                                    entity.lavaAmount += extracted;
                                    needed -= extracted;
                                    dirty = true;
                                    break;
                                }
                            }
                        }
                    }
                } else if (neighbor instanceof LavaProvider lavaProvider) {
                    if (entity.fluidType == MoltenMetal.LAVA || entity.lavaAmount == 0) {
                        if (lavaProvider.canExtractLava()) {
                            int extracted = lavaProvider.extractLava(needed, false);
                            if (extracted > 0) {
                                entity.fluidType = MoltenMetal.LAVA;
                                entity.lavaAmount += extracted;
                                needed -= extracted;
                                dirty = true;
                            }
                        }
                    }
                }
            }
        }

        // 2. Push fluid to directly adjacent consumers (Tank Inbound Ports, Casting Ports, Generators)
        if (entity.lavaAmount > 0 && entity.fluidType != MoltenMetal.NONE) {
            boolean hasDedicatedAdjacent = hasDedicatedNeighbor(world, pos, entity.fluidType);
            boolean dedicatedExistsElsewhere = hasDedicatedAdjacent || findDirectionToConsumer(world, pos, entity.fluidType, true) != null;

            for (Direction dir : Direction.values()) {
                if (entity.lavaAmount <= 0) break;
                BlockPos neighborPos = pos.offset(dir);
                BlockEntity neighbor = world.getBlockEntity(neighborPos);
                if (neighbor == null || neighbor instanceof TitaniumLavaPipeBlockEntity) continue;

                boolean isDedicated = isDedicatedConsumer(neighbor, entity.fluidType, dir);
                boolean canConsume = isDedicated || canConsumeFluid(neighbor, entity.fluidType, dir);

                if (!canConsume) continue;

                // If this neighbor is NOT dedicated (e.g. an empty unreserved tank), but a dedicated tank exists elsewhere,
                // do not push into this empty tank; let the fluid route through pipes to the dedicated tank!
                if (!isDedicated && dedicatedExistsElsewhere) {
                    continue;
                }

                if (neighbor instanceof MoltenMetalProvider consumer) {
                    int toSend = Math.min(TRANSFER_RATE, entity.lavaAmount);
                    int accepted = consumer.insertFluid(entity.fluidType, toSend, false);
                    if (accepted > 0) {
                        entity.lavaAmount -= accepted;
                        dirty = true;
                    }
                } else if (entity.fluidType == MoltenMetal.LAVA && neighbor instanceof LavaProvider lavaConsumer) {
                    int toSend = Math.min(TRANSFER_RATE, entity.lavaAmount);
                    int accepted = lavaConsumer.insertLava(toSend, false);
                    if (accepted > 0) {
                        entity.lavaAmount -= accepted;
                        dirty = true;
                    }
                }
            }

            if (entity.lavaAmount <= 0) {
                entity.lavaAmount = 0;
                entity.fluidType = MoltenMetal.NONE;
                dirty = true;
            }
        }

        // 3. Intelligent Network Routing: BFS towards a destination that can accept this specific fluid
        if (entity.lavaAmount > 0 && entity.fluidType != MoltenMetal.NONE) {
            // First priority: seek dedicated consumer (tank that already has this fluid or is filtered to it)
            Direction targetDir = findDirectionToConsumer(world, pos, entity.fluidType, true);
            if (targetDir == null) {
                // Second priority: seek any consumer that can accept this fluid (e.g. empty tanks)
                targetDir = findDirectionToConsumer(world, pos, entity.fluidType, false);
            }

            if (targetDir != null) {
                BlockEntity nextBe = world.getBlockEntity(pos.offset(targetDir));
                if (nextBe instanceof TitaniumLavaPipeBlockEntity nextPipe) {
                    if (nextPipe.canInsertFluid(entity.fluidType)) {
                        int space = BUFFER_CAPACITY - nextPipe.lavaAmount;
                        int toSend = Math.min(TRANSFER_RATE, Math.min(entity.lavaAmount, space));
                        if (toSend > 0) {
                            int accepted = nextPipe.insertFluid(entity.fluidType, toSend, false);
                            if (accepted > 0) {
                                entity.lavaAmount -= accepted;
                                if (entity.lavaAmount <= 0) {
                                    entity.lavaAmount = 0;
                                    entity.fluidType = MoltenMetal.NONE;
                                }
                                dirty = true;
                            }
                        }
                    }
                }
            } else {
                // Fallback: push to any neighboring pipe that has space and matching/empty fluid
                for (Direction dir : Direction.values()) {
                    if (entity.lavaAmount <= 0) break;
                    BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                    if (neighbor instanceof TitaniumLavaPipeBlockEntity otherPipe) {
                        if (otherPipe.canInsertFluid(entity.fluidType) && otherPipe.lavaAmount < entity.lavaAmount) {
                            int diff = entity.lavaAmount - otherPipe.lavaAmount;
                            int toSend = Math.min(TRANSFER_RATE, Math.max(1, diff / 2));
                            int accepted = otherPipe.insertFluid(entity.fluidType, toSend, false);
                            if (accepted > 0) {
                                entity.lavaAmount -= accepted;
                                if (entity.lavaAmount <= 0) {
                                    entity.lavaAmount = 0;
                                    entity.fluidType = MoltenMetal.NONE;
                                }
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

    public static boolean hasDedicatedNeighbor(ServerWorld world, BlockPos pos, MoltenMetal fluidType) {
        for (Direction dir : Direction.values()) {
            BlockEntity be = world.getBlockEntity(pos.offset(dir));
            if (isDedicatedConsumer(be, fluidType, dir)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDedicatedConsumer(@Nullable BlockEntity be, MoltenMetal fluidType, @Nullable Direction fromDir) {
        if (be == null || be instanceof TitaniumLavaPipeBlockEntity) return false;
        if (be instanceof MoltenMetalProvider metalConsumer) {
            return metalConsumer.canInsertFluid(fluidType) && metalConsumer.isDedicatedTo(fluidType);
        } else if (fluidType == MoltenMetal.LAVA && be instanceof LavaProvider lavaConsumer) {
            if (be instanceof GeothermalGeneratorBlockEntity && fromDir != null && fromDir != Direction.DOWN) {
                return false;
            }
            return lavaConsumer.canInsertLava() && lavaConsumer.getLavaAmount() > 0;
        }
        return false;
    }

    public static boolean canConsumeFluid(@Nullable BlockEntity be, MoltenMetal fluidType, @Nullable Direction fromDir) {
        if (be == null || be instanceof TitaniumLavaPipeBlockEntity) return false;
        if (be instanceof MoltenMetalProvider metalConsumer) {
            return metalConsumer.canInsertFluid(fluidType);
        } else if (fluidType == MoltenMetal.LAVA && be instanceof LavaProvider lavaConsumer) {
            if (be instanceof GeothermalGeneratorBlockEntity && fromDir != null && fromDir != Direction.DOWN) {
                return false;
            }
            return lavaConsumer.canInsertLava();
        }
        return false;
    }

    public static @Nullable Direction findDirectionToConsumer(ServerWorld world, BlockPos startPos, MoltenMetal fluidType) {
        Direction dedicated = findDirectionToConsumer(world, startPos, fluidType, true);
        if (dedicated != null) return dedicated;
        return findDirectionToConsumer(world, startPos, fluidType, false);
    }

    /**
     * Breadth-First Search through connected pipes to find the step direction leading to a consumer.
     * @param dedicatedOnly if true, only considers consumers that already hold this fluid or are filtered to it.
     */
    public static @Nullable Direction findDirectionToConsumer(ServerWorld world, BlockPos startPos, MoltenMetal fluidType, boolean dedicatedOnly) {
        if (fluidType == MoltenMetal.NONE) return null;

        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Direction> firstStepMap = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();

        visited.add(startPos);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = startPos.offset(dir);
            BlockEntity be = world.getBlockEntity(neighborPos);
            if (be instanceof TitaniumLavaPipeBlockEntity pipe) {
                if (pipe.lavaAmount == 0 || pipe.fluidType == fluidType) {
                    visited.add(neighborPos);
                    firstStepMap.put(neighborPos, dir);
                    queue.add(neighborPos);
                }
            }
        }

        int maxNodes = 128;
        int inspected = 0;

        while (!queue.isEmpty() && inspected < maxNodes) {
            BlockPos current = queue.poll();
            inspected++;
            Direction firstStep = firstStepMap.get(current);

            for (Direction dir : Direction.values()) {
                BlockPos targetPos = current.offset(dir);
                if (targetPos.equals(startPos)) continue;
                BlockEntity target = world.getBlockEntity(targetPos);
                if (target == null || target instanceof TitaniumLavaPipeBlockEntity) continue;

                if (dedicatedOnly) {
                    if (isDedicatedConsumer(target, fluidType, dir)) {
                        return firstStep;
                    }
                } else {
                    if (canConsumeFluid(target, fluidType, dir)) {
                        return firstStep;
                    }
                }
            }

            for (Direction dir : Direction.values()) {
                BlockPos nextPos = current.offset(dir);
                if (!visited.contains(nextPos)) {
                    visited.add(nextPos);
                    BlockEntity nextBe = world.getBlockEntity(nextPos);
                    if (nextBe instanceof TitaniumLavaPipeBlockEntity nextPipe) {
                        if (nextPipe.lavaAmount == 0 || nextPipe.fluidType == fluidType) {
                            firstStepMap.put(nextPos, firstStep);
                            queue.add(nextPos);
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("LavaAmount", this.lavaAmount);
        view.putString("FluidType", this.fluidType.getId());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.lavaAmount = view.getInt("LavaAmount", 0);
        if (this.lavaAmount <= 0) {
            this.lavaAmount = 0;
            this.fluidType = MoltenMetal.NONE;
        } else {
            this.fluidType = MoltenMetal.fromId(view.getString("FluidType", "none"));
        }
    }
}
