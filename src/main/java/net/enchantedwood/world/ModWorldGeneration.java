package net.enchantedwood.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.world.gen.ConvergenceVegetationFeature;
import net.enchantedwood.world.gen.ResonanceArenaFeature;

public class ModWorldGeneration {
    public static final Feature<DefaultFeatureConfig> CONVERGENCE_VEGETATION_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(EnchantedWoodMod.MOD_ID, "convergence_vegetation"),
            new ConvergenceVegetationFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<ConfiguredFeature<?, ?>> CONVERGENCE_VEGETATION_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "convergence_vegetation"));

    public static final RegistryKey<PlacedFeature> CONVERGENCE_VEGETATION_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "convergence_vegetation"));

    public static final Feature<DefaultFeatureConfig> RESONANCE_ARENA_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_arena"),
            new ResonanceArenaFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<ConfiguredFeature<?, ?>> RESONANCE_ARENA_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_arena"));

    public static final RegistryKey<PlacedFeature> RESONANCE_ARENA_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_arena"));

    public static final Feature<DefaultFeatureConfig> RIFTWOOD_VILLAGE_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(EnchantedWoodMod.MOD_ID, "riftwood_village"),
            new net.enchantedwood.world.gen.RiftwoodVillageFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<ConfiguredFeature<?, ?>> RIFTWOOD_VILLAGE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "riftwood_village"));

    public static final RegistryKey<PlacedFeature> RIFTWOOD_VILLAGE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "riftwood_village"));

    public static final Feature<DefaultFeatureConfig> NETHER_INCURSION_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(EnchantedWoodMod.MOD_ID, "nether_incursion"),
            new net.enchantedwood.world.gen.NetherIncursionFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<ConfiguredFeature<?, ?>> NETHER_INCURSION_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "nether_incursion"));

    public static final RegistryKey<PlacedFeature> NETHER_INCURSION_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "nether_incursion"));

    public static final Feature<DefaultFeatureConfig> RIFT_CHASM_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(EnchantedWoodMod.MOD_ID, "rift_chasm"),
            new net.enchantedwood.world.gen.RiftChasmFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<ConfiguredFeature<?, ?>> RIFT_CHASM_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "rift_chasm"));

    public static final RegistryKey<PlacedFeature> RIFT_CHASM_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "rift_chasm"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> AVOCADO_TREE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "avocado_tree"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> STARFRUIT_TREE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "starfruit_tree"));
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

    // Convergence Cave Ores
    public static final RegistryKey<ConfiguredFeature<?, ?>> FLUORITE_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "fluorite_ore"));
    public static final RegistryKey<PlacedFeature> FLUORITE_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "fluorite_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> ZIRCONIA_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "zirconia_ore"));
    public static final RegistryKey<PlacedFeature> ZIRCONIA_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "zirconia_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> TANTALUM_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "tantalum_ore"));
    public static final RegistryKey<PlacedFeature> TANTALUM_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "tantalum_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> HAFNIUM_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "hafnium_ore"));
    public static final RegistryKey<PlacedFeature> HAFNIUM_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "hafnium_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> NEODYMIUM_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "neodymium_ore"));
    public static final RegistryKey<PlacedFeature> NEODYMIUM_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "neodymium_ore"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> AEROGEL_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "aerogel_ore"));
    public static final RegistryKey<PlacedFeature> AEROGEL_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(EnchantedWoodMod.MOD_ID, "aerogel_ore"));

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

        // Convergence Dimension Ore Generations
        RegistryKey<net.minecraft.world.biome.Biome> riftwoodHaven =
                RegistryKey.of(RegistryKeys.BIOME, Identifier.of(EnchantedWoodMod.MOD_ID, "riftwood_haven"));
        RegistryKey<net.minecraft.world.biome.Biome> causticMire =
                RegistryKey.of(RegistryKeys.BIOME, Identifier.of(EnchantedWoodMod.MOD_ID, "caustic_mire"));
        RegistryKey<net.minecraft.world.biome.Biome> scorchedCaldera =
                RegistryKey.of(RegistryKeys.BIOME, Identifier.of(EnchantedWoodMod.MOD_ID, "scorched_caldera"));
        RegistryKey<net.minecraft.world.biome.Biome> anoxicBarrens =
                RegistryKey.of(RegistryKeys.BIOME, Identifier.of(EnchantedWoodMod.MOD_ID, "anoxic_barrens"));
        RegistryKey<net.minecraft.world.biome.Biome> resonanceSanctum =
                RegistryKey.of(RegistryKeys.BIOME, Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_sanctum"));

        // Standard Vanilla Ores for Convergence Custom Biomes in exact canonical vanilla order from minecraft-merged.jar
        String[] vanillaOreIds = {
                "ore_dirt",
                "ore_gravel",
                "ore_granite_upper",
                "ore_granite_lower",
                "ore_diorite_upper",
                "ore_diorite_lower",
                "ore_andesite_upper",
                "ore_andesite_lower",
                "ore_tuff",
                "ore_coal_upper",
                "ore_coal_lower",
                "ore_iron_upper",
                "ore_iron_middle",
                "ore_iron_small",
                "ore_gold",
                "ore_gold_lower",
                "ore_redstone",
                "ore_redstone_lower",
                "ore_diamond",
                "ore_diamond_medium",
                "ore_diamond_large",
                "ore_diamond_buried",
                "ore_lapis",
                "ore_lapis_buried",
                "ore_copper",
                "underwater_magma",
                "disk_sand",
                "disk_clay",
                "disk_gravel"
        };

        for (String oreId : vanillaOreIds) {
            RegistryKey<PlacedFeature> placedKey = RegistryKey.of(
                    RegistryKeys.PLACED_FEATURE,
                    Identifier.of("minecraft", oreId)
            );
            BiomeModifications.addFeature(
                    BiomeSelectors.includeByKey(riftwoodHaven, anoxicBarrens, causticMire, scorchedCaldera),
                    GenerationStep.Feature.UNDERGROUND_ORES,
                    placedKey
            );
        }

        // Add Tin Ore to Overworld and Convergence Custom Biomes
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                TIN_ORE_PLACED_KEY
        );
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(riftwoodHaven, anoxicBarrens, causticMire, scorchedCaldera),
                GenerationStep.Feature.UNDERGROUND_ORES,
                TIN_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(causticMire, riftwoodHaven),
                GenerationStep.Feature.UNDERGROUND_ORES,
                FLUORITE_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(causticMire, riftwoodHaven),
                GenerationStep.Feature.UNDERGROUND_ORES,
                TANTALUM_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(scorchedCaldera),
                GenerationStep.Feature.UNDERGROUND_ORES,
                ZIRCONIA_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(scorchedCaldera),
                GenerationStep.Feature.UNDERGROUND_ORES,
                HAFNIUM_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(anoxicBarrens, riftwoodHaven, resonanceSanctum),
                GenerationStep.Feature.UNDERGROUND_ORES,
                NEODYMIUM_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(anoxicBarrens, causticMire, scorchedCaldera),
                GenerationStep.Feature.UNDERGROUND_ORES,
                AEROGEL_ORE_PLACED_KEY
        );

        // Convergence Dimension Vegetation (Wild Rice, Wild Cucumbers, Avocado Trees)
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(riftwoodHaven, causticMire),
                GenerationStep.Feature.VEGETAL_DECORATION,
                CONVERGENCE_VEGETATION_PLACED_KEY
        );

        // Resonance Sanctum Boss Arena Dais
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(resonanceSanctum),
                GenerationStep.Feature.SURFACE_STRUCTURES,
                RESONANCE_ARENA_PLACED_KEY
        );

        // Riftwood Haven Custom Villages (Avocado & Starfruit Architecture)
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(riftwoodHaven),
                GenerationStep.Feature.SURFACE_STRUCTURES,
                RIFTWOOD_VILLAGE_PLACED_KEY
        );

        // Fractured Nether Incursions in Convergence: Overworld Cave Carvers & Nether Terrain/Ores
        BiomeModifications.addCarver(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.biome.BiomeKeys.WARPED_FOREST,
                        net.minecraft.world.biome.BiomeKeys.CRIMSON_FOREST,
                        net.minecraft.world.biome.BiomeKeys.SOUL_SAND_VALLEY,
                        net.minecraft.world.biome.BiomeKeys.BASALT_DELTAS
                ),
                net.minecraft.world.gen.carver.ConfiguredCarvers.CAVE
        );
        BiomeModifications.addCarver(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.biome.BiomeKeys.WARPED_FOREST,
                        net.minecraft.world.biome.BiomeKeys.CRIMSON_FOREST,
                        net.minecraft.world.biome.BiomeKeys.SOUL_SAND_VALLEY,
                        net.minecraft.world.biome.BiomeKeys.BASALT_DELTAS
                ),
                net.minecraft.world.gen.carver.ConfiguredCarvers.CANYON
        );
        BiomeModifications.addCarver(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.biome.BiomeKeys.WARPED_FOREST,
                        net.minecraft.world.biome.BiomeKeys.CRIMSON_FOREST,
                        net.minecraft.world.biome.BiomeKeys.SOUL_SAND_VALLEY,
                        net.minecraft.world.biome.BiomeKeys.BASALT_DELTAS
                ),
                net.minecraft.world.gen.carver.ConfiguredCarvers.CAVE_EXTRA_UNDERGROUND
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.biome.BiomeKeys.WARPED_FOREST,
                        net.minecraft.world.biome.BiomeKeys.CRIMSON_FOREST,
                        net.minecraft.world.biome.BiomeKeys.SOUL_SAND_VALLEY,
                        net.minecraft.world.biome.BiomeKeys.BASALT_DELTAS
                ),
                GenerationStep.Feature.VEGETAL_DECORATION,
                NETHER_INCURSION_PLACED_KEY
        );

        // Surface Rift Chasms & Subterranean Entrances in The Convergence
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        riftwoodHaven,
                        anoxicBarrens,
                        causticMire,
                        scorchedCaldera,
                        net.minecraft.world.biome.BiomeKeys.PLAINS,
                        net.minecraft.world.biome.BiomeKeys.DESERT,
                        net.minecraft.world.biome.BiomeKeys.SAVANNA,
                        net.minecraft.world.biome.BiomeKeys.TAIGA,
                        net.minecraft.world.biome.BiomeKeys.BADLANDS,
                        net.minecraft.world.biome.BiomeKeys.ERODED_BADLANDS,
                        net.minecraft.world.biome.BiomeKeys.DARK_FOREST,
                        net.minecraft.world.biome.BiomeKeys.OLD_GROWTH_PINE_TAIGA,
                        net.minecraft.world.biome.BiomeKeys.WINDSWEPT_HILLS,
                        net.minecraft.world.biome.BiomeKeys.WINDSWEPT_GRAVELLY_HILLS,
                        net.minecraft.world.biome.BiomeKeys.STONY_PEAKS,
                        net.minecraft.world.biome.BiomeKeys.MEADOW,
                        net.minecraft.world.biome.BiomeKeys.JAGGED_PEAKS
                ),
                GenerationStep.Feature.LOCAL_MODIFICATIONS,
                RIFT_CHASM_PLACED_KEY
        );
    }
}
