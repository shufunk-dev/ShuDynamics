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

public class RoadTransitionRampBlock extends HorizontalFacingBlock {
    public static final MapCodec<RoadTransitionRampBlock> CODEC = createCodec(RoadTransitionRampBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    // Smooth 0-to-8px wedge ramp matching Asphalt Slabs
    private static final VoxelShape RAMP_NORTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 12, 16, 2, 16),
            Block.createCuboidShape(0, 0, 8, 16, 4, 12),
            Block.createCuboidShape(0, 0, 4, 16, 6, 8),
            Block.createCuboidShape(0, 0, 0, 16, 8, 4)
    );
    private static final VoxelShape RAMP_SOUTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 2, 4),
            Block.createCuboidShape(0, 0, 4, 16, 4, 8),
            Block.createCuboidShape(0, 0, 8, 16, 6, 12),
            Block.createCuboidShape(0, 0, 12, 16, 8, 16)
    );
    private static final VoxelShape RAMP_WEST = VoxelShapes.union(
            Block.createCuboidShape(12, 0, 0, 16, 2, 16),
            Block.createCuboidShape(8, 0, 0, 12, 4, 16),
            Block.createCuboidShape(4, 0, 0, 8, 6, 16),
            Block.createCuboidShape(0, 0, 0, 4, 8, 16)
    );
    private static final VoxelShape RAMP_EAST = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 4, 2, 16),
            Block.createCuboidShape(4, 0, 0, 8, 4, 16),
            Block.createCuboidShape(8, 0, 0, 12, 6, 16),
            Block.createCuboidShape(12, 0, 0, 16, 8, 16)
    );

    public RoadTransitionRampBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> RAMP_SOUTH;
            case WEST -> RAMP_WEST;
            case EAST -> RAMP_EAST;
            default -> RAMP_NORTH;
        };
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
