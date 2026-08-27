package net.enchantedwood.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.enchantedwood.EnchantedWoodMod;

public class ModWorldGeneration {
    public static final RegistryKey<ConfiguredFeature<?, ?>> TIN_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "tin_ore"));

    public static final RegistryKey<PlacedFeature> TIN_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "tin_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> TITANIUM_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_ore"));

    public static final RegistryKey<PlacedFeature> TITANIUM_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> BAUXITE_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "bauxite_ore"));

    public static final RegistryKey<PlacedFeature> BAUXITE_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "bauxite_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> RUBBER_TREE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "rubber_tree"));

    public static final RegistryKey<PlacedFeature> RUBBER_TREE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "rubber_tree"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> OIL_SAND_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "oil_sand"));

    public static final RegistryKey<PlacedFeature> OIL_SAND_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "oil_sand"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> COBALT_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "cobalt_ore"));

    public static final RegistryKey<PlacedFeature> COBALT_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "cobalt_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> ARDITE_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "ardite_ore"));

    public static final RegistryKey<PlacedFeature> ARDITE_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "ardite_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> NETHER_TUNGSTEN_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "nether_tungsten_ore"));

    public static final RegistryKey<PlacedFeature> NETHER_TUNGSTEN_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "nether_tungsten_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> DEEPSLATE_TUNGSTEN_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "deepslate_tungsten_ore"));

    public static final RegistryKey<PlacedFeature> DEEPSLATE_TUNGSTEN_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "deepslate_tungsten_ore"));

    public static void generateOres() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                TIN_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                TITANIUM_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                BAUXITE_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                DEEPSLATE_TUNGSTEN_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.VEGETAL_DECORATION,
                RUBBER_TREE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.biome.BiomeKeys.DESERT,
                        net.minecraft.world.biome.BiomeKeys.BADLANDS,
                        net.minecraft.world.biome.BiomeKeys.ERODED_BADLANDS,
                        net.minecraft.world.biome.BiomeKeys.WOODED_BADLANDS
                ),
                GenerationStep.Feature.UNDERGROUND_ORES,
                OIL_SAND_PLACED_KEY
        );

        // Nether Ore Generations
        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                COBALT_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                ARDITE_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                NETHER_TUNGSTEN_ORE_PLACED_KEY
        );
    }
}
