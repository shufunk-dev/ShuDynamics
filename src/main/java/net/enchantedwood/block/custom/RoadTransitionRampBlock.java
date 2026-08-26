package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SlabBlock;
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

    // Bottom Half Ramps (0..8px) - for ground level entry up to slab
    private static final VoxelShape RAMP_NORTH_BOTTOM = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 12, 16, 2, 16),
            Block.createCuboidShape(0, 0, 8, 16, 4, 12),
            Block.createCuboidShape(0, 0, 4, 16, 6, 8),
            Block.createCuboidShape(0, 0, 0, 16, 8, 4)
    );
    private static final VoxelShape RAMP_SOUTH_BOTTOM = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 2, 4),
            Block.createCuboidShape(0, 0, 4, 16, 4, 8),
            Block.createCuboidShape(0, 0, 8, 16, 6, 12),
            Block.createCuboidShape(0, 0, 12, 16, 8, 16)
    );
    private static final VoxelShape RAMP_WEST_BOTTOM = VoxelShapes.union(
            Block.createCuboidShape(12, 0, 0, 16, 2, 16),
            Block.createCuboidShape(8, 0, 0, 12, 4, 16),
            Block.createCuboidShape(4, 0, 0, 8, 6, 16),
            Block.createCuboidShape(0, 0, 0, 4, 8, 16)
    );
    private static final VoxelShape RAMP_EAST_BOTTOM = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 4, 2, 16),
            Block.createCuboidShape(4, 0, 0, 8, 4, 16),
            Block.createCuboidShape(8, 0, 0, 12, 6, 16),
            Block.createCuboidShape(12, 0, 0, 16, 8, 16)
    );

    // Top Half Ramps with solid lower base (0..16px) - sits directly on top of bottom slabs/asphalt with NO floating gap!
    private static final VoxelShape RAMP_NORTH_TOP = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16), // Solid foundation filling the slab space
            Block.createCuboidShape(0, 8, 12, 16, 10, 16),
            Block.createCuboidShape(0, 8, 8, 16, 12, 12),
            Block.createCuboidShape(0, 8, 4, 16, 14, 8),
            Block.createCuboidShape(0, 8, 0, 16, 16, 4)
    );
    private static final VoxelShape RAMP_SOUTH_TOP = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16),
            Block.createCuboidShape(0, 8, 0, 16, 10, 4),
            Block.createCuboidShape(0, 8, 4, 16, 12, 8),
            Block.createCuboidShape(0, 8, 8, 16, 14, 12),
            Block.createCuboidShape(0, 8, 12, 16, 16, 16)
    );
    private static final VoxelShape RAMP_WEST_TOP = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16),
            Block.createCuboidShape(12, 8, 0, 16, 10, 16),
            Block.createCuboidShape(8, 8, 0, 12, 12, 16),
            Block.createCuboidShape(4, 8, 0, 8, 14, 16),
            Block.createCuboidShape(0, 8, 0, 4, 16, 16)
    );
    private static final VoxelShape RAMP_EAST_TOP = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16),
            Block.createCuboidShape(0, 8, 0, 4, 10, 16),
            Block.createCuboidShape(4, 8, 0, 8, 12, 16),
            Block.createCuboidShape(8, 8, 0, 12, 14, 16),
            Block.createCuboidShape(12, 8, 0, 16, 16, 16)
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
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        BlockPos pos = ctx.getBlockPos();
        BlockState stateBelow = ctx.getWorld().getBlockState(pos.down());
        double hitY = ctx.getHitPos().y - (double) pos.getY();

        // If placed directly on top of an asphalt slab or any half slab, use TOP mode (solid base + slope)
        boolean isOverSlab = stateBelow.getBlock() instanceof SlabBlock 
                || stateBelow.getBlock() instanceof AsphaltSlabBlock
                || (side == Direction.UP && (ctx.getWorld().getBlockState(pos).isAir() && (stateBelow.getBlock() instanceof SlabBlock || stateBelow.getBlock() instanceof AsphaltSlabBlock)));

        BlockHalf half = (isOverSlab || side == Direction.DOWN || (side != Direction.UP && hitY > 0.5)) 
                ? BlockHalf.TOP 
                : BlockHalf.BOTTOM;

        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing())
                .with(HALF, half);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean isTop = state.get(HALF) == BlockHalf.TOP;
        return switch (state.get(FACING)) {
            case SOUTH -> isTop ? RAMP_SOUTH_TOP : RAMP_SOUTH_BOTTOM;
            case WEST -> isTop ? RAMP_WEST_TOP : RAMP_WEST_BOTTOM;
            case EAST -> isTop ? RAMP_EAST_TOP : RAMP_EAST_BOTTOM;
            default -> isTop ? RAMP_NORTH_TOP : RAMP_NORTH_BOTTOM;
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
