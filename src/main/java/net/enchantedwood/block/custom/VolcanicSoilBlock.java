package net.enchantedwood.block.custom;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class VolcanicSoilBlock extends Block {
    public VolcanicSoilBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hit.getDirection() == Direction.UP) {
            BlockPos cropPos = pos.above();
            if (world.getBlockState(cropPos).isAir()) {
                BlockState toPlant = null;
                Item item = stack.getItem();

                if (item == Items.WHEAT_SEEDS) toPlant = Blocks.WHEAT.defaultBlockState();
                else if (item == Items.CARROT) toPlant = Blocks.CARROTS.defaultBlockState();
                else if (item == Items.POTATO) toPlant = Blocks.POTATOES.defaultBlockState();
                else if (item == Items.BEETROOT_SEEDS) toPlant = Blocks.BEETROOTS.defaultBlockState();
                else if (item == ModItems.CORN_SEEDS) toPlant = ModBlocks.CORN_CROP.defaultBlockState();
                else if (item == ModItems.RICE_SEEDS) toPlant = ModBlocks.RICE_CROP.defaultBlockState();
                else if (item == ModItems.CUCUMBER_SEEDS) toPlant = ModBlocks.CUCUMBER_CROP.defaultBlockState();
                else if (item == Items.PUMPKIN_SEEDS) toPlant = Blocks.PUMPKIN_STEM.defaultBlockState();
                else if (item == Items.MELON_SEEDS) toPlant = Blocks.MELON_STEM.defaultBlockState();
                else if (item == Items.TORCHFLOWER_SEEDS) toPlant = Blocks.TORCHFLOWER_CROP.defaultBlockState();
                else if (item == Items.PITCHER_POD) toPlant = Blocks.PITCHER_CROP.defaultBlockState();
                else if (item instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();
                    if (block instanceof VegetationBlock || block instanceof SaplingBlock || block instanceof FlowerBlock) {
                        toPlant = block.defaultBlockState();
                    }
                }

                if (toPlant != null) {
                    if (!world.isClientSide()) {
                        world.setBlock(cropPos, toPlant, Block.UPDATE_ALL);
                        world.playSound(null, cropPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        super.randomTick(state, world, pos, random);

        BlockPos cropPos = pos.above();
        BlockState cropState = world.getBlockState(cropPos);
        Block cropBlock = cropState.getBlock();

        // 1. Fertilizable Crops (Wheat, Carrots, Potatoes, Corn, Beetroots, Saplings, Pitcher, Torchflower)
        if (cropBlock instanceof BonemealableBlock fertilizable) {
            if (fertilizable.isValidBonemealTarget(world, cropPos, cropState, net.minecraft.world.level.block.BonemealSource.INTERACTION) && fertilizable.isBonemealSuccess(world, random, cropPos, cropState, net.minecraft.world.level.block.BonemealSource.INTERACTION)) {
                fertilizable.performBonemeal(world, random, cropPos, cropState, net.minecraft.world.level.block.BonemealSource.INTERACTION);
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5, cropPos.getY() + 0.3, cropPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
            }
        } else if (cropBlock instanceof CropBlock crop) {
            if (!crop.isMaxAge(cropState)) {
                world.setBlock(cropPos, crop.getStateForAge(crop.getAge(cropState) + 1), Block.UPDATE_ALL);
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5, cropPos.getY() + 0.3, cropPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
            }
        } else if (cropBlock instanceof StemBlock) {
            int age = cropState.getValue(StemBlock.AGE);
            if (age < 7) {
                world.setBlock(cropPos, cropState.setValue(StemBlock.AGE, age + 1), Block.UPDATE_ALL);
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5, cropPos.getY() + 0.3, cropPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }
}
