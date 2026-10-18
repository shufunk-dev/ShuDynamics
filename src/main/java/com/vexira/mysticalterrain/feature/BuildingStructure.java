package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.BuildingBuilder;
import com.vexira.mysticalterrain.building.BuildingParams;
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

public class BuildingStructure extends Structure {
   public static final MapCodec<BuildingStructure> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(settingsCodec(instance), Feature.CODEC.fieldOf("building").forGetter(structure -> structure.building))
         .apply(instance, BuildingStructure::new)
   );
   public static final StructureType<BuildingStructure> TYPE = () -> CODEC;
   private final Holder<Feature> building;

   public BuildingStructure(Structure.StructureSettings settings, Holder<Feature> building) {
      super(settings);
      this.building = building;
   }

   @Override
   protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
      if (this.building.value() instanceof BuildingFeature bf && this.building.unwrapKey().isPresent()) {
         BuildingConfiguration config = bf.config();
         ChunkPos chunk = context.chunkPos();
         int x = chunk.getMinBlockX();
         int z = chunk.getMinBlockZ();
         ChunkGenerator generator = context.chunkGenerator();
         LevelHeightAccessor heights = context.heightAccessor();
         RandomState randomState = context.randomState();

         Optional<Integer> y = switch (config.anchor()) {
            case SURFACE -> dry(generator, heights, randomState, x, z);
            case SEABED -> seabed(generator, heights, randomState, x, z);
            case FLOOR -> floor(generator.getBaseColumn(x, z, heights, randomState), heights, config.elevation().sample(context.random()));
         };
         if (y.isPresent()
            && context.validBiome()
               .test(
                  context.biomeSource()
                     .createUncachedResolver(randomState).getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y.get()), QuartPos.fromBlock(z))
               )) {
            BuildingParams params = config.sample(context.random());
            Blueprint plan = BuildingBuilder.build(params);
            if (config.anchor() == BuildingConfiguration.Anchor.SURFACE && !level(generator, heights, randomState, x, z, y.get(), plan)) {
               return Optional.empty();
            }

            BlockPos base = new BlockPos(x, y.get(), z);
            return Optional.of(
               new Structure.GenerationStub(
                  base,
                  builder -> builder.addPiece(new BuildingPiece(this.building.unwrapKey().get(), config, base, params))
               )
            );
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private static Optional<Integer> dry(ChunkGenerator generator, LevelHeightAccessor heights, RandomState randomState, int x, int z) {
      int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
      return y == generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState) && y > heights.getMinY() + 8
         ? Optional.of(y)
         : Optional.empty();
   }

   private static boolean level(ChunkGenerator generator, LevelHeightAccessor heights, RandomState randomState, int x, int z, int y, Blueprint plan) {
      int spread = Math.max(3, plan.reach() * 2 / 3);
      int lo = y;
      int hi = y;
      int[][] around = new int[][]{
         {spread, 0}, {-spread, 0}, {0, spread}, {0, -spread}, {spread, spread}, {-spread, -spread}, {spread, -spread}, {-spread, spread}
      };

      for (int[] offset : around) {
         int h = generator.getBaseHeight(x + offset[0], z + offset[1], Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
         if (h <= heights.getMinY() + 8) {
            return false;
         }

         lo = Math.min(lo, h);
         hi = Math.max(hi, h);
      }

      return hi - lo <= 8 + spread / 3;
   }

   private static Optional<Integer> seabed(ChunkGenerator generator, LevelHeightAccessor heights, RandomState randomState, int x, int z) {
      int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
      int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
      return surface - floor >= 4 ? Optional.of(floor) : Optional.empty();
   }

   private static Optional<Integer> floor(NoiseColumn column, LevelHeightAccessor heights, int top) {
      int start = Math.min(top, heights.getMaxY() - 24);

      for (int y = start; y > Math.max(32, heights.getMinY()); y--) {
         BlockState below = column.getBlock(y - 1);
         if (column.getBlock(y).isAir() && !below.isAir() && below.getFluidState().isEmpty()) {
            boolean open = true;

            for (int dy = 1; dy < 10 && open; dy++) {
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
