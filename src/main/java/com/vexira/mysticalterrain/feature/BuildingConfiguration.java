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
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import net.minecraft.util.math.floatprovider.FloatProvider;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public record BuildingConfiguration(
   BuildingKind kind,
   IntProvider size,
   IntProvider floors,
   FloatProvider decay,
   Map<Part, BlockStateProvider> materials,
   Optional<RegistryKey<LootTable>> loot,
   Optional<RegistryKey<LootTable>> treasure,
   List<EntityType<?>> spawners,
   BuildingConfiguration.Anchor anchor,
   IntProvider elevation
) implements FeatureConfig {
   public static final Codec<BuildingConfiguration> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            named(BuildingKind.class).fieldOf("kind").forGetter(BuildingConfiguration::kind),
            IntProvider.createValidatingCodec(1, 48).fieldOf("size").forGetter(BuildingConfiguration::size),
            IntProvider.createValidatingCodec(1, 8).optionalFieldOf("floors", ConstantIntProvider.create(2)).forGetter(BuildingConfiguration::floors),
            FloatProvider.createValidatedCodec(0.0F, 1.0F).optionalFieldOf("decay", ConstantFloatProvider.create(0.0F)).forGetter(BuildingConfiguration::decay),
            Codec.unboundedMap(named(Part.class), BlockStateProvider.TYPE_CODEC).fieldOf("materials").forGetter(BuildingConfiguration::materials),
            RegistryKey.createCodec(RegistryKeys.LOOT_TABLE).optionalFieldOf("loot").forGetter(BuildingConfiguration::loot),
            RegistryKey.createCodec(RegistryKeys.LOOT_TABLE).optionalFieldOf("treasure").forGetter(BuildingConfiguration::treasure),
            Registries.ENTITY_TYPE.getCodec().listOf().optionalFieldOf("spawners", List.of()).forGetter(BuildingConfiguration::spawners),
            named(BuildingConfiguration.Anchor.class).optionalFieldOf("anchor", BuildingConfiguration.Anchor.SURFACE).forGetter(BuildingConfiguration::anchor),
            IntProvider.createValidatingCodec(-64, 320).optionalFieldOf("elevation", ConstantIntProvider.create(100)).forGetter(BuildingConfiguration::elevation)
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

   public BuildingParams sample(Random random) {
      return new BuildingParams(
         this.kind, this.size.get(random), this.floors.get(random), this.decay.get(random), random.nextLong()
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
