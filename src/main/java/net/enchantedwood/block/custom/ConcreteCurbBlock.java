package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.StairShape;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class ConcreteCurbBlock extends HorizontalFacingBlock {
    public static final MapCodec<ConcreteCurbBlock> CODEC = createCodec(ConcreteCurbBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<StairShape> SHAPE = Properties.STAIR_SHAPE;

    // Base Half Slab (0 to 8px)
    private static final VoxelShape BASE = Block.createCuboidShape(0, 0, 0, 16, 8, 16);

    // Straight Shapes (8 to 12px 6px-wide lip)
    private static final VoxelShape SHAPE_STRAIGHT_NORTH = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 0, 16, 12, 6));
    private static final VoxelShape SHAPE_STRAIGHT_SOUTH = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 10, 16, 12, 16));
    private static final VoxelShape SHAPE_STRAIGHT_WEST  = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 0, 6, 12, 16));
    private static final VoxelShape SHAPE_STRAIGHT_EAST  = VoxelShapes.union(BASE, Block.createCuboidShape(10, 8, 0, 16, 12, 16));

    // Inner Corner Shapes (L-shaped raised 12px lip)
    private static final VoxelShape SHAPE_INNER_LEFT_NORTH = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 0, 16, 12, 6), Block.createCuboidShape(0, 8, 6, 6, 12, 16));
    private static final VoxelShape SHAPE_INNER_LEFT_SOUTH = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 10, 16, 12, 16), Block.createCuboidShape(10, 8, 0, 16, 12, 10));
    private static final VoxelShape SHAPE_INNER_LEFT_WEST  = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 0, 6, 12, 16), Block.createCuboidShape(6, 8, 10, 16, 12, 16));
    private static final VoxelShape SHAPE_INNER_LEFT_EAST  = VoxelShapes.union(BASE, Block.createCuboidShape(10, 8, 0, 16, 12, 16), Block.createCuboidShape(0, 8, 0, 10, 12, 6));

    private static final VoxelShape SHAPE_INNER_RIGHT_NORTH = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 0, 16, 12, 6), Block.createCuboidShape(10, 8, 6, 16, 12, 16));
    private static final VoxelShape SHAPE_INNER_RIGHT_SOUTH = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 10, 16, 12, 16), Block.createCuboidShape(0, 8, 0, 6, 12, 10));
    private static final VoxelShape SHAPE_INNER_RIGHT_WEST  = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 0, 6, 12, 16), Block.createCuboidShape(6, 8, 0, 16, 12, 6));
    private static final VoxelShape SHAPE_INNER_RIGHT_EAST  = VoxelShapes.union(BASE, Block.createCuboidShape(10, 8, 0, 16, 12, 16), Block.createCuboidShape(0, 8, 10, 10, 12, 16));

    // Outer Corner Shapes (6x6 corner raised 12px lip)
    private static final VoxelShape SHAPE_OUTER_LEFT_NORTH = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 0, 6, 12, 6));
    private static final VoxelShape SHAPE_OUTER_LEFT_SOUTH = VoxelShapes.union(BASE, Block.createCuboidShape(10, 8, 10, 16, 12, 16));
    private static final VoxelShape SHAPE_OUTER_LEFT_WEST  = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 10, 6, 12, 16));
    private static final VoxelShape SHAPE_OUTER_LEFT_EAST  = VoxelShapes.union(BASE, Block.createCuboidShape(10, 8, 0, 16, 12, 6));

    private static final VoxelShape SHAPE_OUTER_RIGHT_NORTH = VoxelShapes.union(BASE, Block.createCuboidShape(10, 8, 0, 16, 12, 6));
    private static final VoxelShape SHAPE_OUTER_RIGHT_SOUTH = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 10, 6, 12, 16));
    private static final VoxelShape SHAPE_OUTER_RIGHT_WEST  = VoxelShapes.union(BASE, Block.createCuboidShape(0, 8, 0, 6, 12, 6));
    private static final VoxelShape SHAPE_OUTER_RIGHT_EAST  = VoxelShapes.union(BASE, Block.createCuboidShape(10, 8, 10, 16, 12, 16));

    public ConcreteCurbBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(SHAPE, StairShape.STRAIGHT));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction dir = ctx.getHorizontalPlayerFacing().getOpposite();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = this.getDefaultState().with(FACING, dir);
        return state.with(SHAPE, getCurbShape(state, ctx.getWorld(), pos));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (direction.getAxis().isHorizontal()) {
            return state.with(SHAPE, getCurbShape(state, world, pos));
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    public static StairShape getCurbShape(BlockState state, BlockView world, BlockPos pos) {
        Direction dir = state.get(FACING);
        BlockState backState = world.getBlockState(pos.offset(dir));
        if (isCurb(backState)) {
            Direction backDir = backState.get(FACING);
            if (backDir.getAxis() != dir.getAxis() && isDifferentOrientation(state, world, pos, backDir.getOpposite())) {
                if (backDir == dir.rotateYCounterclockwise()) {
                    return StairShape.OUTER_LEFT;
                }
                return StairShape.OUTER_RIGHT;
            }
        }

        BlockState frontState = world.getBlockState(pos.offset(dir.getOpposite()));
        if (isCurb(frontState)) {
            Direction frontDir = frontState.get(FACING);
            if (frontDir.getAxis() != dir.getAxis() && isDifferentOrientation(state, world, pos, frontDir)) {
                if (frontDir == dir.rotateYCounterclockwise()) {
                    return StairShape.INNER_LEFT;
                }
                return StairShape.INNER_RIGHT;
            }
        }

        return StairShape.STRAIGHT;
    }

    private static boolean isDifferentOrientation(BlockState state, BlockView world, BlockPos pos, Direction dir) {
        BlockState neighborState = world.getBlockState(pos.offset(dir));
        return !isCurb(neighborState) || neighborState.get(FACING) != state.get(FACING);
    }

    public static boolean isCurb(BlockState state) {
        return state.getBlock() instanceof ConcreteCurbBlock;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = state.get(FACING);
        StairShape shape = state.get(SHAPE);

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
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        Direction direction = state.get(FACING);
        StairShape stairShape = state.get(SHAPE);
        switch (mirror) {
            case LEFT_RIGHT:
                if (direction.getAxis() == Direction.Axis.Z) {
                    switch (stairShape) {
                        case INNER_LEFT -> { return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_RIGHT); }
                        case INNER_RIGHT -> { return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_LEFT); }
                        case OUTER_LEFT -> { return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_RIGHT); }
                        case OUTER_RIGHT -> { return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_LEFT); }
                        default -> { return state.rotate(BlockRotation.CLOCKWISE_180); }
                    }
                }
                break;
            case FRONT_BACK:
                if (direction.getAxis() == Direction.Axis.X) {
                    switch (stairShape) {
                        case INNER_LEFT -> { return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_LEFT); }
                        case INNER_RIGHT -> { return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_RIGHT); }
                        case OUTER_LEFT -> { return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_RIGHT); }
                        case OUTER_RIGHT -> { return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_LEFT); }
                        case STRAIGHT -> { return state.rotate(BlockRotation.CLOCKWISE_180); }
                    }
                }
                break;
        }
        return super.mirror(state, mirror);
    }
}
