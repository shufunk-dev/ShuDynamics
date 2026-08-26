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
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class RoadTransitionRampBlock extends HorizontalFacingBlock {
    public static final MapCodec<RoadTransitionRampBlock> CODEC = createCodec(RoadTransitionRampBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<RampType> RAMP_TYPE = EnumProperty.of("type", RampType.class);

    public enum RampType implements StringIdentifiable {
        GROUND("ground"),
        ROAD("road");

        private final String name;

        RampType(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }

    // Ground Ramp Shapes (0 to 8px height)
    private static final VoxelShape SHAPE_GROUND_NORTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 12, 16, 2, 16),
            Block.createCuboidShape(0, 0, 8, 16, 4, 12),
            Block.createCuboidShape(0, 0, 4, 16, 6, 8),
            Block.createCuboidShape(0, 0, 0, 16, 8, 4)
    );
    private static final VoxelShape SHAPE_GROUND_SOUTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 2, 4),
            Block.createCuboidShape(0, 0, 4, 16, 4, 8),
            Block.createCuboidShape(0, 0, 8, 16, 6, 12),
            Block.createCuboidShape(0, 0, 12, 16, 8, 16)
    );
    private static final VoxelShape SHAPE_GROUND_WEST = VoxelShapes.union(
            Block.createCuboidShape(12, 0, 0, 16, 2, 16),
            Block.createCuboidShape(8, 0, 0, 12, 4, 16),
            Block.createCuboidShape(4, 0, 0, 8, 6, 16),
            Block.createCuboidShape(0, 0, 0, 4, 8, 16)
    );
    private static final VoxelShape SHAPE_GROUND_EAST = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 4, 2, 16),
            Block.createCuboidShape(4, 0, 0, 8, 4, 16),
            Block.createCuboidShape(8, 0, 0, 12, 6, 16),
            Block.createCuboidShape(12, 0, 0, 16, 8, 16)
    );

    // Road Ramp Shapes (0 to 8px solid base + 8 to 16px ramp slope)
    private static final VoxelShape SHAPE_ROAD_NORTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16),
            Block.createCuboidShape(0, 8, 12, 16, 10, 16),
            Block.createCuboidShape(0, 8, 8, 16, 12, 12),
            Block.createCuboidShape(0, 8, 4, 16, 14, 8),
            Block.createCuboidShape(0, 8, 0, 16, 16, 4)
    );
    private static final VoxelShape SHAPE_ROAD_SOUTH = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16),
            Block.createCuboidShape(0, 8, 0, 16, 10, 4),
            Block.createCuboidShape(0, 8, 4, 16, 12, 8),
            Block.createCuboidShape(0, 8, 8, 16, 14, 12),
            Block.createCuboidShape(0, 8, 12, 16, 16, 16)
    );
    private static final VoxelShape SHAPE_ROAD_WEST = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16),
            Block.createCuboidShape(12, 8, 0, 16, 10, 16),
            Block.createCuboidShape(8, 8, 0, 12, 12, 16),
            Block.createCuboidShape(4, 8, 0, 8, 14, 16),
            Block.createCuboidShape(0, 8, 0, 4, 16, 16)
    );
    private static final VoxelShape SHAPE_ROAD_EAST = VoxelShapes.union(
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
                .with(RAMP_TYPE, RampType.GROUND));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction dir = state.get(FACING);
        boolean isRoad = state.get(RAMP_TYPE) == RampType.ROAD;
        if (isRoad) {
            return switch (dir) {
                case SOUTH -> SHAPE_ROAD_SOUTH;
                case WEST -> SHAPE_ROAD_WEST;
                case EAST -> SHAPE_ROAD_EAST;
                default -> SHAPE_ROAD_NORTH;
            };
        } else {
            return switch (dir) {
                case SOUTH -> SHAPE_GROUND_SOUTH;
                case WEST -> SHAPE_GROUND_WEST;
                case EAST -> SHAPE_GROUND_EAST;
                default -> SHAPE_GROUND_NORTH;
            };
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        Direction playerFacing = ctx.getHorizontalPlayerFacing().getOpposite();

        // If placing on top of an asphalt slab or block, auto-detect ROAD ramp mode
        BlockState belowState = ctx.getWorld().getBlockState(pos.down());
        boolean onAsphalt = belowState.isOf(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB)
                || belowState.isOf(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK)
                || ctx.getWorld().getBlockState(pos).isOf(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB);

        RampType type = onAsphalt ? RampType.ROAD : RampType.GROUND;

        return this.getDefaultState()
                .with(FACING, playerFacing)
                .with(RAMP_TYPE, type);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, RAMP_TYPE);
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
