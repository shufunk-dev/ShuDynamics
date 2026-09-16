package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
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

public class ConcreteCurbRampBlock extends HorizontalFacingBlock {
    public static final MapCodec<ConcreteCurbRampBlock> CODEC = createCodec(ConcreteCurbRampBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    // 8-step micro-voxel ramp collision shapes (0 to 16px high)
    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 2, 16),
            Block.createCuboidShape(0, 2, 0, 16, 4, 14),
            Block.createCuboidShape(0, 4, 0, 16, 6, 12),
            Block.createCuboidShape(0, 6, 0, 16, 8, 10),
            Block.createCuboidShape(0, 8, 0, 16, 10, 8),
            Block.createCuboidShape(0, 10, 0, 16, 12, 6),
            Block.createCuboidShape(0, 12, 0, 16, 14, 4),
            Block.createCuboidShape(0, 14, 0, 16, 16, 2)
    );

    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 2, 16),
            Block.createCuboidShape(0, 2, 2, 16, 4, 16),
            Block.createCuboidShape(0, 4, 4, 16, 6, 16),
            Block.createCuboidShape(0, 6, 6, 16, 8, 16),
            Block.createCuboidShape(0, 8, 8, 16, 10, 16),
            Block.createCuboidShape(0, 10, 10, 16, 12, 16),
            Block.createCuboidShape(0, 12, 12, 16, 14, 16),
            Block.createCuboidShape(0, 14, 14, 16, 16, 16)
    );

    private static final VoxelShape SHAPE_WEST = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 2, 16),
            Block.createCuboidShape(0, 2, 0, 14, 4, 16),
            Block.createCuboidShape(0, 4, 0, 12, 6, 16),
            Block.createCuboidShape(0, 6, 0, 10, 8, 16),
            Block.createCuboidShape(0, 8, 0, 8, 10, 16),
            Block.createCuboidShape(0, 10, 0, 6, 12, 16),
            Block.createCuboidShape(0, 12, 0, 4, 14, 16),
            Block.createCuboidShape(0, 14, 0, 2, 16, 16)
    );

    private static final VoxelShape SHAPE_EAST = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 2, 16),
            Block.createCuboidShape(2, 2, 0, 16, 4, 16),
            Block.createCuboidShape(4, 4, 0, 16, 6, 16),
            Block.createCuboidShape(6, 6, 0, 16, 8, 16),
            Block.createCuboidShape(8, 8, 0, 16, 10, 16),
            Block.createCuboidShape(10, 10, 0, 16, 12, 16),
            Block.createCuboidShape(12, 12, 0, 16, 14, 16),
            Block.createCuboidShape(14, 14, 0, 16, 16, 16)
    );

    public ConcreteCurbRampBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
