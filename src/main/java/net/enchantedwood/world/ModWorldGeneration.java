package net.enchantedwood.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.world.gen.ConvergenceVegetationFeature;
import net.enchantedwood.world.gen.ResonanceArenaFeature;

public class ModWorldGeneration {
    public static final com.mojang.serialization.MapCodec<? extends Feature> CONVERGENCE_VEGETATION_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE_TYPE,
            Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence_vegetation"),
            ConvergenceVegetationFeature.CODEC
    );

    public static final ResourceKey<Feature> CONVERGENCE_VEGETATION_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence_vegetation"));

    public static final ResourceKey<PlacedFeature> CONVERGENCE_VEGETATION_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence_vegetation"));

    public static final com.mojang.serialization.MapCodec<? extends Feature> RESONANCE_ARENA_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE_TYPE,
            Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "resonance_arena"),
            ResonanceArenaFeature.CODEC
    );

    public static final ResourceKey<Feature> RESONANCE_ARENA_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "resonance_arena"));

    public static final ResourceKey<PlacedFeature> RESONANCE_ARENA_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "resonance_arena"));

    public static final com.mojang.serialization.MapCodec<? extends Feature> RIFTWOOD_VILLAGE_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE_TYPE,
            Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "riftwood_village"),
            net.enchantedwood.world.gen.RiftwoodVillageFeature.CODEC
    );

    public static final ResourceKey<Feature> RIFTWOOD_VILLAGE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "riftwood_village"));

    public static final ResourceKey<PlacedFeature> RIFTWOOD_VILLAGE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "riftwood_village"));

    public static final com.mojang.serialization.MapCodec<? extends Feature> NETHER_INCURSION_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE_TYPE,
            Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "nether_incursion"),
            net.enchantedwood.world.gen.NetherIncursionFeature.CODEC
    );

    public static final ResourceKey<Feature> NETHER_INCURSION_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "nether_incursion"));

    public static final ResourceKey<PlacedFeature> NETHER_INCURSION_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "nether_incursion"));

    public static final com.mojang.serialization.MapCodec<? extends Feature> RIFT_CHASM_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE_TYPE,
            Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "rift_chasm"),
            net.enchantedwood.world.gen.RiftChasmFeature.CODEC
    );

    public static final ResourceKey<Feature> RIFT_CHASM_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "rift_chasm"));

    public static final ResourceKey<PlacedFeature> RIFT_CHASM_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "rift_chasm"));

    public static final com.mojang.serialization.MapCodec<? extends Feature> VOLCANIC_CALDERA_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE_TYPE,
            Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "volcanic_caldera"),
            net.enchantedwood.world.gen.VolcanicCalderaFeature.CODEC
    );

    public static final ResourceKey<Feature> VOLCANIC_CALDERA_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "volcanic_caldera"));

    public static final ResourceKey<PlacedFeature> VOLCANIC_CALDERA_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "volcanic_caldera"));

    public static final ResourceKey<Feature> AVOCADO_TREE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "avocado_tree"));
    public static final ResourceKey<Feature> STARFRUIT_TREE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "starfruit_tree"));
    public static final ResourceKey<Feature> TIN_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "tin_ore"));

    public static final ResourceKey<PlacedFeature> TIN_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "tin_ore"));

    public static final ResourceKey<Feature> TITANIUM_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "titanium_ore"));

    public static final ResourceKey<PlacedFeature> TITANIUM_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "titanium_ore"));

    public static final ResourceKey<Feature> BAUXITE_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "bauxite_ore"));

    public static final ResourceKey<PlacedFeature> BAUXITE_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "bauxite_ore"));

    public static final ResourceKey<Feature> RUBBER_TREE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "rubber_tree"));

    public static final ResourceKey<PlacedFeature> RUBBER_TREE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "rubber_tree"));

    public static final ResourceKey<Feature> OIL_SAND_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "oil_sand"));

    public static final ResourceKey<PlacedFeature> OIL_SAND_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "oil_sand"));

    public static final ResourceKey<Feature> COBALT_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "cobalt_ore"));

    public static final ResourceKey<PlacedFeature> COBALT_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "cobalt_ore"));

    public static final ResourceKey<Feature> ARDITE_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "ardite_ore"));

    public static final ResourceKey<PlacedFeature> ARDITE_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "ardite_ore"));

    public static final ResourceKey<Feature> NETHER_TUNGSTEN_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "nether_tungsten_ore"));

    public static final ResourceKey<PlacedFeature> NETHER_TUNGSTEN_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "nether_tungsten_ore"));

    public static final ResourceKey<Feature> DEEPSLATE_TUNGSTEN_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "deepslate_tungsten_ore"));

    public static final ResourceKey<PlacedFeature> DEEPSLATE_TUNGSTEN_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "deepslate_tungsten_ore"));

    // Convergence Cave Ores
    public static final ResourceKey<Feature> FLUORITE_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "fluorite_ore"));
    public static final ResourceKey<PlacedFeature> FLUORITE_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "fluorite_ore"));

    public static final ResourceKey<Feature> ZIRCONIA_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "zirconia_ore"));
    public static final ResourceKey<PlacedFeature> ZIRCONIA_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "zirconia_ore"));

    public static final ResourceKey<Feature> TANTALUM_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "tantalum_ore"));
    public static final ResourceKey<PlacedFeature> TANTALUM_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "tantalum_ore"));

    public static final ResourceKey<Feature> HAFNIUM_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "hafnium_ore"));
    public static final ResourceKey<PlacedFeature> HAFNIUM_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "hafnium_ore"));

    public static final ResourceKey<Feature> NEODYMIUM_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "neodymium_ore"));
    public static final ResourceKey<PlacedFeature> NEODYMIUM_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "neodymium_ore"));

    public static final ResourceKey<Feature> AEROGEL_ORE_KEY =
            ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "aerogel_ore"));
    public static final ResourceKey<PlacedFeature> AEROGEL_ORE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "aerogel_ore"));

    public static void generateOres() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                TIN_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                TITANIUM_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                BAUXITE_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                DEEPSLATE_TUNGSTEN_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                RUBBER_TREE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.level.biome.Biomes.DESERT,
                        net.minecraft.world.level.biome.Biomes.BADLANDS,
                        net.minecraft.world.level.biome.Biomes.ERODED_BADLANDS,
                        net.minecraft.world.level.biome.Biomes.WOODED_BADLANDS
                ),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                OIL_SAND_PLACED_KEY
        );

        // Nether Ore Generations
        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                COBALT_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ARDITE_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheNether(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                NETHER_TUNGSTEN_ORE_PLACED_KEY
        );

        // Convergence Dimension Ore Generations
        ResourceKey<net.minecraft.world.level.biome.Biome> riftwoodHaven =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "riftwood_haven"));
        ResourceKey<net.minecraft.world.level.biome.Biome> causticMire =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "caustic_mire"));
        ResourceKey<net.minecraft.world.level.biome.Biome> scorchedCaldera =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "scorched_caldera"));
        ResourceKey<net.minecraft.world.level.biome.Biome> anoxicBarrens =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "anoxic_barrens"));
        ResourceKey<net.minecraft.world.level.biome.Biome> resonanceSanctum =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "resonance_sanctum"));

        // Convergence Lost Biomes
        ResourceKey<net.minecraft.world.level.biome.Biome> alphaRainforest =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "alpha_rainforest"));
        ResourceKey<net.minecraft.world.level.biome.Biome> seasonalForest =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "seasonal_forest"));
        ResourceKey<net.minecraft.world.level.biome.Biome> shrubland =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "shrubland"));
        ResourceKey<net.minecraft.world.level.biome.Biome> modifiedJungleEdge =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "modified_jungle_edge"));
        ResourceKey<net.minecraft.world.level.biome.Biome> alphaTundra =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "alpha_tundra"));
        ResourceKey<net.minecraft.world.level.biome.Biome> desertLakes =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "desert_lakes"));
        ResourceKey<net.minecraft.world.level.biome.Biome> gravellyMountains =
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "gravelly_mountains"));

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
            ResourceKey<PlacedFeature> placedKey = ResourceKey.create(
                    Registries.PLACED_FEATURE,
                    Identifier.fromNamespaceAndPath("minecraft", oreId)
            );
            BiomeModifications.addFeature(
                    BiomeSelectors.includeByKey(
                            riftwoodHaven, anoxicBarrens, causticMire, scorchedCaldera,
                            alphaRainforest, seasonalForest, shrubland, modifiedJungleEdge,
                            alphaTundra, desertLakes, gravellyMountains
                    ),
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    placedKey
            );
        }

        // Add Tin Ore to Overworld and Convergence Custom Biomes
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                TIN_ORE_PLACED_KEY
        );
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        riftwoodHaven, anoxicBarrens, causticMire, scorchedCaldera,
                        alphaRainforest, seasonalForest, shrubland, modifiedJungleEdge,
                        alphaTundra, desertLakes, gravellyMountains
                ),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                TIN_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(causticMire, riftwoodHaven),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                FLUORITE_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(causticMire, riftwoodHaven),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                TANTALUM_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(scorchedCaldera),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ZIRCONIA_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(scorchedCaldera),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                HAFNIUM_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(anoxicBarrens, riftwoodHaven, resonanceSanctum),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                NEODYMIUM_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(anoxicBarrens, causticMire, scorchedCaldera),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                AEROGEL_ORE_PLACED_KEY
        );

        // Convergence Dimension Vegetation (Wild Rice, Wild Cucumbers, Avocado Trees)
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(riftwoodHaven, causticMire),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                CONVERGENCE_VEGETATION_PLACED_KEY
        );

        // Resonance Sanctum Boss Arena Dais
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(resonanceSanctum),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                RESONANCE_ARENA_PLACED_KEY
        );

        // Riftwood Haven Custom Villages (Avocado & Starfruit Architecture)
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(riftwoodHaven),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                RIFTWOOD_VILLAGE_PLACED_KEY
        );

        // Scorched Caldera Volcanic Strata, Peaks & Active Lava Craters
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(scorchedCaldera),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                VOLCANIC_CALDERA_PLACED_KEY
        );

        // Fractured Nether & End Incursions in Convergence: Overworld Cave Carvers & Terrain/Ores
        BiomeModifications.addCarver(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.level.biome.Biomes.WARPED_FOREST,
                        net.minecraft.world.level.biome.Biomes.CRIMSON_FOREST,
                        net.minecraft.world.level.biome.Biomes.SOUL_SAND_VALLEY,
                        net.minecraft.world.level.biome.Biomes.BASALT_DELTAS,
                        net.minecraft.world.level.biome.Biomes.END_HIGHLANDS
                ),
                net.minecraft.data.worldgen.Carvers.CAVE
        );
        BiomeModifications.addCarver(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.level.biome.Biomes.WARPED_FOREST,
                        net.minecraft.world.level.biome.Biomes.CRIMSON_FOREST,
                        net.minecraft.world.level.biome.Biomes.SOUL_SAND_VALLEY,
                        net.minecraft.world.level.biome.Biomes.BASALT_DELTAS,
                        net.minecraft.world.level.biome.Biomes.END_HIGHLANDS
                ),
                net.minecraft.data.worldgen.Carvers.CANYON
        );
        BiomeModifications.addCarver(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.level.biome.Biomes.WARPED_FOREST,
                        net.minecraft.world.level.biome.Biomes.CRIMSON_FOREST,
                        net.minecraft.world.level.biome.Biomes.SOUL_SAND_VALLEY,
                        net.minecraft.world.level.biome.Biomes.BASALT_DELTAS,
                        net.minecraft.world.level.biome.Biomes.END_HIGHLANDS
                ),
                net.minecraft.data.worldgen.Carvers.CAVE_EXTRA_UNDERGROUND
        );

        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        net.minecraft.world.level.biome.Biomes.WARPED_FOREST,
                        net.minecraft.world.level.biome.Biomes.CRIMSON_FOREST,
                        net.minecraft.world.level.biome.Biomes.SOUL_SAND_VALLEY,
                        net.minecraft.world.level.biome.Biomes.BASALT_DELTAS,
                        net.minecraft.world.level.biome.Biomes.END_HIGHLANDS
                ),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                NETHER_INCURSION_PLACED_KEY
        );

        // Surface Rift Chasms & Subterranean Entrances in The Convergence
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        riftwoodHaven,
                        anoxicBarrens,
                        causticMire,
                        scorchedCaldera,
                        net.minecraft.world.level.biome.Biomes.PLAINS,
                        net.minecraft.world.level.biome.Biomes.DESERT,
                        net.minecraft.world.level.biome.Biomes.SAVANNA,
                        net.minecraft.world.level.biome.Biomes.TAIGA,
                        net.minecraft.world.level.biome.Biomes.BADLANDS,
                        net.minecraft.world.level.biome.Biomes.ERODED_BADLANDS,
                        net.minecraft.world.level.biome.Biomes.DARK_FOREST,
                        net.minecraft.world.level.biome.Biomes.OLD_GROWTH_PINE_TAIGA,
                        net.minecraft.world.level.biome.Biomes.WINDSWEPT_HILLS,
                        net.minecraft.world.level.biome.Biomes.WINDSWEPT_GRAVELLY_HILLS,
                        net.minecraft.world.level.biome.Biomes.STONY_PEAKS,
                        net.minecraft.world.level.biome.Biomes.MEADOW,
                        net.minecraft.world.level.biome.Biomes.JAGGED_PEAKS
                ),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                RIFT_CHASM_PLACED_KEY
        );
    }
}
