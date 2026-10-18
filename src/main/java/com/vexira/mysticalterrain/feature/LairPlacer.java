package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.BuildingBuilder;
import com.vexira.mysticalterrain.building.BuildingParams;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class LairPlacer {
   private LairPlacer() {
   }

   public static boolean place(ServerLevel level, String boss, BlockPos at) {
      ResourceKey<Feature> key = ResourceKey.create(Registries.FEATURE, MysticalTerrain.id("building/lair/" + boss));
      Optional<Reference<Feature>> holder = level.registryAccess().lookupOrThrow(Registries.FEATURE).get(key);
      if (!holder.isEmpty() && holder.get().value() instanceof BuildingFeature bf) {
         BuildingConfiguration config = bf.config();
         BuildingParams params = config.sample(level.getRandom());
         Blueprint plan = BuildingBuilder.build(params);
         int r = plan.reach() + 4;
         BoundingBox box = new BoundingBox(at.getX() - r, level.getMinY(), at.getZ() - r, at.getX() + r, level.getMaxY(), at.getZ() + r);
         BuildingPlacer.place(level, config, plan, at, params.seed(), box);
         return true;
      } else {
         return false;
      }
   }
}
