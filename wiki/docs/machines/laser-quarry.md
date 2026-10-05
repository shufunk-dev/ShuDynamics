# ⚡ Digital Laser Quarry & Extraction Cores

The **Digital Laser Quarry** is an autonomous high-throughput chunk excavation machine. It projects a focused holographic boundary across an entire chunk or multi-chunk radius, excavates all materials layer-by-layer down to bedrock, and channels extracted ores directly into your digital network.

---

## 🏗️ Machine Overview

<MachineShowcase 
  name="Digital Laser Quarry"
  icon="/textures/block/laser_quarry_front.png"
  tier="Tier 4 (High-Tech Excavation)"
  tierClass="tier-high"
  category="Autonomous Multiblock Excavation"
  description="High-frequency laser excavation station. Mines an entire chunk radius down to bedrock with Fortune or Silk Touch, drawing wireless power from your base and beaming items directly into digital storage crystals!"
  :specs="{
    'Base Mining Footprint': '1x1 Chunk (16x16 blocks)',
    'Range Upgrade Tier 1': '3x3 Chunks (48x48 blocks)',
    'Range Upgrade Tier 2': '5x5 Chunks (80x80 blocks)',
    'Energy Buffer': '500,000 FE',
    'Operating Cost': '150 FE per block mined',
    'Automatic Chunk Loading': 'Force-loads active perimeter chunks 24/7 while mining',
    'Modifier Cores': 'Fortune Core, Silk Touch Core, Range T1/T2, Blaze Overclock Core'
  }"
/>

---

## 🛰️ Remote Wrench Base Linking

The Digital Laser Quarry features **Zero-Latency Remote Storage & Energy Binding**:

1. **Bind Base Network**: With an **Industrial Wrench** in hand, `Shift + Right-Click` your home base **Storage Controller** or **Storage Terminal**. The wrench will store the dimensional coordinates and network address.
2. **Bind Quarry**: Travel to your desired mining site (in the Overworld, Nether, or Mining Dimension) and `Shift + Right-Click` the placed **Digital Laser Quarry**.
3. **Wireless Operation**: The quarry GUI will illuminate a cyan status indicator (`● Connected (Remote Base)`). It will immediately draw power wirelessly from your base batteries and teleport all harvested ores directly into your Drive Bay crystals!

---

## 🧭 Autonomous Chunk Loading & Bounding Beacons

- **24/7 Autonomous Chunk Loading**: The quarry force-loads all chunks within its current operating radius ($1\times1$, $3\times3$, or $5\times5$ chunks) automatically while powered and active. Tickets are automatically released when mining completes or if the machine is broken.
- **Holographic Boundary Projector**: Projects vibrant visual laser beams delineating the exact mining perimeter so you always know where excavation is occurring.

---

## 🔮 Upgrade Cores & Modifiers

The Laser Quarry includes dedicated sockets for range and harvesting modifiers:

| Upgrade Core | Effect | Power Draw Modifier |
| :--- | :--- | :--- |
| **Tier 1 Range Core** | Expands scan perimeter to **3x3 Chunks (48x48 blocks)** | +50 FE / block |
| **Tier 2 Range Core** | Expands scan perimeter to **5x5 Chunks (80x80 blocks)** | +100 FE / block |
| **Quarry Fortune Core** | Applies **Fortune III** ore multiplication yields | +50 FE / block |
| **Quarry Silk Touch Core** | Harvests raw ore blocks intact without breaking them | +25 FE / block |
| **Blaze Overclock Core** | Accelerates laser sweep speed up to **8x** | +150 FE / block |

---

## 📜 Crafting Recipes

### Digital Laser Quarry
<MinecraftRecipe id="laser_quarry" />

### Tier 1 Quarry Range Core (3x3 Chunks)
<MinecraftRecipe id="range_upgrade_t1" />

### Tier 2 Quarry Range Core (5x5 Chunks)
<MinecraftRecipe id="range_upgrade_t2" />

### Quarry Fortune Core
<MinecraftRecipe id="fortune_core" />

### Quarry Silk Touch Core
<MinecraftRecipe id="silk_touch_core" />
