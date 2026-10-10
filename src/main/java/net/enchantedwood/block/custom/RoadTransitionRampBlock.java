package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RoadTransitionRampBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<RampType> RAMP_TYPE = EnumProperty.create("type", RampType.class);

    public enum RampType implements StringRepresentable {
        GROUND("ground"),
        ROAD("road");

        private final String name;

        RampType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    // Ground Ramp Shapes (0 to 8px height)
    private static final VoxelShape SHAPE_GROUND_NORTH = Shapes.or(
            Block.box(0, 0, 12, 16, 2, 16),
            Block.box(0, 0, 8, 16, 4, 12),
            Block.box(0, 0, 4, 16, 6, 8),
            Block.box(0, 0, 0, 16, 8, 4)
    );
    private static final VoxelShape SHAPE_GROUND_SOUTH = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 4),
            Block.box(0, 0, 4, 16, 4, 8),
            Block.box(0, 0, 8, 16, 6, 12),
            Block.box(0, 0, 12, 16, 8, 16)
    );
    private static final VoxelShape SHAPE_GROUND_WEST = Shapes.or(
            Block.box(12, 0, 0, 16, 2, 16),
            Block.box(8, 0, 0, 12, 4, 16),
            Block.box(4, 0, 0, 8, 6, 16),
            Block.box(0, 0, 0, 4, 8, 16)
    );
    private static final VoxelShape SHAPE_GROUND_EAST = Shapes.or(
            Block.box(0, 0, 0, 4, 2, 16),
            Block.box(4, 0, 0, 8, 4, 16),
            Block.box(8, 0, 0, 12, 6, 16),
            Block.box(12, 0, 0, 16, 8, 16)
    );

    // Road Ramp Shapes (0 to 8px solid base + 8 to 16px ramp slope)
    private static final VoxelShape SHAPE_ROAD_NORTH = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 8, 12, 16, 10, 16),
            Block.box(0, 8, 8, 16, 12, 12),
            Block.box(0, 8, 4, 16, 14, 8),
            Block.box(0, 8, 0, 16, 16, 4)
    );
    private static final VoxelShape SHAPE_ROAD_SOUTH = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 8, 0, 16, 10, 4),
            Block.box(0, 8, 4, 16, 12, 8),
            Block.box(0, 8, 8, 16, 14, 12),
            Block.box(0, 8, 12, 16, 16, 16)
    );
    private static final VoxelShape SHAPE_ROAD_WEST = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(12, 8, 0, 16, 10, 16),
            Block.box(8, 8, 0, 12, 12, 16),
            Block.box(4, 8, 0, 8, 14, 16),
            Block.box(0, 8, 0, 4, 16, 16)
    );
    private static final VoxelShape SHAPE_ROAD_EAST = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 8, 0, 4, 10, 16),
            Block.box(4, 8, 0, 8, 12, 16),
            Block.box(8, 8, 0, 12, 14, 16),
            Block.box(12, 8, 0, 16, 16, 16)
    );

    public RoadTransitionRampBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(RAMP_TYPE, RampType.GROUND));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        boolean isRoad = state.getValue(RAMP_TYPE) == RampType.ROAD;
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
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Direction playerFacing = ctx.getHorizontalDirection();

        // Check the block in front (where the ramp is rising towards)
        BlockPos frontPos = pos.relative(playerFacing);
        BlockState frontState = ctx.getLevel().getBlockState(frontPos);

        // Check the block below
        BlockState belowState = ctx.getLevel().getBlockState(pos.below());

        boolean isConnectedToFullBlock = frontState.is(net.enchantedwood.block.ModBlocks.CONCRETE_CURB)
                || frontState.is(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK)
                || frontState.isSolidRender();

        boolean onSlab = belowState.is(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB);

        boolean inFrontIsRoadRamp = (frontState.getBlock() instanceof RoadTransitionRampBlock)
                && frontState.getValue(FACING) == playerFacing
                && frontState.getValue(RAMP_TYPE) == RampType.ROAD;

        RampType type = RampType.GROUND;
        if ((isConnectedToFullBlock || onSlab) && !inFrontIsRoadRamp) {
            type = RampType.ROAD;
        }

        if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
            type = (type == RampType.ROAD) ? RampType.GROUND : RampType.ROAD;
        }

        return this.defaultBlockState()
                .setValue(FACING, playerFacing)
                .setValue(RAMP_TYPE, type);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level world, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (player.getMainHandItem().isEmpty()) {
            if (!world.isClientSide()) {
                if (player.isShiftKeyDown()) {
                    RampType newType = state.getValue(RAMP_TYPE) == RampType.GROUND ? RampType.ROAD : RampType.GROUND;
                    world.setBlock(pos, state.setValue(RAMP_TYPE, newType), 3);
                    world.playSound(null, pos, net.minecraft.world.level.block.SoundType.STONE.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
                } else {
                    Direction newFacing = state.getValue(FACING).getClockWise();
                    world.setBlock(pos, state.setValue(FACING, newFacing), 3);
                    world.playSound(null, pos, net.minecraft.world.level.block.SoundType.STONE.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RAMP_TYPE);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
