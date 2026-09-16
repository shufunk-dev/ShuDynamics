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
            // Speed boost on asphalt ramp
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 0, false, false, true));
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        Direction playerFacing = ctx.getHorizontalPlayerFacing().getOpposite();

        BlockState belowState = ctx.getWorld().getBlockState(pos.down());
        boolean onAsphalt = belowState.isOf(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB)
                || belowState.isOf(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK)
                || belowState.isOf(net.enchantedwood.block.ModBlocks.ROAD_TRANSITION_RAMP)
                || belowState.isOf(this)
                || ctx.getWorld().getBlockState(pos).isOf(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB);

        RampType type = onAsphalt ? RampType.ROAD : RampType.GROUND;
        // Sneak to invert placement mode
        if (ctx.getPlayer() != null && ctx.getPlayer().isSneaking()) {
            type = (type == RampType.ROAD) ? RampType.GROUND : RampType.ROAD;
        }

        return this.getDefaultState()
                .with(FACING, playerFacing)
                .with(RAMP_TYPE, type);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.isSneaking() && player.getMainHandStack().isEmpty()) {
            if (!world.isClient()) {
                RampType newType = state.get(RAMP_TYPE) == RampType.GROUND ? RampType.ROAD : RampType.GROUND;
                world.setBlockState(pos, state.with(RAMP_TYPE, newType), 3);
                world.playSound(null, pos, BlockSoundGroup.STONE.getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.2f);
            }
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hit);
    }
}
