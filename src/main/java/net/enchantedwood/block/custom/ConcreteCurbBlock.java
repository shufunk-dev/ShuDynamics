package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConcreteCurbBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;

    // Base Half Slab (0 to 8px)
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 8, 16);

    // Straight Shapes (8 to 12px 6px-wide lip)
    private static final VoxelShape SHAPE_STRAIGHT_NORTH = Shapes.or(BASE, Block.box(0, 8, 0, 16, 12, 6));
    private static final VoxelShape SHAPE_STRAIGHT_SOUTH = Shapes.or(BASE, Block.box(0, 8, 10, 16, 12, 16));
    private static final VoxelShape SHAPE_STRAIGHT_WEST  = Shapes.or(BASE, Block.box(0, 8, 0, 6, 12, 16));
    private static final VoxelShape SHAPE_STRAIGHT_EAST  = Shapes.or(BASE, Block.box(10, 8, 0, 16, 12, 16));

    // Inner Corner Shapes (L-shaped raised 12px lip)
    private static final VoxelShape SHAPE_INNER_LEFT_NORTH = Shapes.or(BASE, Block.box(0, 8, 0, 16, 12, 6), Block.box(0, 8, 6, 6, 12, 16));
    private static final VoxelShape SHAPE_INNER_LEFT_SOUTH = Shapes.or(BASE, Block.box(0, 8, 10, 16, 12, 16), Block.box(10, 8, 0, 16, 12, 10));
    private static final VoxelShape SHAPE_INNER_LEFT_WEST  = Shapes.or(BASE, Block.box(0, 8, 0, 6, 12, 16), Block.box(6, 8, 10, 16, 12, 16));
    private static final VoxelShape SHAPE_INNER_LEFT_EAST  = Shapes.or(BASE, Block.box(10, 8, 0, 16, 12, 16), Block.box(0, 8, 0, 10, 12, 6));

    private static final VoxelShape SHAPE_INNER_RIGHT_NORTH = Shapes.or(BASE, Block.box(0, 8, 0, 16, 12, 6), Block.box(10, 8, 6, 16, 12, 16));
    private static final VoxelShape SHAPE_INNER_RIGHT_SOUTH = Shapes.or(BASE, Block.box(0, 8, 10, 16, 12, 16), Block.box(0, 8, 0, 6, 12, 10));
    private static final VoxelShape SHAPE_INNER_RIGHT_WEST  = Shapes.or(BASE, Block.box(0, 8, 0, 6, 12, 16), Block.box(6, 8, 0, 16, 12, 6));
    private static final VoxelShape SHAPE_INNER_RIGHT_EAST  = Shapes.or(BASE, Block.box(10, 8, 0, 16, 12, 16), Block.box(0, 8, 10, 10, 12, 16));

    // Outer Corner Shapes (6x6 corner raised 12px lip)
    private static final VoxelShape SHAPE_OUTER_LEFT_NORTH = Shapes.or(BASE, Block.box(0, 8, 0, 6, 12, 6));
    private static final VoxelShape SHAPE_OUTER_LEFT_SOUTH = Shapes.or(BASE, Block.box(10, 8, 10, 16, 12, 16));
    private static final VoxelShape SHAPE_OUTER_LEFT_WEST  = Shapes.or(BASE, Block.box(0, 8, 10, 6, 12, 16));
    private static final VoxelShape SHAPE_OUTER_LEFT_EAST  = Shapes.or(BASE, Block.box(10, 8, 0, 16, 12, 6));

    private static final VoxelShape SHAPE_OUTER_RIGHT_NORTH = Shapes.or(BASE, Block.box(10, 8, 0, 16, 12, 6));
    private static final VoxelShape SHAPE_OUTER_RIGHT_SOUTH = Shapes.or(BASE, Block.box(0, 8, 10, 6, 12, 16));
    private static final VoxelShape SHAPE_OUTER_RIGHT_WEST  = Shapes.or(BASE, Block.box(0, 8, 0, 6, 12, 6));
    private static final VoxelShape SHAPE_OUTER_RIGHT_EAST  = Shapes.or(BASE, Block.box(10, 8, 10, 16, 12, 16));

    public ConcreteCurbBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, StairsShape.STRAIGHT));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getHorizontalDirection().getOpposite();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = this.defaultBlockState().setValue(FACING, dir);
        return state.setValue(SHAPE, getCurbShape(state, ctx.getLevel(), pos));
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(SHAPE, getCurbShape(state, world, pos));
        }
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    public static StairsShape getCurbShape(BlockState state, BlockGetter world, BlockPos pos) {
        Direction dir = state.getValue(FACING);
        BlockState backState = world.getBlockState(pos.relative(dir));
        if (isCurb(backState)) {
            Direction backDir = backState.getValue(FACING);
            if (backDir.getAxis() != dir.getAxis() && isDifferentOrientation(state, world, pos, backDir.getOpposite())) {
                if (backDir == dir.getCounterClockWise()) {
                    return StairsShape.OUTER_LEFT;
                }
                return StairsShape.OUTER_RIGHT;
            }
        }

        BlockState frontState = world.getBlockState(pos.relative(dir.getOpposite()));
        if (isCurb(frontState)) {
            Direction frontDir = frontState.getValue(FACING);
            if (frontDir.getAxis() != dir.getAxis() && isDifferentOrientation(state, world, pos, frontDir)) {
                if (frontDir == dir.getCounterClockWise()) {
                    return StairsShape.INNER_LEFT;
                }
                return StairsShape.INNER_RIGHT;
            }
        }

        return StairsShape.STRAIGHT;
    }

    private static boolean isDifferentOrientation(BlockState state, BlockGetter world, BlockPos pos, Direction dir) {
        BlockState neighborState = world.getBlockState(pos.relative(dir));
        return !isCurb(neighborState) || neighborState.getValue(FACING) != state.getValue(FACING);
    }

    public static boolean isCurb(BlockState state) {
        return state.getBlock() instanceof ConcreteCurbBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        StairsShape shape = state.getValue(SHAPE);

        return switch (shape) {
            case STRAIGHT -> switch (facing) {
                case SOUTH -> SHAPE_STRAIGHT_SOUTH;
                case WEST  -> SHAPE_STRAIGHT_WEST;
                case EAST  -> SHAPE_STRAIGHT_EAST;
                default    -> SHAPE_STRAIGHT_NORTH;
            };
            case INNER_LEFT -> switch (facing) {
                case SOUTH -> SHAPE_INNER_LEFT_SOUTH;
                case WEST  -> SHAPE_INNER_LEFT_WEST;
                case EAST  -> SHAPE_INNER_LEFT_EAST;
                default    -> SHAPE_INNER_LEFT_NORTH;
            };
            case INNER_RIGHT -> switch (facing) {
                case SOUTH -> SHAPE_INNER_RIGHT_SOUTH;
                case WEST  -> SHAPE_INNER_RIGHT_WEST;
                case EAST  -> SHAPE_INNER_RIGHT_EAST;
                default    -> SHAPE_INNER_RIGHT_NORTH;
            };
            case OUTER_LEFT -> switch (facing) {
                case SOUTH -> SHAPE_OUTER_LEFT_SOUTH;
                case WEST  -> SHAPE_OUTER_LEFT_WEST;
                case EAST  -> SHAPE_OUTER_LEFT_EAST;
                default    -> SHAPE_OUTER_LEFT_NORTH;
            };
            case OUTER_RIGHT -> switch (facing) {
                case SOUTH -> SHAPE_OUTER_RIGHT_SOUTH;
                case WEST  -> SHAPE_OUTER_RIGHT_WEST;
                case EAST  -> SHAPE_OUTER_RIGHT_EAST;
                default    -> SHAPE_OUTER_RIGHT_NORTH;
            };
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction direction = state.getValue(FACING);
        StairsShape stairShape = state.getValue(SHAPE);
        switch (mirror) {
            case LEFT_RIGHT:
                if (direction.getAxis() == Direction.Axis.Z) {
                    switch (stairShape) {
                        case INNER_LEFT -> { return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT); }
                        case INNER_RIGHT -> { return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT); }
                        case OUTER_LEFT -> { return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT); }
                        case OUTER_RIGHT -> { return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT); }
                        default -> { return state.rotate(Rotation.CLOCKWISE_180); }
                    }
                }
                break;
            case FRONT_BACK:
                if (direction.getAxis() == Direction.Axis.X) {
                    switch (stairShape) {
                        case INNER_LEFT -> { return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT); }
                        case INNER_RIGHT -> { return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT); }
                        case OUTER_LEFT -> { return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT); }
                        case OUTER_RIGHT -> { return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT); }
                        case STRAIGHT -> { return state.rotate(Rotation.CLOCKWISE_180); }
                    }
                }
                break;
        }
        return super.mirror(state, mirror);
    }
}
