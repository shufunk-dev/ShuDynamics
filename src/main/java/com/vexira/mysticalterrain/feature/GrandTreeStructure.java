package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vexira.mysticalterrain.tree.TreeParams;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class GrandTreeStructure extends Structure {
   public static final MapCodec<GrandTreeStructure> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(settingsCodec(instance), Feature.CODEC.fieldOf("tree").forGetter(structure -> structure.tree))
         .apply(instance, GrandTreeStructure::new)
   );
   public static final StructureType<GrandTreeStructure> TYPE = () -> CODEC;
   private final Holder<Feature> tree;

   public GrandTreeStructure(Structure.StructureSettings settings, Holder<Feature> tree) {
      super(settings);
      this.tree = tree;
   }

   @Override
   protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
      if (this.tree.value() instanceof GrandTreeFeature gtf && this.tree.unwrapKey().isPresent()) {
         GrandTreeConfiguration config = gtf.config();
         ChunkPos chunk = context.chunkPos();
         int x = chunk.getMinBlockX();
         int z = chunk.getMinBlockZ();
         ChunkGenerator generator = context.chunkGenerator();
         LevelHeightAccessor heights = context.heightAccessor();
         int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, context.randomState());
         int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, context.randomState());
         if ((surface <= floor || config.aquatic()) && heights.getMaxY() - floor >= 16) {
            BlockPos base = new BlockPos(x, floor, z);
            TreeParams params = config.sample(context.random(), config.reach(), heights.getMaxY() - floor);
            return Optional.of(
               new Structure.GenerationStub(
                  base, builder -> builder.addPiece(new GrandTreePiece(this.tree.unwrapKey().get(), config, base, params))
               )
            );
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   @Override
   public StructureType<?> type() {
      return TYPE;
   }
}
