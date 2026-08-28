# 🧪 Refiners, Metallurgy & Blast Furnaces

ShuDynamics provides specialized high-temperature metallurgical processing machines to refine advanced ores, bauxite, and forge industrial steel alloys.

---

## 🥈 Aluminum Refiner (Tier 2)

<MachineShowcase 
  name="Aluminum Refiner"
  icon="/textures/block/aluminum_refiner_front.png"
  tier="Tier 2 (Mid Game)"
  tierClass="tier-mid"
  category="Thermal Electric Smelting"
  description="High-temperature electric refiner powered by 20 E/t. Smelts raw bauxite ore into aluminum ingots and processes complex alloys."
  :specs="{
    'Energy Consumption': '20 E/t during active refining',
    'Max Energy Buffer': '50,000 E',
    'Process Duration': '100 ticks (5 seconds per item)',
    'Inputs': 'Bauxite Ore, Bauxite Dust, Raw Ores',
    'Outputs': 'Aluminum Ingots, Purified Metals'
  }"
  placedImage="/images/machines/aluminum_refiner_placed.png"
  guiImage="/images/machines/aluminum_refiner_gui.png"
  videoUrl=""
/>

---

## 🥇 Steel Blast Furnace (Tier 3)

<MachineShowcase 
  name="Steel Blast Furnace"
  icon="/textures/block/steel_blast_furnace_front.png"
  tier="Tier 3 (Heavy Metallurgy)"
  tierClass="tier-high"
  category="Pyrometallurgy & Steel Synthesis"
  description="Industrial blast furnace designed to synthesize Steel Ingots. Supports dual operating modes: traditional Coke Coal reduction or high-speed Green Steel Direct Reduction using Hydrogen Gas."
  :specs="{
    'Energy Consumption': '200 E/tick',
    'Max Energy Buffer': '100,000 E',
    'Hydrogen Capacity': '4,000 mB H2 Gas',
    'Traditional Smelt Time': '100 ticks (5s with Iron + Coke Coal)',
    'Green Steel Smelt Time': '60 ticks (3s with Iron + H2 Gas — 40% faster!)',
    'Gas Insertion': 'Hydrogen Pipe or Canister Charging Slot'
  }"
  placedImage="/images/machines/steel_blast_furnace_placed.png"
  guiImage="/images/machines/steel_blast_furnace_gui.png"
  videoUrl=""
/>

---

## 🏭 Alloy Foundry (Tier 4)

<MachineShowcase 
  name="Alloy Foundry"
  icon="/textures/block/alloy_foundry_front.png"
  tier="Tier 4 (Advanced Metallurgy)"
  tierClass="tier-high"
  category="Thermal Alloy Fusion"
  description="Dual-input pyrometallurgical furnace engineered to fuse complex multi-element metal alloys (Bronze, Steel, Titanium alloys) using electric thermal arc induction."
  :specs="{
    'Energy Consumption': '150 E/tick',
    'Max Energy Buffer': '250,000 E',
    'Process Duration': '120 ticks (6.0 seconds per batch)',
    'Dual Reagent Slots': 'Accepts 2 distinct primary input metals',
    'Output': 'High-purity alloy ingots'
  }"
  placedImage=""
  guiImage=""
/>

---

## 💎 Magma Crucible (Tier 4)

<MachineShowcase 
  name="Magma Crucible"
  icon="/textures/block/magma_crucible_front.png"
  tier="Tier 4 (Nether Tech)"
  tierClass="tier-high"
  category="Thermal Phase Change & Liquefaction"
  description="High-temperature electric melting chamber. Melts stone, cobblestone, netherrack, and mineral blocks into liquid lava and molten metals."
  :specs="{
    'Energy Consumption': '200 E/tick',
    'Max Energy Buffer': '500,000 E',
    'Lava Tank Buffer': '16,000 mB (16 Buckets of liquid Lava)',
    'Melting Rates': 'Cobblestone, Netherrack, Basalt, Magma Blocks $\\rightarrow$ Liquid Lava',
    'Fluid Pushing': 'Automatically feeds connected Lava Pipes and Titanium Reservoirs'
  }"
  placedImage=""
  guiImage=""
/>

---

## 📋 Metallurgy & Refining Recipes

| Input Reagents | Energy Cost | Processing Time | Output Product | Machine / Method |
| :--- | :--- | :--- | :--- | :--- |
| **Bauxite Dust / Raw** | 2,000 E | 5 Seconds | **1x Aluminum Ingot** | Aluminum Refiner |
| **Raw Titanium** | 5,000 E | 10 Seconds | **1x Titanium Ingot** | Aluminum Refiner |
| **Raw Tin** | 1,600 E | 4 Seconds | **1x Tin Ingot** | Aluminum Refiner |
| **Iron Ingot + Coke Coal** | 20,000 E | 5 Seconds | **1x Steel Ingot** | Steel Blast Furnace (Standard) |
| **Iron Ingot + 200 mB H₂** | 12,000 E | 3 Seconds | **1x Steel Ingot** | Steel Blast Furnace (Green Steel) |
| **Copper Ingot + Tin Ingot** | 18,000 E | 6 Seconds | **2x Bronze Ingot** | Alloy Foundry |
| **4x Cobblestone / Netherrack** | 24,000 E | 6 Seconds | **1,000 mB Lava** | Magma Crucible |


