package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.TungstenCableBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import org.jetbrains.annotations.Nullable;

public class TungstenCableBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    // VoxelShapes for 6-way pipe connections (core is 6x6x6: 5 to 11)
    private static final VoxelShape CORE_SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape UP_SHAPE = Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape DOWN_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape EAST_SHAPE = Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);

    public TungstenCableBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TungstenCableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.TUNGSTEN_CABLE_BE) {
            return (w, pos, st, blockEntity) -> TungstenCableBlockEntity.tick(serverWorld, pos, st, (TungstenCableBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        return shape;
    }

    public boolean canConnectTo(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof TungstenCableBlock ||
                neighborState.getBlock() instanceof BasaltCableBlock ||
                neighborState.getBlock() instanceof SteelCableBlock ||
                neighborState.getBlock() instanceof AluminumCableBlock ||
                neighborState.getBlock() instanceof CopperCableBlock ||
                neighborState.is(net.enchantedwood.block.ModBlocks.CRUSHER) ||
                neighborState.is(net.enchantedwood.block.ModBlocks.DUST_SMELTER)) {
            return true;
        }
        BlockEntity be = world.getBlockEntity(neighborPos);
        if (be instanceof EnergyProvider provider) {
            return provider.getEnergyStorage(direction.getOpposite()) != null;
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        return this.defaultBlockState()
                .setValue(NORTH, canConnectTo(world, pos, Direction.NORTH))
                .setValue(SOUTH, canConnectTo(world, pos, Direction.SOUTH))
                .setValue(EAST, canConnectTo(world, pos, Direction.EAST))
                .setValue(WEST, canConnectTo(world, pos, Direction.WEST))
                .setValue(UP, canConnectTo(world, pos, Direction.UP))
                .setValue(DOWN, canConnectTo(world, pos, Direction.DOWN));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        return state.setValue(
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
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (!world.isClientSide()) {
            BlockState updated = state
                    .setValue(NORTH, canConnectTo(world, pos, Direction.NORTH))
                    .setValue(SOUTH, canConnectTo(world, pos, Direction.SOUTH))
                    .setValue(EAST, canConnectTo(world, pos, Direction.EAST))
                    .setValue(WEST, canConnectTo(world, pos, Direction.WEST))
                    .setValue(UP, canConnectTo(world, pos, Direction.UP))
                    .setValue(DOWN, canConnectTo(world, pos, Direction.DOWN));
            if (updated != state) {
                world.setBlock(pos, updated, 3);
            }
        }
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }
}
