# ♻️ Automated Item Salvager & Recycler

The **Automated Item Salvager** is an industrial thermal recycling unit designed to deconstruct obsolete gear, damaged tools, superseded machinery, conduits, and excess components back into their original base ingots, gems, and materials.

---

## 🏗️ Machine Overview

<MachineShowcase 
  name="Item Salvager"
  icon="/textures/block/item_salvager_front.png"
  tier="Tier 3 (Industrial Recycling)"
  tierClass="tier-mid"
  category="Material Recovery & Deconstruction"
  description="High-temperature thermal disassembler. Deconstructs obsolete weapons, damaged armor, outdated machines, gears, and cables back into pure base ingots, gems, and constituent raw materials!"
  :specs="{
    'Energy Consumption': '60 FE / operation',
    'Max Energy Buffer': '100,000 FE',
    'Base Disassembly Time': '120 ticks (6.0 seconds)',
    'Gear Upgrade Socket': 'Accelerates deconstruction speed up to 8x with Blaze Core',
    'Output Channels': '4-slot segregated extraction bay'
  }"
/>

---

## ⚙️ Gear Overclocking Tiers

Inserting a gear into the top-right upgrade socket accelerates the disassembly cycle:

| Installed Gear | Cycle Speed Multiplier | Processing Time |
| :--- | :--- | :--- |
| **No Gear / Iron Gear** | 1.0x (Standard) | 120 ticks (6.0s) |
| **Copper / Bronze Gear** | 1.5x Fast | 80 ticks (4.0s) |
| **Gold Gear** | 2.0x High-Speed | 60 ticks (3.0s) |
| **Diamond / Titanium Gear** | 3.5x Heavy Industrial | 34 ticks (1.7s) |
| **Netherite Gear** | 5.0x Turbo | 24 ticks (1.2s) |
| **Blaze Overclock Core** | 8.0x Instant Flash Deconstruct | 15 ticks (0.75s) |

---

## 📋 Deconstruction Capabilities

The Item Salvager supports complete breakdown of hundreds of items, including:
- **All Armor & Tool Sets:** Iron, Copper, Bronze, Steel, Titanium, Diamond, and Netherite equipment.
- **Machinery & Generators:** Crushers, Generators, Batteries, Blast Furnaces, Refiners, and Laser Quarries.
- **Chests & Storage:** Modular Enchanted Chests, Drive Bays, Terminals, and Controllers.
- **Pipes & Conduits:** Copper, Aluminum, Steel, and Basalt cables, gas pipes, and fluid conduits.

---

## 📜 Crafting Recipe

<MinecraftRecipe id="item_salvager" />
