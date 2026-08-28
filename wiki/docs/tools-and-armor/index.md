# 🛡️ Armor Sets & Protective Gear

ShuDynamics features unique custom armor sets across all progression tiers. Each set provides tailored defenses, toughness, and special environmental perks.

---

## 🪓 Enchanted Wood Armor (Early Tier)

<ArmorShowcase 
  name="Enchanted Wood Armor"
  tier="Tier 1 (Early Game)"
  tierClass="tier-early"
  description="Crafted from treated Enchanted Wood planks. Lightweight natural armor offering early protection with high durability."
  image="/images/armor/enchanted_wood_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Enchanted Wood Helmet', defense: 2, durability: 165, icon: '/textures/item/enchanted_wood_helmet.png' },
    { slot: 'chestplate', name: 'Enchanted Wood Chestplate', defense: 5, durability: 240, icon: '/textures/item/enchanted_wood_chestplate.png' },
    { slot: 'leggings', name: 'Enchanted Wood Leggings', defense: 4, durability: 225, icon: '/textures/item/enchanted_wood_leggings.png' },
    { slot: 'boots', name: 'Enchanted Wood Boots', defense: 2, durability: 195, icon: '/textures/item/enchanted_wood_boots.png' }
  ]"
  :toughness="0"
  :knockbackResistance="0"
  setBonus="Forest Stride: High early-game durability and natural resistance to thorns."
/>

---

## 🪨 Enchanted Cobblestone Armor (Early Tier)

<ArmorShowcase 
  name="Enchanted Cobblestone Armor"
  tier="Tier 1 (Early Game)"
  tierClass="tier-early"
  description="Infused stone plates offering heavy physical impact defense and early knockback dampening."
  image="/images/armor/enchanted_cobblestone_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Enchanted Cobble Helmet', defense: 2, durability: 200, icon: '/textures/item/enchanted_cobblestone_helmet.png' },
    { slot: 'chestplate', name: 'Enchanted Cobble Chestplate', defense: 6, durability: 280, icon: '/textures/item/enchanted_cobblestone_chestplate.png' },
    { slot: 'leggings', name: 'Enchanted Cobble Leggings', defense: 5, durability: 260, icon: '/textures/item/enchanted_cobblestone_leggings.png' },
    { slot: 'boots', name: 'Enchanted Cobble Boots', defense: 2, durability: 220, icon: '/textures/item/enchanted_cobblestone_boots.png' }
  ]"
  :toughness="1"
  :knockbackResistance="0.1"
  setBonus="Stone Fortitude: +10% Knockback Resistance against physical monster hits."
/>

---

## 🥫 Tin Armor (Early/Mid Tier)

<ArmorShowcase 
  name="Tin Armor"
  tier="Tier 1.5 (Early/Mid Game)"
  tierClass="tier-early"
  description="Lightweight and accessible early-game metal armor smelted from Raw Tin. Provides dependable starter protection before establishing alloy furnaces."
  image="/images/armor/tin_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Tin Helmet', defense: 2, durability: 165, icon: '/textures/item/tin_helmet.png' },
    { slot: 'chestplate', name: 'Tin Chestplate', defense: 5, durability: 240, icon: '/textures/item/tin_chestplate.png' },
    { slot: 'leggings', name: 'Tin Leggings', defense: 4, durability: 225, icon: '/textures/item/tin_leggings.png' },
    { slot: 'boots', name: 'Tin Boots', defense: 2, durability: 195, icon: '/textures/item/tin_boots.png' }
  ]"
  :toughness="0"
  :knockbackResistance="0"
  setBonus="Lightweight Plating: Low-cost starter metal protection with balanced durability."
/>

---

## 🪶 Bronze Armor (Mid Tier)

<ArmorShowcase 
  name="Bronze Armor"
  tier="Tier 2 (Mid Game)"
  tierClass="tier-mid"
  description="Heavy metallurgical alloy armor offering defense superior to standard Iron with high enchantability."
  image="/images/armor/bronze_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Bronze Helmet', defense: 3, durability: 220, icon: '/textures/item/bronze_helmet.png' },
    { slot: 'chestplate', name: 'Bronze Chestplate', defense: 6, durability: 320, icon: '/textures/item/bronze_chestplate.png' },
    { slot: 'leggings', name: 'Bronze Leggings', defense: 5, durability: 300, icon: '/textures/item/bronze_leggings.png' },
    { slot: 'boots', name: 'Bronze Boots', defense: 3, durability: 260, icon: '/textures/item/bronze_boots.png' }
  ]"
  :toughness="1"
  :knockbackResistance="0.05"
  setBonus="Alloy Plating: High kinetic damage reduction and superior durability."
/>

---

## ✈️ Aluminum Armor (Advanced Tier)

<ArmorShowcase 
  name="Aluminum Armor"
  tier="Tier 2.5 (Industrial Tier)"
  tierClass="tier-mid"
  description="Refined industrial metal suit processed with bauxite and oxygen in the Aluminum Refiner. Lightweight aerospace-grade metal with innate toughness."
  image="/images/armor/aluminum_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Aluminum Helmet', defense: 2, durability: 198, icon: '/textures/item/aluminum_helmet.png' },
    { slot: 'chestplate', name: 'Aluminum Chestplate', defense: 6, durability: 288, icon: '/textures/item/aluminum_chestplate.png' },
    { slot: 'leggings', name: 'Aluminum Leggings', defense: 5, durability: 270, icon: '/textures/item/aluminum_leggings.png' },
    { slot: 'boots', name: 'Aluminum Boots', defense: 2, durability: 234, icon: '/textures/item/aluminum_boots.png' }
  ]"
  :toughness="0.5"
  :knockbackResistance="0"
  setBonus="Aerospace Agility: Lightweight industrial armor providing high mobility and solid defense."
/>

---

## ⚙️ Steel Armor (Heavy Industrial Tier)

<ArmorShowcase 
  name="Steel Armor"
  tier="Tier 2.8 (Industrial Heavy)"
  tierClass="tier-mid"
  description="Heavy blast furnace steel suit forged from refined iron and coke coal. Offers substantial kinetic absorption and knockback dampening."
  image="/images/armor/steel_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Steel Helmet', defense: 3, durability: 308, icon: '/textures/item/steel_helmet.png' },
    { slot: 'chestplate', name: 'Steel Chestplate', defense: 8, durability: 448, icon: '/textures/item/steel_chestplate.png' },
    { slot: 'leggings', name: 'Steel Leggings', defense: 6, durability: 420, icon: '/textures/item/steel_leggings.png' },
    { slot: 'boots', name: 'Steel Boots', defense: 3, durability: 364, icon: '/textures/item/steel_boots.png' }
  ]"
  :toughness="1.5"
  :knockbackResistance="0.1"
  setBonus="Heavy Reinforcement: High physical impact reduction and knockback dampening."
/>

---

## 🚀 Titanium Armor (Advanced Tier)

<ArmorShowcase 
  name="Titanium Armor"
  tier="Tier 3 (Late Game)"
  tierClass="tier-high"
  description="High-density aerospace titanium alloy suit. Provides top-tier blast resistance, extreme toughness, and high armor rating."
  image="/images/armor/titanium_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Titanium Helmet', defense: 3, durability: 385, icon: '/textures/item/titanium_helmet.png' },
    { slot: 'chestplate', name: 'Titanium Chestplate', defense: 8, durability: 560, icon: '/textures/item/titanium_chestplate.png' },
    { slot: 'leggings', name: 'Titanium Leggings', defense: 6, durability: 525, icon: '/textures/item/titanium_leggings.png' },
    { slot: 'boots', name: 'Titanium Boots', defense: 3, durability: 455, icon: '/textures/item/titanium_boots.png' }
  ]"
  :toughness="2"
  :knockbackResistance="0.2"
  setBonus="Titanium Shell: Massive blast dampening and heavy armor rating exceeding Diamond."
/>

---

## 💎 Enchanted Diamond Armor (High Tier)

<ArmorShowcase 
  name="Enchanted Diamond Armor"
  tier="Tier 3 (High Tier)"
  tierClass="tier-high"
  description="Forged from pure Enchanted Diamonds and infused reagents. Radiates an innate magical sheen, granting superior protection, enhanced agility, and constant health recovery."
  image="/images/armor/enchanted_diamond_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Enchanted Diamond Helmet', defense: 4, durability: 550, icon: '/textures/item/enchanted_diamond_helmet.png' },
    { slot: 'chestplate', name: 'Enchanted Diamond Chestplate', defense: 8, durability: 800, icon: '/textures/item/enchanted_diamond_chestplate.png' },
    { slot: 'leggings', name: 'Enchanted Diamond Leggings', defense: 7, durability: 750, icon: '/textures/item/enchanted_diamond_leggings.png' },
    { slot: 'boots', name: 'Enchanted Diamond Boots', defense: 4, durability: 650, icon: '/textures/item/enchanted_diamond_boots.png' }
  ]"
  :toughness="3"
  :knockbackResistance="0.1"
  setBonus="Diamond Ward: Passive Resistance II, Speed I, and Regeneration I active while the complete set is equipped."
/>

---

## 🔮 Enchanted Netherite Armor (Endgame)

<ArmorShowcase 
  name="Enchanted Netherite Armor"
  tier="Endgame Masterpiece"
  tierClass="tier-end"
  description="Forged from enchanted netherite ingots and infused catalysts. Completely fireproof and unmatched in defensive capability."
  image="/images/armor/enchanted_netherite_worn.png"
  :pieces="[
    { slot: 'helmet', name: 'Enchanted Netherite Helmet', defense: 5, durability: 800, icon: '/textures/item/enchanted_netherite_helmet.png' },
    { slot: 'chestplate', name: 'Enchanted Netherite Chestplate', defense: 10, durability: 1100, icon: '/textures/item/enchanted_netherite_chestplate.png' },
    { slot: 'leggings', name: 'Enchanted Netherite Leggings', defense: 8, durability: 950, icon: '/textures/item/enchanted_netherite_leggings.png' },
    { slot: 'boots', name: 'Enchanted Netherite Boots', defense: 5, durability: 850, icon: '/textures/item/enchanted_netherite_boots.png' }
  ]"
  :toughness="4"
  :knockbackResistance="0.3"
  setBonus="Netherite Aura: Fire immunity, extreme knockback resistance, and ultimate defense."
/>

---

## 🌋 Nether Survival Modules & Specialized Heavy Tools

### 🛡️ Thermal Refractory Plating
An advanced protective matrix engineered for extreme volcanic exploration:
* **Passive Fire Resistance**: Grants continuous **Fire Resistance** and instant auto-extinguishing while carried in your inventory or off-hand.
* **Lava Buoyancy & Mobility**: Enables frictionless movement and positive buoyancy across lava lakes.
* **Fireproof**: Cannot burn or despawn in lava.

#### Crafting Recipe
<MinecraftRecipe id="thermal_refractory_plating" />

---

### 🔨 Infernal Auto-Smelt Sledgehammer
A heavy Netherite-tier mining sledgehammer embedded with Fire Crystals:
* **3x3 Excavation**: Clears 3x3 block areas in a single swing.
* **Instant Auto-Smelt**: Instantly converts mined raw ores into smelted ingots (Raw Iron $\rightarrow$ Iron Ingot, Raw Copper $\rightarrow$ Copper Ingot, Raw Gold $\rightarrow$ Gold Ingot, Sand $\rightarrow$ Glass, Cobblestone $\rightarrow$ Stone).
* **Durability**: 3,200 uses, 100% fireproof.

#### Crafting Recipe
<MinecraftRecipe id="infernal_hammer" />

---

### 🔫 Thermal Plasma Flamethrower
A handheld plasma weapon projecting a sustained cone of superheated fire:
* **Continuous Plasma Stream**: Unleashes a 12-block piercing plasma beam that passes through mobs.
* **Target Ignition**: Sets enemies on fire for 8 seconds and deals direct thermal arc damage.
* **Durability**: 850 Uses.

#### Crafting Recipe
<MinecraftRecipe id="plasma_flamethrower" />


