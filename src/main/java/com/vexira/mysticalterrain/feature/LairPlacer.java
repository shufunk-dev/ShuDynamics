package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.BuildingBuilder;
import com.vexira.mysticalterrain.building.BuildingParams;
import java.util.Optional;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.ConfiguredFeature;

public final class LairPlacer {
   private LairPlacer() {
   }

   public static boolean place(ServerWorld level, String boss, BlockPos at) {
      RegistryKey<ConfiguredFeature<?, ?>> key = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, MysticalTerrain.id("building/lair/" + boss));
      Optional<Reference<ConfiguredFeature<?, ?>>> holder = level.getRegistryManager().getOrThrow(RegistryKeys.CONFIGURED_FEATURE).getOptional(key);
      if (!holder.isEmpty() && ((ConfiguredFeature)holder.get().value()).config() instanceof BuildingConfiguration config) {
         BuildingParams params = config.sample(level.getRandom());
         Blueprint plan = BuildingBuilder.build(params);
         int r = plan.reach() + 4;
         BlockBox box = new BlockBox(at.getX() - r, level.getBottomY(), at.getZ() - r, at.getX() + r, level.getTopYInclusive(), at.getZ() + r);
         BuildingPlacer.place(level, config, plan, at, params.seed(), box);
         return true;
      } else {
         return false;
      }
   }
}
