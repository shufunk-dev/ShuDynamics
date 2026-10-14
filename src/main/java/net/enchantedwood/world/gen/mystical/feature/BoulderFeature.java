package net.enchantedwood.world.gen.mystical.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class BoulderFeature extends Feature<BoulderConfiguration> {
    public BoulderFeature(Codec<BoulderConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<BoulderConfiguration> context) {
        StructureWorldAccess level = context.getWorld();
        Random random = context.getRandom();
        BoulderConfiguration config = context.getConfig();
        BlockPos origin = context.getOrigin();
        BlockState ground = level.getBlockState(origin.down());

        if (!ground.isAir() && ground.isOpaque()) {
            int radius = config.radius().get(random);
            double rx = radius * (0.8 + random.nextDouble() * 0.4);
            double ry = radius * (0.6 + random.nextDouble() * 0.3);
            double rz = radius * (0.8 + random.nextDouble() * 0.4);
            BlockPos center = origin.up(Math.max(1, radius / 3));
            BlockPos.Mutable pos = new BlockPos.Mutable();
            int reachX = MathHelper.ceil(rx);
            int reachY = MathHelper.ceil(ry);
            int reachZ = MathHelper.ceil(rz);

            for (int dx = -reachX; dx <= reachX; dx++) {
                for (int dy = -reachY; dy <= reachY; dy++) {
                    for (int dz = -reachZ; dz <= reachZ; dz++) {
                        double d = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
                        if (d <= 1.0 && (d <= 0.8 || random.nextFloat() >= 0.4f)) {
                            pos.set(center, dx, dy, dz);
                            if (!level.getBlockState(pos).isIn(BlockTags.FEATURES_CANNOT_REPLACE)) {
                                this.setBlockState(level, pos, config.stateProvider().get(random, pos));
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
