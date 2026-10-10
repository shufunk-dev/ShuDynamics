# ✨ ShuDynamics — The Industrial, Dimensional & Culinary Odyssey ⚡🍕❄️🌌

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3%20%7C%201.21.11%2B-brightgreen.svg?style=flat-square&logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg?style=flat-square)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25%20%7C%2021-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Latest Release](https://img.shields.io/badge/Release-v2.3.2--26.3%20%7C%20v2.3.2-purple.svg?style=flat-square)](https://github.com/shufunk-dev/ShuDynamics/releases)
[![Wiki & Documentation](https://img.shields.io/badge/Wiki-shudynamics.shufunk.net-8B5CF6.svg?style=flat-square)](https://shudynamics.shufunk.net)
[![CurseForge](https://img.shields.io/badge/CurseForge-ShuDynamics-F16436.svg?style=flat-square&logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/shudynamics)
[![License](https://img.shields.io/badge/License-MIT-purple.svg?style=flat-square)](LICENSE)
[![Company](https://img.shields.io/badge/By-Shufelt%20Designs-indigo.svg?style=flat-square)](https://github.com/shufunk-dev)

**ShuDynamics** is an expansive tech, magic, logistics, modular vehicles, dimensional exploration, culinary arts, and musical mod for **Minecraft Fabric**, officially maintained for both **Minecraft 26.3 ("Wilderness Bound")** and **Minecraft 1.21.11+ / 1.21.2+**. Built from the ground up to deliver a deeply rewarding survival tech tree—from early-game wooden enchantments and metallurgy to **high-voltage geothermal energy grids**, **wireless crystal storage networks**, **recursive autocrafting supercomputers**, **modular drivable ATVs with industrial tools**, **autonomous chunk laser quarries**, the **Cleanroom Complex with needleless hypospray medicine**, the **Modular Power Exosuit**, the **Harmonic Record Press with a full 10-track original soundtrack**, the **Multi-Course Bento Boxes**, **artisan kitchen appliances & cryogenic logistics**, **7 nostalgic Lost Biomes in the fractured Convergence dimension**, and the multi-tier boss gauntlet culminating in **The Primordial Cataclysm**.

---

## 🌿 Dual-Version Architecture & Git Branches

ShuDynamics maintains parallel releases for modern and legacy-stable Minecraft versions:

| Target Minecraft | Git Branch | Mod JAR Name | Java Runtime | Loader Target |
|:---:|:---:|:---:|:---:|:---:|
| **Minecraft 26.3** ("Wilderness Bound") | [`26.3`](https://github.com/shufunk-dev/ShuDynamics/tree/26.3) | `shudynamics-2.3.2-26.3.jar` | **Java 25** | Fabric Loader `0.19.5+` |
| **Minecraft 1.21.11+ / 1.21.2+** | [`main`](https://github.com/shufunk-dev/ShuDynamics/tree/main) | `shudynamics-2.3.2.jar` | **Java 21** | Fabric Loader `0.16.9+` |

*All ongoing feature updates and patches are actively synchronized across both branches.*

* 🌐 **Official Wiki & Documentation**: [https://shudynamics.shufunk.net](https://shudynamics.shufunk.net)
* ▶️ **Official Soundtrack (YouTube)**: [https://www.youtube.com/playlist?list=PLAEoPqUv2z90](https://www.youtube.com/playlist?list=PLAEoPqUv2z90)
* 🎧 **Official Soundtrack (Suno)**: [https://suno.com/playlist/343b0f74-ce3c-424d-8324-c26ef30eb78e](https://suno.com/playlist/343b0f74-ce3c-424d-8324-c26ef30eb78e)
* 🔥 **CurseForge Project Page**: [https://www.curseforge.com/minecraft/mc-mods/shudynamics](https://www.curseforge.com/minecraft/mc-mods/shudynamics)
* 💬 **Issues & Bug Tracker**: [https://github.com/shufunk-dev/ShuDynamics/issues](https://github.com/shufunk-dev/ShuDynamics/issues)

---

## 🔍 Recommended Companion Mods

To get the best survival experience and view all custom multi-slot machine recipes, dynamic overclocking boosts, and catalytic uses, we strongly recommend installing:

* **[EMI](https://www.curseforge.com/minecraft/mc-mods/emi)**: Modern Fabric recipe engine powering custom machine tabs (Induction Smelter, Circuit Fabricator, Harmonic Record Press, Alloy Foundry, Crusher, Soil Infuser, Blast Furnace, Coke Oven, Refinery, Magma Crucible, Super Computer, Cryo Freezer, Ice Cream Machine, Brick Oven), 1-click recipe transfer (`[+]` button), and catalytic boosts (**Basalt Flux Catalyst**, **Blaze Overclock Cores**, and **Speed Gears**).
* **[Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei)**: Standard in-game recipe browser with quick item search and catalyst indexing.
* **[Jade](https://www.curseforge.com/minecraft/mc-mods/jade)**: Real-time in-world HUD block inspection, machine progress bars, energy states, and custom chest tier names.

---

## 🌟 What's New in v2.3.2 (Patch 2.3.2)

* 💡 **Super Computer Recipe Solver Priority & Scrap Smelting Prevention**:
  * **Crafting Prioritization**: Reordered recipe resolution to check 3×3 crafting table recipes before furnace smelting. Previously, when resolving nuggets (such as for lanterns), the solver prioritized smelting, matching vanilla's tool scrap recipe (`1 iron pickaxe -> 1 iron nugget`) and crafting full iron pickaxes to smelt them into scrap rather than crafting nuggets directly from ingots (`1 iron ingot -> 9 iron nuggets`).
  * **Equipment Recycling Blacklist**: Added comprehensive equipment recycling filtering (`isEquipmentRecycleRecipe`) to block damageable tools, weapons, armor, and horse armor from ever being synthesized or scheduled as furnace inputs for nugget production.
  * **Direct Smelting Guard**: Hardened direct 3×3 pattern smelting so equipment recycling recipes cannot be programmed or queued manually in the Super Computer grid.

---

## 🌟 What's New in v2.3.1 (Patch 2.3.1)

* 🛡️ **Super Computer Free-Crafting Exploit Patch**:
  * Fixed an emergency flaw where the Super Computer's 3×3 ghost blueprint matrix was queried as real physical inventory during recursive dependency verification, allowing items to be crafted freely without prerequisite materials or dimension gating.
* ⛏️ **Universal Iron Pickaxe Machine Harvesting**:
  * Added block drop loot tables and mining tags to all newly added machines and appliances (**Brick Oven**, **Ice Cream Machine**, **Harmonic Record Press**, **Water Pump**, **Cryo Freezer**, **Water Pipe**, **Crusher Mk2**, and **Dust Smelter Mk2**).
  * Players can now safely mine and pick up these machines using an **Iron Pickaxe or higher** without fear of lost blocks if they don't have a Wrench.
  * Preserved full instant Shift + Right-Click **Wrench dismantling** directly into the player's inventory for quick, lossless reorganization.
* 📦 **Machine Inventory Protection**:
  * Implemented container item scattering on the **Ice Cream Machine** upon destruction, ensuring contained ingredients, ice, and ice creams safely drop onto the ground rather than being voided.

---

## 🌟 What's New in v2.3.0 — The Culinary Arts & Cryogenic Logistics Edition

### 🌾 1. Agronomy & 5 New Agricultural Crops
* **5 Cultivated Crops**: Grow **Tomatoes**, **Onions**, **Lettuce**, **Chili Peppers**, and **Soybeans** with dedicated multi-stage crop blocks.
* **Seed Extraction & Wild Foraging**: Extract seeds from crops or discover wild seeds across temperate and forest biomes.
* **Auto-Harvest Hoe Integration**: Fully compatible with right-click manual harvesting and automatic multi-block replanting via the **Auto-Harvest Hoe**.
* **Universal Tag Support**: Registered under standard `#c:crops`, `#c:seeds`, and `#minecraft:crops` tags.

### 🍳 2. Artisan Kitchen Appliances & Fluid Automation
* **Brick Oven**: High-heat culinary baking appliance powered by solid fuels or heated coils. Features front-facing animated ember glow, an amber heating gauge, and dedicated baking recipes for artisan breads, pizzas, and roasted dishes.
* **Ice Cream Machine**: Dual-mode manual hand-cranking or electrical grid-powered appliance. Churns milk/soy bases and fruits into decadent frozen soft-serve cups with an animated churning chevron indicator.
* **Cryo Freezer**: Industrial sub-zero cryogenic chamber that sublimates water into **Ice Cubes** and flash-freezes delicate biological compounds.
* **Water Pump & Water Pipes**: Continuous aquatic pumping apparatus that extracts infinite adjacent water sources and pipes fluid directly into automated kitchen appliances, boilers, and industrial machinery through dynamic 6-way connecting **Water Pipes**.

### 🍕 3. Gourmet Cuisine & Food Matrix
* **Artisan Pizzas**: Craft raw dough and bake **Margherita**, **Meat Lovers**, **Supreme**, and **Anchovy Onion** pizzas with balanced nutritional values.
* **Burgers & Buns**: Golden-baked Burger Buns, seasoned Burger Patties, **Classic Cheeseburgers**, and **Deluxe Bacon Burgers**.
* **Street Tacos**: Crispy fried Taco Shells packed with seasoned beef or fresh-caught fish.
* **Fresh Salads**: Toss crisp **Garden Salads** and antioxidant-rich **Berry Medley Salads**.
* **Ice Cream Delicacies**: Churn Vanilla (dairy & soy), Chocolate, Strawberry, Sweet Berry, and Blueberry ice creams.
* **Pantry Staples & Plant-Based Alternatives**: Wheat Flour, Salt (by evaporating ocean water), Tomato Sauce, Pizza Dough, Prepared Anchovies, pressed **Tofu**, bottled **Soy Milk**, and sliced Cheese.

### 🎨 4. Visual Excellence & EMI / JEI Integration
* **Pixel-Perfect Machine GUIs**: 100% custom, beveled 256×256 graphical user interfaces for the Brick Oven, Ice Cream Machine, Cryo Freezer, and Water Pump.
* **Dedicated EMI Recipe Views**: Authentic textured backgrounds, animated churning chevrons, and live illuminated energy gauges in recipe viewers.
* **Bug Fixes**: Resolved corn right-click harvesting and replanting, fixed EMI churning arrow coordinates, and polished industrial energy bar sprites.

---

## 🌟 Previous Highlights (v2.2.2) — Convergence Superalloys & Progression Balance Patch

* ⚡ **True Superalloy Progression**: All 4 Convergence exotic minerals are now deeply integrated into the survival tech tree:
  * **Quantum Ion Repulsor Module**: Strictly requires **4× Neo-Titanium Spring-Steel Ingots** (forged from Neodymium Ore + Titanium in the Alloy Foundry) and **2× Neodymium Magnets**. Infinite electric flight is now a true endgame milestone earned by conquering The Convergence!
  * **High-Jump Actuator Module**: Requires **4× Neo-Titanium Spring-Steel Ingots** alongside Titanium Suspensions for enhanced kinetic recoil.
  * **Thermal Refractory Plating**: Requires **Hafnium-Tungsten Carbide Ingots** (forged from Hafnium Ore mined in the deep Scorched Caldera + Tungsten), making total lava swimming and caldera heat immunity an earned reward.
  * **Fluoropolymer Acid-Proof Plating**: Requires **Tan-Ti Superalloy Ingots** (forged from Tantalum Ore mined in the Caustic Mire + Titanium), making caustic acid immunity an earned reward.
* 🛡️ **Paced Mid-Game Flight**: Early exosuit flight is centered around **Hydrogen Thrusters** and fuel canister management, ensuring petrochemical infrastructure has meaningful gameplay value before unlocking late-game Iron-Man mode.
* ♻️ **Item Salvager Synchronization**: Salvaging modules now accurately returns their authentic superalloys and advanced components.

---

## 🌟 Major Highlights — The Convergence & Lost Biomes Overhaul (v2.2.0)

### 🌌 1. The Convergence Dimension & The 7 Lost Biomes
* **Player-Built Gateway**: Construct a Nether Portal-sized frame (2×3 interior) containing **all 6 distinct Anomaly Keystones** and **at least 4 Crying Obsidian** blocks, then right-click any frame block to rupture the dimensional barrier and ignite the portal!
* **4 Alien Core Biomes**: Explore the **Caustic Mire**, **Scorched Caldera**, **Anoxic Barrens**, and **Riftwood Haven**.
* **🌲 7 Nostalgic "Lost" Biomes Restored**: Dimensional rifts have pulled forgotten eras of Minecraft geography into The Convergence:
  * **Alpha Rainforest**: Vibrant retro foliage (`#48b518`), classic Alpha oak canopies, plains flora, and draping vines.
  * **Seasonal Forest**: Fiery autumn amber canopies (`#d66b18`) with golden meadow grass (`#a69a34`), birch and oak leaf litter, and wild pumpkin patches.
  * **Shrubland**: Arid transitional scrubland populated by custom shrubland bushes, dead bushes, tall grass, and grazing herds.
  * **Modified Jungle Edge**: Hyper-rare nostalgic edge biome featuring steep rolling terrain (`#59c93c`), sparse jungle trees, bamboo groves, melons, and parrot/ocelot wildlife.
  * **Alpha Tundra**: Crisp retro winter landscape with pale grass (`#80b497`), snowy spruce trees, top-layer freezing ice, and arctic polar bears & rabbits.
  * **Desert Lakes**: Lush oasis basins featuring clusters of 4 connected natural spring lakes per chunk, sandy shorelines, sugar cane, cacti, and camel herds.
  * **Gravelly Mountains**: Rugged windswept mountain peaks layered in surface gravel disks, high-altitude alpine pines, and mountain goats.
* **Full Vanilla & Dimensional Structure Suite**: Strongholds, Woodland Mansions, Surface Bastions & Fortresses, Desert/Jungle Temples, Ocean Monuments, Ancient Cities, and Trial Chambers seamlessly integrated across their thematic Convergence biomes.
* **Continuous 3D Cave Systems**: Integrated `minecraft:cave_extra_underground` carvers across all 12 Convergence biomes, connecting surface entrances down to bedrock and deepslate.

### 👑 2. Three-Tier Boss Progression & Creative-Tier Relics
* **3-Tier Ritual Gauntlet**:
  * **Tier 1 — The Resonance Colossus (500 HP)**: Awakened with the *Core of Awakening*. Drops Resonance Cleaver, Singularity Staff, and Eternal Bento Box.
  * **Tier 2 — The Ascendant Colossus (1,200 HP)**: Awakened with the *Ascendant Awakening Core*. Unlocks the Ascendant Cleaver and Primordial Catalyst.
  * **Tier 3 — The Primordial Cataclysm (2,500 HP)**: The ultimate raid boss awakened with the *Primordial Cataclysm Core*. Unleashes thunderstorm lightning strikes, kinetic void waves, and debilitating Weakness III debuffs.
* **Pinnacle Creative-Tier Drops**: Conquering Tier 3 awards the **Ring of Gravitational Mastery** (permanent creative flight in survival), the **Trophy of Omnipotence**, and the **Infinite Dimensional Matrix**.

### 🦺 3. Modular Power Exosuit & Emergency Chassis Lock
* **Full Armor Chassis**: Modular Power Helmet, Chestplate, Leggings, and Boots with in-game **'V' Key** configuration panel.
* **🛡️ Emergency Chassis Lock**: Suit pieces lock safely at **1 HP** under fatal durability damage, 100% preserving installed chips, batteries, and matrices.
* **Combat Nanite Overclock**: Continuous live-chassis welding under active fire with zero combat delay using Quantum Cores.
* **8 Modular Upgrades**: Auto-dimming Night Vision HUD, Hydrogen Thrusters, 100% Electric Quantum Ion Repulsors, Speed Servo Legs, Hydraulic Step-Assist, High-Jump Actuators, Fluoropolymer Acid-Proof Plating, and Thermal Refractory Plating.

### 🎵 4. 10-Track Original Soundtrack & Harmonic Record Press
* **10 Original Music Discs**: Streamed original soundtrack composed specifically for ShuDynamics and playable in any vanilla Jukebox.
* **Stream Online**: [YouTube Official Playlist](https://www.youtube.com/playlist?list=PLAEoPqUv2z90) | [Suno Official Playlist](https://suno.com/playlist/343b0f74-ce3c-424d-8324-c26ef30eb78e)
* **Harmonic Record Press**: Industrial machine used to stamp Blank Vinyl Discs into master music records using thematic catalyst drops and FE power.

### 🧼 5. Grade-A Cleanroom Complex & Needleless Hypospray Medicine
* **Airtight Sterile Facilities**: Polymer Loom, Cleanroom Bunny Suit (Hood, Smock, Trousers, Booties), and BFS flood-fill Cleanroom Controller.
* **✦ Pure Medicine Synthesis**: Yields **✦ Pure Cartridges** with doubled buff durations (12:00) and wireless 1,000 FE/t cleanroom power broadcasting.
* **Pneumatic Needleless Hypospray**: High-velocity aerosol injection for self, teammates, or pets with 5 medical cartridges and metabolic saturation safeguards.

---

## ⚡ Core Mod Systems & Technology Tree

### 🍳 6. Agronomy, Artisan Cuisine & Kitchen Appliances
* **Cultivated Crops**: Tomatoes, Onions, Lettuce, Chili Peppers, Soybeans, Sweet Corn, and Rice Paddies.
* **Kitchen Appliances**: The Brick Oven for high-heat baking, the Ice Cream Machine for frozen desserts, and the Cryo Freezer for instant ice cube generation.
* **Multi-Course Bento Boxes (Eternal & Omega)**: Cycle between Feast of the Colossus, Wasabi Combat Rush, and Honey Mochi Fortification on the fly.
* **Gourmet Delicacies**: Master Rainbow Sushi Rolls, Artisan Pizzas, Deluxe Bacon Burgers, Street Tacos, Fresh Salads, and Homemade Ice Creams.

### ⛏️ 7. Digital Laser Quarry & Extraction Cores
* **Autonomous Chunk Excavation**: Autonomous laser excavation station deployed in the Overworld or the Mining Dimension (*Quarry Expanse*).
* **Infinite-Range Remote Linking**: Wireless base Controller binding with 24/7 chunk loading and Fortune / Silk Touch cores.

### 🖥️ 8. Modular Super Computer & Autocrafting Mainframe
* **Centralized Recipe Synthesis**: Integrates directly into your digital storage network to calculate and craft complex multi-stage items on demand.
* **1-Click EMI Recipe Encoding**: Ghost blueprint matrix supports manual item placement or instant 1-click recipe transfer (`[+]` button) from EMI / JEI.
* **Recursive Dependency Resolver**: Intelligently detects and synthesizes missing intermediate prerequisites in a single request.

### 🏎️ 9. Modular All-Terrain Vehicles (ATV) & Industrial Attachments
* **Drivable ATV**: High-mobility exploration vehicle with 6 customizable module slots.
* **Industrial Attachments**: Front-mounted 2×2 Mining Drills, Lumberjack Tree Saws, Agricultural Harvesters/Planters, and Directional Headlights.

### 💾 10. Digital Storage Networks & Modular Enchanted Chests
* **Modular Enchanted Chests**: In-world right-click upgradeable chests (Base $\rightarrow$ Netherite) expanding up to 108 slots with zero item loss.
* **Enchanted Storage Controller & Crystal Drive Bays**: 1k–64k Crystal Storage Drives with interdimensional cross-world access cards.

### ⚡ 11. High-Voltage Power Grids, Geothermal Energy & Fluid Logistics
* **✦ Infinite Dimensional Matrix**: Pinnacle relic from The Primordial Cataclysm pumping out up to 100,000 FE/t endless power.
* **Heavy Energy Storage**: Up to 100,000,000 FE stationary Tungsten Battery Units and 1,000,000 FE portable Battery Packs.
* **Fluid Logistics**: Submersible Lava & Water Pumps, insulated Lava & Water Pipes, and 5×5 Titanium Multiblock Reservoirs.

### 🏭 12. Pyrometallurgy & Induction Smelting
* **Alloy Foundry & Blast Furnace**: High-purity alloy synthesis (Bronze, Manyullyn, Tungsten Carbide, Signalum, Lumium, Enderium).
* **Dual-Powered Induction Smelter**: 32,400 mB dynamic multi-fluid chamber with 100% metal reclaim from weapons, tools, and anvils.
* **Mechanical Crusher Mk2**: Dual-chamber pulverizer scaling ore outputs up to 8× with speed gears.

---

## 🚀 Quick Start Survival Guide

1. **Enchanted Forest Beginnings**: Locate an Enchanted Forest biome, fell Enchanted Wood trees, and craft **Infused Heartwood** and **Enchanted Coal**.
2. **First Power & Smelting**: Build a **Solid Fuel Generator** to produce your first Forge Energy (FE). Construct the **Mechanical Crusher** and **Dust Smelter** to double your metal yields.
3. **Expand Your Storage**: Craft a starter **Enchanted Chest** and right-click it with Copper, Bronze, and Iron upgrades to expand capacity up to 108 slots without moving a single item.
4. **Kitchen Appliances & Cuisine**: Cultivate wild seeds to harvest tomatoes, onions, lettuce, and peppers. Build the **Brick Oven** and **Ice Cream Machine** to bake gourmet pizzas and churn refreshing ice creams.
5. **Alloy Metallurgy & Vehicles**: Build the **Alloy Foundry** to smelt Bronze and Steel. Construct the **Vehicle Fabricator** and assemble your first **Modular ATV** for rapid landscape exploration.
6. **Digital Automation**: Transition to the **Enchanted Storage Controller** and **Crystal Drive Bays**. Craft the **Modular Super Computer** to enable 1-click autocrafting from EMI/JEI.
7. **The Convergence & Lost Biomes**: Gather all 6 **Anomaly Keystones**, forge a portal frame with Crying Obsidian, equip your **Modular Power Exosuit** and **Hypospray**, and step into **The Convergence** to explore the 7 nostalgic Lost Biomes and face the **Resonance Colossus**!

---

## 🛠️ Building & Compiling from Source

### Prerequisites
* **Java Development Kit (JDK) 21** or higher.
* Git.

### Build Instructions
Clone the repository and compile using the included Gradle wrapper:

```bash
# Clone the repository
git clone https://github.com/shufunk-dev/ShuDynamics.git
cd ShuDynamics

# Build the mod JAR on Windows
.\gradlew.bat build

# Build the mod JAR on Linux / macOS
./gradlew build
```

The compiled mod JAR will be generated in:
```text
build/libs/shudynamics-2.3.1.jar
```

---

## 📄 License & Copyright

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for more details.

**Copyright © 2026 Shufelt Designs. All rights reserved.**
