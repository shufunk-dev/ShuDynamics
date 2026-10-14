package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.vexira.mysticalterrain.tree.Noise;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class GroundCoverFeature extends Feature<GroundCoverConfiguration> {
   public GroundCoverFeature(Codec<GroundCoverConfiguration> codec) {
      super(codec);
   }

   @Override
   public boolean generate(FeatureContext<GroundCoverConfiguration> context) {
      StructureWorldAccess level = context.getWorld();
      GroundCoverConfiguration config = context.getConfig();
      Random random = context.getRandom();
      ChunkPos chunk = new ChunkPos(context.getOrigin());
      long seed = level.getSeed();
      BlockPos.Mutable pos = new BlockPos.Mutable();
      boolean painted = false;

      for (int x = chunk.getStartX(); x <= chunk.getEndX(); x++) {
         for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
            boolean open = true;
            int under = 0;

            for (int y = level.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1; y > level.getBottomY(); y--) {
               pos.set(x, y, z);
               BlockState state = level.getBlockState(pos);
               if (state.isAir()) {
                  open = true;
                  under = 0;
               } else {
                  boolean replaceable = state.isIn(config.replaceable());
                  if (open && replaceable && level.getBiome(pos).matchesKey(config.biome())) {
                     level.setBlockState(pos, top(config, seed, x, z).get(random, pos), 2);
                     under = config.depth();
                     painted = true;
                  } else if (under > 0 && replaceable) {
                     level.setBlockState(pos, config.under().get(random, pos), 2);
                     under--;
                  } else {
                     under = 0;
                  }

                  open = false;
               }
            }
         }
      }

      return painted;
   }

   private static BlockStateProvider top(GroundCoverConfiguration config, long seed, int x, int z) {
      return config.accent().isPresent() && Noise.smooth(seed, x / config.accentScale(), 0.0, z / config.accentScale()) > config.accentThreshold()
         ? config.accent().get()
         : config.top();
   }
}
