package net.enchantedwood.block.custom;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class VolcanicSoilBlock extends Block {
    public VolcanicSoilBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hit.getSide() == Direction.UP) {
            BlockPos cropPos = pos.up();
            if (world.getBlockState(cropPos).isAir()) {
                BlockState toPlant = null;
                Item item = stack.getItem();

                if (item == Items.WHEAT_SEEDS) toPlant = Blocks.WHEAT.getDefaultState();
                else if (item == Items.CARROT) toPlant = Blocks.CARROTS.getDefaultState();
                else if (item == Items.POTATO) toPlant = Blocks.POTATOES.getDefaultState();
                else if (item == Items.BEETROOT_SEEDS) toPlant = Blocks.BEETROOTS.getDefaultState();
                else if (item == ModItems.CORN_SEEDS) toPlant = ModBlocks.CORN_CROP.getDefaultState();
                else if (item == Items.PUMPKIN_SEEDS) toPlant = Blocks.PUMPKIN_STEM.getDefaultState();
                else if (item == Items.MELON_SEEDS) toPlant = Blocks.MELON_STEM.getDefaultState();
                else if (item == Items.TORCHFLOWER_SEEDS) toPlant = Blocks.TORCHFLOWER_CROP.getDefaultState();
                else if (item == Items.PITCHER_POD) toPlant = Blocks.PITCHER_CROP.getDefaultState();
                else if (item instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();
                    if (block instanceof PlantBlock || block instanceof SaplingBlock || block instanceof FlowerBlock) {
                        toPlant = block.getDefaultState();
                    }
                }

                if (toPlant != null) {
                    if (!world.isClient()) {
                        world.setBlockState(cropPos, toPlant, Block.NOTIFY_ALL);
                        world.playSound(null, cropPos, SoundEvents.ITEM_CROP_PLANT, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        if (!player.isCreative()) {
                            stack.decrement(1);
                        }
                    }
                    return ActionResult.SUCCESS;
                }
            }
        }
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);

        BlockPos cropPos = pos.up();
        BlockState cropState = world.getBlockState(cropPos);
        Block cropBlock = cropState.getBlock();

        // 1. Fertilizable Crops (Wheat, Carrots, Potatoes, Corn, Beetroots, Saplings, Pitcher, Torchflower)
        if (cropBlock instanceof Fertilizable fertilizable) {
            if (fertilizable.isFertilizable(world, cropPos, cropState) && fertilizable.canGrow(world, random, cropPos, cropState)) {
                fertilizable.grow(world, random, cropPos, cropState);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5, cropPos.getY() + 0.3, cropPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
            }
        } else if (cropBlock instanceof CropBlock crop) {
            if (!crop.isMature(cropState)) {
                world.setBlockState(cropPos, crop.withAge(crop.getAge(cropState) + 1), Block.NOTIFY_ALL);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5, cropPos.getY() + 0.3, cropPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
            }
        } else if (cropBlock instanceof StemBlock) {
            int age = cropState.get(StemBlock.AGE);
            if (age < 7) {
                world.setBlockState(cropPos, cropState.with(StemBlock.AGE, age + 1), Block.NOTIFY_ALL);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5, cropPos.getY() + 0.3, cropPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }
}
