package net.enchantedwood.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;

public class VolcanicSoilBlock extends Block {
    public VolcanicSoilBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);

        BlockPos cropPos = pos.up();
        BlockState cropState = world.getBlockState(cropPos);

        // Accelerated growth engine: boosts crops directly above volcanic soil
        if (cropState.getBlock() instanceof Fertilizable fertilizable) {
            if (fertilizable.isFertilizable(world, cropPos, cropState) && fertilizable.canGrow(world, random, cropPos, cropState)) {
                fertilizable.grow(world, random, cropPos, cropState);

                // Small gentle mineral sparkle
                if (random.nextInt(3) == 0) {
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5, cropPos.getY() + 0.3, cropPos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }
    }
}
