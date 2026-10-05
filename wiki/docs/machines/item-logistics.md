# Universal Item Logistics & Industrial Wrench

ShuDynamics features a built-in, lightweight, modular item transport and automation system designed for high-performance factory routing on Minecraft 1.21.2+.

---

## 📦 Core Logistics Components

| Component | Identifier | Description |
| :--- | :--- | :--- |
| **Item Transport Pipe** | `enchantedwood:item_pipe` | 6-way modular item conduit with automatic routing and internal buffer. |
| **Item Extractor** | `enchantedwood:item_extractor` | Active directional puller that extracts items from inventories and machine outputs. |
| **Item Inserter** | `enchantedwood:item_inserter` | Active directional injector that pushes items into destination inventories and machine inputs. |
| **Industrial Wrench** | `enchantedwood:wrench` | Titanium tool used to toggle side connections, cap pipes, rotate machines, and dismantle blocks. |

---

## ⚡ Internal Buffers & Backpressure System

::: tip IMPORTANT: 4-Slot Buffer & Safe Queuing
Every **Item Pipe**, **Item Extractor**, and **Item Inserter** contains a built-in **4-slot internal storage buffer**.
:::

### How Queuing Works in Action:
1. **Multi-Item Processing**:
   - Suppose your **Crusher** is processing Copper Ore while your **Dust Smelter** is currently busy smelting Iron Dust.
   - When Copper Dust is extracted, the pipe network and Inserter safely hold the Copper Dust in their internal 4-slot buffers while waiting for the Dust Smelter to finish.
2. **Automatic Immediate Handoff**:
   - The exact moment the Dust Smelter finishes its Iron Dust and clears its input slot, the Inserter instantly transfers the queued Copper Dust directly into the machine.
3. **Zero Item Loss & Overflow Prevention**:
   - If the downstream pipe line and inserter buffers become completely full, the **Item Extractor automatically halts extraction** from the source machine/chest until downstream buffer space opens up.
   - Items will **never spill on the ground, overflow, or despawn**.

---

## 🔧 Industrial Wrench & Pipe Disconnections

When building compact automation setups where multiple pipes run parallel or cross near each other, you can use the **Industrial Wrench** (`enchantedwood:wrench`) to isolate lines.

### Wrench Controls:
- **Right-Click on any Pipe / Extractor / Inserter Face**:
  - Toggles connection on/off for that specific face.
  - **Bolted Warning Cap**: Disconnecting a face renders a distinct **metallic bolted plug with a red/orange hazard seal**, making it immediately visible that the connection is closed.
  - Sub-arm click targeting allows you to click specific arms from any camera angle.
- **Right-Click on Machine / Nozzle**:
  - Rotates the facing direction of machines and nozzles clockwise without breaking the block.
- **Shift + Right-Click**:
  - Instantly dismantles and retrieves any pipe, machine, or logistics block without losing internal contents.

---

## 🛠️ Crafting Recipes

### 1. Item Transport Pipe
<MinecraftRecipe id="item_pipe" />

### 2. Item Extractor
<MinecraftRecipe id="item_extractor" />

### 3. Item Inserter
<MinecraftRecipe id="item_inserter" />

### 4. Industrial Wrench
<MinecraftRecipe id="wrench" />

