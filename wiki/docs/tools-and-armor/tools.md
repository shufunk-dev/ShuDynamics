# 🔨 Tools & 3x3 Mining Hammers

ShuDynamics provides complete tool sets (Pickaxes, Swords, Axes, Shovels, Hoes) for all custom materials, alongside specialized **3x3 Mining Hammers** for rapid excavation.

---

## ⛏️ 3x3 Area Mining Hammers

Hammers mine a **3x3 area** centered on the block you break. Sneaking allows you to mine a single 1x1 block when precise mining is needed!

| Hammer Tier | Mining Speed | Durability | Mining Level |
| :--- | :--- | :--- | :--- |
| **Wooden Hammer** | 2.0x | 118 | Wood / Stone level |
| **Stone Hammer** | 4.0x | 262 | Iron level |
| **Iron Hammer** | 6.0x | 500 | Diamond level |
| **Golden Hammer** | 12.0x | 64 | Fast / Low durability |
| **Diamond Hammer** | 8.0x | 3,122 | Obsidian level |
| **Netherite Hammer** | 9.0x | 4,062 | Obsidian level (Fireproof) |
| **Enchanted Cobblestone Hammer** | 5.5x | 650 | High stone durability |
| **Tin Hammer** | 5.0x | 420 | Iron level |
| **Bronze Hammer** | 6.5x | 900 | Diamond level |
| **Titanium Hammer** | 8.5x | 4,200 | Maximum quarrying power |
| **Enchanted Diamond Hammer** | 10.0x | 5,500 | Supercharged excavation |
| **Enchanted Netherite Hammer** | 12.0x | 8,000 | Ultimate 3x3 destruction |
| **Infernal Sledgehammer** | 10.0x | 5,000 | **3x3 Auto-Smelt Mining** (Ores $\rightarrow$ Ingots, Cobblestone $\rightarrow$ Stone) |

---

## 🛠️ Crafting a 3x3 Hammer

<MinecraftRecipe 
  title="Crafting a Bronze Hammer"
  :grid="[
    { name: 'Bronze Ingot', icon: '/textures/item/bronze_ingot.png' }, { name: 'Bronze Ingot', icon: '/textures/item/bronze_ingot.png' }, { name: 'Bronze Ingot', icon: '/textures/item/bronze_ingot.png' },
    { name: 'Bronze Ingot', icon: '/textures/item/bronze_ingot.png' }, { name: 'Stick', icon: '/textures/item/stick.png' }, { name: 'Bronze Ingot', icon: '/textures/item/bronze_ingot.png' },
    null, { name: 'Stick', icon: '/textures/item/stick.png' }, null
  ]"
  :output="{ name: 'Bronze Hammer', icon: '/textures/item/bronze_hammer.png', count: 1 }"
  note="Mines a full 3x3 area in front of the player. Sneak to mine single blocks."
/>
