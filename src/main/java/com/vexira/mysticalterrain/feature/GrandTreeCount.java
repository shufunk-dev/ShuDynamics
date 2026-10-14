package com.vexira.mysticalterrain.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifierType;

public class GrandTreeCount extends PlacementModifier {
   private static final String RATES = "/data/mysticalterrain/grand_tree_rates.json";
   private static final Map<String, Map<String, Double>> TABLE = load();
   public static final MapCodec<GrandTreeCount> CODEC = Identifier.CODEC.fieldOf("tree").xmap(GrandTreeCount::new, count -> count.tree);
   public static final PlacementModifierType<GrandTreeCount> TYPE = () -> CODEC;
   private final Identifier tree;
   private final Map<String, Double> rates;

   public GrandTreeCount(Identifier tree) {
      this.tree = tree;
      this.rates = TABLE.getOrDefault(tree.toString(), Map.of());
   }

   @Override
   public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
      int x = pos.getX() + 8;
      int z = pos.getZ() + 8;
      BlockPos middle = new BlockPos(x, context.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z), z);
      double rate = context.getWorld()
         .getBiome(middle)
         .getKey()
         .map(key -> this.rates.getOrDefault(key.getValue().toString(), 0.0))
         .orElse(0.0);
      int count = (int)rate + (random.nextDouble() < rate - Math.floor(rate) ? 1 : 0);
      return Stream.generate(() -> pos).limit(count);
   }

   @Override
   public PlacementModifierType<?> getType() {
      return TYPE;
   }

   private static Map<String, Map<String, Double>> load() {
      try (InputStream in = GrandTreeCount.class.getResourceAsStream(RATES)) {
         if (in == null) {
            return Map.of();
         } else {
            return (Map<String, Map<String, Double>>)Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.DOUBLE))
               .parse(JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
               .result()
               .orElse(Map.of());
         }
      } catch (Exception e) {
         return Map.of();
      }
   }
}
