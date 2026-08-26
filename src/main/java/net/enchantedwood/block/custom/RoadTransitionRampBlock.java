package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class RoadTransitionRampBlock extends HorizontalFacingBlock {
    public static final MapCodec<RoadTransitionRampBlock> CODEC = createCodec(RoadTransitionRampBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;

    // Bottom Half Shapes (0 to 8px)
    private static final VoxelShape SHAPE_BOTTOM_NORTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 4),
            Block.createCuboidShape(0, 0, 4, 16, 6, 8),
            Block.createCuboidShape(0, 0, 8, 16, 4, 12),
            Block.createCuboidShape(0, 0, 12, 16, 2, 16)
    );
    private static final VoxelShape SHAPE_BOTTOM_SOUTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 12, 16, 8, 16),
            Block.createCuboidShape(0, 0, 8, 16, 6, 12),
            Block.createCuboidShape(0, 0, 4, 16, 4, 8),
            Block.createCuboidShape(0, 0, 0, 16, 2, 4)
    );
    private static final VoxelShape SHAPE_BOTTOM_WEST = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 4, 8, 16),
            Block.createCuboidShape(4, 0, 0, 8, 6, 16),
            Block.createCuboidShape(8, 0, 0, 12, 4, 16),
            Block.createCuboidShape(12, 0, 0, 16, 2, 16)
    );
    private static final VoxelShape SHAPE_BOTTOM_EAST = VoxelShapes.union(
            Block.createCuboidShape(12, 0, 0, 16, 8, 16),
            Block.createCuboidShape(8, 0, 0, 12, 6, 16),
            Block.createCuboidShape(4, 0, 0, 8, 4, 16),
            Block.createCuboidShape(0, 0, 0, 4, 2, 16)
    );

    // Top Half Shapes (8px to 16px)
    private static final VoxelShape SHAPE_TOP_NORTH = VoxelShapes.union(
            Block.createCuboidShape(0, 8, 0, 16, 16, 4),
            Block.createCuboidShape(0, 8, 4, 16, 14, 8),
            Block.createCuboidShape(0, 8, 8, 16, 12, 12),
            Block.createCuboidShape(0, 8, 12, 16, 10, 16)
    );
    private static final VoxelShape SHAPE_TOP_SOUTH = VoxelShapes.union(
            Block.createCuboidShape(0, 8, 12, 16, 16, 16),
            Block.createCuboidShape(0, 8, 8, 16, 14, 12),
            Block.createCuboidShape(0, 8, 4, 16, 12, 8),
            Block.createCuboidShape(0, 8, 0, 16, 10, 4)
    );
    private static final VoxelShape SHAPE_TOP_WEST = VoxelShapes.union(
            Block.createCuboidShape(0, 8, 0, 4, 16, 16),
            Block.createCuboidShape(4, 8, 0, 8, 14, 16),
            Block.createCuboidShape(8, 8, 0, 12, 12, 16),
            Block.createCuboidShape(12, 8, 0, 16, 10, 16)
    );
    private static final VoxelShape SHAPE_TOP_EAST = VoxelShapes.union(
            Block.createCuboidShape(12, 8, 0, 16, 16, 16),
            Block.createCuboidShape(8, 8, 0, 12, 14, 16),
            Block.createCuboidShape(4, 8, 0, 8, 12, 16),
            Block.createCuboidShape(0, 8, 0, 4, 10, 16)
    );

    public RoadTransitionRampBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, BlockHalf.BOTTOM));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction dir = state.get(FACING);
        boolean isTop = state.get(HALF) == BlockHalf.TOP;
        if (isTop) {
            return switch (dir) {
                case SOUTH -> SHAPE_TOP_SOUTH;
                case WEST -> SHAPE_TOP_WEST;
                case EAST -> SHAPE_TOP_EAST;
                default -> SHAPE_TOP_NORTH;
            };
        } else {
            return switch (dir) {
                case SOUTH -> SHAPE_BOTTOM_SOUTH;
                case WEST -> SHAPE_BOTTOM_WEST;
                case EAST -> SHAPE_BOTTOM_EAST;
                default -> SHAPE_BOTTOM_NORTH;
            };
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        Direction side = ctx.getSide();
        Direction playerFacing = ctx.getHorizontalPlayerFacing().getOpposite();

        BlockHalf half = BlockHalf.BOTTOM;
        if (side == Direction.DOWN || (side != Direction.UP && ctx.getHitPos().y - (double)pos.getY() > 0.5)) {
            half = BlockHalf.TOP;
        }

        return this.getDefaultState()
                .with(FACING, playerFacing)
                .with(HALF, half);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
}
