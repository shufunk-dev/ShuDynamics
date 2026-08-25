# Petrochemicals, Refining & Road Infrastructure

::: warning 🧪 Experimental Feature (v1.3.0 Snapshot)
This feature is part of the **v1.3.0 development snapshot** and is currently in active gameplay testing. Distillation recipes, crop yields, and road paver energy costs are subject to balance adjustments prior to the official v1.3.0 release.
:::

ShuDynamics introduces a complete petrochemical and biofuels processing network. Refine crude deposits from arid biomes or ferment agricultural crops to power vehicles, machines, and construct automated highway networks.

---

## 🌾 Agriculture: Sweet Corn

Sweet Corn is a fast-growing, high-starch crop that serves both as a nutrient-dense food source and as high-efficiency feedstock for ethanol biofuel synthesis.

- **Corn Kernels**: Plant on tilled farmland to grow 8-stage Sweet Corn stalks.
- **Roasted Sweet Corn**: Cook on a campfire, smoker, or furnace for **7 Food points & 0.8 saturation**.
- **Refinery Feedstock**: Yields concentrated ethanol when processed in the **Fuel Refinery**.

---

## 🛢️ Petrochemical Resources & Distillation

### 1. Oil Sand & Crude Oil Sludge
- **Oil Sand** deposits generate naturally throughout **Deserts, Badlands, and Wooded Badlands**.
- Mining Oil Sand yields **Crude Oil Sludge**, the raw hydrocarbon basis for gasoline and asphalt synthesis.

### 2. The Fuel Refinery
The **Fuel Refinery** is a powered industrial distillation chamber (32,000 FE capacity, draws 20 FE/t) that synthesizes canisters of fuel and mineral byproducts.

| Feedstock | Reagent | Primary Output | Byproduct | Process Time |
| :--- | :--- | :--- | :--- | :--- |
| **Crude Oil Sludge (1)** | Empty Gas Canister (1) | **Gasoline Canister** | **Mineral Tar (1)** | 5.0s (100t) |
| **Corn on the Cob (2)** | Empty Gas Canister (1) | **Biofuel Canister** | *(None)* | 4.0s (80t) |
| **Wheat / Sugar Cane (4)** | Empty Gas Canister (1) | **Biofuel Canister** | *(None)* | 5.0s (100t) |
| **Gasoline Canister (1)** | Corn on the Cob (2) | **High-Octane Racing Fuel** | *(None)* | 6.0s (120t) |

---

## 🛣️ Asphalt & Autonomous Road Paver

### 1. Asphalt Recycling
Synthesize smooth, durable **Asphalt Blocks & Slabs** by recycling excess aggregate rock (**Cobblestone, Deepslate, Granite, Andesite, Diorite**) combined with **Mineral Tar** byproduct.

- Walking or driving across Asphalt provides a **+25% movement speed multiplier**.

### 2. Autonomous Road Paver
The **Autonomous Road Paver** is an automated construction machine that builds highways without manual labor:
- Place facing your desired road trajectory.
- Supply FE power (or battery in slot) and load its 3x3 storage grid with **Asphalt Blocks**.
- The Paver autonomously clears trees, foliage, and stone obstructions 3 blocks wide, paves asphalt beneath, and steps forward every 1.5 seconds!
- **Pause/Control**: Apply a redstone signal to pause paving.

---

## 📜 Crafting Recipes

### Fuel Refinery
<RecipeDisplay recipeId="fuel_refinery" />

### Autonomous Road Paver
<RecipeDisplay recipeId="road_paver" />

### Asphalt Block (from Cobblestone)
<RecipeDisplay recipeId="asphalt_block_from_cobblestone" />

### Asphalt Block (from Deepslate)
<RecipeDisplay recipeId="asphalt_block_from_deepslate" />

### Asphalt Slab
<RecipeDisplay recipeId="asphalt_slab" />
