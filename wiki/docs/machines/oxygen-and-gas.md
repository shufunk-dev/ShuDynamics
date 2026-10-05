# 💨 Oxygen Generator & Industrial Gas Canisters

The **Oxygen Generator** is an advanced electrolysis unit that splits water into high-purity **Oxygen Gas ($O_2$)** and **Hydrogen Gas ($H_2$)**. These gases are compressed directly into portable canisters to power metallurgical furnaces, high-temp welding torches, and jetpacks.

---

## 🌬️ Oxygen Generator

<MachineShowcase 
  name="Oxygen Generator"
  icon="/textures/block/oxygen_generator_front.png"
  tier="Tier 2 (Mid Game)"
  tierClass="tier-mid"
  category="Electrolysis & Gas Extraction"
  description="Consumes water and electrical power (30 E/t) to split H2O into Oxygen and Hydrogen. Automatically fills Empty Gas Canisters inserted into its charging slots."
  :specs="{
    'Energy Consumption': '30 E/tick',
    'Water Tank Capacity': '10,000 mB Water',
    'Oxygen Storage': '8,000 mB O2 Gas',
    'Hydrogen Storage': '8,000 mB H2 Gas',
    'Gas Filling Slots': 'O2 Canister Slots (2->3) & H2 Canister Slots (4->5)'
  }"
  placedImage="/images/machines/oxygen_generator_placed.png"
  guiImage="/images/machines/oxygen_generator_gui.png"
/>

---

## 🛢️ Gas Canisters & Their Uses

<div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; margin: 20px 0;">
  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(255,255,255,0.08); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/empty_gas_canister.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h4 style="margin:0; color:#c4b5fd;">Empty Gas Canister</h4>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">Standard pressurized vessel. Insert into the Oxygen Generator slots to charge with Oxygen or Hydrogen (1,000 mB each).</p>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(6,182,212,0.3); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/oxygen_canister.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h4 style="margin:0; color:#38bdf8;">Oxygen Canister (O2)</h4>
    </div>
    <ul style="font-size:12px; color:#d1d5db; margin:8px 0 0 0; padding-left:18px; line-height:1.6;">
      <li><strong>Aluminum Refiner</strong>: Required for high-temperature bauxite purification.</li>
      <li><strong>Oxy-Hydrogen Torch</strong>: High-output chemical fuel.</li>
    </ul>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(168,85,247,0.3); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/hydrogen_canister.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h4 style="margin:0; color:#c084fc;">Hydrogen Canister (H2)</h4>
    </div>
    <ul style="font-size:12px; color:#d1d5db; margin:8px 0 0 0; padding-left:18px; line-height:1.6;">
      <li><strong>Steel Blast Furnace</strong>: Reducing agent for smelting Steel.</li>
      <li><strong>Hydrogen Jetpack</strong>: High-thrust flight propellant.</li>
      <li><strong>Oxy-Hydrogen Torch</strong>: High-temperature ignition.</li>
    </ul>
  </div>
</div>
