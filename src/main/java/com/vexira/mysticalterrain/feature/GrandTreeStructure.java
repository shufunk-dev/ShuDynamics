package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enchantedwood.world.gen.mystical.feature.GrandTreeConfiguration;
import net.enchantedwood.world.gen.mystical.tree.TreeParams;
import java.util.Optional;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

public class GrandTreeStructure extends Structure {
   public static final MapCodec<GrandTreeStructure> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(configCodecBuilder(instance), ConfiguredFeature.REGISTRY_CODEC.fieldOf("tree").forGetter(structure -> structure.tree))
         .apply(instance, GrandTreeStructure::new)
   );
   public static final StructureType<GrandTreeStructure> TYPE = () -> CODEC;
   private final RegistryEntry<ConfiguredFeature<?, ?>> tree;

   public GrandTreeStructure(Structure.Config settings, RegistryEntry<ConfiguredFeature<?, ?>> tree) {
      super(settings);
      this.tree = tree;
   }

   @Override
   protected Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
      if (this.tree.value().config() instanceof GrandTreeConfiguration config && this.tree.getKey().isPresent()) {
         ChunkPos chunk = context.chunkPos();
         int x = chunk.getStartX();
         int z = chunk.getStartZ();
         ChunkGenerator generator = context.chunkGenerator();
         HeightLimitView heights = context.world();
         int floor = generator.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, heights, context.noiseConfig());
         int surface = generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, heights, context.noiseConfig());
         if ((surface <= floor || config.aquatic()) && heights.getTopYInclusive() - floor >= 16) {
            BlockPos base = new BlockPos(x, floor, z);
            TreeParams params = config.sample(context.random(), config.reach(), heights.getTopYInclusive() - floor);
            return Optional.of(
               new Structure.StructurePosition(
                  base, builder -> builder.addPiece(new GrandTreePiece(this.tree.getKey().get(), config, base, params))
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
   public StructureType<?> getType() {
      return TYPE;
   }
}
