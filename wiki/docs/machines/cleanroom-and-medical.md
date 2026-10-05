# 🧼 Cleanroom Complex & Advanced Medical Laboratory Guide

Introduced in **ShuDynamics 2.0**, the **Cleanroom Complex** and **Advanced Medical Laboratory Suite** introduce industrial sterilization, high-potency pharmacological synthesis, and needleless hypospray inoculations.

---

## 🏗️ The Grade-A Cleanroom Complex

Certain high-precision chemical compounding and semiconductor manufacturing operations require a Grade-A sterile environment.

### 1. Polymer Loom & Sterile Fabric
* **Polymer Loom** (`enchantedwood:polymer_loom`): Dedicated textile weaving machine with an animated GUI and 50,000 FE buffer.
* **Weaving Recipe**: Rubber + String + Silicon $\rightarrow$ **4× Sterile Polymer Fabric** (`enchantedwood:sterile_polymer_fabric`).

### 2. 4-Piece Cleanroom Bunny Suit
Crafted on the tailoring bench with Sterile Polymer Fabric, Rubber, and lightweight alloys:
* **Cleanroom Sanitary Hood** (`enchantedwood:cleanroom_hood`)
* **Cleanroom Smock** (`enchantedwood:cleanroom_smock`)
* **Cleanroom Trousers** (`enchantedwood:cleanroom_trousers`)
* **Cleanroom Booties** (`enchantedwood:cleanroom_booties`)

Wearing the full 4-piece suit prevents airborne particulate shedding, allowing personnel to pass through sterilization checkpoints into Grade-A cleanrooms.

---

## 🌀 Cleanroom Air Scrubber & Wireless Induction Grid

The **Cleanroom Air Scrubber & Controller** (`enchantedwood:cleanroom_air_scrubber`) is the central environmental regulator for your facility:

* **3D BFS Flood-Fill Detection**: When supplied with FE power (250,000 FE internal buffer), scans the enclosed room up to **2,500 blocks**.
* **Valid Airtight Boundaries**: Cleanroom Casings, Filter Casings, Gowning / Decontamination Airlock Doors, Tinted / Standard / Aerogel Glass, and Sterile Cleanroom Lamps.
* **Sterile Status**: When an airtight seal is validated, the status indicator turns green: `ACTIVE (STERILE - GRADE A)`.
* **Wireless Cleanroom Power Grid**: Broadcasts electrical power wirelessly to all interior machines (Industrial Centrifuges, Chemical Synthesizers, and Polymer Looms) at up to **1,000 FE/t per machine**—eliminating clutter and preserving sterile cable-free floors!
* **✦ Pure Medicine Synthesis**: Any Chemical Synthesizer operating within an active cleanroom compounds **✦ Pure Hypospray Cartridges**, granting **doubled buff durations (12:00)** and amplified healing.

---

## 🚪 Airlock Checkpoints & Infrastructure

### 1. Gowning Airlock Door (`enchantedwood:gowning_airlock_door`)
* Directional radar proximity sensor auto-opens outward when personnel approach the anteroom / gowning chamber from the exterior.
* Automatically closes and seals after 3.5 seconds.

### 2. Decontamination Airlock Door (`enchantedwood:decontamination_airlock_door`)
* Integrated biosensor scans arriving personnel for the complete 4-piece Cleanroom Bunny Suit.
* Approaching without a full suit keeps the door locked and flashes red particles.
* When fully suited, triggers an automated **3-second pressurized steam wash shower** with aerosol hissing before sliding open into the cleanroom.
* Features an interior manual override button for instantaneous exit.

### 3. Sterile Cleanroom Lamp (`enchantedwood:sterile_cleanroom_lamp`)
* Flush airtight ceiling light fixture.
* Right-click to cycle through 3 operational modes:
  * **Daylight White** (Light Level 15)
  * **Germicidal UV-C** (Light Level 7, ambient purple fluorescence)
  * **Off** (Light Level 0)

### 4. Sterile Medical Cabinet (`enchantedwood:sterile_medical_cabinet`)
* 36-slot dedicated pharmacy dispensary with 4 color-coded functional rows:
  * **Row 1 (Tooling & Shells)**: Hypospray, Empty Cartridges, Glass, Metals, Redstone.
  * **Row 2 (Essences & Byproducts)**: Chemical extracts + Centrifuge byproducts.
  * **Row 3 (Feeds & Catalysts)**: Raw biological feedstocks and reaction catalysts.
  * **Row 4 (Dispensary Buffer)**: Pure medicine ampoules and ready-to-use Hyposprays.

---

## 💉 Advanced Medical Suite & Hypospray System

### 1. Industrial Centrifuge (`enchantedwood:industrial_centrifuge`)
High-speed biological separator (50,000 FE buffer, 25 FE/t draw) that fractionates organic matter into potent extracts and zero-waste catalytic byproducts:

* **Slime Ball** $\rightarrow$ **Alkaline Base Extract** + Sulfur Dust
* **Magma Cream / Crimson Fungus** $\rightarrow$ **Cryo-Thermal Extract** + Volcanic Ash
* **Kelp / Seagrass / Cucumber** $\rightarrow$ **Oxygenated Extract** + Bone Meal
* **Dragon Fruit / Nether Wart** $\rightarrow$ **Cellular Nanite Extract** + Sugar
* **Glow Berries / Wasabi Root** $\rightarrow$ **Adrenal Essence** + Glowstone Dust

*Zero-Waste Loop*: Centrifuge byproducts directly supply reaction catalysts for synthesizing finished medical cartridges!

### 2. Chemical Synthesizer (`enchantedwood:chemical_synthesizer`)
Combines Empty Cartridges, Chemical Extracts, and Catalysts into medical ampoules:

| Cartridge | Recipe (Synthesizer) | Base Duration | ✦ Pure Duration (Cleanroom) | Primary Medical Effect |
| :--- | :--- | :--- | :--- | :--- |
| **Acid-Neutralizing Cartridge** | Empty Cartridge + Alkaline Extract + Redstone | 6:00 | 12:00 | **Acid Protection** (100% caustic immunity) |
| **Endothermic Heat-Buffer Cartridge** | Empty Cartridge + Cryo-Thermal Extract + Blaze Powder | 6:00 | 12:00 | **Thermal Protection** (100% fire/lava/caldera immunity) |
| **Hyper-Oxygenation Cartridge** | Empty Cartridge + Oxygenated Extract + Titanium Ingot | 6:00 | 12:00 | **Atmospheric Protection** + **Water Breathing** |
| **Nanite Trauma Cartridge** | Empty Cartridge + Nanite Extract + Golden Apple | Instant | Instant | Critical Care: **+8 HP (4 hearts)** + **Regen II (0:20)** + Debuff Cleanse |
| **Adrenaline Combat Stim Cartridge** | Empty Cartridge + Adrenal Essence + Sugar | 3:00 | 6:00 | Combat Surge: **Speed II, Haste II, Resistance I** |

### 3. Pneumatic Hypospray Device (`enchantedwood:hypospray`)
* **Operation**: Hold in main hand with any cartridge in offhand (or inventory). Right-click to discharge a high-velocity aerosol jet into the bloodstream with a pneumatic hiss sound and white vapor clouds.
* **Friendly Inoculation**: Right-click teammates, tamed wolves, or villagers to inoculate them directly!
* **Container Return**: Automatically returns an **Empty Cartridge** (`enchantedwood:empty_cartridge`) upon firing.

### 4. Metabolic Saturation & Overdose Safeguard
* **Metabolic Saturation**: Inoculating triggers a 5-second metabolic buffer.
* **Overdose Trigger**: Inoculating a second time *within* the 5-second window causes dangerous chemical shock:
  * Inflicts **Nausea II**, **Slowness II**, and **Weakness II** for 10 seconds.
  * Deals **4.0 unblockable magic damage** (bypasses all armor).
  * Halves the incoming cartridge buff duration by 50%.
  * Warning alert: *"⚠ WARNING: Metabolic Inoculant Overload! Chemical sickness induced! ⚠"*
