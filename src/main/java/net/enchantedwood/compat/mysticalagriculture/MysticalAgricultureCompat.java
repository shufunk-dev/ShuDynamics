package net.enchantedwood.compat.mysticalagriculture;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles automatic crop configuration export for Mystical Agriculture / Mystical Customization.
 * When Mystical Agriculture (or Mystical Customization) is installed in a modpack,
 * this automatically generates the JSON crop definitions so players can grow ShuDynamics materials.
 */
public class MysticalAgricultureCompat {

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded("mysticalcustomization")) {
            return;
        }

        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path mcDir = configDir.resolve("mysticalcustomization");
        Path cropsDir = mcDir.resolve("crops");

        exportDefaultCrops(cropsDir);
    }

    private static void exportDefaultCrops(Path cropsDir) {
        try {
            if (!Files.exists(cropsDir)) {
                Files.createDirectories(cropsDir);
            }

            Map<String, String> crops = new LinkedHashMap<>();

            // Tier 3: Magitech
            crops.put("infused_heartwood", createCropJson(
                    "Infused Heartwood",
                    "mysticalagriculture:3",
                    "enchantedwood:infused_heartwood",
                    "2ecc71",
                    "mysticalagriculture:block/flower_dust",
                    "mysticalagriculture:item/essence_dust",
                    null
            ));

            crops.put("enchanted_dust", createCropJson(
                    "Enchanted Dust",
                    "mysticalagriculture:3",
                    "enchantedwood:enchanted_dust",
                    "9b59b6",
                    "mysticalagriculture:block/flower_dust",
                    "mysticalagriculture:item/essence_dust",
                    null
            ));

            // Tier 4: Volcanic
            crops.put("fire_crystal", createCropJson(
                    "Fire Crystal",
                    "mysticalagriculture:4",
                    "enchantedwood:fire_crystal",
                    "e67e22",
                    "mysticalagriculture:block/flower_rock",
                    "mysticalagriculture:item/essence_rock",
                    null
            ));

            // Tier 5: Exotic Minerals & Crystals
            crops.put("enchanted_diamond", createCropJson(
                    "Enchanted Diamond",
                    "mysticalagriculture:5",
                    "enchantedwood:enchanted_diamond",
                    "1abc9c",
                    "mysticalagriculture:block/flower_rock",
                    "mysticalagriculture:item/essence_rock",
                    null
            ));

            crops.put("zirconia", createCropJson(
                    "Zirconia",
                    "mysticalagriculture:5",
                    "enchantedwood:zirconia_nodule",
                    "ecf0f1",
                    "mysticalagriculture:block/flower_rock",
                    "mysticalagriculture:item/essence_rock",
                    null
            ));

            crops.put("aerogel", createCropJson(
                    "Aerogel",
                    "mysticalagriculture:5",
                    "enchantedwood:aerogel_shard",
                    "70a1ff",
                    "mysticalagriculture:block/flower_rock",
                    "mysticalagriculture:item/essence_rock",
                    null
            ));

            crops.put("tantalum", createCropJson(
                    "Tantalum",
                    "mysticalagriculture:5",
                    "enchantedwood:tantalum_ingot",
                    "57606f",
                    "mysticalagriculture:block/flower_ingot",
                    "mysticalagriculture:item/essence_ingot",
                    null
            ));

            crops.put("hafnium", createCropJson(
                    "Hafnium",
                    "mysticalagriculture:5",
                    "enchantedwood:hafnium_ingot",
                    "747d8c",
                    "mysticalagriculture:block/flower_ingot",
                    "mysticalagriculture:item/essence_ingot",
                    null
            ));

            crops.put("neodymium", createCropJson(
                    "Neodymium",
                    "mysticalagriculture:5",
                    "enchantedwood:neodymium_magnet",
                    "a55eea",
                    "mysticalagriculture:block/flower_rock",
                    "mysticalagriculture:item/essence_rock",
                    null
            ));

            // Tier 6: Convergence Superalloys (Insanium Tier / Mystical Agradditions)
            crops.put("enchanted_netherite", createCropJson(
                    "Enchanted Netherite",
                    "mysticalagriculture:6",
                    "enchantedwood:enchanted_netherite_ingot",
                    "3b243b",
                    "mysticalagriculture:block/flower_ingot",
                    "mysticalagriculture:item/essence_ingot",
                    "mysticalagradditions:netherite_crux"
            ));

            crops.put("tan_ti", createCropJson(
                    "Tan-Ti",
                    "mysticalagriculture:6",
                    "enchantedwood:tan_ti_ingot",
                    "e17055",
                    "mysticalagriculture:block/flower_ingot",
                    "mysticalagriculture:item/essence_ingot",
                    "enchantedwood:tan_ti_block"
            ));

            crops.put("hafnium_tungsten_carbide", createCropJson(
                    "Hafnium-Tungsten Carbide",
                    "mysticalagriculture:6",
                    "enchantedwood:hafnium_tungsten_carbide_ingot",
                    "2f3542",
                    "mysticalagriculture:block/flower_ingot",
                    "mysticalagriculture:item/essence_ingot",
                    "enchantedwood:hafnium_tungsten_carbide_block"
            ));

            crops.put("neo_titanium", createCropJson(
                    "Neo-Titanium",
                    "mysticalagriculture:6",
                    "enchantedwood:neo_titanium_ingot",
                    "0984e3",
                    "mysticalagriculture:block/flower_ingot",
                    "mysticalagriculture:item/essence_ingot",
                    "enchantedwood:neo_titanium_block"
            ));

            int createdCount = 0;
            for (Map.Entry<String, String> entry : crops.entrySet()) {
                Path targetFile = cropsDir.resolve(entry.getKey() + ".json");
                if (!Files.exists(targetFile)) {
                    Files.writeString(targetFile, entry.getValue());
                    createdCount++;
                }
            }

            if (createdCount > 0) {
                EnchantedWoodMod.LOGGER.info("Exported {} Mystical Agriculture crop definitions into config/mysticalcustomization/crops/", createdCount);
            }
        } catch (IOException e) {
            EnchantedWoodMod.LOGGER.warn("Could not export Mystical Agriculture crop definitions", e);
        }
    }

    private static String createCropJson(String name, String tier, String ingredient, String hexColor, String flowerTex, String essenceTex, String crux) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(name).append("\",\n");
        sb.append("  \"type\": \"mysticalagriculture:resource\",\n");
        sb.append("  \"tier\": \"").append(tier).append("\",\n");
        if (crux != null && !crux.isEmpty()) {
            sb.append("  \"crux\": \"").append(crux).append("\",\n");
        }
        sb.append("  \"ingredient\": {\n");
        sb.append("    \"item\": \"").append(ingredient).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"color\": \"").append(hexColor).append("\",\n");
        sb.append("  \"textures\": {\n");
        sb.append("    \"flower\": \"").append(flowerTex).append("\",\n");
        sb.append("    \"essence\": \"").append(essenceTex).append("\",\n");
        sb.append("    \"seeds\": \"mysticalagriculture:item/mystical_seeds\"\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
