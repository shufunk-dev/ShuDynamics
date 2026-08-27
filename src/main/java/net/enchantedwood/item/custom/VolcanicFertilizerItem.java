package net.enchantedwood.item.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VolcanicFertilizerItem extends Item {
    public VolcanicFertilizerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (block instanceof Fertilizable fertilizable) {
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                // Check if fertilizable
                if (fertilizable.isFertilizable(world, pos, state)) {
                    // Universal instant growth: cycle until fully grown or max age
                    for (int i = 0; i < 15; i++) {
                        BlockState currentState = world.getBlockState(pos);
                        if (currentState.getBlock() instanceof CropBlock crop) {
                            if (crop.isMature(currentState)) break;
                        }
                        if (currentState.getBlock() instanceof Fertilizable f) {
                            if (f.canGrow(world, world.random, pos, currentState)) {
                                f.grow(serverWorld, world.random, pos, currentState);
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }

                    // Special instant handling for CropBlocks to guarantee max maturity
                    BlockState endState = world.getBlockState(pos);
                    if (endState.getBlock() instanceof CropBlock crop && !crop.isMature(endState)) {
                        world.setBlockState(pos, crop.withAge(crop.getMaxAge()), 3);
                    }

                    // Spurt rich volcanic and happy villager particles
                    serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.4, 0.4, 0.4, 0.05);
                    serverWorld.spawnParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0.02);

                    world.playSound(null, pos, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1.0f, 1.2f);
                    world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.8f, 1.8f);

                    if (player != null && !player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    return ActionResult.SUCCESS;
                }
            } else {
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }
}
