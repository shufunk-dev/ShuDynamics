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
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.LASER_QUARRY_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> LaserQuarryBlockEntity.tick(serverWorld, pos, st, (LaserQuarryBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (state.get(LIT)) {
            double d = pos.getX() + 0.5;
            double e = pos.getY() + 0.5;
            double f = pos.getZ() + 0.5;

            // Concentrated scanning core particles on the machine
            world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, d, e + 0.5, f, 0.0, 0.1, 0.0);
            world.addParticleClient(ParticleTypes.PORTAL, d + (random.nextDouble() - 0.5) * 0.4, e + 0.8, f + (random.nextDouble() - 0.5) * 0.4, 0.0, -0.2, 0.0);

            // Glowing neon laser boundary frame at machine level
            BlockEntity be = world.getBlockEntity(pos);
            int radius = 0;
            if (be instanceof LaserQuarryBlockEntity quarry) {
                radius = quarry.getRangeChunkRadius();
            }

            net.minecraft.util.math.ChunkPos originChunk = new net.minecraft.util.math.ChunkPos(pos);
            double minX = (originChunk.x - radius) * 16.0;
            double maxX = (originChunk.x + radius) * 16.0 + 16.0;
            double minZ = (originChunk.z - radius) * 16.0;
            double maxZ = (originChunk.z + radius) * 16.0 + 16.0;
            double laserY = pos.getY() + 0.5;

            // Spawn laser boundary particles along the 4 chunk edges
            for (int i = 0; i < 4; i++) {
                double t = random.nextDouble();
                // Edge 1 (North: minZ)
                world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, minX + t * (maxX - minX), laserY, minZ, 0.0, 0.01, 0.0);
                // Edge 2 (South: maxZ)
                world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, minX + t * (maxX - minX), laserY, maxZ, 0.0, 0.01, 0.0);
                // Edge 3 (West: minX)
                world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, minX, laserY, minZ + t * (maxZ - minZ), 0.0, 0.01, 0.0);
                // Edge 4 (East: maxX)
                world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, maxX, laserY, minZ + t * (maxZ - minZ), 0.0, 0.01, 0.0);
            }
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
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
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
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
