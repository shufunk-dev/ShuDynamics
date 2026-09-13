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

import java.util.ArrayList;
import java.util.List;

public class TitaniumLavaPipeBlockEntity extends BlockEntity implements LavaProvider, MoltenMetalProvider {
    public static final int BUFFER_CAPACITY = 1000; // 1000 mB (1 bucket)
    public static final int TRANSFER_RATE = 500;   // 500 mB/t (10 buckets/sec)

    private int lavaAmount = 0;
    private MoltenMetal fluidType = MoltenMetal.LAVA;

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

        // 1. Pull fluid from adjacent producers (Pumps, Smelters, Tank Outbounds) if pipe has space
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
                        // Empty pipe: adopt first available fluid
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

        // 2. Push fluid to adjacent consumers (Generators, Inbound Ports, Tanks, Casting Ports) and adjacent pipes
        if (entity.lavaAmount > 0 && entity.fluidType != MoltenMetal.NONE) {
            List<MoltenMetalProvider> metalConsumers = new ArrayList<>();
            List<LavaProvider> pureLavaConsumers = new ArrayList<>();
            List<TitaniumLavaPipeBlockEntity> pipeNeighbors = new ArrayList<>();

            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                if (neighbor instanceof TitaniumLavaPipeBlockEntity otherPipe) {
                    if (otherPipe.lavaAmount < entity.lavaAmount && (otherPipe.lavaAmount == 0 || otherPipe.fluidType == entity.fluidType)) {
                        pipeNeighbors.add(otherPipe);
                    }
                } else if (neighbor instanceof MoltenMetalProvider metalProvider) {
                    if (neighbor instanceof TitaniumTankControllerBlockEntity) {
                        // Only insert into Tank Inbound Port from above
                        if (dir == Direction.DOWN && metalProvider.canInsertFluid(entity.fluidType)) {
                            metalConsumers.add(metalProvider);
                        }
                    } else if (metalProvider.canInsertFluid(entity.fluidType)) {
                        metalConsumers.add(metalProvider);
                    }
                } else if (entity.fluidType == MoltenMetal.LAVA && neighbor instanceof LavaProvider lavaProvider) {
                    if (neighbor instanceof GeothermalGeneratorBlockEntity) {
                        if (dir == Direction.DOWN && lavaProvider.canInsertLava()) {
                            pureLavaConsumers.add(lavaProvider);
                        }
                    } else if (lavaProvider.canInsertLava()) {
                        pureLavaConsumers.add(lavaProvider);
                    }
                }
            }

            // Prioritize machine consumers (Tanks, Casting Ports, Generators)
            if (!metalConsumers.isEmpty()) {
                int perConsumer = Math.max(1, Math.min(TRANSFER_RATE, entity.lavaAmount) / metalConsumers.size());
                for (MoltenMetalProvider consumer : metalConsumers) {
                    if (entity.lavaAmount <= 0) break;
                    int toSend = Math.min(perConsumer, entity.lavaAmount);
                    int accepted = consumer.insertFluid(entity.fluidType, toSend, false);
                    if (accepted > 0) {
                        entity.lavaAmount -= accepted;
                        dirty = true;
                    }
                }
            } else if (!pureLavaConsumers.isEmpty()) {
                int perConsumer = Math.max(1, Math.min(TRANSFER_RATE, entity.lavaAmount) / pureLavaConsumers.size());
                for (LavaProvider consumer : pureLavaConsumers) {
                    if (entity.lavaAmount <= 0) break;
                    int toSend = Math.min(perConsumer, entity.lavaAmount);
                    int accepted = consumer.insertLava(toSend, false);
                    if (accepted > 0) {
                        entity.lavaAmount -= accepted;
                        dirty = true;
                    }
                }
            }

            // Reset fluid type if pipe became empty
            if (entity.lavaAmount <= 0) {
                entity.fluidType = MoltenMetal.NONE;
                dirty = true;
            }

            // Distribute remaining fluid evenly among pipe network
            if (entity.lavaAmount > 0 && !pipeNeighbors.isEmpty()) {
                for (TitaniumLavaPipeBlockEntity otherPipe : pipeNeighbors) {
                    if (entity.lavaAmount <= otherPipe.lavaAmount) continue;
                    int diff = entity.lavaAmount - otherPipe.lavaAmount;
                    int toEqualize = Math.min(TRANSFER_RATE, diff / 2);
                    if (toEqualize > 0) {
                        int accepted = otherPipe.insertFluid(entity.fluidType, toEqualize, false);
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
        view.putString("FluidType", this.fluidType.getId());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.lavaAmount = view.getInt("LavaAmount", 0);
        this.fluidType = MoltenMetal.fromId(view.getString("FluidType", "lava"));
    }
}
