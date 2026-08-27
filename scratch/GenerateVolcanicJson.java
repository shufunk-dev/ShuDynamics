import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GenerateVolcanicJson {
    public static void main(String[] args) throws Exception {
        String base = "src/main/resources/";
        String assets = base + "assets/enchantedwood/";
        String data = base + "data/enchantedwood/";

        // 1. Simple Cube Blocks: volcanic_soil, pozzolanic_asphalt, volcanic_bricks
        String[] cubes = {"volcanic_soil", "pozzolanic_asphalt", "volcanic_bricks"};
        for (String name : cubes) {
            // Blockstate
            write(assets + "blockstates/" + name + ".json",
                    "{\"variants\":{\"\":{\"model\":\"enchantedwood:block/" + name + "\"}}}");
            // Block Model
            write(assets + "models/block/" + name + ".json",
                    "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"enchantedwood:block/" + name + "\"}}");
            // Item Model
            write(assets + "models/item/" + name + ".json",
                    "{\"parent\":\"enchantedwood:block/" + name + "\"}");
            // 1.21.2+ Item Definition
            write(assets + "items/" + name + ".json",
                    "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"enchantedwood:item/" + name + "\"}}");
            // Loot Table
            write(data + "loot_table/blocks/" + name + ".json",
                    "{\"type\":\"minecraft:block\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"enchantedwood:" + name + "\"}],\"conditions\":[{\"condition\":\"minecraft:survives_explosion\"}]}]}");
        }

        // 2. Stairs: volcanic_brick_stairs
        write(assets + "blockstates/volcanic_brick_stairs.json", """
        {
          "variants": {
            "facing=east,half=bottom,shape=straight": { "model": "enchantedwood:block/volcanic_brick_stairs" },
            "facing=west,half=bottom,shape=straight": { "model": "enchantedwood:block/volcanic_brick_stairs", "y": 180, "uvlock": true },
            "facing=south,half=bottom,shape=straight": { "model": "enchantedwood:block/volcanic_brick_stairs", "y": 90, "uvlock": true },
            "facing=north,half=bottom,shape=straight": { "model": "enchantedwood:block/volcanic_brick_stairs", "y": 270, "uvlock": true },
            "facing=east,half=bottom,shape=outer_right": { "model": "enchantedwood:block/volcanic_brick_stairs_outer" },
            "facing=west,half=bottom,shape=outer_right": { "model": "enchantedwood:block/volcanic_brick_stairs_outer", "y": 180, "uvlock": true },
            "facing=south,half=bottom,shape=outer_right": { "model": "enchantedwood:block/volcanic_brick_stairs_outer", "y": 90, "uvlock": true },
            "facing=north,half=bottom,shape=outer_right": { "model": "enchantedwood:block/volcanic_brick_stairs_outer", "y": 270, "uvlock": true },
            "facing=east,half=bottom,shape=outer_left": { "model": "enchantedwood:block/volcanic_brick_stairs_outer", "y": 270, "uvlock": true },
            "facing=west,half=bottom,shape=outer_left": { "model": "enchantedwood:block/volcanic_brick_stairs_outer", "y": 90, "uvlock": true },
            "facing=south,half=bottom,shape=outer_left": { "model": "enchantedwood:block/volcanic_brick_stairs_outer" },
            "facing=north,half=bottom,shape=outer_left": { "model": "enchantedwood:block/volcanic_brick_stairs_outer", "y": 180, "uvlock": true },
            "facing=east,half=bottom,shape=inner_right": { "model": "enchantedwood:block/volcanic_brick_stairs_inner" },
            "facing=west,half=bottom,shape=inner_right": { "model": "enchantedwood:block/volcanic_brick_stairs_inner", "y": 180, "uvlock": true },
            "facing=south,half=bottom,shape=inner_right": { "model": "enchantedwood:block/volcanic_brick_stairs_inner", "y": 90, "uvlock": true },
            "facing=north,half=bottom,shape=inner_right": { "model": "enchantedwood:block/volcanic_brick_stairs_inner", "y": 270, "uvlock": true },
            "facing=east,half=bottom,shape=inner_left": { "model": "enchantedwood:block/volcanic_brick_stairs_inner", "y": 270, "uvlock": true },
            "facing=west,half=bottom,shape=inner_left": { "model": "enchantedwood:block/volcanic_brick_stairs_inner", "y": 90, "uvlock": true },
            "facing=south,half=bottom,shape=inner_left": { "model": "enchantedwood:block/volcanic_brick_stairs_inner" },
            "facing=north,half=bottom,shape=inner_left": { "model": "enchantedwood:block/volcanic_brick_stairs_inner", "y": 180, "uvlock": true },
            "facing=east,half=top,shape=straight": { "model": "enchantedwood:block/volcanic_brick_stairs", "x": 180, "uvlock": true },
            "facing=west,half=top,shape=straight": { "model": "enchantedwood:block/volcanic_brick_stairs", "x": 180, "y": 180, "uvlock": true },
            "facing=south,half=top,shape=straight": { "model": "enchantedwood:block/volcanic_brick_stairs", "x": 180, "y": 90, "uvlock": true },
            "facing=north,half=top,shape=straight": { "model": "enchantedwood:block/volcanic_brick_stairs", "x": 180, "y": 270, "uvlock": true }
          }
        }
        """);
        write(assets + "models/block/volcanic_brick_stairs.json",
                "{\"parent\":\"minecraft:block/stairs\",\"textures\":{\"bottom\":\"enchantedwood:block/volcanic_bricks\",\"top\":\"enchantedwood:block/volcanic_bricks\",\"side\":\"enchantedwood:block/volcanic_bricks\"}}");
        write(assets + "models/block/volcanic_brick_stairs_inner.json",
                "{\"parent\":\"minecraft:block/inner_stairs\",\"textures\":{\"bottom\":\"enchantedwood:block/volcanic_bricks\",\"top\":\"enchantedwood:block/volcanic_bricks\",\"side\":\"enchantedwood:block/volcanic_bricks\"}}");
        write(assets + "models/block/volcanic_brick_stairs_outer.json",
                "{\"parent\":\"minecraft:block/outer_stairs\",\"textures\":{\"bottom\":\"enchantedwood:block/volcanic_bricks\",\"top\":\"enchantedwood:block/volcanic_bricks\",\"side\":\"enchantedwood:block/volcanic_bricks\"}}");
        write(assets + "models/item/volcanic_brick_stairs.json", "{\"parent\":\"enchantedwood:block/volcanic_brick_stairs\"}");
        write(assets + "items/volcanic_brick_stairs.json", "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"enchantedwood:item/volcanic_brick_stairs\"}}");
        write(data + "loot_table/blocks/volcanic_brick_stairs.json",
                "{\"type\":\"minecraft:block\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"enchantedwood:volcanic_brick_stairs\"}],\"conditions\":[{\"condition\":\"minecraft:survives_explosion\"}]}]}");

        // 3. Slab: volcanic_brick_slab
        write(assets + "blockstates/volcanic_brick_slab.json", """
        {
          "variants": {
            "type=bottom": { "model": "enchantedwood:block/volcanic_brick_slab" },
            "type=top": { "model": "enchantedwood:block/volcanic_brick_slab_top" },
            "type=double": { "model": "enchantedwood:block/volcanic_bricks" }
          }
        }
        """);
        write(assets + "models/block/volcanic_brick_slab.json",
                "{\"parent\":\"minecraft:block/slab\",\"textures\":{\"bottom\":\"enchantedwood:block/volcanic_bricks\",\"top\":\"enchantedwood:block/volcanic_bricks\",\"side\":\"enchantedwood:block/volcanic_bricks\"}}");
        write(assets + "models/block/volcanic_brick_slab_top.json",
                "{\"parent\":\"minecraft:block/slab_top\",\"textures\":{\"bottom\":\"enchantedwood:block/volcanic_bricks\",\"top\":\"enchantedwood:block/volcanic_bricks\",\"side\":\"enchantedwood:block/volcanic_bricks\"}}");
        write(assets + "models/item/volcanic_brick_slab.json", "{\"parent\":\"enchantedwood:block/volcanic_brick_slab\"}");
        write(assets + "items/volcanic_brick_slab.json", "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"enchantedwood:item/volcanic_brick_slab\"}}");
        write(data + "loot_table/blocks/volcanic_brick_slab.json",
                "{\"type\":\"minecraft:block\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"enchantedwood:volcanic_brick_slab\",\"functions\":[{\"function\":\"minecraft:set_count\",\"count\":2,\"conditions\":[{\"condition\":\"minecraft:block_state_property\",\"block\":\"enchantedwood:volcanic_brick_slab\",\"properties\":{\"type\":\"double\"}}]}]}],\"conditions\":[{\"condition\":\"minecraft:survives_explosion\"}]}]}");

        // 4. Machine: soil_infuser
        write(assets + "blockstates/soil_infuser.json", """
        {
          "variants": {
            "facing=north,lit=false": { "model": "enchantedwood:block/soil_infuser" },
            "facing=east,lit=false": { "model": "enchantedwood:block/soil_infuser", "y": 90 },
            "facing=south,lit=false": { "model": "enchantedwood:block/soil_infuser", "y": 180 },
            "facing=west,lit=false": { "model": "enchantedwood:block/soil_infuser", "y": 270 },
            "facing=north,lit=true": { "model": "enchantedwood:block/soil_infuser_on" },
            "facing=east,lit=true": { "model": "enchantedwood:block/soil_infuser_on", "y": 90 },
            "facing=south,lit=true": { "model": "enchantedwood:block/soil_infuser_on", "y": 180 },
            "facing=west,lit=true": { "model": "enchantedwood:block/soil_infuser_on", "y": 270 }
          }
        }
        """);
        write(assets + "models/block/soil_infuser.json", """
        {
          "parent": "minecraft:block/cube",
          "textures": {
            "particle": "enchantedwood:block/soil_infuser_side",
            "north": "enchantedwood:block/soil_infuser_front",
            "south": "enchantedwood:block/soil_infuser_side",
            "east": "enchantedwood:block/soil_infuser_side",
            "west": "enchantedwood:block/soil_infuser_side",
            "up": "enchantedwood:block/soil_infuser_top",
            "down": "enchantedwood:block/soil_infuser_bottom"
          }
        }
        """);
        write(assets + "models/block/soil_infuser_on.json", """
        {
          "parent": "minecraft:block/cube",
          "textures": {
            "particle": "enchantedwood:block/soil_infuser_side",
            "north": "enchantedwood:block/soil_infuser_front_on",
            "south": "enchantedwood:block/soil_infuser_side",
            "east": "enchantedwood:block/soil_infuser_side",
            "west": "enchantedwood:block/soil_infuser_side",
            "up": "enchantedwood:block/soil_infuser_top",
            "down": "enchantedwood:block/soil_infuser_bottom"
          }
        }
        """);
        write(assets + "models/item/soil_infuser.json", "{\"parent\":\"enchantedwood:block/soil_infuser\"}");
        write(assets + "items/soil_infuser.json", "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"enchantedwood:item/soil_infuser\"}}");
        write(data + "loot_table/blocks/soil_infuser.json",
                "{\"type\":\"minecraft:block\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"enchantedwood:soil_infuser\"}],\"conditions\":[{\"condition\":\"minecraft:survives_explosion\"}]}]}");

        // 5. Item: volcanic_fertilizer
        write(assets + "models/item/volcanic_fertilizer.json",
                "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"enchantedwood:item/volcanic_fertilizer\"}}");
        write(assets + "items/volcanic_fertilizer.json",
                "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"enchantedwood:item/volcanic_fertilizer\"}}");

        // 6. Recipes
        write(data + "recipe/volcanic_fertilizer.json", """
        {
          "type": "minecraft:crafting_shapeless",
          "category": "misc",
          "ingredients": [
            "enchantedwood:volcanic_ash",
            "minecraft:bone_meal",
            "enchantedwood:sulfur_dust"
          ],
          "result": {
            "count": 4,
            "id": "enchantedwood:volcanic_fertilizer"
          }
        }
        """);

        write(data + "recipe/pozzolanic_asphalt.json", """
        {
          "type": "minecraft:crafting_shaped",
          "category": "building",
          "pattern": [
            "AVA",
            "VT ",
            "   "
          ],
          "key": {
            "V": "enchantedwood:volcanic_ash",
            "A": "minecraft:sand",
            "T": "enchantedwood:mineral_tar"
          },
          "result": {
            "count": 8,
            "id": "enchantedwood:pozzolanic_asphalt"
          }
        }
        """);

        write(data + "recipe/volcanic_bricks.json", """
        {
          "type": "minecraft:crafting_shaped",
          "category": "building",
          "pattern": [
            "VB",
            "BV"
          ],
          "key": {
            "V": "enchantedwood:volcanic_ash",
            "B": "minecraft:basalt"
          },
          "result": {
            "count": 4,
            "id": "enchantedwood:volcanic_bricks"
          }
        }
        """);

        write(data + "recipe/volcanic_brick_stairs.json", """
        {
          "type": "minecraft:crafting_shaped",
          "category": "building",
          "pattern": [
            "B  ",
            "BB ",
            "BBB"
          ],
          "key": {
            "B": "enchantedwood:volcanic_bricks"
          },
          "result": {
            "count": 4,
            "id": "enchantedwood:volcanic_brick_stairs"
          }
        }
        """);

        write(data + "recipe/volcanic_brick_slab.json", """
        {
          "type": "minecraft:crafting_shaped",
          "category": "building",
          "pattern": [
            "BBB"
          ],
          "key": {
            "B": "enchantedwood:volcanic_bricks"
          },
          "result": {
            "count": 6,
            "id": "enchantedwood:volcanic_brick_slab"
          }
        }
        """);

        write(data + "recipe/soil_infuser.json", """
        {
          "type": "minecraft:crafting_shaped",
          "category": "misc",
          "pattern": [
            "PVP",
            "SMF",
            "TCT"
          ],
          "key": {
            "P": "minecraft:piston",
            "V": "enchantedwood:volcanic_fertilizer",
            "S": "enchantedwood:steel_plate",
            "M": "enchantedwood:manyullyn_ingot",
            "F": "minecraft:furnace",
            "T": "enchantedwood:tungsten_carbide_ingot",
            "C": "enchantedwood:tungsten_cable"
          },
          "result": {
            "count": 1,
            "id": "enchantedwood:soil_infuser"
          }
        }
        """);

        System.out.println("All Volcanic JSON models and definitions generated successfully!");
    }

    private static void write(String path, String content) throws Exception {
        File f = new File(path);
        f.getParentFile().mkdirs();
        Files.writeString(Paths.get(path), content);
    }
}
