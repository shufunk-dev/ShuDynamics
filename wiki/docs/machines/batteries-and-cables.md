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

## ♾️ The Infinite Dimensional Matrix (Apex Power Core)

Earned exclusively by defeating the Tier 3 world-boss, **The Primordial Cataclysm**, the **Infinite Dimensional Matrix** (`enchantedwood:infinite_dimensional_matrix`) represents the ultimate source of limitless electrical power in ShuDynamics:

* **Infinite Grid Provider (100,000 FE/t)**: Insert the matrix into the battery discharge slot of any stationary Battery block (Copper, Aluminum, or Steel Battery). It functions as an inexhaustible internal generator that pumps **100,000 FE/t** directly into the battery's buffer and outgoing cable network without burning fuel, requiring maintenance, or losing durability.
* **Portable & Exosuit Integration**: Can be inserted into ATV battery slots or any [Modular Power Exosuit](/tools-and-armor/modular-exosuit) battery socket to supply perpetual energy in the field.

---

## 🔌 Energy Cables

Cables connect machines, generators, and batteries seamlessly across all 6 block faces.

| Cable Tier | Icon | Max Flow Rate | Best Paired With |
| :--- | :---: | :--- | :--- |
| **Copper Energy Cable** | <img src="/textures/item/copper_cable.png" style="width:32px;height:32px;image-rendering:pixelated;" /> | **200 E/t** | Copper Generator & Copper Battery |
| **Aluminum Energy Cable** | <img src="/textures/item/aluminum_cable.png" style="width:32px;height:32px;image-rendering:pixelated;" /> | **800 E/t** | Aluminum Generator, Refiners, Oxygen Gen |
| **Steel Energy Cable** | <img src="/textures/item/steel_cable.png" style="width:32px;height:32px;image-rendering:pixelated;" /> | **3,200 E/t** | Steel Generator & Heavy Machinery |
| **Tungsten Heavy Energy Cable** | <img src="/textures/item/tungsten_cable.png" style="width:32px;height:32px;image-rendering:pixelated;" /> | **12,800 E/t** | Geothermal Generators, Foundries & High-Tier Batteries |
| **Basalt Super Energy Cable** | <img src="/textures/item/basalt_cable.png" style="width:32px;height:32px;image-rendering:pixelated;" /> | **25,600 E/t** | Heavy Multiblocks, Overclocked Foundries & Extreme Grids (Explosion/Fireproof) |

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

### Tungsten Heavy Energy Cable
<MinecraftRecipe id="tungsten_cable" />

### Basalt Super Energy Cable
<MinecraftRecipe id="basalt_cable" />

