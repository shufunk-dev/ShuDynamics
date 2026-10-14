package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vexira.mysticalterrain.formation.FormationKind;
import com.vexira.mysticalterrain.formation.FormationParams;
import com.vexira.mysticalterrain.formation.Material;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import net.minecraft.util.math.floatprovider.FloatProvider;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public record FormationConfiguration(
   FormationKind kind,
   IntProvider size,
   IntProvider count,
   FloatProvider lean,
   FloatProvider girth,
   int sides,
   Map<Material, BlockStateProvider> materials,
   Optional<RegistryKey<LootTable>> loot,
   FormationConfiguration.Anchor anchor,
   IntProvider elevation
) implements FeatureConfig {
   public static final Codec<FormationConfiguration> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            named(FormationKind.class).fieldOf("kind").forGetter(FormationConfiguration::kind),
            IntProvider.createValidatingCodec(1, 160).fieldOf("size").forGetter(FormationConfiguration::size),
            IntProvider.createValidatingCodec(-1, 16).optionalFieldOf("count", ConstantIntProvider.create(-1)).forGetter(FormationConfiguration::count),
            FloatProvider.createValidatedCodec(0.0F, 1.5F).optionalFieldOf("lean", ConstantFloatProvider.create(0.5F)).forGetter(FormationConfiguration::lean),
            FloatProvider.createValidatedCodec(0.3F, 3.0F).optionalFieldOf("girth", ConstantFloatProvider.create(1.0F)).forGetter(FormationConfiguration::girth),
            Codec.intRange(3, 12).optionalFieldOf("sides", 6).forGetter(FormationConfiguration::sides),
            Codec.unboundedMap(named(Material.class), BlockStateProvider.TYPE_CODEC).fieldOf("materials").forGetter(FormationConfiguration::materials),
            RegistryKey.createCodec(RegistryKeys.LOOT_TABLE).optionalFieldOf("loot").forGetter(FormationConfiguration::loot),
            named(FormationConfiguration.Anchor.class)
               .optionalFieldOf("anchor", FormationConfiguration.Anchor.SURFACE)
               .forGetter(FormationConfiguration::anchor),
            IntProvider.createValidatingCodec(-64, 320).optionalFieldOf("elevation", ConstantIntProvider.create(100)).forGetter(FormationConfiguration::elevation)
         )
         .apply(instance, FormationConfiguration::new)
   );

   private static <E extends Enum<E>> Codec<E> named(Class<E> type) {
      return Codec.STRING.comapFlatMap(name -> {
         try {
            return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
         } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
         }
      }, value -> value.name().toLowerCase(Locale.ROOT));
   }

   public FormationParams sample(Random random) {
      return new FormationParams(
         this.kind,
         this.size.get(random),
         this.count.get(random),
         this.lean.get(random),
         this.girth.get(random),
         this.sides,
         random.nextLong()
      );
   }

   public BlockStateProvider material(Material material) {
      BlockStateProvider provider = this.materials.get(material);
      return provider != null ? provider : this.materials.get(Material.BODY);
   }

   public enum Anchor {
      SURFACE,
      FLOOR,
      FLOAT;
   }
}
