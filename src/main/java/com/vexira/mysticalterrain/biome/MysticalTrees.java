package com.vexira.mysticalterrain.biome;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class MysticalTrees {
    private static final Logger LOGGER = LoggerFactory.getLogger("mysticalterrain");
    private static final Codec<Map<String, List<RegistryKey<PlacedFeature>>>> CODEC = Codec.unboundedMap(
        Codec.STRING, RegistryKey.createCodec(RegistryKeys.PLACED_FEATURE).listOf()
    );

    private MysticalTrees() {}

    public static void register() {
        Map<String, List<RegistryKey<PlacedFeature>>> byBiome;
        try (InputStream in = MysticalTrees.class.getResourceAsStream("/data/mysticalterrain/grand_trees.json")) {
            if (in == null) {
                LOGGER.warn("Missing /data/mysticalterrain/grand_trees.json, skipping grand tree injection");
                return;
            }

            byBiome = CODEC.parse(
                    JsonOps.INSTANCE, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
            ).getOrThrow(error -> new IllegalStateException("Couldn't read grand_trees.json: " + error));
        } catch (Exception e) {
            LOGGER.error("Failed to load grand_trees.json", e);
            return;
        }

        // Apply grand trees ONLY to Convergence and Mystical Terrain biomes
        // Never touch vanilla Minecraft Overworld biomes!
        BiomeModifications.create(Identifier.of("mysticalterrain", "grand_trees"))
            .add(
                ModificationPhase.ADDITIONS,
                context -> {
                    String id = context.getBiomeKey().getValue().toString();
                    String namespace = context.getBiomeKey().getValue().getNamespace();
                    return (namespace.equals("enchantedwood") || namespace.equals("mysticalterrain")) && byBiome.containsKey(id);
                },
                (selection, modification) -> {
                    String id = selection.getBiomeKey().getValue().toString();
                    List<RegistryKey<PlacedFeature>> features = byBiome.get(id);
                    if (features != null) {
                        for (RegistryKey<PlacedFeature> key : features) {
                            modification.getGenerationSettings().addFeature(GenerationStep.Feature.VEGETAL_DECORATION, key);
                        }
                    }
                }
            );

        long placements = byBiome.values().stream().flatMap(Collection::stream).distinct().count();
        LOGGER.info("Planted grand trees in {} biomes with {} distinct placements (Convergence only)", byBiome.size(), placements);
    }
}
