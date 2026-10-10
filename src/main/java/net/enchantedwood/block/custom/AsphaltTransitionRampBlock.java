package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AsphaltTransitionRampBlock extends RoadTransitionRampBlock {

    public AsphaltTransitionRampBlock(Properties settings) {
        super(settings);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide() && entity instanceof LivingEntity living) {
            // Native continuous speed boost on asphalt
            living.addEffect(new MobEffectInstance(MobEffects.SPEED, 20, 0, false, false, true));
        }
        super.stepOn(world, pos, state, entity);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        // Facing in the direction the player is looking, so the ramp rises forward towards target
        Direction playerFacing = ctx.getHorizontalDirection();

        // Check the block in front (where the ramp is rising towards)
        BlockPos frontPos = pos.relative(playerFacing);
        BlockState frontState = ctx.getLevel().getBlockState(frontPos);

        // Check the block below
        BlockState belowState = ctx.getLevel().getBlockState(pos.below());

        // Auto-detect ROAD mode (8px to 16px) if:
        // 1. Placing directly against an elevated Asphalt Block, full block, or road deck
        // 2. Placing on top of an asphalt slab
        // 3. Or clicking on the upper half of a side face
        boolean isConnectedToFullBlock = frontState.is(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK)
                || frontState.isSolidRender();

        boolean onSlab = belowState.is(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB);

        // If the block in front is an existing ramp of type ROAD facing the same way, this ramp should be GROUND (0 to 8px)
        boolean inFrontIsRoadRamp = (frontState.getBlock() instanceof RoadTransitionRampBlock)
                && frontState.getValue(FACING) == playerFacing
                && frontState.getValue(RAMP_TYPE) == RampType.ROAD;

        RampType type = RampType.GROUND;
        if ((isConnectedToFullBlock || onSlab) && !inFrontIsRoadRamp) {
            type = RampType.ROAD;
        }

        // Sneak during placement to manually invert the detected type
        if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
            type = (type == RampType.ROAD) ? RampType.GROUND : RampType.ROAD;
        }

        return this.defaultBlockState()
                .setValue(FACING, playerFacing)
                .setValue(RAMP_TYPE, type);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.getMainHandItem().isEmpty()) {
            if (!world.isClientSide()) {
                if (player.isShiftKeyDown()) {
                    // Sneak + empty hand: toggle between GROUND (0-8px) and ROAD (8-16px)
                    RampType newType = state.getValue(RAMP_TYPE) == RampType.GROUND ? RampType.ROAD : RampType.GROUND;
                    world.setBlock(pos, state.setValue(RAMP_TYPE, newType), 3);
                    world.playSound(null, pos, SoundType.STONE.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.2f);
                } else {
                    // Empty hand: rotate ramp facing 90 degrees clockwise
                    Direction newFacing = state.getValue(FACING).getClockWise();
                    world.setBlock(pos, state.setValue(FACING, newFacing), 3);
                    world.playSound(null, pos, SoundType.STONE.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }
}
