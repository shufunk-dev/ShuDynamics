package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.BuildingBuilder;
import com.vexira.mysticalterrain.building.BuildingParams;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class BuildingFeature extends Feature<BuildingConfiguration> {
   private static final int LOCAL_REACH = 16;

   public BuildingFeature(Codec<BuildingConfiguration> codec) {
      super(codec);
   }

   @Override
   public boolean generate(FeatureContext<BuildingConfiguration> context) {
      StructureWorldAccess level = context.getWorld();
      BuildingParams params = context.getConfig().sample(context.getRandom());
      Blueprint plan = BuildingBuilder.build(params);
      BlockPos origin = context.getOrigin();
      if (plan.reach() <= LOCAL_REACH && !level.getBlockState(origin.down()).isAir()) {
         ChunkPos chunk = new ChunkPos(origin);
         BlockBox box = new BlockBox(
            chunk.getStartX() - 16, level.getBottomY(), chunk.getStartZ() - 16, chunk.getEndX() + 16, level.getTopYInclusive(), chunk.getEndZ() + 16
         );
         BuildingPlacer.place(level, context.getConfig(), plan, origin, params.seed(), box);
         return true;
      } else {
         return false;
      }
   }
}
