# ✨ ShuDynamics

> [!WARNING]
> **Active Development Branch (`main` — v2.0 Experimental)**: This branch contains active, experimental pre-release code for **ShuDynamics 2.0**. For the verified stable release, switch to the [`1.5-stable`](https://github.com/shufunk-dev/ShuDynamics/tree/1.5-stable) branch or download directly from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/shudynamics).

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11%2B-brightgreen.svg?style=flat-square&logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg?style=flat-square)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Development](https://img.shields.io/badge/Branch-2.0--Experimental-orange.svg?style=flat-square)](https://github.com/shufunk-dev/ShuDynamics)
[![Stable Release](https://img.shields.io/badge/Stable-v1.5.0-green.svg?style=flat-square)](https://github.com/shufunk-dev/ShuDynamics/tree/1.5-stable)
[![Wiki & Documentation](https://img.shields.io/badge/Wiki-shudynamics.shufunk.net-8B5CF6.svg?style=flat-square)](https://shudynamics.shufunk.net)
[![License](https://img.shields.io/badge/License-MIT-purple.svg?style=flat-square)](LICENSE)
[![CurseForge](https://img.shields.io/badge/CurseForge-ShuDynamics-F16436.svg?style=flat-square&logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/shudynamics)
[![Company](https://img.shields.io/badge/By-Shufelt%20Designs-indigo.svg?style=flat-square)](https://github.com/shufunk-dev)

**ShuDynamics** is an all-in-one tech, magic, energy, modular vehicle, digital storage, recursive autocrafting, chunk excavation, and advanced metallurgy progression mod for **Minecraft Fabric**. Built from the ground up to provide seamless survival progression—from early-game wooden enchantments and metallurgy to high-voltage power grids, multi-slot digital storage networks, modular all-terrain vehicles, geothermal fluid networks, autonomous chunk quarries, and automated road infrastructure.

* 🌐 **Official Wiki & Documentation**: [https://shudynamics.shufunk.net](https://shudynamics.shufunk.net)
* 🔥 **CurseForge Project Page**: [https://www.curseforge.com/minecraft/mc-mods/shudynamics](https://www.curseforge.com/minecraft/mc-mods/shudynamics)
* 💬 **Issues & Bug Tracker**: [https://github.com/shufunk-dev/ShuDynamics/issues](https://github.com/shufunk-dev/ShuDynamics/issues)

---

## 🔍 Recommended Companion Mods (Recipe & Uses Lookups)

To get the best in-game survival experience and view all custom multi-slot machine recipes, dynamic overclocking boosts, and catalytic uses, we strongly recommend installing:

* **[EMI](https://www.curseforge.com/minecraft/mc-mods/emi)** (or **REI + EMI**): Modern Fabric recipe engine powering custom machine tabs (Alloy Foundry, Crusher, Soil Infuser, Blast Furnace, Coke Oven, Refinery, Magma Crucible, Super Computer), 1-click ghost blueprint transfer, and catalytic boosts (**Basalt Flux Catalyst**, **Blaze Overclock Cores**, and **Speed Gears**).
* **[Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei)**: Standard in-game recipe browser with quick item search.
* **[Jade](https://www.curseforge.com/minecraft/mc-mods/jade)**: Provides real-time in-world HUD block inspection, machine progress, inventory counts, and custom chest tier names.

---

## 🌟 Core Mod Features (v1.5.0 Milestone)

### ⛏️ 1. Digital Laser Quarry & Extraction Cores (v1.5.0)
* **Autonomous Chunk Excavation**: High-throughput laser mining station capable of excavating entire chunk columns (from surface down to bedrock) in the Overworld or the Resettable Mining Dimension (*Quarry Expanse*).
* **Infinite-Range Remote Linking**: Use the Industrial Wrench to wirelessly bind your Quarry to your base's Storage Controller over infinite distances—transmitting power and beaming mined items directly into storage crystals without physical cable runs.
* **24/7 Autonomous Chunk Loading**: Keeps active mining footprints (1×1, 3×3, or 5×5 chunks) fully loaded and operational around the clock while powered.
* **Modular Upgrade Cores**:
  * **Range Upgrades (T1 & T2)**: Expands excavation area from 1×1 chunk up to 3×3 and 5×5 chunk footprints.
  * **Fortune Core**: Equips high-energy harmonic beams with Fortune III ore multiplication.
  * **Silk Touch Core**: Safely extracts delicate ores, ice, and fragile blocks in their pure state.

### 🖥️ 2. Modular Super Computer & Autocrafting Mainframe (v1.5.0)
* **Centralized Recipe Synthesis**: Integrates directly into your digital storage network to calculate and craft complex multi-stage items on demand.
* **1-Click EMI Recipe Encoding**: Ghost blueprint matrix supports manual item placement or instant 1-click recipe transfer (`[+]` button) from EMI / JEI recipe viewers.
* **Recursive Dependency Resolver**: Intelligently detects and synthesizes missing intermediate prerequisites (e.g., Raw Ore $\rightarrow$ Dust $\rightarrow$ Ingot $\rightarrow$ Gear $\rightarrow$ Final Machine) in a single request.
* **Live HUD Status Notifications**: Real-time on-screen banner alerts notify you of missing ingredients, energy deficits, or active synthesis cycles.
* **Gear Upgrade Socket**: Scales synthesis speed with standard and enchanted gears, up to instantaneous processing with the Blaze Overclock Core.

### 🚜 3. ATV Industrial Attachments & Dynamic Lighting (v1.5.0)
* **Mobile Mining Drill Bits** *(Iron, Steel, Titanium, Netherite)*: Front-mounted heavy drills that bore 2×2 horizontal tunnels through mountains, automatically depositing extracted blocks into the rear cargo trunk.
* **Lumberjack Tree Saws**: Automatically fells full tree trunks and canopy foliage upward as you drive through dense forests.
* **Agricultural Crop Harvesters & Planters**: Reaps mature farmland crops across 3–7 block swaths while immediately replanting seeds into fertile soil.
* **Dynamic Directional Headlights**: Halogen and LED high beams cast real-time directional illumination cones while driving at night or in dark underground caverns.
* **Balanced Quad-Suspension**: ATV chassis requires a matched set of 4 suspension units (one per wheel) for balanced handling, shock dampening, and terrain step-up capabilities.

### ♻️ 4. Automated Item Salvager & Recycler (Uncrafter) (v1.5.0)
* **Thermal Deconstruction Station**: Breaks down obsolete weapons, damaged armor, superseded machinery, conduits, chests, and gears back into pure base ingots, gems, and constituent materials.
* **100% Component Recovery**: Full recipe reversal for supported equipment and machinery.
* **4-Slot Segregated Output Tray**: Separates composite parts into dedicated physical extraction bins without jamming.
* **Gear Overclocking**: Accelerates disassembly cycles up to 8× with the Blaze Overclock Core.

### 🎨 5. Harmonized Metallurgy & 3D Conduits (v1.5.0)
* **Color-Harmonized Metals**: Complete color-theory rework across all ingots, nuggets, gears, and metal blocks for Copper, Bronze, Tin, Steel, Titanium, Manyullyn, Ardite, Cobalt, and Tungsten.
* **Distinct Copper vs. Bronze Contrast**: Vibrant golden-bronze styling ensuring instant visual distinction from vanilla Copper.
* **High-Contrast 3D Energy Conduit Icons**: Custom handcrafted 3D conduit item textures for all 5 cable tiers (*Copper, Aluminum, Steel, Tungsten, and Basalt*).

### 🌋 6. Nether Metallurgy & Advanced Metal Tiers
* **Nether & End Worldgen Ores**: Cobalt, Ardite, Tungsten, Lead, Lumium, Ender, and Enderite worldgen ores.
* **Advanced Alloys**: Manyullyn, Tungsten Carbide, Signalum, Lumium, Enderium, Bronze, and Enchanted Netherite.
* **Indestructible Containment**: Blast-proof **Reinforced Obsidian** (1200.0 resistance, Wither-immune) and transparent **Tough Volcanic Glass** (600.0 resistance, drops itself without Silk Touch).

### ⚡ 7. Tier 4 Geothermal Energy & High-Voltage Grid
* **Geothermal Generators**: Continuous 400 FE/t passive power generated from liquid lava.
* **High-Capacity Battery Banks**: 100,000,000 FE Tungsten Battery Units and 1,000,000 FE portable Battery Packs.
* **Peak Power Cabling**: 25,600 FE/t **Basalt Energy Cables** and 25,000 FE/t **Tungsten Cables** with explosion and heat immunity.
* **Fluid Logistics**: Submersible **Lava Pumps**, refractory **Lava Pipes**, and 5×5 Titanium Multiblock Reservoirs.

### 🏭 8. Pyrometallurgy, Overclocking & Machining
* **Alloy Foundry**: Dual-input pyrometallurgical arc furnace for high-purity alloy synthesis.
* **Steel Blast Furnace**: Traditional Coke Coal smelting or zero-carbon Green Hydrogen reduction with **Basalt Flux Catalyst** 2× speed/yield boosts.
* **Mechanical Crusher Mk2**: Dual-chamber pulverizer with speed gear sockets scaling ore outputs up to 8×!
* **Magma Crucible**: High-temperature rock liquefier converting stone, basalt, and obsidian into liquid lava.
* **Blaze Overclock Cores**: Dedicated turbo overclock slots granting +200% operating speed across all machines.

### 📦 9. Universal Item Logistics
* **Item Transport Pipes**: 6-way modular conduit network for high-throughput item routing.
* **Extractors & Inserters**: Intelligent directional machine feeding and chest extraction.
* **Industrial Wrench**: 4-way side configuration tool for instant pipe connection tuning and machine rotation.

### 🌾 10. Agronomy & Volcanic Soil
* **Soil Infuser**: Powered machine that enriches standard dirt with ash, sulfur, or catalysts.
* **Volcanic Soil**: Farmland with built-in perpetual hydration and accelerated growth ticks—even in the Nether!
* **Volcanic Fertilizer & Sweet Corn**: High-saturation agricultural crops and bio-ethanol feedstock.

### 🏎️ 11. Modular All-Terrain Vehicles (ATV) & Tuning Bay
* **Drivable ATV**: High-mobility exploration vehicle featuring 6 customizable module slots (Engines, Tires, Suspensions, Chassis, Cargo Trunks, and Fuel/Batteries).
* **Vehicle Fabricator**: Dedicated tuning bay workstation with shift-click vehicle retrieval.
* **Fall Damage Immunity**: Suspension shocks completely absorb 100% of vertical fall impact.

### 🛣️ 12. Petrochemicals & Road Infrastructure
* **Oil Sand & Distillation**: Refines crude oil sludge into Gasoline, Biofuel, and High-Octane Racing Fuel.
* **Asphalt Roads & Concrete Curbs**: Speed-boosting highways (+25% top speed) with auto-connecting corner curbs and transition ramps.
* **Autonomous Road Paver**: Mobile machine that automatically lays 3-wide asphalt roads and extrudes curbs.

### 💾 13. Digital Storage Networks & Modular Enchanted Chests
* **Modular Enchanted Chests**: In-world right-click upgradeable chests (Base $\rightarrow$ Copper $\rightarrow$ Bronze $\rightarrow$ Iron $\rightarrow$ Gold $\rightarrow$ Diamond $\rightarrow$ Netherite) expanding up to 108 slots with zero item loss.
* **Enchanted Storage Controller**: Central mainframe with ambient audio, dual hybrid power inputs, and 3 expansion bays.
* **6-Slot Drive Bay & 54-Slot Terminal**: 1k–64k Crystal Storage Drives with dynamic LED matrix and anti-data loss locks.
* **Chunk Loader & Interdimensional Cards**: Infinite Overworld and cross-dimensional storage access.

### 🔨 14. Tools, Armor, 3×3 Excavation & Aquatic Gear
* **3×3 Mining Hammers**: Heavy-duty hammers from Wood to Enchanted Netherite.
* **Infernal Sledgehammer**: 3×3 excavation hammer with built-in auto-smelting for ores and cobblestone.
* **Aquatic Scuba Gear**: Full modular diving suit with infinite pressurized oxygen and Dolphin's Grace swimming.

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
build/libs/shudynamics-2.0.0-experimental.1.jar
```

---

## 📄 License & Copyright

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for more details.

**Copyright © 2026 Shufelt Designs. All rights reserved.**
