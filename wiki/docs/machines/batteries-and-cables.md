# 🔋 Batteries & Energy Cabling

Power grids require buffers to store excess energy generated during low-demand periods and distribute it to high-consumption machines. All battery tiers and cables share the same plug-and-play mechanic with scaling transfer rates and capacities.

---

## 🎥 Unified Batteries & Cabling Video Walkthrough

<div style="margin: 20px 0;">
  <VideoPlayer 
    url="" 
    title="Guide to Power Storage Banks & Cabling Networks (All Tiers)" 
  />
</div>

---

## ⚡ Energy Storage Units (Batteries)

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

## 🔌 Energy Cables

Cables connect machines, generators, and batteries seamlessly across all 6 block faces.

| Cable Tier | Max Flow Rate | Best Paired With |
| :--- | :--- | :--- |
| **Copper Cable** | **200 E/t** | Copper Generator & Copper Battery |
| **Aluminum Cable** | **800 E/t** | Aluminum Generator, Refiners, Oxygen Gen |
| **Steel Cable** | **3,200 E/t** | Steel Generator & Heavy Machinery |
