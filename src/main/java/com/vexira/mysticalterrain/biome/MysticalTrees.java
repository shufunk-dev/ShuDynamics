package com.vexira.mysticalterrain.biome;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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
    private static final Codec<Map<String, List<ResourceKey<PlacedFeature>>>> CODEC = Codec.unboundedMap(
        Codec.STRING, ResourceKey.codec(Registries.PLACED_FEATURE).listOf()
    );

    private MysticalTrees() {}

    public static void register() {
        Map<String, List<ResourceKey<PlacedFeature>>> byBiome;
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

        // Apply grand trees to all configured biomes in grand_trees.json
        BiomeModifications.create(Identifier.fromNamespaceAndPath("mysticalterrain", "grand_trees"))
            .add(
                ModificationPhase.ADDITIONS,
                context -> byBiome.containsKey(context.getBiomeKey().identifier().toString()),
                (selection, modification) -> {
                    String id = selection.getBiomeKey().identifier().toString();
                    List<ResourceKey<PlacedFeature>> features = byBiome.get(id);
                    if (features != null) {
                        for (ResourceKey<PlacedFeature> key : features) {
                            modification.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, key);
                        }
                    }
                }
            );

        long placements = byBiome.values().stream().flatMap(Collection::stream).distinct().count();
        LOGGER.info("Planted grand trees in {} biomes with {} distinct placements", byBiome.size(), placements);
    }
}
