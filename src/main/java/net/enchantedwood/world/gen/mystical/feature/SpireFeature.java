package net.enchantedwood.world.gen.mystical.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class SpireFeature extends Feature<SpireConfiguration> {
    private static final int MAX_REACH = 14;

    public SpireFeature(Codec<SpireConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<SpireConfiguration> context) {
        StructureWorldAccess level = context.getWorld();
        Random random = context.getRandom();
        SpireConfiguration config = context.getConfig();
        BlockPos origin = context.getOrigin();
        boolean hanging = config.hanging();
        BlockState anchor = level.getBlockState(origin.offset(hanging ? Direction.UP : Direction.DOWN));

        if (!anchor.isAir() && anchor.isOpaque()) {
            int radius = config.radius().get(random);
            int height = hanging
                ? Math.min(config.height().get(random), origin.getY() - level.getBottomY() - 2)
                : Math.min(config.height().get(random), level.getTopYInclusive() - 2 - origin.getY());

            if (height < 3) {
                return false;
            }

            double maxLean = (double) Math.max(0, MAX_REACH - radius - 1) / height;
            double leanX = MathHelper.clamp((random.nextDouble() - 0.5) * 0.5, -maxLean, maxLean);
            double leanZ = MathHelper.clamp((random.nextDouble() - 0.5) * 0.5, -maxLean, maxLean);
            int dir = hanging ? -1 : 1;
            BlockPos.Mutable pos = new BlockPos.Mutable();

            for (int i = -3; i < height; i++) {
                double t = (double) Math.max(0, i) / height;
                double r = (radius + 0.3) * Math.pow(1.0 - t, 1.2) + (i < 0 ? 0.6 : 0.0);
                if (r < 0.15) {
                    break;
                }

                int ox = (int) Math.round(leanX * Math.max(0, i));
                int oz = (int) Math.round(leanZ * Math.max(0, i));
                int reach = MathHelper.ceil(r);

                for (int dx = -reach; dx <= reach; dx++) {
                    for (int dz = -reach; dz <= reach; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz) + (random.nextFloat() - 0.5F) * 0.5;
                        if (!(d > Math.max(r, 0.5))) {
                            pos.set(origin, ox + dx, dir * i, oz + dz);
                            BlockState existing = level.getBlockState(pos);
                            boolean allowed = i < 0 ? !existing.isIn(BlockTags.FEATURES_CANNOT_REPLACE) : canReplace(existing);
                            if (allowed) {
                                BlockState state = d > r - 1.2 ? config.shell().get(random, pos) : config.core().get(random, pos);
                                this.setBlockState(level, pos, state);
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
        return state.isAir() || state.isLiquid() || state.isIn(BlockTags.REPLACEABLE_BY_TREES) || state.isIn(BlockTags.REPLACEABLE);
    }
}
