package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public class SpireFeature implements Feature {
    private static final int MAX_REACH = 14;
    public static final MapCodec<SpireFeature> CODEC = SpireConfiguration.CODEC.fieldOf("config")
        .xmap(SpireFeature::new, SpireFeature::config);

    private final SpireConfiguration config;

    public SpireFeature(SpireConfiguration config) {
        this.config = config;
    }

    public SpireConfiguration config() {
        return this.config;
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        boolean hanging = this.config.hanging();
        BlockState anchor = level.getBlockState(origin.relative(hanging ? Direction.UP : Direction.DOWN));

        if (!anchor.isAir() && anchor.canOcclude()) {
            int radius = this.config.radius().sample(random);
            int height = hanging
                ? Math.min(this.config.height().sample(random), origin.getY() - level.getMinY() - 2)
                : Math.min(this.config.height().sample(random), level.getMaxY() - 2 - origin.getY());

            if (height < 3) {
                return false;
            }

            double maxLean = (double) Math.max(0, MAX_REACH - radius - 1) / height;
            double leanX = Mth.clamp((random.nextDouble() - 0.5) * 0.5, -maxLean, maxLean);
            double leanZ = Mth.clamp((random.nextDouble() - 0.5) * 0.5, -maxLean, maxLean);
            int dir = hanging ? -1 : 1;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int i = -3; i < height; i++) {
                double t = (double) Math.max(0, i) / height;
                double r = (radius + 0.3) * Math.pow(1.0 - t, 1.2) + (i < 0 ? 0.6 : 0.0);
                if (r < 0.15) {
                    break;
                }

                int ox = (int) Math.round(leanX * Math.max(0, i));
                int oz = (int) Math.round(leanZ * Math.max(0, i));
                int reach = Mth.ceil(r);

                for (int dx = -reach; dx <= reach; dx++) {
                    for (int dz = -reach; dz <= reach; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz) + (random.nextFloat() - 0.5F) * 0.5;
                        if (!(d > Math.max(r, 0.5))) {
                            pos.setWithOffset(origin, ox + dx, dir * i, oz + dz);
                            BlockState existing = level.getBlockState(pos);
                            boolean allowed = i < 0 ? !existing.is(BlockTags.FEATURES_CANNOT_REPLACE) : canReplace(existing);
                            if (allowed) {
                                BlockState state = d > r - 1.2 ? this.config.shell().getState(level, random, pos) : this.config.core().getState(level, random, pos);
                                this.setBlock(level, pos, state);
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

    private static boolean canReplace(BlockState state) {
        return state.isAir() || state.liquid() || state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.REPLACEABLE);
    }
}
