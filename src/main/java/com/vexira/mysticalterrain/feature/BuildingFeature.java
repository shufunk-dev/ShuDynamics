package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.BuildingBuilder;
import com.vexira.mysticalterrain.building.BuildingParams;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class BuildingFeature implements Feature {
   private static final int LOCAL_REACH = 16;
   public static final MapCodec<BuildingFeature> CODEC = BuildingConfiguration.CODEC.fieldOf("config")
       .xmap(BuildingFeature::new, BuildingFeature::config);

   private final BuildingConfiguration config;

   public BuildingFeature(BuildingConfiguration config) {
      this.config = config;
   }

   public BuildingConfiguration config() {
      return this.config;
   }

   @Override
   public MapCodec<? extends Feature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      BuildingParams params = this.config.sample(random);
      Blueprint plan = BuildingBuilder.build(params);
      if (plan.reach() <= LOCAL_REACH && !level.getBlockState(origin.below()).isAir()) {
         ChunkPos chunk = ChunkPos.containing(origin);
         BoundingBox box = new BoundingBox(
            chunk.getMinBlockX() - 16, level.getMinY(), chunk.getMinBlockZ() - 16, chunk.getMaxBlockX() + 16, level.getMaxY(), chunk.getMaxBlockZ() + 16
         );
         BuildingPlacer.place(level, this.config, plan, origin, params.seed(), box);
         return true;
      } else {
         return false;
      }
   }
}
