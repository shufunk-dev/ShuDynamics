package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public class BoulderFeature implements Feature {
    public static final MapCodec<BoulderFeature> CODEC = BoulderConfiguration.CODEC.fieldOf("config")
        .xmap(BoulderFeature::new, BoulderFeature::config);

    private final BoulderConfiguration config;

    public BoulderFeature(BoulderConfiguration config) {
        this.config = config;
    }

    public BoulderConfiguration config() {
        return this.config;
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockState ground = level.getBlockState(origin.below());

        if (!ground.isAir() && ground.canOcclude()) {
            int radius = config.radius().sample(random);
            double rx = radius * (0.8 + random.nextDouble() * 0.4);
            double ry = radius * (0.6 + random.nextDouble() * 0.3);
            double rz = radius * (0.8 + random.nextDouble() * 0.4);
            BlockPos center = origin.above(Math.max(1, radius / 3));
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int reachX = Mth.ceil(rx);
            int reachY = Mth.ceil(ry);
            int reachZ = Mth.ceil(rz);

            for (int dx = -reachX; dx <= reachX; dx++) {
                for (int dy = -reachY; dy <= reachY; dy++) {
                    for (int dz = -reachZ; dz <= reachZ; dz++) {
                        double d = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
                        if (d <= 1.0 && (d <= 0.8 || random.nextFloat() >= 0.4f)) {
                            pos.setWithOffset(center, dx, dy, dz);
                            if (!level.getBlockState(pos).is(BlockTags.FEATURES_CANNOT_REPLACE)) {
                                this.setBlock(level, pos, config.stateProvider().getState(level, random, pos));
                            }
                        }
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
