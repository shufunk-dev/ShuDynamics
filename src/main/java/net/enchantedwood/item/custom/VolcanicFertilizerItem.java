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

        if (block instanceof BonemealableBlock fertilizable) {
            if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
                // Check if fertilizable
                if (fertilizable.isValidBonemealTarget(world, pos, state, net.minecraft.world.level.block.BonemealSource.INTERACTION)) {
                    // Universal instant growth: cycle until fully grown or max age
                    for (int i = 0; i < 15; i++) {
                        BlockState currentState = world.getBlockState(pos);
                        if (currentState.getBlock() instanceof CropBlock crop) {
                            if (crop.isMaxAge(currentState)) break;
                        }
                        if (currentState.getBlock() instanceof BonemealableBlock f) {
                            if (f.isBonemealSuccess(world, world.getRandom(), pos, currentState, net.minecraft.world.level.block.BonemealSource.INTERACTION)) {
                                f.performBonemeal(serverWorld, world.getRandom(), pos, currentState, net.minecraft.world.level.block.BonemealSource.INTERACTION);
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }

                    // Special instant handling for CropBlocks to guarantee max maturity
                    BlockState endState = world.getBlockState(pos);
                    if (endState.getBlock() instanceof CropBlock crop && !crop.isMaxAge(endState)) {
                        world.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 3);
                    }

                    // Spurt rich volcanic and happy villager particles
                    serverWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.4, 0.4, 0.4, 0.05);
                    serverWorld.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0.02);

                    world.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0f, 1.2f);
                    world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.8f);

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
