import os
import json
import zlib
import struct

items_items_dir = "src/main/resources/assets/enchantedwood/items"
models_item_dir = "src/main/resources/assets/enchantedwood/models/item"
textures_item_dir = "src/main/resources/assets/enchantedwood/textures/item"
recipe_dir = "src/main/resources/data/enchantedwood/recipe"
recipes_dir = "src/main/resources/data/enchantedwood/recipes"

os.makedirs(items_items_dir, exist_ok=True)
os.makedirs(models_item_dir, exist_ok=True)
os.makedirs(textures_item_dir, exist_ok=True)
os.makedirs(recipe_dir, exist_ok=True)
os.makedirs(recipes_dir, exist_ok=True)

def write_png(filename, width, height, rgba_data):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)
        for x in range(width):
            idx = (y * width + x) * 4
            raw_data.extend(rgba_data[idx:idx+4])

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(raw_data)))
    png += chunk(b'IEND', b'')

    with open(filename, 'wb') as f:
        f.write(png)
    print(f"Generated {filename}")

def register_asset(item_name):
    # 1. Item definition
    item_def = {
        "model": {
            "type": "minecraft:model",
            "model": f"enchantedwood:item/{item_name}"
        }
    }
    with open(f"{items_items_dir}/{item_name}.json", "w") as f:
        json.dump(item_def, f, indent=2)

    # 2. Model definition
    model_def = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"enchantedwood:item/{item_name}"
        }
    }
    with open(f"{models_item_dir}/{item_name}.json", "w") as f:
        json.dump(model_def, f, indent=2)

def generate_pixel_art(item_name, grid, color_map):
    rgba = bytearray()
    for y in range(16):
        for x in range(16):
            ch = grid[y][x]
            r, g, b, a = color_map.get(ch, (0, 0, 0, 0))
            rgba.extend([r, g, b, a])
    write_png(f"{textures_item_dir}/{item_name}.png", 16, 16, rgba)
    register_asset(item_name)

# Palettes
iron_pal = {'H': (240, 240, 245, 255), 'L': (210, 210, 215, 255), 'M': (155, 155, 160, 255), 'D': (100, 100, 105, 255), 'O': (50, 50, 55, 255), '.': (0,0,0,0)}
steel_pal = {'H': (220, 235, 245, 255), 'L': (160, 185, 205, 255), 'M': (110, 130, 150, 255), 'D': (70, 85, 100, 255), 'O': (35, 45, 55, 255), '.': (0,0,0,0)}
dia_pal = {'H': (195, 255, 250, 255), 'L': (100, 240, 230, 255), 'M': (45, 190, 180, 255), 'D': (20, 135, 130, 255), 'O': (10, 75, 75, 255), '.': (0,0,0,0)}
tit_pal = {'H': (180, 215, 255, 255), 'L': (90, 150, 240, 255), 'M': (50, 95, 190, 255), 'D': (30, 60, 130, 255), 'O': (15, 30, 70, 255), '.': (0,0,0,0)}
neth_pal = {'H': (130, 115, 120, 255), 'L': (95, 80, 85, 255), 'M': (68, 55, 60, 255), 'D': (48, 38, 42, 255), 'O': (25, 20, 22, 255), '.': (0,0,0,0)}

# Tree Saw Matrix (Circular toothed saw blade)
saw_grid = [
    "......OOOO......",
    "....OOHHLMOO....",
    "...OHHLMMMMDO...",
    "..OHHLM....MDO..",
    ".OHLM........DO.",
    ".OHLM...OO...DO.",
    "OHLM...OHLDO.MDO",
    "OHLM...OLMDO.MDO",
    "OHLM...ODDDO.MDO",
    "OHLM...ODDDO.MDO",
    ".OHLM...OO...DO.",
    ".OHLM........DO.",
    "..OHLM.....MDO..",
    "...OHMDDDDDDO...",
    "....OOMMDDOO....",
    "......OOOO......"
]

generate_pixel_art("iron_tree_saw", saw_grid, iron_pal)
generate_pixel_art("steel_tree_saw", saw_grid, steel_pal)
generate_pixel_art("diamond_tree_saw", saw_grid, dia_pal)
generate_pixel_art("titanium_tree_saw", saw_grid, tit_pal)
generate_pixel_art("netherite_tree_saw", saw_grid, neth_pal)

# Crop Harvester Matrix (Scythe / Agricultural Combine Reel)
harvester_grid = [
    "....OOOOOOOO....",
    "...OHHHHHHHLO...",
    "..OHLLLLLLLLMO..",
    ".OHLMMMMMMMMMDO.",
    ".OLM.........MD.",
    "OHLM..OOOOO..MDO",
    "OHLM.OHHLMDO.MDO",
    "OHLM.OLM.MDO.MDO",
    "OHLM.OLM.MDO.MDO",
    "OHLM.ODD.DDO.MDO",
    "OHLM..OOOOO..MDO",
    ".OLM.........MD.",
    ".OHLMMMMMMMMMDO.",
    "..OHMDDDDDDDMO..",
    "...OMDDDDDDLO...",
    "....OOOOOOOO...."
]

generate_pixel_art("iron_crop_harvester", harvester_grid, iron_pal)
generate_pixel_art("steel_crop_harvester", harvester_grid, steel_pal)
generate_pixel_art("diamond_crop_harvester", harvester_grid, dia_pal)
generate_pixel_art("titanium_crop_harvester", harvester_grid, tit_pal)
generate_pixel_art("netherite_crop_harvester", harvester_grid, neth_pal)

# Headlights
halogen_pal = {
    'G': (255, 245, 180, 255), # warm yellow glow
    'H': (255, 255, 220, 255),
    'M': (180, 160, 100, 255),
    'C': (140, 140, 145, 255), # metal casing
    'D': (80, 80, 85, 255),
    'O': (40, 40, 45, 255),
    '.': (0,0,0,0)
}

led_pal = {
    'G': (180, 240, 255, 255), # cool white/cyan LED
    'H': (240, 255, 255, 255),
    'M': (100, 180, 220, 255),
    'C': (110, 130, 150, 255),
    'D': (60, 75, 90, 255),
    'O': (25, 35, 45, 255),
    '.': (0,0,0,0)
}

xenon_pal = {
    'G': (220, 180, 255, 255), # high beam violet xenon
    'H': (255, 240, 255, 255),
    'M': (160, 100, 230, 255),
    'C': (90, 120, 180, 255),
    'D': (40, 60, 110, 255),
    'O': (15, 25, 55, 255),
    '.': (0,0,0,0)
}

light_grid = [
    "................",
    "...OOOO..OOOO...",
    "..OCCCO..OCCCO..",
    ".OCGGHCO.OCGGHCO",
    ".OCGGHCO.OCGGHCO",
    ".OCMMDCO.OCMMDCO",
    ".OCDDCDO.OCDDCDO",
    "..OOOOO..OOOOO..",
    "....OC....CO....",
    "...OCC....CCO...",
    "..OCC......CCO..",
    ".OCC........CCO.",
    ".OD..........DO.",
    ".OD..........DO.",
    "..OOOOOOOOOOOO..",
    "................"
]

generate_pixel_art("halogen_headlights", light_grid, halogen_pal)
generate_pixel_art("led_floodlights", light_grid, led_pal)
generate_pixel_art("xenon_high_beams", light_grid, xenon_pal)

# Recipes Generation
def save_recipe(name, recipe_data):
    for d in [recipe_dir, recipes_dir]:
        with open(f"{d}/{name}.json", "w") as f:
            json.dump(recipe_data, f, indent=2)
    print(f"Saved recipe {name}")

# Tree Saws
save_recipe("iron_tree_saw", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "I": "minecraft:iron_ingot",
        "A": "minecraft:iron_axe",
        "G": "enchantedwood:iron_gear"
    },
    "pattern": [
        " I ",
        "AGA",
        " I "
    ],
    "result": {"count": 1, "id": "enchantedwood:iron_tree_saw"}
})

save_recipe("steel_tree_saw", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "S": "enchantedwood:steel_ingot",
        "G": "enchantedwood:steel_gear",
        "P": "enchantedwood:iron_tree_saw"
    },
    "pattern": [
        " S ",
        "SPS",
        " G "
    ],
    "result": {"count": 1, "id": "enchantedwood:steel_tree_saw"}
})

save_recipe("diamond_tree_saw", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "D": "minecraft:diamond",
        "G": "enchantedwood:diamond_gear",
        "P": "enchantedwood:steel_tree_saw"
    },
    "pattern": [
        " D ",
        "DPD",
        " G "
    ],
    "result": {"count": 1, "id": "enchantedwood:diamond_tree_saw"}
})

save_recipe("titanium_tree_saw", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "T": "enchantedwood:titanium_ingot",
        "G": "enchantedwood:titanium_gear",
        "P": "enchantedwood:diamond_tree_saw"
    },
    "pattern": [
        " T ",
        "TPT",
        " G "
    ],
    "result": {"count": 1, "id": "enchantedwood:titanium_tree_saw"}
})

save_recipe("netherite_tree_saw", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "N": "minecraft:netherite_ingot",
        "G": "enchantedwood:netherite_gear",
        "P": "enchantedwood:diamond_tree_saw"
    },
    "pattern": [
        " N ",
        "NPN",
        " G "
    ],
    "result": {"count": 1, "id": "enchantedwood:netherite_tree_saw"}
})

# Crop Harvesters
save_recipe("iron_crop_harvester", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "I": "minecraft:iron_ingot",
        "H": "minecraft:iron_hoe",
        "G": "enchantedwood:iron_gear"
    },
    "pattern": [
        " I ",
        "HGH",
        " I "
    ],
    "result": {"count": 1, "id": "enchantedwood:iron_crop_harvester"}
})

save_recipe("steel_crop_harvester", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "S": "enchantedwood:steel_ingot",
        "G": "enchantedwood:steel_gear",
        "P": "enchantedwood:iron_crop_harvester"
    },
    "pattern": [
        " S ",
        "SPS",
        " G "
    ],
    "result": {"count": 1, "id": "enchantedwood:steel_crop_harvester"}
})

save_recipe("diamond_crop_harvester", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "D": "minecraft:diamond",
        "G": "enchantedwood:diamond_gear",
        "P": "enchantedwood:steel_crop_harvester"
    },
    "pattern": [
        " D ",
        "DPD",
        " G "
    ],
    "result": {"count": 1, "id": "enchantedwood:diamond_crop_harvester"}
})

save_recipe("titanium_crop_harvester", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "T": "enchantedwood:titanium_ingot",
        "G": "enchantedwood:titanium_gear",
        "P": "enchantedwood:diamond_crop_harvester"
    },
    "pattern": [
        " T ",
        "TPT",
        " G "
    ],
    "result": {"count": 1, "id": "enchantedwood:titanium_crop_harvester"}
})

save_recipe("netherite_crop_harvester", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "N": "minecraft:netherite_ingot",
        "G": "enchantedwood:netherite_gear",
        "P": "enchantedwood:diamond_crop_harvester"
    },
    "pattern": [
        " N ",
        "NPN",
        " G "
    ],
    "result": {"count": 1, "id": "enchantedwood:netherite_crop_harvester"}
})

# Headlights Recipes
save_recipe("halogen_headlights", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "I": "minecraft:iron_ingot",
        "G": "minecraft:glass_pane",
        "R": "minecraft:glowstone_dust"
    },
    "pattern": [
        "IGI",
        "IRI",
        " I "
    ],
    "result": {"count": 1, "id": "enchantedwood:halogen_headlights"}
})

save_recipe("led_floodlights", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "S": "enchantedwood:steel_ingot",
        "L": "minecraft:redstone_lamp",
        "H": "enchantedwood:halogen_headlights"
    },
    "pattern": [
        "SLS",
        "LHL",
        " S "
    ],
    "result": {"count": 1, "id": "enchantedwood:led_floodlights"}
})

save_recipe("xenon_high_beams", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {
        "T": "enchantedwood:titanium_ingot",
        "V": "enchantedwood:volcanic_glass",
        "L": "enchantedwood:led_floodlights"
    },
    "pattern": [
        "TVT",
        "VLV",
        " T "
    ],
    "result": {"count": 1, "id": "enchantedwood:xenon_high_beams"}
})

print("Asset generation complete!")
