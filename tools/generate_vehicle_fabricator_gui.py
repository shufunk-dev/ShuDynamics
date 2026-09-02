import zlib
import struct
import os

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

    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, 'wb') as f:
        f.write(png)
    print(f"Generated {filename}")

W, H = 256, 256
img = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]

# Background rectangle: 176x222 at (0, 0)
BG_W, BG_H = 176, 222
BG_COLOR = (198, 198, 198, 255)
BORDER_DARK = (55, 55, 55, 255)
BORDER_LIGHT = (255, 255, 255, 255)

for y in range(BG_H):
    for x in range(BG_W):
        img[y][x] = BG_COLOR

# Draw 3D outer border
for x in range(BG_W):
    img[0][x] = BORDER_LIGHT
    img[BG_H-1][x] = BORDER_DARK
for y in range(BG_H):
    img[y][0] = BORDER_LIGHT
    img[y][BG_W-1] = BORDER_DARK

def draw_slot(sx, sy, is_large=False):
    sw, sh = (26, 26) if is_large else (18, 18)
    for y in range(sh):
        for x in range(sw):
            px, py = sx + x, sy + y
            if px < W and py < H:
                if x == 0 or y == 0:
                    img[py][px] = (55, 55, 55, 255)
                elif x == sw - 1 or y == sh - 1:
                    img[py][px] = (255, 255, 255, 255)
                else:
                    img[py][px] = (139, 139, 139, 255)

# 1. Machine Slots
# Vehicle Upgrade slot (x=142, y=24)
draw_slot(141, 23)

# Seat slot (x=70, y=22)
draw_slot(69, 21)

# Engine slot (x=34, y=44)
draw_slot(33, 43)

# Chassis slot (x=70, y=54)
draw_slot(69, 53)

# Suspension slot (x=106, y=44)
draw_slot(105, 43)

# Tires slot (x=34, y=86)
draw_slot(33, 85)

# Headlights slot (x=70, y=86) -> NEW PROMINENT SLOT BOX
draw_slot(69, 85)

# Trunk slot (x=106, y=86)
draw_slot(105, 85)

# Output slot (x=142, y=82) -> Large slot box (26x26)
draw_slot(137, 77, is_large=True)

# Battery slot (x=6, y=118)
draw_slot(5, 117)

# 2. Energy Bar (x=8, y=18, w=12, h=96)
for y in range(96):
    for x in range(12):
        px, py = 8 + x, 18 + y
        if x == 0 or y == 0:
            img[py][px] = (55, 55, 55, 255)
        elif x == 11 or y == 95:
            img[py][px] = (255, 255, 255, 255)
        else:
            img[py][px] = (30, 30, 30, 255)

# 3. Player Inventory (3 rows x 9 cols at 8, 140)
for r in range(3):
    for c in range(9):
        draw_slot(7 + c * 18, 139 + r * 18)

# 4. Player Hotbar (1 row x 9 cols at 8, 198)
for c in range(9):
    draw_slot(7 + c * 18, 197)

# Flatten to RGBA
rgba_data = bytearray()
for y in range(H):
    for x in range(W):
        r, g, b, a = img[y][x]
        rgba_data.extend([r, g, b, a])

write_png("src/main/resources/assets/enchantedwood/textures/gui/container/vehicle_fabricator_gui.png", W, H, rgba_data)
