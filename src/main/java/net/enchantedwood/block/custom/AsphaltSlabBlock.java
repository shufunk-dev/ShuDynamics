package net.enchantedwood.block.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;

public class AsphaltSlabBlock extends SlabBlock {
    public AsphaltSlabBlock(Properties settings) {
        super(settings);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide()) {
            if (entity instanceof LivingEntity living) {
                // Give a subtle continuous speed boost when running on asphalt roads
                living.addEffect(new MobEffectInstance(MobEffects.SPEED, 20, 0, false, false, true));
            }
        }
        super.stepOn(world, pos, state, entity);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(net.enchantedwood.block.ModBlocks.ROAD_TRANSITION_RAMP.asItem())) {
            if (!world.isClientSide()) {
                Direction facing = player.getDirection().getOpposite();
                BlockState rampState = net.enchantedwood.block.ModBlocks.ROAD_TRANSITION_RAMP.defaultBlockState()
                        .setValue(RoadTransitionRampBlock.FACING, facing)
                        .setValue(RoadTransitionRampBlock.RAMP_TYPE, RoadTransitionRampBlock.RampType.ROAD);
                world.setBlock(pos, rampState, 3);
                world.playSound(null, pos, SoundType.STONE.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(net.enchantedwood.block.ModBlocks.ASPHALT_TRANSITION_RAMP.asItem())) {
            if (!world.isClientSide()) {
                Direction facing = player.getDirection().getOpposite();
                BlockState rampState = net.enchantedwood.block.ModBlocks.ASPHALT_TRANSITION_RAMP.defaultBlockState()
                        .setValue(RoadTransitionRampBlock.FACING, facing)
                        .setValue(RoadTransitionRampBlock.RAMP_TYPE, RoadTransitionRampBlock.RampType.ROAD);
                world.setBlock(pos, rampState, 3);
                world.playSound(null, pos, SoundType.STONE.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandItem();
            if (AsphaltBlock.isIronOrBetterPickaxe(tool)) {
                int slabCount = state.getValue(TYPE) == SlabType.DOUBLE ? 2 : 1;
                if (AsphaltBlock.hasSilkTouch(tool) || world.getRandom().nextBoolean()) {
                    // 50% chance: Drop Asphalt Slab intact
                    popResource(world, pos, new ItemStack(this, slabCount));
                } else {
                    // 50% chance: Reverts into Mineral Tar
                    popResource(world, pos, new ItemStack(ModItems.MINERAL_TAR, slabCount));
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
