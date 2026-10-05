# 🛢️ Petrochemicals, Biofuels & Road Infrastructure

ShuDynamics introduces a complete petrochemical, biofuels, and road construction network. Refine crude hydrocarbon deposits from arid biomes or ferment agricultural crops in the **Fuel Refinery** to synthesize high-energy fuels for vehicles and machines, and construct automated highway networks.

---

## 🌾 Agriculture: Sweet Corn

Sweet Corn is a fast-growing, high-starch agricultural crop that serves both as a nutrient-dense food source and as high-efficiency feedstock for ethanol biofuel synthesis.

* **Corn Kernels**: Plant on tilled farmland to grow 8-stage Sweet Corn stalks.
* **Roasted Sweet Corn**: Cook on a campfire, smoker, or furnace for **7 Food points & 0.8 saturation**.
* **Refinery Feedstock**: Yields concentrated ethanol when processed in the **Fuel Refinery**.

---

## 🛢️ Petrochemical Resources & Distillation

### 1. Oil Sand & Crude Oil Sludge
* **Oil Sand** deposits generate naturally throughout **Deserts, Badlands, and Wooded Badlands**.
* Excavating Oil Sand with a shovel yields **Crude Oil Sludge** (or the Oil Sand block itself with Silk Touch).
* Crude Oil Sludge is the raw hydrocarbon foundation for gasoline, high-octane racing fuel, and mineral tar synthesis.

### 2. The Fuel Refinery
The **Fuel Refinery** is a powered distillation chamber (32,000 FE capacity, draws 20 FE/t) that synthesizes canisters of fuel and mineral byproducts from organic feedstocks and crude oil.

| Feedstock | Reagent | Primary Output | Byproduct | Process Time |
| :--- | :--- | :--- | :--- | :--- |
| **Crude Oil Sludge (1)** | Empty Gas Canister (1) | **Gasoline Canister** | **Mineral Tar (1)** | 5.0s (100t) |
| **Corn on the Cob (2)** | Empty Gas Canister (1) | **Biofuel Canister** | *(None)* | 4.0s (80t) |
| **Wheat / Sugar Cane (4)** | Empty Gas Canister (1) | **Biofuel Canister** | *(None)* | 5.0s (100t) |
| **Gasoline Canister (1)** | Corn on the Cob (2) | **High-Octane Racing Fuel** | *(None)* | 6.0s (120t) |

---

## ⛽ Vehicle Fuels & Energy Comparison

ShuDynamics fuels provide different burn durations, energy densities, and speed boosts when loaded into vehicles and hybrid generators:

| Fuel Type | Source | ATV Burn Duration | ATV Speed Multiplier | Special Passives |
| :--- | :--- | :--- | :--- | :--- |
| **Biofuel Canister** | Fermented Corn / Wheat | **10,000 ticks (~8.3 min)** | 1.0x (Standard) | Clean burning, renewable agricultural fuel |
| **Gasoline Canister** | Distilled Crude Oil Sludge | **16,000 ticks (~13.3 min)** | **1.15x (+15% Speed)** | High torque & rapid hill-climbing acceleration |
| **High-Octane Racing Fuel**| Gasoline + Sweet Corn | **24,000 ticks (~20.0 min)** | **1.30x (+30% Speed)** | Maximum performance, nitro-speed scaling |
| **Mineral Tar** | Refinery byproduct / Coke Oven | *Component* | — | Essential binder for paving Asphalt Roads & Slabs |
| **Enchanted Coal** | Coal + Enchanted Dust | 6,400 ticks | 1.0x | Solid fallback fuel for combustion engines |
| **Lava Bucket** | Molten Lava | 20,000 ticks | 1.0x | High-heat liquid fallback |

---

## 🛣️ Highway Infrastructure & Road Construction

### 1. Asphalt Roads & Slabs
Paved road surfaces constructed by combining aggregate rock (**Cobblestone, Deepslate, Granite, Andesite, Diorite**) with **Mineral Tar**.
* **Speed Multiplier:** Moving across Asphalt provides players with a **+25% on-foot speed boost** and unlocks the maximum top-speed multiplier for ATVs.
* **Harvesting:** Requires an Iron Pickaxe or better (drops 50% block / 50% tar, or 100% intact with Silk Touch).

### 2. Auto-Connecting Concrete Curbs & Ramps
* **Concrete Curbs:** Multi-directional sidewalk curbs with automatic corner connections (`Straight`, `Inner Corner`, and `Outer Corner`) and 20 directional collision shapes.
* **Road Transition Ramps:** Dual-mode ramps (`Ground` 0–8px and `Road` 8–16px) with right-click slab conversion for smooth vehicular road on-ramps.
* **Clay Molding:** Craft **Unfired Concrete Curbs** and **Unfired Road Transition Ramps** using clay balls and water, then smelt/blast into finished concrete!

### 3. Autonomous Road Paver Mk1
The **Autonomous Road Paver** (`enchantedwood:road_paver`) is an automated civil engineering machine that lays highways without manual labor:
* Place facing your desired road trajectory.
* Supply FE power (40,000 FE internal buffer, 50 FE/step, charges via battery in Slot 9) and combustible fuel (3,000 unit tank in Slot 10).
* Clears trees, foliage, and stone 3-blocks wide, lays asphalt foundation from Slots 0–8, and steps forward.
* Apply a redstone signal to pause paving at intersections.

### 4. Autonomous Road Paver Mk2 (Heavy Viaduct & Bridge Crawler)
Introduced in **ShuDynamics 2.0**, the **Road Paver Mk2** (`enchantedwood:road_paver_mk2`) is a heavy viaduct and bridge construction crawler:
* **Dual-Power Engine**: Requires simultaneous FE electricity (80 FE/step from 60,000 FE buffer, rechargeable via portable batteries in Slot 12) + Gasoline/Fuel (1 unit/step from 5,000 unit tank in Slot 13).
* **5-Wide Deck & Sub-Deck**: Lays a wide 5-block asphalt roadway deck from Slots 0–8, clears foliage/obstacles 4 blocks high, and automatically places structural sub-deck girder foundations beneath.
* **Automated Support Pillars (Up to 48 Blocks Deep)**: Every 5 steps, probes downward up to 48 blocks for cliffs, ravines, canyons, or water/oceans. Automatically casts dual vertical structural pillars down to solid ground using structural materials from Slots 9–11 (Deepslate Bricks, Stone Bricks, Cobblestone, or Concrete).
* **Water Displacement**: Cleanly displaces water blocks when crossing rivers, lakes, or oceans to create seamless solid causeways and viaduct bridges.
* **State & Distance Persistence**: Preserves `pavedSteps`, remaining fuel, energy, and inventory supplies across forward movements.
* **Redstone Automation**: Pauses advancing and paving when powered by redstone.

### 5. Highway Off-Ramp Transitions
* **Asphalt Transition Ramp** (`enchantedwood:asphalt_transition_ramp`): Smooth half-slab (0 to 8px ground / 8 to 16px road) driving slope for asphalt highways and multi-block off-ramps (maintains +25% player speed and vehicle velocity).
* **Concrete Curb Transition Ramp** (`enchantedwood:road_transition_ramp`): Smooth half-slab (0 to 8px ground / 8 to 16px road) matching concrete curb barrier ramp.
* **Asphalt Ramp** (`enchantedwood:asphalt_ramp`): Full 1-block high (0 to 16px) smooth driving slope for single-block highway drop-offs and overpasses.
* **Concrete Curb Ramp** (`enchantedwood:concrete_curb_ramp`): Full 1-block high (0 to 16px) smooth curb barrier ramp for single-block elevation transitions.

---

## 📜 Crafting Recipes

### Empty Gas Canister
<MinecraftRecipe id="empty_gas_canister" />

### Fuel Refinery
<MinecraftRecipe id="fuel_refinery" />

### Autonomous Road Paver
<MinecraftRecipe id="road_paver" />

### Asphalt Block (from Cobblestone)
<MinecraftRecipe id="asphalt_block_from_cobblestone" />

### Asphalt Block (from Deepslate)
<MinecraftRecipe id="asphalt_block_from_deepslate" />

### Asphalt Slab
<MinecraftRecipe id="asphalt_slab" />

### Unfired Concrete Curb (Clay Mold)
<MinecraftRecipe id="unfired_concrete_curb" />

### Finished Concrete Curb (Smelting)
<MinecraftRecipe id="smelting_unfired_concrete_curb" />

### Unfired Road Transition Ramp (Clay Mold)
<MinecraftRecipe id="unfired_road_transition_ramp" />

### Finished Road Transition Ramp (Smelting)
<MinecraftRecipe id="smelting_unfired_road_transition_ramp" />

