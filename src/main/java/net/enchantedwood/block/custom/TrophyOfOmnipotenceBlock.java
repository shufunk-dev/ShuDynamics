package net.enchantedwood.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrophyOfOmnipotenceBlock extends Block {

    // Pedestal and Trophy Cup VoxelShape
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public TrophyOfOmnipotenceBlock(Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.8 + random.nextDouble() * 0.4;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            world.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.02, 0.0);
            world.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.01, 0.0);
        }
    }
}
