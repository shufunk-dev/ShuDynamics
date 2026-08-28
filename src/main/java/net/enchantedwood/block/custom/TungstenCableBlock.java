package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.world.block.WireOrientation;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.TungstenCableBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import org.jetbrains.annotations.Nullable;

public class TungstenCableBlock extends BlockWithEntity {
    public static final MapCodec<TungstenCableBlock> CODEC = createCodec(TungstenCableBlock::new);

    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;

    // VoxelShapes for 6-way pipe connections (core is 6x6x6: 5 to 11)
    private static final VoxelShape CORE_SHAPE = Block.createCuboidShape(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);

    public TungstenCableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(NORTH, false)
                .with(SOUTH, false)
                .with(EAST, false)
                .with(WEST, false)
                .with(UP, false)
                .with(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TungstenCableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.TUNGSTEN_CABLE_BE) {
            return (w, pos, st, blockEntity) -> TungstenCableBlockEntity.tick(serverWorld, pos, st, (TungstenCableBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE_SHAPE;
        if (state.get(UP)) shape = VoxelShapes.union(shape, UP_SHAPE);
        if (state.get(DOWN)) shape = VoxelShapes.union(shape, DOWN_SHAPE);
        if (state.get(NORTH)) shape = VoxelShapes.union(shape, NORTH_SHAPE);
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape, SOUTH_SHAPE);
        if (state.get(WEST)) shape = VoxelShapes.union(shape, WEST_SHAPE);
        if (state.get(EAST)) shape = VoxelShapes.union(shape, EAST_SHAPE);
        return shape;
    }

    public boolean canConnectTo(BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof TungstenCableBlock ||
                neighborState.getBlock() instanceof SteelCableBlock ||
                neighborState.getBlock() instanceof AluminumCableBlock ||
                neighborState.getBlock() instanceof CopperCableBlock ||
                neighborState.isOf(net.enchantedwood.block.ModBlocks.CRUSHER) ||
                neighborState.isOf(net.enchantedwood.block.ModBlocks.DUST_SMELTER)) {
            return true;
        }
        BlockEntity be = world.getBlockEntity(neighborPos);
        if (be instanceof EnergyProvider provider) {
            return provider.getEnergyStorage(direction.getOpposite()) != null;
        }
        return false;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        return this.getDefaultState()
                .with(NORTH, canConnectTo(world, pos, Direction.NORTH))
                .with(SOUTH, canConnectTo(world, pos, Direction.SOUTH))
                .with(EAST, canConnectTo(world, pos, Direction.EAST))
                .with(WEST, canConnectTo(world, pos, Direction.WEST))
                .with(UP, canConnectTo(world, pos, Direction.UP))
                .with(DOWN, canConnectTo(world, pos, Direction.DOWN));
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.math.random.Random random) {
        return state.with(
                switch (direction) {
                    case NORTH -> NORTH;
                    case SOUTH -> SOUTH;
                    case EAST -> EAST;
                    case WEST -> WEST;
                    case UP -> UP;
                    case DOWN -> DOWN;
                },
                canConnectTo(world, pos, direction)
        );
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (!world.isClient()) {
            BlockState updated = state
                    .with(NORTH, canConnectTo(world, pos, Direction.NORTH))
                    .with(SOUTH, canConnectTo(world, pos, Direction.SOUTH))
                    .with(EAST, canConnectTo(world, pos, Direction.EAST))
                    .with(WEST, canConnectTo(world, pos, Direction.WEST))
                    .with(UP, canConnectTo(world, pos, Direction.UP))
                    .with(DOWN, canConnectTo(world, pos, Direction.DOWN));
            if (updated != state) {
                world.setBlockState(pos, updated, 3);
            }
        }
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }
}
