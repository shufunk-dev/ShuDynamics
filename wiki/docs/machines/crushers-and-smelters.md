# ⚙️ Mechanical Crushers & Dust Smelters

ShuDynamics introduces gear-driven industrial processing machinery. By upgrading the internal **Gear Socket** with higher-tier gears (from Copper to Enchanted Netherite), machines dramatically scale their ore yield multiplier and processing speed.

---

## 🪨 Mechanical Crusher

<MachineShowcase 
  name="Mechanical Crusher"
  icon="/textures/block/crusher_front.png"
  tier="Tier 1-3 (Gear Scalable)"
  tierClass="tier-early"
  category="Ore Multiplication & Pulverization"
  description="Pulverizes raw ores, metal blocks, and minerals into high-potency metal dusts. Upgrading the internal Gear slot multiplies ore output up to 7x per raw ore!"
  :specs="{
    'Energy Consumption': '40 E/tick',
    'Max Energy Buffer': '50,000 E',
    'Base Processing Time': '160 ticks (8.0 seconds)',
    'Gear Socket': 'Accepts Copper, Bronze, Iron, Gold, Diamond, Netherite & Enchanted Gears',
    'Maximum Yield Multiplier': 'Up to 7x Dust per Raw Ore (Enchanted Netherite Gear)'
  }"
  placedImage="/images/machines/crusher_placed.png"
  guiImage="/images/machines/crusher_gui.png"
/>

### ⚙️ Crusher Gear Multiplier Tiers

Inserting a crafted gear into the dedicated **Gear Socket** permanently scales the ore yield multiplier:

| Gear Installed | Ore Yield Multiplier | Stone to Bauxite Yield |
| :--- | :--- | :--- |
| **No Gear / Iron Gear** | **2x Dust** (Standard Doubling) | 1x Raw Bauxite |
| **Copper Gear / Bronze Gear** | **3x Dust** (Tripling) | 2x Raw Bauxite |
| **Gold Gear** | **4x Dust** (Quadrupling) | 2x Raw Bauxite |
| **Diamond Gear** | **5x Dust** | 3x Raw Bauxite |
| **Netherite Gear** | **6x Dust** | 4x Raw Bauxite |
| **Enchanted Gears (Any Tier)** | **+1 Additional Bonus Dust** *(e.g., 7x with Enchanted Netherite!)* | +1 Bonus Bauxite |

---

## ⚡ High-Speed Dust Smelter

<MachineShowcase 
  name="High-Speed Dust Smelter"
  icon="/textures/block/dust_smelter_front.png"
  tier="Tier 1-3 (Gear Scalable)"
  tierClass="tier-early"
  category="Thermal Induction Smelting"
  description="High-throughput electric furnace specifically engineered to smelt mineral and metal dusts into pure ingots and gems. Gear upgrades accelerate cook times up to 6.4x faster!"
  :specs="{
    'Energy Consumption': '50 E/tick',
    'Max Energy Buffer': '50,000 E',
    'Default Cook Time': '160 ticks (8.0 seconds)',
    'Max Upgraded Speed': '25 ticks (1.25 seconds with Netherite Gear)',
    'Supported Dusts': 'Iron, Copper, Tin, Bronze, Titanium, Gold, Diamond, Netherite, Emerald'
  }"
  placedImage="/images/machines/dust_smelter_placed.png"
  guiImage="/images/machines/dust_smelter_gui.png"
/>

### ⏱️ Dust Smelter Gear Speed Scaling

| Gear Installed | Cook Time per Dust | Speed Multiplier |
| :--- | :--- | :--- |
| **No Gear** | 160 ticks (8.0s) | 1.0x (Baseline) |
| **Iron Gear** | 140 ticks (7.0s) | 1.15x |
| **Copper Gear** | 120 ticks (6.0s) | 1.33x |
| **Bronze Gear** | 100 ticks (5.0s) | 1.6x |
| **Gold Gear** | 80 ticks (4.0s) | 2.0x (Double Speed) |
| **Diamond Gear** | 50 ticks (2.5s) | 3.2x |
| **Netherite Gear** | **25 ticks (1.25s)** | **6.4x Extreme Speed** |

---

## 📋 Crushing & Recycling Recipes

| Input Material | Output Product | Notes |
| :--- | :--- | :--- |
| **Raw Iron / Copper / Gold / Tin / Titanium / Bauxite** | Corresponding Metal Dust | Yield scales with installed Gear (2x to 7x) |
| **Raw Metal Blocks (9x Ores)** | 9x Base Scaled Dust | Instantly processes full compressed blocks |
| **Diorite / Granite / Terracotta** | **Raw Bauxite** | Early-game stone recycling for aluminum |
| **Coal / Coke Coal** | Coal Dust | Used for carbon synthesis and alloys |
| **Enchanted Coal / Enchanted Coal Block** | **Enchanted Dust** *(9x for Block)* | Reclaims precious magical dust (scales with Gear!) |
| **Diamond / Emerald / Netherite** | Diamond / Emerald / Netherite Dust | Catalyst reagent for high-tier tech |
