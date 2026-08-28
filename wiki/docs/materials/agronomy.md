# 🌾 Agronomy, Volcanic Soil & Nether Farming

ShuDynamics introduces a high-potency agronomy and automated farming suite engineered to cultivate crops and timber even in extreme environments like the **Nether**.

---

## 🌋 Volcanic Soil & Soil Infusion

<div style="background:rgba(26,28,43,0.8); border:1px solid rgba(234,88,12,0.4); border-radius:12px; padding:20px; margin:20px 0;">
  <div style="display:flex; align-items:center; gap:14px; margin-bottom:12px;">
    <img src="/textures/block/volcanic_soil.png" style="width:40px; height:40px; image-rendering:pixelated;" />
    <div>
      <h3 style="margin:0; font-size:18px; color:#fdba74;">Volcanic Soil</h3>
      <span style="font-size:12px; color:#fb923c; text-transform:uppercase; font-weight:700;">Self-Hydrating Fertile Earth</span>
    </div>
  </div>
  <p style="font-size:14px; color:#d1d5db; line-height:1.6; margin:0 0 12px 0;">
    A supercharged agricultural block infused with volcanic ash and geothermal minerals. It provides perpetual hydration and active biological acceleration without requiring any adjacent water sources.
  </p>
  <ul style="font-size:13px; color:#e5e7eb; line-height:1.6; padding-left:20px; margin:0;">
    <li><strong>Zero Water Required:</strong> Never dries out, degrades, or turns back to dirt—even in the fiery heat of the Nether.</li>
    <li><strong>Bone-Meal Pulse Acceleration:</strong> The soil itself receives active random ticks, periodically emitting green <code>HAPPY_VILLAGER</code> sparkles and applying an instant <strong>2 to 5 stage growth boost</strong> to crops and tree saplings planted above it.</li>
    <li><strong>Direct Planting:</strong> Right-click with Wheat Seeds, Carrots, Potatoes, Beetroots, Corn Seeds, Pumpkin/Melon Seeds, Torchflowers, Pitcher Pods, or Tree Saplings to plant directly.</li>
    <li><strong>Dimensions:</strong> Operates at maximum efficiency in the Overworld, Nether, End, and Mining Dimension.</li>
  </ul>
</div>

---

## 🏭 Soil Infuser Machine

<MachineShowcase 
  name="Soil Infuser"
  icon="/textures/block/soil_infuser_front.png"
  tier="Tier 3 (Agronomy Tech)"
  tierClass="tier-high"
  category="Soil Infusion & Bio-Engineering"
  description="Industrial catalytic infuser powered by RF energy. Blends mineral dirt with Volcanic Ash and thermal lava to produce rich Volcanic Soil."
  :specs="{
    'Energy Consumption': '40 E/tick',
    'Max Energy Buffer': '50,000 E',
    'Processing Time': '100 ticks (5.0 seconds)',
    'Inputs': 'Dirt + Volcanic Ash + Lava / Catalyst',
    'Output': 'Volcanic Soil'
  }"
  placedImage="/images/machines/soil_infuser_placed.png"
  guiImage="/images/machines/soil_infuser_gui.png"
/>

### 📜 Soil Infuser Recipe
<MinecraftRecipe id="soil_infuser" />

---

## 🌽 Sweet Corn & Crop Farming

<div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap:16px; margin:20px 0;">
  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(234,179,8,0.3); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:8px;">
      <img src="/textures/item/corn.png" style="width:32px; height:32px; image-rendering:pixelated;" />
      <h4 style="margin:0; font-size:15px; color:#fde047;">Sweet Corn Crop</h4>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0 0 8px 0;">An 8-stage high-yield agricultural crop producing fresh Corn and Corn Seeds.</p>
    <ul style="font-size:12px; color:#d1d5db; padding-left:18px; margin:0;">
      <li>Can be smelted in a furnace or smoker into <strong>Roasted Corn</strong> for high saturation.</li>
      <li>Serves as primary biomass feedstock in the <strong>Fuel Refinery</strong> for producing Biofuel.</li>
    </ul>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(249,115,22,0.3); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:8px;">
      <img src="/textures/item/volcanic_fertilizer.png" style="width:32px; height:32px; image-rendering:pixelated;" />
      <h4 style="margin:0; font-size:15px; color:#fdba74;">Volcanic Fertilizer</h4>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0 0 8px 0;">Super-concentrated organic mineral fertilizer synthesized from Volcanic Ash.</p>
    <ul style="font-size:12px; color:#d1d5db; padding-left:18px; margin:0;">
      <li>Provides instantaneous multi-stage growth to all crops, saplings, and plants.</li>
      <li>Has a 3x higher efficacy rate than standard Bone Meal.</li>
    </ul>
  </div>
</div>

---

## 🪓 Auto-Harvest & Replanting Hoes

ShuDynamics enchanted tools include integrated auto-harvesting capabilities on mature crops:

| Tool | Material | Durability | Special Ability |
| :--- | :--- | :--- | :--- |
| **Elderwood Hoe** | Enchanted Wood | 350 | Right-click mature crop to harvest drops & auto-replant seed |
| **Enchanted Cobblestone Hoe** | Enchanted Cobble | 450 | Right-click mature crop to harvest drops & auto-replant seed |

### How Auto-Harvest Works:
1. **Right-Click:** Point at any fully mature crop (Wheat, Carrots, Potatoes, Beetroots, Corn, Torchflowers, Pitcher, Cocoa Beans, Nether Wart).
2. **Instant Replant:** Consumes 1 seed from your inventory (or from the harvested crop drops) and resets the crop to age `0`.
3. **Drops in Place:** Spawns the harvested crop drops on the ground and consumes 1 tool durability.

---

## 💡 The Nether Greenhouse Setup

Combine **Volcanic Soil** with the **Enchanted Lamp** for optimal Nether farming:
1. **Light Level 15:** Crops require a light level $\ge 8$ in the Nether (since there is no skylight) to prevent popping off. The Enchanted Lamp provides permanent max illumination.
2. **Mob Sanctuary Ward:** The Enchanted Lamp automatically repels and dissolves all Ghasts, Blazes, Magma Cubes, and Hoglins within a 32-block radius, keeping your farm completely peaceful.
