import zlib
import struct
import os

def write_png(filename, width, height, rgba_data):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0) # Filter type 0
        for x in range(width):
            idx = (y * width + x) * 4
            raw_data.extend(rgba_data[idx:idx+4])

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(raw_data)))
    png += chunk(b'IEND', b'')

    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, 'wb') as f:
        f.write(png)
    print(f"Saved {filename}")

def create_drill_bit(palette, output_path):
    hl, ml, md, dk, ol = palette

    grid = [
        "................",
        "...........OHO..",
        "..........OHLMO.",
        ".........OHLMDO.",
        "........OHLMDO..",
        ".......OHLMDOO..",
        "......OHLMDOO...",
        ".....OHLMDOO....",
        "....OHLMDOO.....",
        "...OHLMDOO......",
        "..OHLMDOO.......",
        "..OLMDOO........",
        "...ODDDO........",
        "...ODDDO........",
        "....OOO.........",
        "................"
    ]

    color_map = {
        '.': (0, 0, 0, 0),
        'H': hl,
        'L': ml,
        'M': md,
        'D': dk,
        'O': ol
    }

    rgba_data = bytearray()
    for y in range(16):
        for x in range(16):
            ch = grid[y][x]
            r, g, b, a = color_map[ch]
            rgba_data.extend([r, g, b, a])

    write_png(output_path, 16, 16, rgba_data)

iron_palette = [
    (240, 240, 245, 255),
    (210, 210, 215, 255),
    (155, 155, 160, 255),
    (100, 100, 105, 255),
    (50, 50, 55, 255)
]

steel_palette = [
    (220, 235, 245, 255),
    (160, 185, 205, 255),
    (110, 130, 150, 255),
    (70, 85, 100, 255),
    (35, 45, 55, 255)
]

diamond_palette = [
    (195, 255, 250, 255),
    (100, 240, 230, 255),
    (45, 190, 180, 255),
    (20, 135, 130, 255),
    (10, 75, 75, 255)
]

titanium_palette = [
    (180, 215, 255, 255),
    (90, 150, 240, 255),
    (50, 95, 190, 255),
    (30, 60, 130, 255),
    (15, 30, 70, 255)
]

netherite_palette = [
    (130, 115, 120, 255),
    (95, 80, 85, 255),
    (68, 55, 60, 255),
    (48, 38, 42, 255),
    (25, 20, 22, 255)
]

out_dir = "src/main/resources/assets/enchantedwood/textures/item"
create_drill_bit(iron_palette, f"{out_dir}/iron_drill_bit.png")
create_drill_bit(steel_palette, f"{out_dir}/steel_drill_bit.png")
create_drill_bit(diamond_palette, f"{out_dir}/diamond_drill_bit.png")
create_drill_bit(titanium_palette, f"{out_dir}/titanium_drill_bit.png")
create_drill_bit(netherite_palette, f"{out_dir}/netherite_drill_bit.png")
