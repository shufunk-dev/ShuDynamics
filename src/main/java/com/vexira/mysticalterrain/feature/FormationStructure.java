package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vexira.mysticalterrain.formation.Formation;
import com.vexira.mysticalterrain.formation.FormationBuilder;
import com.vexira.mysticalterrain.formation.FormationParams;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

public class FormationStructure extends Structure {
   public static final MapCodec<FormationStructure> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(configCodecBuilder(instance), ConfiguredFeature.REGISTRY_CODEC.fieldOf("formation").forGetter(structure -> structure.formation))
         .apply(instance, FormationStructure::new)
   );
   public static final StructureType<FormationStructure> TYPE = () -> CODEC;
   private final RegistryEntry<ConfiguredFeature<?, ?>> formation;

   public FormationStructure(Structure.Config settings, RegistryEntry<ConfiguredFeature<?, ?>> formation) {
      super(settings);
      this.formation = formation;
   }

   @Override
   protected Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
      if (this.formation.value().config() instanceof FormationConfiguration config && this.formation.getKey().isPresent()) {
         ChunkPos chunk = context.chunkPos();
         int x = chunk.getStartX();
         int z = chunk.getStartZ();
         FormationParams params = config.sample(context.random());
         ChunkGenerator generator = context.chunkGenerator();
         HeightLimitView heights = context.world();
         NoiseConfig randomState = context.noiseConfig();
         int top = config.anchor() == FormationConfiguration.Anchor.SURFACE ? 0 : config.elevation().get(context.random());

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
               y = floor(generator.getColumnSample(x, z, heights, randomState), heights, top, Math.max(8, Math.min(shape.maxY(), 24)));
               if (y.isEmpty()) {
                  return Optional.empty();
               }
            }

            BlockPos base = new BlockPos(x, y.get(), z);
            return Optional.of(
               new Structure.StructurePosition(
                  base,
                  builder -> builder.addPiece(new FormationPiece(this.formation.getKey().get(), config, base, params))
               )
            );
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private static boolean fits(Structure.Context context, int x, int y, int z) {
      return context.biomePredicate()
         .test(
            context.biomeSource()
               .getBiome(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(y), BiomeCoords.fromBlock(z), context.noiseConfig().getMultiNoiseSampler())
         );
   }

   private static Optional<Integer> dry(ChunkGenerator generator, HeightLimitView heights, NoiseConfig randomState, int x, int z) {
      int y = generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, heights, randomState);
      return y > heights.getBottomY() + 8 && y == generator.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, heights, randomState)
         ? Optional.of(y)
         : Optional.empty();
   }

   private static boolean grounded(ChunkGenerator generator, HeightLimitView heights, NoiseConfig randomState, int x, int z, int spread) {
      int floor = heights.getBottomY() + 8;
      int[][] around = new int[][]{{spread, 0}, {-spread, 0}, {0, spread}, {0, -spread}};

      for (int[] offset : around) {
         if (generator.getHeight(x + offset[0], z + offset[1], Heightmap.Type.WORLD_SURFACE_WG, heights, randomState) <= floor) {
            return false;
         }
      }

      return true;
   }

   private static Optional<Integer> floor(VerticalBlockSample column, HeightLimitView heights, int top, int headroom) {
      int start = Math.min(top, heights.getTopYInclusive() - headroom);

      for (int y = start; y > Math.max(32, heights.getBottomY()); y--) {
         BlockState below = column.getState(y - 1);
         if (column.getState(y).isAir() && !below.isAir() && below.getFluidState().isEmpty()) {
            boolean open = true;

            for (int dy = 1; dy < headroom && open; dy++) {
               open = column.getState(y + dy).isAir();
            }

            if (open) {
               return Optional.of(y);
            }
         }
      }

      return Optional.empty();
   }

   @Override
   public StructureType<?> getType() {
      return TYPE;
   }
}
