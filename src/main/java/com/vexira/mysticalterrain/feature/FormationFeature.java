package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.formation.Formation;
import com.vexira.mysticalterrain.formation.FormationBuilder;
import com.vexira.mysticalterrain.formation.FormationParams;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class FormationFeature implements Feature {
   private static final int LOCAL_REACH = 16;
   public static final MapCodec<FormationFeature> CODEC = FormationConfiguration.CODEC.fieldOf("config")
       .xmap(FormationFeature::new, FormationFeature::config);

   private final FormationConfiguration config;

   public FormationFeature(FormationConfiguration config) {
      this.config = config;
   }

   public FormationConfiguration config() {
      return this.config;
   }

   @Override
   public MapCodec<? extends Feature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      ChunkPos chunk = ChunkPos.containing(origin);
      FormationParams params = this.config.sample(random);
      Formation formation = FormationBuilder.build(params);
      BlockPos base = origin;
      if (formation.reach() > LOCAL_REACH) {
         int x = chunk.getMinBlockX();
         int z = chunk.getMinBlockZ();
         if (this.config.anchor() == FormationConfiguration.Anchor.FLOAT) {
            base = new BlockPos(x, origin.getY(), z);
         } else if (!level.dimensionType().hasCeiling()) {
            base = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
         }

         if (!level.getBiome(base).equals(level.getBiome(origin))) {
            return false;
         }
      }

      if (this.config.anchor() == FormationConfiguration.Anchor.FLOAT
         || base.getY() > level.getMinY() + 2 && !level.getBlockState(base.below()).isAir() && level.getFluidState(base).isEmpty()) {
         BoundingBox box = new BoundingBox(
            chunk.getMinBlockX() - 16, level.getMinY(), chunk.getMinBlockZ() - 16, chunk.getMaxBlockX() + 16, level.getMaxY(), chunk.getMaxBlockZ() + 16
         );
         FormationPlacer.place(level, this.config, formation, base, params.seed(), box);
         return true;
      } else {
         return false;
      }
   }
}
