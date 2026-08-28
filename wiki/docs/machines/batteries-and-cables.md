# 🔋 Batteries, Portable Battery Packs & Energy Cabling

Power grids require buffers to store excess energy generated during low-demand periods and distribute it to high-consumption machines. ShuDynamics provides both stationary **Energy Storage Units** for base power grids and portable **Handheld Battery Packs** for vehicle power and field charging.

---

## ⚡ Stationary Energy Storage Units (Batteries)

<MachineShowcase 
  name="Copper Battery"
  icon="/textures/block/copper_battery_front.png"
  tier="Tier 1"
  tierClass="tier-early"
  category="Energy Storage"
  description="Early-game energy accumulator. Buffers up to 100,000 E to keep your refiners and early machines running steadily."
  :specs="{
    'Max Storage Capacity': '100,000 E',
    'Max Input / Output Rate': '200 E/t',
    'Charging Slots': '1 Internal Item Charge Slot'
  }"
  placedImage="/images/machines/copper_battery_placed.png"
  guiImage="/images/machines/copper_battery_gui.png"
/>

<MachineShowcase 
  name="Aluminum Battery"
  icon="/textures/block/aluminum_battery_front.png"
  tier="Tier 2"
  tierClass="tier-mid"
  category="Energy Storage"
  description="Mid-tier cell buffering 500,000 E with high transfer throughput for Oxygen Generators and Refiners."
  :specs="{
    'Max Storage Capacity': '500,000 E',
    'Max Input / Output Rate': '800 E/t',
    'Charging Slots': '1 Internal Item Charge Slot'
  }"
  placedImage="/images/machines/aluminum_battery_placed.png"
  guiImage="/images/machines/aluminum_battery_gui.png"
/>

<MachineShowcase 
  name="Steel Battery"
  icon="/textures/block/steel_battery_front.png"
  tier="Tier 3"
  tierClass="tier-high"
  category="Heavy Energy Storage"
  description="Heavy industrial battery unit storing 2,500,000 E for massive machinery banks and high-tier processors."
  :specs="{
    'Max Storage Capacity': '2,500,000 E',
    'Max Input / Output Rate': '3,200 E/t',
    'Charging Slots': '1 Internal Item Charge Slot'
  }"
  placedImage="/images/machines/steel_battery_placed.png"
  guiImage="/images/machines/steel_battery_gui.png"
/>

---

## 🎒 Portable Handheld Battery Packs

Portable Battery Packs are lightweight, rechargeable energy cells you can carry in your inventory or slot into **ATVs** and portable machinery. Charge them inside any Battery block or Generator, and use them to power electric equipment on the go!

| Battery Pack Tier | Capacity | Max Charge Rate | Max Discharge Rate | Best Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Copper Battery Pack** | **10,000 FE** | 200 FE/t | 200 FE/t | Early exploration, small tool charging |
| **Aluminum Battery Pack** | **50,000 FE** | 500 FE/t | 500 FE/t | Mid-tier ATV cruising, mobile power |
| **Steel Battery Pack** | **250,000 FE** | 2,000 FE/t | 2,000 FE/t | Long-distance expeditions, heavy machinery |
| **Tungsten Battery Pack** | **1,000,000 FE** | 8,000 FE/t | 8,000 FE/t | Extreme operations, high-voltage portable grid |

---

## 🔌 Energy Cables

Cables connect machines, generators, and batteries seamlessly across all 6 block faces.

| Cable Tier | Max Flow Rate | Best Paired With |
| :--- | :--- | :--- |
| **Copper Cable** | **200 E/t** | Copper Generator & Copper Battery |
| **Aluminum Cable** | **800 E/t** | Aluminum Generator, Refiners, Oxygen Gen |
| **Steel Cable** | **3,200 E/t** | Steel Generator & Heavy Machinery |
| **Tungsten Cable** | **12,800 E/t** | Geothermal Generators, Foundries & High-Tier Batteries |
| **Basalt-Insulated Cable** | **25,600 E/t** | Heavy Multiblocks, Overclocked Foundries & Extreme Grids (Explosion/Fireproof) |

---

## 📜 Crafting Recipes

### Copper Battery (Block)
<MinecraftRecipe id="copper_battery" />

### Aluminum Battery (Block)
<MinecraftRecipe id="aluminum_battery" />

### Steel Battery (Block)
<MinecraftRecipe id="steel_battery" />

### Copper Battery Pack (Portable)
<MinecraftRecipe id="copper_battery_pack" />

### Aluminum Battery Pack (Portable)
<MinecraftRecipe id="aluminum_battery_pack" />

### Steel Battery Pack (Portable)
<MinecraftRecipe id="steel_battery_pack" />

### Copper Cable
<MinecraftRecipe id="copper_cable" />

### Aluminum Cable
<MinecraftRecipe id="aluminum_cable" />

### Steel Cable
<MinecraftRecipe id="steel_cable" />

### Basalt-Insulated Super Cable
<MinecraftRecipe id="basalt_cable" />
