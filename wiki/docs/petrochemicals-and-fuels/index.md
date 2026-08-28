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

### 3. Autonomous Road Paver
The **Autonomous Road Paver** is an automated civil engineering machine that lays highways without manual labor:
* Place facing your desired road trajectory.
* Supply FE power (or insert a portable Battery Pack) and load its inventory with **Asphalt Blocks** (and optionally **Concrete Curbs**).
* The Paver clears trees, foliage, and stone 3-blocks wide, paves asphalt beneath, automatically extrudes concrete curbs on the shoulders, and steps forward!
* Apply a redstone signal to pause paving at intersections.

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

### Unfired Road Transition Ramp (Clay Mold)
<MinecraftRecipe id="unfired_road_transition_ramp" />
