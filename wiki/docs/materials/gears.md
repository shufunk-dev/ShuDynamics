# ⚙️ Mechanical Gears & Enchanted Variants

Gears are essential components required for constructing machinery, internal gearboxes, drive shafts, and high-efficiency generators.

---

## ⚙️ Gear Tiers & Variants

Every metal tier has both a **Standard Mechanical Gear** and an **Enchanted Gear** variant:

<div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; margin: 20px 0;">
  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(255,255,255,0.08); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/iron_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <img src="/textures/item/enchanted_iron_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h3 style="margin:0; font-size:16px; color:#c4b5fd;">Iron Gears</h3>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">Standard & Enchanted Iron Gears. Used in basic kinetic components and Copper tier generators.</p>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(255,255,255,0.08); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/copper_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <img src="/textures/item/enchanted_copper_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h3 style="margin:0; font-size:16px; color:#c4b5fd;">Copper Gears</h3>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">High thermal conductivity gears used in cooling systems and early generators.</p>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(255,255,255,0.08); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/bronze_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <img src="/textures/item/enchanted_bronze_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h3 style="margin:0; font-size:16px; color:#c4b5fd;">Bronze Gears</h3>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">Heavy-duty mechanical gears built to withstand extreme torque in refiners and compressors.</p>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(255,255,255,0.08); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/gold_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <img src="/textures/item/enchanted_gold_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h3 style="margin:0; font-size:16px; color:#c4b5fd;">Gold Gears</h3>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">Low-resistance conductor gears used in precision digital storage interfaces and sensor circuits.</p>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(255,255,255,0.08); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/diamond_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <img src="/textures/item/enchanted_diamond_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h3 style="margin:0; font-size:16px; color:#c4b5fd;">Diamond Gears</h3>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">Ultra-hard precision gears required for Aluminum Refiners and Oxygen Generators.</p>
  </div>

  <div style="background:rgba(26,28,43,0.7); border:1px solid rgba(255,255,255,0.08); border-radius:12px; padding:16px;">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:8px;">
      <img src="/textures/item/netherite_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <img src="/textures/item/enchanted_netherite_gear.png" style="width:36px; height:36px; image-rendering:pixelated;" />
      <h3 style="margin:0; font-size:16px; color:#c4b5fd;">Netherite Gears</h3>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">Endgame heavy drive gears utilized in Supercomputer processor units and heavy machinery.</p>
  </div>
</div>

---

## 🛠️ Crafting Standard vs Enchanted Gears

<MinecraftRecipe 
  title="Standard Iron Gear"
  :grid="[
    null, { name: 'Iron Ingot', icon: '/textures/item/iron_ingot.png' }, null,
    { name: 'Iron Ingot', icon: '/textures/item/iron_ingot.png' }, null, { name: 'Iron Ingot', icon: '/textures/item/iron_ingot.png' },
    null, { name: 'Iron Ingot', icon: '/textures/item/iron_ingot.png' }, null
  ]"
  :output="{ name: 'Iron Gear', icon: '/textures/item/iron_gear.png', count: 1 }"
/>

<MinecraftRecipe 
  title="Enchanted Iron Gear (Infused)"
  :grid="[
    null, { name: 'Enchanted Dust', icon: '/textures/item/enchanted_dust.png' }, null,
    { name: 'Enchanted Dust', icon: '/textures/item/enchanted_dust.png' }, { name: 'Iron Gear', icon: '/textures/item/iron_gear.png' }, { name: 'Enchanted Dust', icon: '/textures/item/enchanted_dust.png' },
    null, { name: 'Enchanted Dust', icon: '/textures/item/enchanted_dust.png' }, null
  ]"
  :output="{ name: 'Enchanted Iron Gear', icon: '/textures/item/enchanted_iron_gear.png', count: 1 }"
/>
