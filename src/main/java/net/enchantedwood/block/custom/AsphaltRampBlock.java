package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

public class AsphaltRampBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 8-step micro-voxel ramp collision shapes (0 to 16px high)
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 2, 0, 16, 4, 14),
            Block.box(0, 4, 0, 16, 6, 12),
            Block.box(0, 6, 0, 16, 8, 10),
            Block.box(0, 8, 0, 16, 10, 8),
            Block.box(0, 10, 0, 16, 12, 6),
            Block.box(0, 12, 0, 16, 14, 4),
            Block.box(0, 14, 0, 16, 16, 2)
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 2, 2, 16, 4, 16),
            Block.box(0, 4, 4, 16, 6, 16),
            Block.box(0, 6, 6, 16, 8, 16),
            Block.box(0, 8, 8, 16, 10, 16),
            Block.box(0, 10, 10, 16, 12, 16),
            Block.box(0, 12, 12, 16, 14, 16),
            Block.box(0, 14, 14, 16, 16, 16)
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 2, 0, 14, 4, 16),
            Block.box(0, 4, 0, 12, 6, 16),
            Block.box(0, 6, 0, 10, 8, 16),
            Block.box(0, 8, 0, 8, 10, 16),
            Block.box(0, 10, 0, 6, 12, 16),
            Block.box(0, 12, 0, 4, 14, 16),
            Block.box(0, 14, 0, 2, 16, 16)
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 2, 0, 16, 4, 16),
            Block.box(4, 4, 0, 16, 6, 16),
            Block.box(6, 6, 0, 16, 8, 16),
            Block.box(8, 8, 0, 16, 10, 16),
            Block.box(10, 10, 0, 16, 12, 16),
            Block.box(12, 12, 0, 16, 14, 16),
            Block.box(14, 14, 0, 16, 16, 16)
    );

    public AsphaltRampBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SPEED, 20, 0, false, false, true));
        }
        super.stepOn(world, pos, state, entity);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level world, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (player.getMainHandItem().isEmpty()) {
            if (!world.isClientSide()) {
                Direction newFacing = state.getValue(FACING).getClockWise();
                world.setBlock(pos, state.setValue(FACING, newFacing), 3);
                world.playSound(null, pos, net.minecraft.world.level.block.SoundType.STONE.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
