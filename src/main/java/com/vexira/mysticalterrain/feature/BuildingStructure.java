package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.BuildingBuilder;
import com.vexira.mysticalterrain.building.BuildingParams;
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

public class BuildingStructure extends Structure {
   public static final MapCodec<BuildingStructure> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(configCodecBuilder(instance), ConfiguredFeature.REGISTRY_CODEC.fieldOf("building").forGetter(structure -> structure.building))
         .apply(instance, BuildingStructure::new)
   );
   public static final StructureType<BuildingStructure> TYPE = () -> CODEC;
   private final RegistryEntry<ConfiguredFeature<?, ?>> building;

   public BuildingStructure(Structure.Config settings, RegistryEntry<ConfiguredFeature<?, ?>> building) {
      super(settings);
      this.building = building;
   }

   @Override
   protected Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
      if (this.building.value().config() instanceof BuildingConfiguration config && this.building.getKey().isPresent()) {
         ChunkPos chunk = context.chunkPos();
         int x = chunk.getStartX();
         int z = chunk.getStartZ();
         ChunkGenerator generator = context.chunkGenerator();
         HeightLimitView heights = context.world();
         NoiseConfig randomState = context.noiseConfig();

         Optional<Integer> y = switch (config.anchor()) {
            case SURFACE -> dry(generator, heights, randomState, x, z);
            case SEABED -> seabed(generator, heights, randomState, x, z);
            case FLOOR -> floor(generator.getColumnSample(x, z, heights, randomState), heights, config.elevation().get(context.random()));
         };
         if (y.isPresent()
            && context.biomePredicate()
               .test(
                  context.biomeSource()
                     .getBiome(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(y.get()), BiomeCoords.fromBlock(z), randomState.getMultiNoiseSampler())
               )) {
            BuildingParams params = config.sample(context.random());
            Blueprint plan = BuildingBuilder.build(params);
            if (config.anchor() == BuildingConfiguration.Anchor.SURFACE && !level(generator, heights, randomState, x, z, y.get(), plan)) {
               return Optional.empty();
            }

            BlockPos base = new BlockPos(x, y.get(), z);
            return Optional.of(
               new Structure.StructurePosition(
                  base,
                  builder -> builder.addPiece(new BuildingPiece(this.building.getKey().get(), config, base, params))
               )
            );
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private static Optional<Integer> dry(ChunkGenerator generator, HeightLimitView heights, NoiseConfig randomState, int x, int z) {
      int y = generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, heights, randomState);
      return y == generator.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, heights, randomState) && y > heights.getBottomY() + 8
         ? Optional.of(y)
         : Optional.empty();
   }

   private static boolean level(ChunkGenerator generator, HeightLimitView heights, NoiseConfig randomState, int x, int z, int y, Blueprint plan) {
      int spread = Math.max(3, plan.reach() * 2 / 3);
      int lo = y;
      int hi = y;
      int[][] around = new int[][]{
         {spread, 0}, {-spread, 0}, {0, spread}, {0, -spread}, {spread, spread}, {-spread, -spread}, {spread, -spread}, {-spread, spread}
      };

      for (int[] offset : around) {
         int h = generator.getHeight(x + offset[0], z + offset[1], Heightmap.Type.OCEAN_FLOOR_WG, heights, randomState);
         if (h <= heights.getBottomY() + 8) {
            return false;
         }

         lo = Math.min(lo, h);
         hi = Math.max(hi, h);
      }

      return hi - lo <= 8 + spread / 3;
   }

   private static Optional<Integer> seabed(ChunkGenerator generator, HeightLimitView heights, NoiseConfig randomState, int x, int z) {
      int surface = generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, heights, randomState);
      int floor = generator.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, heights, randomState);
      return surface - floor >= 4 ? Optional.of(floor) : Optional.empty();
   }

   private static Optional<Integer> floor(VerticalBlockSample column, HeightLimitView heights, int top) {
      int start = Math.min(top, heights.getTopYInclusive() - 24);

      for (int y = start; y > Math.max(32, heights.getBottomY()); y--) {
         BlockState below = column.getState(y - 1);
         if (column.getState(y).isAir() && !below.isAir() && below.getFluidState().isEmpty()) {
            boolean open = true;

            for (int dy = 1; dy < 10 && open; dy++) {
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
