# ⚡ Solid Fuel Power Generators

Generators burn combustible solid fuels (Coal, Charcoal, Logs, or high-density **Enchanted Coal**) to produce electrical power (**E**). All three generator tiers share the same core operating mechanics while offering progressive increases in generation rate (E/t) and internal battery buffer capacity.

---

## 🎥 Unified Generator Walkthrough Video

<div style="margin: 20px 0;">
  <VideoPlayer 
    url="" 
    title="Complete Guide to Solid Fuel Generators (Copper, Aluminum & Titanium Tiers)" 
  />
</div>

---

## 🥉 Copper Generator (Tier 1)

<MachineShowcase 
  name="Copper Generator"
  icon="/textures/block/copper_generator_front.png"
  tier="Tier 1 (Early Game)"
  tierClass="tier-early"
  category="Solid Fuel Power Generation"
  description="Your first automated energy source. Efficiently converts standard combustible fuels into stable electrical current."
  :specs="{
    'Output Generation Rate': '40 E/tick',
    'Max Energy Buffer': '20,000 E',
    'Fuel Compatibility': 'Coal, Charcoal, Logs, Enchanted Coal, Lava',
    'Redstone Control': 'Ignition toggle via redstone wire',
    'Auto-Push': 'Automatically outputs energy to connected cables/batteries'
  }"
  placedImage="/images/machines/copper_generator_placed.png"
  guiImage="/images/machines/copper_generator_gui.png"
/>

---

## 🥈 Aluminum Generator (Tier 2)

<MachineShowcase 
  name="Aluminum Generator"
  icon="/textures/block/aluminum_generator_front.png"
  tier="Tier 2 (Mid Game)"
  tierClass="tier-mid"
  category="Solid Fuel Power Generation"
  description="Upgraded turbine and internal heat exchangers enable 3x the power output of the Copper Generator."
  :specs="{
    'Output Generation Rate': '120 E/tick',
    'Max Energy Buffer': '100,000 E',
    'Fuel Compatibility': 'All solid fuels + high efficiency with Enchanted Coal',
    'Internal Heat Dissipation': 'Active thermal insulation',
    'Auto-Push': 'High-throughput direct cable pushing'
  }"
  placedImage="/images/machines/aluminum_generator_placed.png"
  guiImage="/images/machines/aluminum_generator_gui.png"
/>

---

## 🥇 Steel Generator (Tier 3)

<MachineShowcase 
  name="Steel Generator"
  icon="/textures/block/steel_generator_front.png"
  tier="Tier 3 (Late Game)"
  tierClass="tier-high"
  category="Heavy Solid Fuel Generation"
  description="Industrial power generator reinforced with heavy steel plating. Delivers massive energy output and extreme fuel efficiency for heavy tech machinery."
  :specs="{
    'Output Generation Rate': '360 E/tick',
    'Max Energy Buffer': '500,000 E',
    'Fuel Compatibility': 'All solid fuels + Coke Coal (+50% bonus)',
    'Auto-Push': 'High-throughput direct cable pushing'
  }"
  placedImage="/images/machines/steel_generator_placed.png"
  guiImage="/images/machines/steel_generator_gui.png"
/>
