# 🏭 Industrial Metallurgy & Precision Microchip Manufacturing

Introduced in **ShuDynamics 2.0**, the **Dual-Powered Induction Smelter**, **Automated Casting Access Port**, and **Precision Circuit Fabricator** form a high-yield industrial metallurgy and semiconductor manufacturing network.

---

## 💾 Precision Circuit Fabricator

The **Precision Circuit Fabricator** (`enchantedwood:circuit_fabricator`) is an automated semiconductor etching machine that operates cleanly both inside and outside cleanroom complexes:

* **Power & Performance**: 50,000 FE internal buffer with a 35 FE/t draw rate. Supports all Gear Upgrades (Copper through Diamond) and the Blaze Overclock Core (up to 4.0× acceleration).
* **Authentic GUI & Laser Animation**: Features 1 Silicon Substrate slot, 3 Component slots, 1 Output bay, and 1 Gear Overclock socket.

### Microchip Formulations

| Finished Microchip | Substrate | Required Components | Primary Application |
| :--- | :--- | :--- | :--- |
| **Basic Computer Chip** | Silicon Wafer | Copper Ingot + Gold Nugget + Redstone Dust | Early tech, Exosuit Basic Logic |
| **Advanced Computer Chip** | Basic Chip | Diamond + Glowstone Dust + Lapis Lazuli | Decontamination doors, Exosuit Advanced Logic |
| **Quantum Computer Chip** | Advanced Chip | Netherite Dust + Blaze Powder + Enchanted Dust | Exosuit Quantum Logic, Supercomputer Mainframes |
| **Metallurgy Controller Chip** | Silicon Wafer | Gold Ingot + Redstone Dust + Zirconia Nodule | Induction Smelter automated alloying module |

---

## 🔥 Dual-Powered Induction Smelter

The **Dual-Powered Induction Smelter** (`enchantedwood:induction_smelter`) is a heavy metal liquefier engineered for 100% zero-loss recycling and precise stoichiometric liquid alloying.

### 1. Dual-Power Architecture
* **Thermal Reservoir**: 10,000 mB internal Lava reservoir (consumes 5 mB per smelting cycle). Can be piped in directly via Lava Pipes or filled with buckets.
* **Electrical Buffer**: 50,000 FE electrical capacity drawing 45 FE/t during active smelting.

### 2. Multi-Fluid Melting Chamber (32,400 mB)
* Features a dynamic multi-fluid capacity capable of holding up to **15 different molten industrial metals simultaneously** without clogging or cross-contamination.

### 3. 100% Metal Reclaim Ratio
Melting equipment, armor, and industrial blocks returns **100% of their base metal volume** ($1\text{ Nugget} = 10\text{ mB}$, $1\text{ Ingot} = 90\text{ mB}$, $1\text{ Block} = 810\text{ mB}$):

* **Tools & Weapons**: Swords (180 mB / 2 ingots), Pickaxes & Axes (270 mB / 3 ingots), Shovels (90 mB / 1 ingot), Hoes (180 mB / 2 ingots).
* **Armor Sets**: Helmets (450 mB / 5 ingots), Chestplates (720 mB / 8 ingots), Leggings (630 mB / 7 ingots), Boots (360 mB / 4 ingots), Horse Armor (630 mB / 7 ingots).
* **Heavy Utilities**: Damaged or intact **Anvils** (2,790 mB / full 31 ingots!), Minecarts (450 mB), Cauldrons (630 mB), Hoppers (450 mB).

---

## ⚡ Metallurgy Controller Socket & Alloy Controls

Inserting a `metallurgy_controller_chip` into the smelter's dedicated chip socket unlocks the **Interactive Mixing Bay**:

* **Dual Holding Tanks**: Segregates raw melts into **Holding Tank 1** (10,800 mB) and **Holding Tank 2** (10,800 mB). Both melting slots can process metals simultaneously without mixing prematurely.
* **`[⚡ MIX: ON / ○ MIX: OFF]` Toggle**:
  * **When OFF**: Smelted fluids remain completely pure and separated in their respective holding tanks.
  * **When ON**: Executes instantaneous stoichiometric reactions, pumping the resulting alloy directly into **Internal Tank 3**:
    * $30\text{ mB Molten Copper} + 10\text{ mB Molten Tin} \longrightarrow \mathbf{40\text{ mB Molten Bronze}}$
    * $10\text{ mB Molten Cobalt} + 10\text{ mB Molten Ardite} \longrightarrow \mathbf{20\text{ mB Molten Manyullyn}}$
* **Tank Controls**:
  * **`[🗑 Dump]` Button**: Destroys unwanted residue in Holding Tanks 1 & 2.
  * **`[⏏ Eject]` Button**: Actively flushes holding tanks outward into connected pipes.
* **Selective Auto-Drain**: Only Tank 3 passively empties into adjacent Casting Ports or storage tanks.

---

## 🧱 Automated Casting Access Port

The **Casting Access Port** (`enchantedwood:casting_port`) attaches directly to the Induction Smelter, Titanium Multiblock Tanks, or Titanium Lava Pipes:

* **2,000 mB Intake Buffer**: Receives molten metal from connected machines.
* **Interactive Mold Toggle**: Click the on-screen mold button to cycle:
  * **INGOT** ($90\text{ mB}$)
  * **BLOCK** ($810\text{ mB}$)
  * **NUGGET** ($10\text{ mB}$)
* **Automated Solidification**: Runs a 40-tick cooling cycle to transform molten metal into solid physical items.
* **Auto-Extraction**: Automatically pushes newly formed ingots, blocks, and nuggets into adjacent chests, barrels, hoppers, or item transport pipes.
