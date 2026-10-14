package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.vexira.mysticalterrain.formation.Formation;
import com.vexira.mysticalterrain.formation.FormationBuilder;
import com.vexira.mysticalterrain.formation.FormationParams;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class FormationFeature extends Feature<FormationConfiguration> {
   private static final int LOCAL_REACH = 16;

   public FormationFeature(Codec<FormationConfiguration> codec) {
      super(codec);
   }

   @Override
   public boolean generate(FeatureContext<FormationConfiguration> context) {
      StructureWorldAccess level = context.getWorld();
      FormationConfiguration config = context.getConfig();
      BlockPos origin = context.getOrigin();
      ChunkPos chunk = new ChunkPos(origin);
      FormationParams params = config.sample(context.getRandom());
      Formation formation = FormationBuilder.build(params);
      BlockPos base = origin;
      if (formation.reach() > LOCAL_REACH) {
         int x = chunk.getStartX();
         int z = chunk.getStartZ();
         if (config.anchor() == FormationConfiguration.Anchor.FLOAT) {
            base = new BlockPos(x, origin.getY(), z);
         } else if (!level.getDimension().hasCeiling()) {
            base = new BlockPos(x, level.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z), z);
         }

         if (!level.getBiome(base).equals(level.getBiome(origin))) {
            return false;
         }
      }

      if (config.anchor() == FormationConfiguration.Anchor.FLOAT
         || base.getY() > level.getBottomY() + 2 && !level.getBlockState(base.down()).isAir() && level.getFluidState(base).isEmpty()) {
         BlockBox box = new BlockBox(
            chunk.getStartX() - 16, level.getBottomY(), chunk.getStartZ() - 16, chunk.getEndX() + 16, level.getTopYInclusive(), chunk.getEndZ() + 16
         );
         FormationPlacer.place(level, config, formation, base, params.seed(), box);
         return true;
      } else {
         return false;
      }
   }
}
