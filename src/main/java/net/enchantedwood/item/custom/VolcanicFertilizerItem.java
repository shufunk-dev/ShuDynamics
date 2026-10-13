package net.enchantedwood.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class VolcanicFertilizerItem extends Item {
    public VolcanicFertilizerItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        BlockPos targetPos = pos;
        BlockState targetState = state;
        Block targetBlock = block;

        // If clicked on Farmland, Dirt, or Soul Sand, check the crop directly above it
        if (!(targetBlock instanceof CropBlock) && !(targetBlock instanceof BonemealableBlock)) {
            BlockPos abovePos = pos.above();
            BlockState aboveState = world.getBlockState(abovePos);
            if (aboveState.getBlock() instanceof CropBlock || aboveState.getBlock() instanceof BonemealableBlock) {
                targetPos = abovePos;
                targetState = aboveState;
                targetBlock = aboveState.getBlock();
            }
        }

        // 1. Direct handling for CropBlocks (including Mystical Agriculture crops):
        // Bypass vanilla bonemeal restrictions so high-tier crops instantly grow to max maturity!
        if (targetBlock instanceof CropBlock crop) {
            if (!crop.isMaxAge(targetState)) {
                if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
                    world.setBlock(targetPos, crop.getStateForAge(crop.getMaxAge()), 3);

                    // Spurt rich volcanic and happy villager particles
                    serverWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 15, 0.4, 0.4, 0.4, 0.05);
                    serverWorld.sendParticles(ParticleTypes.LAVA, targetPos.getX() + 0.5, targetPos.getY() + 0.3, targetPos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);

                    world.playSound(null, targetPos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0f, 1.2f);
                    world.playSound(null, targetPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.8f);

                    if (player != null && !player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 2. Generic handling for other BonemealableBlocks (saplings, bushes, sweet berries, etc.)
        if (targetBlock instanceof BonemealableBlock fertilizable) {
            if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
                if (fertilizable.isValidBonemealTarget(world, targetPos, targetState, net.minecraft.world.level.block.BonemealSource.INTERACTION)) {
                    for (int i = 0; i < 15; i++) {
                        BlockState currentState = world.getBlockState(targetPos);
                        if (currentState.getBlock() instanceof CropBlock c && c.isMaxAge(currentState)) break;
                        if (currentState.getBlock() instanceof BonemealableBlock f) {
                            if (f.isBonemealSuccess(world, world.getRandom(), targetPos, currentState, net.minecraft.world.level.block.BonemealSource.INTERACTION)) {
                                f.performBonemeal(serverWorld, world.getRandom(), targetPos, currentState, net.minecraft.world.level.block.BonemealSource.INTERACTION);
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }

                    serverWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 15, 0.4, 0.4, 0.4, 0.05);
                    serverWorld.sendParticles(ParticleTypes.LAVA, targetPos.getX() + 0.5, targetPos.getY() + 0.3, targetPos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);

                    world.playSound(null, targetPos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0f, 1.2f);
                    world.playSound(null, targetPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.8f);

                    if (player != null && !player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            } else {
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
