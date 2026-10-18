package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vexira.mysticalterrain.formation.Formation;
import com.vexira.mysticalterrain.formation.FormationBuilder;
import com.vexira.mysticalterrain.formation.FormationParams;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class FormationStructure extends Structure {
   public static final MapCodec<FormationStructure> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(settingsCodec(instance), Feature.CODEC.fieldOf("formation").forGetter(structure -> structure.formation))
         .apply(instance, FormationStructure::new)
   );
   public static final StructureType<FormationStructure> TYPE = () -> CODEC;
   private final Holder<Feature> formation;

   public FormationStructure(Structure.StructureSettings settings, Holder<Feature> formation) {
      super(settings);
      this.formation = formation;
   }

   @Override
   protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
      if (this.formation.value() instanceof FormationFeature ff && this.formation.unwrapKey().isPresent()) {
         FormationConfiguration config = ff.config();
         ChunkPos chunk = context.chunkPos();
         int x = chunk.getMinBlockX();
         int z = chunk.getMinBlockZ();
         FormationParams params = config.sample(context.random());
         ChunkGenerator generator = context.chunkGenerator();
         LevelHeightAccessor heights = context.heightAccessor();
         RandomState randomState = context.randomState();
         int top = config.anchor() == FormationConfiguration.Anchor.SURFACE ? 0 : config.elevation().sample(context.random());

         Optional<Integer> y = switch (config.anchor()) {
            case SURFACE -> dry(generator, heights, randomState, x, z);
            case FLOAT, FLOOR -> Optional.of(top);
         };
         if (y.isPresent() && fits(context, x, y.get(), z)) {
            Formation shape = FormationBuilder.build(params);
            if (config.anchor() == FormationConfiguration.Anchor.SURFACE && !grounded(generator, heights, randomState, x, z, shape.reach() / 2)) {
               return Optional.empty();
            }

            if (config.anchor() == FormationConfiguration.Anchor.FLOOR) {
               y = floor(generator.getBaseColumn(x, z, heights, randomState), heights, top, Math.max(8, Math.min(shape.maxY(), 24)));
               if (y.isEmpty()) {
                  return Optional.empty();
               }
            }

            BlockPos base = new BlockPos(x, y.get(), z);
            return Optional.of(
               new Structure.GenerationStub(
                  base,
                  builder -> builder.addPiece(new FormationPiece(this.formation.unwrapKey().get(), config, base, params))
               )
            );
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private static boolean fits(Structure.GenerationContext context, int x, int y, int z) {
      return context.validBiome()
         .test(
            context.biomeSource()
               .createUncachedResolver(context.randomState()).getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z))
         );
   }

   private static Optional<Integer> dry(ChunkGenerator generator, LevelHeightAccessor heights, RandomState randomState, int x, int z) {
      int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
      return y > heights.getMinY() + 8 && y == generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState)
         ? Optional.of(y)
         : Optional.empty();
   }

   private static boolean grounded(ChunkGenerator generator, LevelHeightAccessor heights, RandomState randomState, int x, int z, int spread) {
      int floor = heights.getMinY() + 8;
      int[][] around = new int[][]{{spread, 0}, {-spread, 0}, {0, spread}, {0, -spread}};

      for (int[] offset : around) {
         if (generator.getBaseHeight(x + offset[0], z + offset[1], Heightmap.Types.WORLD_SURFACE_WG, heights, randomState) <= floor) {
            return false;
         }
      }

      return true;
   }

   private static Optional<Integer> floor(NoiseColumn column, LevelHeightAccessor heights, int top, int headroom) {
      int start = Math.min(top, heights.getMaxY() - headroom);

      for (int y = start; y > Math.max(32, heights.getMinY()); y--) {
         BlockState below = column.getBlock(y - 1);
         if (column.getBlock(y).isAir() && !below.isAir() && below.getFluidState().isEmpty()) {
            boolean open = true;

            for (int dy = 1; dy < headroom && open; dy++) {
               open = column.getBlock(y + dy).isAir();
            }

            if (open) {
               return Optional.of(y);
            }
         }
      }

      return Optional.empty();
   }

   @Override
   public StructureType<?> type() {
      return TYPE;
   }
}
