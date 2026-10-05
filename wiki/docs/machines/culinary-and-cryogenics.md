# 🧊 Cryogenics, Freezers & Kitchen Appliances

ShuDynamics v2.3.0 introduces a dedicated cryogenics, fluid logistics, and gourmet culinary suite designed for both high-voltage industrial automation and off-grid homestead cooking.

---

## ❄️ Cryo Freezer

<div style="background:rgba(26,28,43,0.8); border:1px solid rgba(56,189,248,0.4); border-radius:12px; padding:20px; margin:20px 0;">
  <div style="display:flex; align-items:center; gap:14px; margin-bottom:12px;">
    <img src="/textures/block/cryo_freezer_front.png" style="width:40px; height:40px; image-rendering:pixelated;" />
    <div>
      <h3 style="margin:0; font-size:18px; color:#7dd3fc;">Cryo Freezer</h3>
      <span style="font-size:12px; color:#38bdf8; text-transform:uppercase; font-weight:700;">Sub-Zero Cryogenic Crystallization</span>
    </div>
  </div>
  <p style="font-size:14px; color:#d1d5db; line-height:1.6; margin:0 0 12px 0;">
    An electric machine powered by RF energy that crystallizes pumped water or water buckets into sub-zero ice tiers and compacts snowballs.
  </p>
  <ul style="font-size:13px; color:#e5e7eb; line-height:1.6; padding-left:20px; margin:0;">
    <li><strong>Energy Consumption:</strong> 25 FE/tick (reduced by Speed/Efficiency Gears).</li>
    <li><strong>Internal Water Reservoir:</strong> 10,000 mB fluid tank with live illuminated level indicator.</li>
    <li><strong>Fluid Input:</strong> Automatically accepts piped water directly from Water Pipes or via Water Buckets in the input slot.</li>
    <li><strong>Ice Tiers:</strong>
      <ul>
        <li><code>Water (1,000 mB)</code> &rarr; <strong>Regular Ice</strong> (Melts near heat, generates water sources).</li>
        <li><code>Ice + 1,000 mB</code> &rarr; <strong>Packed Ice</strong> (Never melts near light, high-speed boat highways).</li>
        <li><code>Packed Ice + 1,000 mB</code> &rarr; <strong>Blue Ice</strong> (Hyper-speed 72 m/s boat routes, Nether-safe, super-refrigerant).</li>
        <li><code>Water (250 mB)</code> &rarr; <strong>4x Ice Cubes</strong> (Fast culinary coolant).</li>
        <li><code>4x Snowballs + 250 mB</code> &rarr; <strong>Regular Ice</strong> (Snowball compaction).</li>
      </ul>
    </li>
  </ul>
</div>

---

## 💧 Water Pump & Water Pipes

<div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap:16px; margin:20px 0;">
  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(56,189,248,0.3); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:8px;">
      <img src="/textures/block/water_pump_front.png" style="width:32px; height:32px; image-rendering:pixelated;" />
      <h4 style="margin:0; font-size:16px; color:#7dd3fc;">Water Pump</h4>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0 0 8px 0;">
      Extracts water from source blocks directly below it and pumps it into connected fluid networks at <strong>20 FE/tick</strong>.
    </p>
    <span style="font-size:12px; color:#38bdf8;">• Auto-Detects Water Sources & Infinite Aquifers</span>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(56,189,248,0.3); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:10px; margin-bottom:8px;">
      <img src="/textures/block/water_pipe.png" style="width:32px; height:32px; image-rendering:pixelated;" />
      <h4 style="margin:0; font-size:16px; color:#7dd3fc;">Water Pipes</h4>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0 0 8px 0;">
      6-way auto-connecting fluid conduits that route water seamlessly between pumps, tanks, and Cryo Freezers.
    </p>
    <span style="font-size:12px; color:#38bdf8;">• Zero Pressure Loss & Seamless Collision</span>
  </div>
</div>

---

## 🍦 Ice Cream Machine (Dual-Mode: Manual & Electric)

<div style="background:rgba(26,28,43,0.8); border:1px solid rgba(236,72,153,0.4); border-radius:12px; padding:20px; margin:20px 0;">
  <div style="display:flex; align-items:center; gap:14px; margin-bottom:12px;">
    <img src="/textures/block/ice_cream_machine_front.png" style="width:40px; height:40px; image-rendering:pixelated;" />
    <div>
      <h3 style="margin:0; font-size:18px; color:#f472b6;">Ice Cream Machine</h3>
      <span style="font-size:12px; color:#ec4899; text-transform:uppercase; font-weight:700;">Manual Churner & Industrial Dairy Processor</span>
    </div>
  </div>
  <p style="font-size:14px; color:#d1d5db; line-height:1.6; margin:0 0 12px 0;">
    Features a protruding 3D crank handle on the side. Can be operated completely off-grid by hand-cranking, or connected to 10 FE/t power for automated continuous production.
  </p>
  <ul style="font-size:13px; color:#e5e7eb; line-height:1.6; padding-left:20px; margin:0;">
    <li><strong>Off-Grid Hand Cranking:</strong> Right-click the handle on the side from the front, top, or side. Each click churns the barrel (+20 progress). 10 clicks produce a full batch of <strong>2 Ice Creams</strong> without needing generators or cables!</li>
    <li><strong>Electric Churning:</strong> Connect power cables (10 FE/t) to engage internal electric motor for hands-free automation.</li>
    <li><strong>Zero Idle Coolant Waste:</strong> The cooling timer completely pauses when waiting for ingredients or when output is full.</li>
    <li><strong>Refrigerant Durations:</strong>
      <ul>
        <li><code>Snowball</code>: 150 ticks</li>
        <li><code>Ice Cubes</code>: 400 ticks (2 batches)</li>
        <li><code>Regular Ice / Snow Block</code>: 600 ticks (3 batches)</li>
        <li><code>Salt</code>: 800 ticks (4 batches)</li>
        <li><code>Packed Ice</code>: 1,400 ticks (7 batches)</li>
        <li><code>Blue Ice</code>: 3,600 ticks (18 batches / 36 tubs!)</li>
      </ul>
    </li>
    <li><strong>Flavor Varieties:</strong> Vanilla (plain), Strawberry (Strawberries), Blueberry (Blueberries), Chocolate (Cocoa Beans), Sweet Berry (Sweet Berries).</li>
  </ul>
</div>

---

## 🍕 Brick Oven (Dual-Fuel Kitchen Hearth)

<div style="background:rgba(26,28,43,0.8); border:1px solid rgba(249,115,22,0.4); border-radius:12px; padding:20px; margin:20px 0;">
  <div style="display:flex; align-items:center; gap:14px; margin-bottom:12px;">
    <img src="/textures/block/brick_oven_front.png" style="width:40px; height:40px; image-rendering:pixelated;" />
    <div>
      <h3 style="margin:0; font-size:18px; color:#fb923c;">Brick Oven</h3>
      <span style="font-size:12px; color:#f97316; text-transform:uppercase; font-weight:700;">Artisan Hearth & Electric Baking Chamber</span>
    </div>
  </div>
  <p style="font-size:14px; color:#d1d5db; line-height:1.6; margin:0 0 12px 0;">
    A traditional brick hearth featuring animated chimney smoke and a glowing firebox. Operates off-grid with solid fuels or switches automatically to internal electric heating coils when powered.
  </p>
  <ul style="font-size:13px; color:#e5e7eb; line-height:1.6; padding-left:20px; margin:0;">
    <li><strong>Solid Fuel Support:</strong> Wood Planks/Logs (300t), Coal/Charcoal (1,600t), Blaze Rods (2,400t), Lava Buckets (20,000t).</li>
    <li><strong>Electric Heating:</strong> 10 FE/t heating elements automatically take priority when power is supplied, conserving wood and coal.</li>
    <li><strong>Artisan Recipes:</strong>
      <ul>
        <li><code>Raw Margherita / Meat Lovers / Supreme / Anchovy Onion Pizza</code> &rarr; <strong>Steamy Finished Pizza</strong></li>
        <li><code>Pizza Dough</code> &rarr; <strong>Golden Burger Buns</strong></li>
        <li><code>Raw Burger Patty</code> &rarr; <strong>Cooked Burger Patty</strong></li>
        <li><code>Water Bucket</code> &rarr; <strong>4x Sea Salt</strong> (evaporates water; returns bucket)</li>
        <li>Supports all vanilla food smelting and smoking recipes at high baking efficiency.</li>
      </ul>
    </li>
  </ul>
</div>
