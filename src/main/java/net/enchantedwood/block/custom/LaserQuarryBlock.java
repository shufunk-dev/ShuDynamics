package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.LaserQuarryBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;

public class LaserQuarryBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<LaserQuarryBlock> CODEC = createCodec(LaserQuarryBlock::new);
    public static final BooleanProperty LIT = Properties.LIT;

    public LaserQuarryBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LaserQuarryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.LASER_QUARRY_BLOCK_ENTITY) {
            if (world instanceof ServerWorld serverWorld) {
                return (w, pos, st, blockEntity) -> LaserQuarryBlockEntity.tick(serverWorld, pos, st, (LaserQuarryBlockEntity) blockEntity);
            } else {
                return (w, pos, st, blockEntity) -> LaserQuarryBlockEntity.clientTick(w, pos, st, (LaserQuarryBlockEntity) blockEntity);
            }
        }
        return null;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        double d = pos.getX() + 0.5;
        double e = pos.getY() + 0.5;
        double f = pos.getZ() + 0.5;

        if (state.get(LIT)) {
            // Concentrated scanning core particles on the machine
            world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, d, e + 0.5, f, 0.0, 0.1, 0.0);
            world.addParticleClient(ParticleTypes.PORTAL, d + (random.nextDouble() - 0.5) * 0.4, e + 0.8, f + (random.nextDouble() - 0.5) * 0.4, 0.0, -0.2, 0.0);
        } else {
            // Standby indicator when paused/idle
            world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, d, e + 0.8, f, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.getMainHandStack().getItem() instanceof net.enchantedwood.item.custom.WrenchItem ||
            player.getOffHandStack().getItem() instanceof net.enchantedwood.item.custom.WrenchItem) {
            return ActionResult.PASS;
        }
        if (!world.isClient()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof NamedScreenHandlerFactory factory) {
                player.openHandledScreen(factory);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient()) {
            net.minecraft.component.type.NbtComponent nbtComponent = itemStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
            if (nbtComponent != null) {
                net.minecraft.nbt.NbtCompound nbt = nbtComponent.copyNbt();
                if (nbt.contains("boundX")) {
                    int bx = nbt.getInt("boundX").orElse(0);
                    int by = nbt.getInt("boundY").orElse(0);
                    int bz = nbt.getInt("boundZ").orElse(0);
                    String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof LaserQuarryBlockEntity quarry) {
                        quarry.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        if (placer instanceof PlayerEntity player) {
                            String dimName = bDim.contains("mining_dimension") ? "Mining Dimension" :
                                    bDim.contains("nether") ? "Nether" :
                                    bDim.contains("end") ? "The End" : "Overworld";
                            player.sendMessage(net.minecraft.text.Text.literal("§6[Quarry] §a✨ Auto-connected to Base Network at (" + bx + ", " + by + ", " + bz + ") in " + dimName + "!"), true);
                        }
                    }
                }
            }
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof LaserQuarryBlockEntity quarry) {
                quarry.resetScanCoordinates(state);
                if (world instanceof ServerWorld serverWorld) {
                    quarry.updateChunkLoading(serverWorld);
                }
            }
            if (placer instanceof PlayerEntity player) {
                net.minecraft.util.math.ChunkPos chunk = new net.minecraft.util.math.ChunkPos(pos);
                int relX = pos.getX() - (chunk.x * 16);
                int relZ = pos.getZ() - (chunk.z * 16);
                Direction facing = state.contains(FACING) ? state.get(FACING) : Direction.NORTH;
                player.sendMessage(net.minecraft.text.Text.literal("§6[Quarry] §e⚡ Laser Boundary Active! §7(Facing " + facing.asString().toUpperCase() + " • §bCorner Layout§7)"), false);
            }
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LaserQuarryBlockEntity quarryEntity) {
            quarryEntity.releaseChunkTickets(world);
            ItemScatterer.spawn(world, pos, quarryEntity);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
