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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.storage.loot.LootTable;

public record FormationConfiguration(
   FormationKind kind,
   IntProvider size,
   IntProvider count,
   FloatProvider lean,
   FloatProvider girth,
   int sides,
   Map<Material, BlockStateProvider> materials,
   Optional<ResourceKey<LootTable>> loot,
   FormationConfiguration.Anchor anchor,
   IntProvider elevation
) {
   public static final Codec<FormationConfiguration> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            named(FormationKind.class).fieldOf("kind").forGetter(FormationConfiguration::kind),
            IntProviders.codec(1, 160).fieldOf("size").forGetter(FormationConfiguration::size),
            IntProviders.codec(-1, 16).optionalFieldOf("count", ConstantInt.of(-1)).forGetter(FormationConfiguration::count),
            FloatProviders.codec(0.0F, 1.5F).optionalFieldOf("lean", ConstantFloat.of(0.5F)).forGetter(FormationConfiguration::lean),
            FloatProviders.codec(0.3F, 3.0F).optionalFieldOf("girth", ConstantFloat.of(1.0F)).forGetter(FormationConfiguration::girth),
            Codec.intRange(3, 12).optionalFieldOf("sides", 6).forGetter(FormationConfiguration::sides),
            Codec.unboundedMap(named(Material.class), BlockStateProvider.DIRECT_CODEC).fieldOf("materials").forGetter(FormationConfiguration::materials),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("loot").forGetter(FormationConfiguration::loot),
            named(FormationConfiguration.Anchor.class)
               .optionalFieldOf("anchor", FormationConfiguration.Anchor.SURFACE)
               .forGetter(FormationConfiguration::anchor),
            IntProviders.codec(-64, 320).optionalFieldOf("elevation", ConstantInt.of(100)).forGetter(FormationConfiguration::elevation)
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

   public FormationParams sample(RandomSource random) {
      return new FormationParams(
         this.kind,
         this.size.sample(random),
         this.count.sample(random),
         this.lean.sample(random),
         this.girth.sample(random),
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
