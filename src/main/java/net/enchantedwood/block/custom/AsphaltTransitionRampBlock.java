package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class AsphaltTransitionRampBlock extends RoadTransitionRampBlock {
    public static final MapCodec<AsphaltTransitionRampBlock> CODEC = createCodec(AsphaltTransitionRampBlock::new);

    public AsphaltTransitionRampBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends RoadTransitionRampBlock> getCodec() {
        return CODEC;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient() && entity instanceof LivingEntity living) {
            // Native continuous speed boost on asphalt
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 0, false, false, true));
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        // Facing in the direction the player is looking, so the ramp rises forward towards target
        Direction playerFacing = ctx.getHorizontalPlayerFacing();

        // Check the block in front (where the ramp is rising towards)
        BlockPos frontPos = pos.offset(playerFacing);
        BlockState frontState = ctx.getWorld().getBlockState(frontPos);

        // Check the block below
        BlockState belowState = ctx.getWorld().getBlockState(pos.down());

        // Auto-detect ROAD mode (8px to 16px) if:
        // 1. Placing directly against an elevated Asphalt Block, full block, or road deck
        // 2. Placing on top of an asphalt slab
        // 3. Or clicking on the upper half of a side face
        boolean isConnectedToFullBlock = frontState.isOf(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK)
                || frontState.isOpaqueFullCube();

        boolean onSlab = belowState.isOf(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB);

        // If the block in front is an existing ramp of type ROAD facing the same way, this ramp should be GROUND (0 to 8px)
        boolean inFrontIsRoadRamp = (frontState.getBlock() instanceof RoadTransitionRampBlock)
                && frontState.get(FACING) == playerFacing
                && frontState.get(RAMP_TYPE) == RampType.ROAD;

        RampType type = RampType.GROUND;
        if ((isConnectedToFullBlock || onSlab) && !inFrontIsRoadRamp) {
            type = RampType.ROAD;
        }

        // Sneak during placement to manually invert the detected type
        if (ctx.getPlayer() != null && ctx.getPlayer().isSneaking()) {
            type = (type == RampType.ROAD) ? RampType.GROUND : RampType.ROAD;
        }

        return this.getDefaultState()
                .with(FACING, playerFacing)
                .with(RAMP_TYPE, type);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.getMainHandStack().isEmpty()) {
            if (!world.isClient()) {
                if (player.isSneaking()) {
                    // Sneak + empty hand: toggle between GROUND (0-8px) and ROAD (8-16px)
                    RampType newType = state.get(RAMP_TYPE) == RampType.GROUND ? RampType.ROAD : RampType.GROUND;
                    world.setBlockState(pos, state.with(RAMP_TYPE, newType), 3);
                    world.playSound(null, pos, BlockSoundGroup.STONE.getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.2f);
                } else {
                    // Empty hand: rotate ramp facing 90 degrees clockwise
                    Direction newFacing = state.get(FACING).rotateYClockwise();
                    world.setBlockState(pos, state.with(FACING, newFacing), 3);
                    world.playSound(null, pos, BlockSoundGroup.STONE.getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
            }
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hit);
    }
}
