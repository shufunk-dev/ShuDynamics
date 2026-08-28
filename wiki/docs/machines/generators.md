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

---

## 🌋 Geothermal Magma Generator (Tier 4)

<MachineShowcase 
  name="Geothermal Generator"
  icon="/textures/block/geothermal_generator_front.png"
  tier="Tier 4 (Nether & Magma Tech)"
  tierClass="tier-high"
  category="Thermal Liquid Generation"
  description="High-temperature geothermal turbine driven by molten lava. Connects directly to Lava Pipes or the Lava Pump to generate clean high-voltage RF continuously."
  :specs="{
    'Output Generation Rate': '400 E/tick',
    'Max Energy Buffer': '1,000,000 E',
    'Fuel Type': 'Liquid Lava (Buckets or piped via Lava Pipes)',
    'Lava Tank Capacity': '8,000 mB (8 Buckets)',
    'Auto-Push': 'Direct high-voltage cable pushing to connected batteries and machines'
  }"
  placedImage=""
  guiImage=""
/>

---

## ⛽ Automated Lava Pump & Lava Pipes

<div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap:16px; margin:20px 0;">
  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(234,88,12,0.3); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:8px;">
      <img src="/textures/block/lava_pump_front.png" style="width:32px; height:32px; image-rendering:pixelated;" />
      <h4 style="margin:0; font-size:15px; color:#fdba74;">Lava Pump</h4>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0 0 8px 0;">Submersible thermal pump that continuously draws lava source blocks from natural lava oceans or lakes.</p>
    <ul style="font-size:12px; color:#d1d5db; padding-left:18px; margin:0;">
      <li>Extracts lava without depleting world lakes destructively.</li>
      <li>Feeds Geothermal Generators and Magma Crucibles via connected pipes.</li>
    </ul>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(249,115,22,0.3); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:8px;">
      <img src="/textures/block/titanium_lava_pipe.png" style="width:32px; height:32px; image-rendering:pixelated;" />
      <h4 style="margin:0; font-size:15px; color:#fdba74;">Lava Pipes</h4>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0 0 8px 0;">Reinforced refractory piping engineered to transport molten lava across all 6 faces without melting.</p>
    <ul style="font-size:12px; color:#d1d5db; padding-left:18px; margin:0;">
      <li>Connects Lava Pumps, Geothermal Generators, Crucibles, and Reservoirs.</li>
      <li>Configurable via the Industrial Wrench.</li>
    </ul>
  </div>
</div>

