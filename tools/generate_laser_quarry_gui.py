import zlib
import struct
import os

WIDTH = 256
HEIGHT = 256

# Initialize empty transparent 256x256 image
# Each pixel is RGBA
img = [[(0, 0, 0, 0) for _ in range(WIDTH)] for _ in range(HEIGHT)]

def fill_rect(x, y, w, h, color):
    for j in range(y, min(HEIGHT, y + h)):
        for i in range(x, min(WIDTH, x + w)):
            if 0 <= i < WIDTH and 0 <= j < HEIGHT:
                img[j][i] = color

def draw_slot(x, y, w=18, h=18):
    # Standard Minecraft slot (outer bevel + dark interior)
    # Background inside
    fill_rect(x + 1, y + 1, w - 2, h - 2, (139, 139, 139, 255)) # #8B8B8B
    # Top and left dark shadow
    for i in range(x, x + w - 1):
        img[y][i] = (55, 55, 55, 255) # #373737
    for j in range(y, y + h - 1):
        img[j][x] = (55, 55, 55, 255)
    # Right and bottom white highlight
    for i in range(x + 1, x + w):
        img[y + h - 1][i] = (255, 255, 255, 255)
    for j in range(y + 1, y + h):
        img[j][x + w - 1] = (255, 255, 255, 255)

def draw_slot_tinted(x, y, tint_color, w=18, h=18):
    draw_slot(x, y, w, h)
    # Inner tint for special socket
    for j in range(y + 2, y + h - 2):
        for i in range(x + 2, x + w - 2):
            img[j][i] = tint_color

# 1. Base GUI panel: 176x166
# Main body #C6C6C6
fill_rect(0, 0, 176, 166, (198, 198, 198, 255))

# Outer border highlights & shadows
# Top and Left white highlight
for i in range(175):
    img[0][i] = (255, 255, 255, 255)
    img[1][i] = (219, 219, 219, 255)
for j in range(165):
    img[j][0] = (255, 255, 255, 255)
    img[j][1] = (219, 219, 219, 255)

# Bottom and Right dark shadows
for i in range(176):
    img[165][i] = (0, 0, 0, 255)
    img[164][i] = (85, 85, 85, 255)
for j in range(166):
    img[j][175] = (0, 0, 0, 255)
    img[j][174] = (85, 85, 85, 255)

# 2. Player Inventory (3 rows of 9 slots at x=7, y=83)
for row in range(3):
    for col in range(9):
        draw_slot(7 + col * 18, 83 + row * 18)

# 3. Player Hotbar (1 row of 9 slots at x=7, y=141)
for col in range(9):
    draw_slot(7 + col * 18, 141)

# 4. Energy Bar meter on left: x=7, y=17, w=10, h=56 -> slot interior is x=8, y=18, w=8, h=54
# Outer border
fill_rect(7, 17, 10, 56, (55, 55, 55, 255))
fill_rect(8, 18, 8, 54, (20, 20, 20, 255)) # Dark energy reservoir interior
# Bottom & right highlight for energy slot
for i in range(8, 17):
    img[73][i] = (255, 255, 255, 255)
for j in range(18, 74):
    img[j][16] = (255, 255, 255, 255)

# 5. Telemetry display box background at x=20, y=63, w=54, h=13 (under buttons)
fill_rect(20, 63, 54, 13, (55, 55, 55, 255))
fill_rect(21, 64, 52, 11, (30, 35, 40, 255)) # Dark navy telemetry screen
for i in range(21, 74):
    img[75][i] = (255, 255, 255, 255)
for j in range(64, 76):
    img[j][73] = (255, 255, 255, 255)

# 6. Output Buffer (3x3 grid at x=79, y=17)
for row in range(3):
    for col in range(3):
        draw_slot(79 + col * 18, 17 + row * 18)

# 7. Upgrade Sockets at x=151:
# Speed Socket (Slot 9) at y=17 (Amber/Orange tint #9C6B30)
draw_slot_tinted(151, 17, (130, 100, 60, 255))
# Range Socket (Slot 10) at y=35 (Cyan tint #308090)
draw_slot_tinted(151, 35, (60, 110, 125, 255))
# Extraction Socket (Slot 11) at y=53 (Emerald/Purple tint #508550)
draw_slot_tinted(151, 53, (70, 120, 80, 255))

# 8. Digital Storage Network LED bezel at top right: x=159, y=4, w=11, h=11
fill_rect(159, 4, 11, 11, (55, 55, 55, 255))
fill_rect(160, 5, 9, 9, (35, 35, 35, 255))
for i in range(160, 170):
    img[14][i] = (255, 255, 255, 255)
for j in range(5, 15):
    img[j][169] = (255, 255, 255, 255)

# Encode PNG
def save_png(filename):
    raw_data = bytearray()
    for row in img:
        raw_data.append(0) # Filter type 0 (None)
        for r, g, b, a in row:
            raw_data.extend([r, g, b, a])
    
    compressed = zlib.compress(raw_data, level=9)
    
    ihdr_data = struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 6, 0, 0, 0)
    ihdr_crc = zlib.crc32(b'IHDR' + ihdr_data)
    
    idat_crc = zlib.crc32(b'IDAT' + compressed)
    
    iend_crc = zlib.crc32(b'IEND')
    
    with open(filename, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        # IHDR
        f.write(struct.pack(">I", 13) + b'IHDR' + ihdr_data + struct.pack(">I", ihdr_crc))
        # IDAT
        f.write(struct.pack(">I", len(compressed)) + b'IDAT' + compressed + struct.pack(">I", idat_crc))
        # IEND
        f.write(struct.pack(">I", 0) + b'IEND' + struct.pack(">I", iend_crc))

target_path = r'c:\Projects\Minecraft Mod\src\main\resources\assets\enchantedwood\textures\gui\container\laser_quarry_gui.png'
save_png(target_path)
print("Saved laser_quarry_gui.png successfully!")
