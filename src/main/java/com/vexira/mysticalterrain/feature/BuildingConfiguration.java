package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vexira.mysticalterrain.building.BuildingKind;
import com.vexira.mysticalterrain.building.BuildingParams;
import com.vexira.mysticalterrain.building.Part;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.storage.loot.LootTable;

public record BuildingConfiguration(
   BuildingKind kind,
   IntProvider size,
   IntProvider floors,
   FloatProvider decay,
   Map<Part, BlockStateProvider> materials,
   Optional<ResourceKey<LootTable>> loot,
   Optional<ResourceKey<LootTable>> treasure,
   List<EntityType<?>> spawners,
   BuildingConfiguration.Anchor anchor,
   IntProvider elevation
) {
   public static final Codec<BuildingConfiguration> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            named(BuildingKind.class).fieldOf("kind").forGetter(BuildingConfiguration::kind),
            IntProviders.codec(1, 48).fieldOf("size").forGetter(BuildingConfiguration::size),
            IntProviders.codec(1, 8).optionalFieldOf("floors", ConstantInt.of(2)).forGetter(BuildingConfiguration::floors),
            FloatProviders.codec(0.0F, 1.0F).optionalFieldOf("decay", ConstantFloat.of(0.0F)).forGetter(BuildingConfiguration::decay),
            Codec.unboundedMap(named(Part.class), BlockStateProvider.DIRECT_CODEC).fieldOf("materials").forGetter(BuildingConfiguration::materials),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("loot").forGetter(BuildingConfiguration::loot),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("treasure").forGetter(BuildingConfiguration::treasure),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().listOf().optionalFieldOf("spawners", List.of()).forGetter(BuildingConfiguration::spawners),
            named(BuildingConfiguration.Anchor.class).optionalFieldOf("anchor", BuildingConfiguration.Anchor.SURFACE).forGetter(BuildingConfiguration::anchor),
            IntProviders.codec(-64, 320).optionalFieldOf("elevation", ConstantInt.of(100)).forGetter(BuildingConfiguration::elevation)
         )
         .apply(instance, BuildingConfiguration::new)
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

   public BuildingParams sample(RandomSource random) {
      return new BuildingParams(
         this.kind, this.size.sample(random), this.floors.sample(random), this.decay.sample(random), random.nextLong()
      );
   }

   public BlockStateProvider material(Part part) {
      BlockStateProvider provider = this.materials.get(part);
      if (provider != null) {
         return provider;
      }

      return switch (part) {
         case FOUNDATION, TRIM -> this.materials.get(Part.WALL);
         case FLOOR -> this.material(Part.FOUNDATION);
         case ROOF -> this.material(Part.TRIM);
         case PATH -> this.material(Part.FLOOR);
         case ROOF_STAIRS -> this.materials.get(Part.STAIRS);
         case ROOF_SLAB -> this.materials.get(Part.SLAB);
         default -> null;
      };
   }

   public enum Anchor {
      SURFACE,
      SEABED,
      FLOOR;
   }
}
