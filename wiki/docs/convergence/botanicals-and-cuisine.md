# 🍱 Convergence Botanicals, Agronomy & Gourmet Cuisine

**ShuDynamics 2.1** expands agricultural and culinary mechanics with exotic botanical species native to The Convergence, multi-stage farming, gourmet sushi rolls granting combat advantages and environmental hazard protections, and the legendary **Multi-Course Bento Box Artifacts**.

::: tip 🎵 Official Culinary Soundtrack
Enjoy the relaxing, lo-fi kitchen groove **"Haven Bloom (The Bento Groove)"** pressed via the [Harmonic Record Press](/machines/harmonic-record-press) or streamed on the [Official Suno Playlist](https://suno.com/playlist/343b0f74-ce3c-424d-8324-c26ef30eb78e)!
:::

---

## 🍱 The Legendary Multi-Course Bento Boxes

The pinnacle of culinary achievement in ShuDynamics. These infinite relics are never consumed upon eating and feature **active course switching**:

<style>
.bento-card-wrap {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
  margin: 16px 0;
}

.bento-card {
  background: rgba(26, 28, 43, 0.7);
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 12px;
  padding: 18px;
}

.bento-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.bento-icon {
  width: 40px;
  height: 40px;
  image-rendering: pixelated;
}

.bento-title {
  font-size: 16px;
  font-weight: 700;
  color: #f3f4f6;
}
</style>

<div class="bento-card-wrap">
  <div class="bento-card">
    <div class="bento-header">
      <img src="/textures/item/eternal_bento_box.png" class="bento-icon" alt="Eternal Bento Box" />
      <div>
        <div class="bento-title">Eternal Bento Box</div>
        <span style="font-size:12px; color:#a78bfa;">Dimensional Relic (Tier 1 Drop)</span>
      </div>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">
      A never-ending bento box recovered from the Resonance Colossus. Offers infinite nourishment across three selectable culinary courses.
    </p>
  </div>

  <div class="bento-card" style="border-color: rgba(239, 68, 68, 0.4);">
    <div class="bento-header">
      <img src="/textures/item/omega_bento_box.png" class="bento-icon" alt="Omega Bento Box" />
      <div>
        <div class="bento-title" style="color:#fca5a5;">Omega Bento Box</div>
        <span style="font-size:12px; color:#f87171;">Apex Artifact (Tier 3 Upgrade)</span>
      </div>
    </div>
    <p style="font-size:13px; color:#9ca3af; margin:0;">
      Forged by infusing an Eternal Bento Box with a <strong>Cataclysm Heart</strong>, 4x Master Rainbow Rolls, and 2x Netherite Ingots. Upgrades all course durations and effect tiers to maximum potency!
    </p>
  </div>
</div>

### 🔄 Multi-Course Switching Mechanics
* **Shift + Right-Click**: Hold Shift and Right-Click while holding either Bento Box to cycle instantly to the next course.
* **Audio & Action Bar Feedback**: Plays a crisp chime sound and displays an on-screen notification showing the newly selected course.
* **The 3 Selectable Courses**:
  1. 🍣 **Course 1: Master Feast** — Restores full hunger and saturation, granting extended **Regeneration II** and Saturation.
  2. ⚡ **Course 2: Wasabi Combat Rush** — Grants **Strength II**, **Speed II**, and active debuff immunity. Immediately cleanses and completely prevents **Weakness**, **Mining Fatigue**, and **Slowness**!
  3. 🍯 **Course 3: Honey Mochi Fortification** — Grants **Absorption IV** (8 bonus hearts), **Resistance II**, **Fire Resistance**, and **Kinetic Dampening** (100% knockback negation).

---

## 🌿 Exotic Convergence Flora

### 1. Wild Wasabi (`enchantedwood:wild_wasabi`)
* **Habitat**: Naturally spawns along caustic riverbanks and water edges in The Convergence.
* **Harvest**: Drops **Wasabi Root** (`enchantedwood:wasabi_root`). Used in sushi rolling and the synthesis of **Adrenal Essence**.

### 2. Wild Dragon Fruit (`enchantedwood:wild_dragon_fruit`)
* **Habitat**: Grows across volcanic soils and basalt fields in the Scorched Caldera.
* **Harvest**: Drops **Dragon Fruit** (`enchantedwood:dragon_fruit`). Eaten directly for quick nourishment or refined in the Centrifuge for **Cellular Nanite Extract**.

### 3. Starfruit Woodset
* **Components**: Starfruit Log, Wood, Stripped Wood, Planks, Leaves, and Sapling.
* **Harvest**: Leaves naturally drop golden **Starfruit** (`enchantedwood:starfruit`). Crafted into the celestial **Starfruit Tart**.

### 4. Avocado Woodset
* **Components**: Avocado Log, Wood, Stripped Wood, Planks, Leaves, and Sapling.
* **Harvest**: Leaves naturally drop ripe **Avocado** (`enchantedwood:avocado`). Essential staple for California Rolls and Avocado Cucumber Rolls.

### 5. Water-Saturated Rice Paddies
* **Crop Cycle**: Plant **Rice Seeds** in water-saturated tilled farmland. Advances through 8 visual growth stages into **Rice** (`enchantedwood:rice`).
* **Processing**: Cooking raw rice in a furnace, smoker, or campfire with seasoning produces sticky **Sushi Rice** (`enchantedwood:sushi_rice`).

### 6. Cucumber Vines
* **Crop Cycle**: Plant **Cucumber Seeds** in tilled soil. Grows through multiple vine stages into crisp **Cucumber** (`enchantedwood:cucumber`).

---

## 🍣 Gourmet Sushi Rolls

Using **Sushi Rice** and **Nori Sheets** (crafted by drying or cooking Kelp), players can roll culinary delicacies:

| Gourmet Roll | Recipe Ingredients | Restored Stats | Special Status Effects |
| :--- | :--- | :--- | :--- |
| **Salmon Roll** | Sushi Rice + Nori + Raw Salmon | 7 Hunger, 6.0 Saturation | **Dolphin's Grace I** (20s) |
| **Cod Roll** | Sushi Rice + Nori + Raw Cod | 6 Hunger, 5.0 Saturation | **Haste I** (30s) |
| **Avocado Cucumber Roll** | Sushi Rice + Nori + Avocado + Cucumber | 6 Hunger, 5.5 Saturation | **Speed I** (35s) |
| **California Roll** | Sushi Rice + Nori + Avocado + Cucumber + Crab/Salmon | 9 Hunger, 8.0 Saturation | **Regeneration I** (8s) |
| **Garden Roll** | Sushi Rice + Nori + Cucumber + Carrot | 8 Hunger, 7.0 Saturation | **Night Vision** (45s) |
| **Master Rainbow Roll** | Sushi Rice + Nori + Salmon + Cod + Avocado + Cucumber | 14 Hunger, 12.0 Saturation | **Speed II, Haste II, Resistance I, Regeneration II** (60s) + Full Saturation |

---

## 🛡️ Protective Hazard-Shield Foods

Crafted specifically to counter the extreme conditions of The Convergence, these edible items serve as emergency alternatives to Hypospray inoculants:

### 1. Alkaline Detox Roll (`enchantedwood:alkaline_detox_roll`)
* **Recipe**: Sushi Rice + Nori + Cucumber + Wasabi Root.
* **Effect**: Grants **Acid Protection (4:00)**—100% immunity to caustic acid rain and pools.

### 2. Volcanic Dragon Roll (`enchantedwood:volcanic_dragon_roll`)
* **Recipe**: Sushi Rice + Nori + Dragon Fruit + Magma Cream.
* **Effect**: Grants **Thermal Protection (4:00)**—100% immunity to caldera heat and lava burns.

### 3. High-Altitude Kelp Roll (`enchantedwood:high_altitude_kelp_roll`)
* **Recipe**: Sushi Rice + 2x Nori + Seagrass + Titanium Nugget.
* **Effect**: Grants **Atmospheric Protection (4:00)**—maintains internal air supply in anoxic zones.

### 4. Wasabi Nigiri (`enchantedwood:wasabi_nigiri`)
* **Recipe**: Sushi Rice + Wasabi Root.
* **Effect**: Instant medicinal detox—immediately cleanses **Slowness**, **Weakness**, **Nausea**, and **Poison**. Essential during the Resonance Colossus boss fight!

### 5. Pitaya Bowl (`enchantedwood:pitaya_bowl`)
* **Recipe**: Dragon Fruit + Starfruit + Bowl + Honey Bottle.
* **Effect**: Grants **Absorption II (2:00)** and **Fire Resistance (2:00)**.

### 6. Golden Honey Mochi (`enchantedwood:golden_honey_mochi`)
* **Recipe**: Rice + Honey Bottle + Gold Nugget.
* **Effect**: Grants **Kinetic Dampening (3:00)**—provides 100% knockback resistance and cushions high-velocity impacts.

### 7. Starfruit Tart (`enchantedwood:starfruit_tart`)
* **Recipe**: Starfruit + Sugar + Egg + Wheat.
* **Effect**: Grants **Celestial Leap (2:00)**—Jump Boost III paired with continuous Slow Falling glide.
