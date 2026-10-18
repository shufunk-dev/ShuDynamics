package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.tree.Noise;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class GroundCoverFeature implements Feature {
   public static final MapCodec<GroundCoverFeature> CODEC = GroundCoverConfiguration.CODEC.fieldOf("config")
       .xmap(GroundCoverFeature::new, GroundCoverFeature::config);

   private final GroundCoverConfiguration config;

   public GroundCoverFeature(GroundCoverConfiguration config) {
      this.config = config;
   }

   public GroundCoverConfiguration config() {
      return this.config;
   }

   @Override
   public MapCodec<? extends Feature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      ChunkPos chunk = ChunkPos.containing(origin);
      long seed = level.getSeed();
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      boolean painted = false;

      for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
         for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
            boolean open = true;
            int under = 0;

            for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1; y > level.getMinY(); y--) {
               pos.set(x, y, z);
               BlockState state = level.getBlockState(pos);
               if (state.isAir()) {
                  open = true;
                  under = 0;
               } else {
                  boolean replaceable = state.is(this.config.replaceable());
                  if (open && replaceable && level.getBiome(pos).is(this.config.biome())) {
                     level.setBlock(pos, top(this.config, seed, x, z).getState(level, random, pos), 2);
                     under = this.config.depth();
                     painted = true;
                  } else if (under > 0 && replaceable) {
                     level.setBlock(pos, this.config.under().getState(level, random, pos), 2);
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
