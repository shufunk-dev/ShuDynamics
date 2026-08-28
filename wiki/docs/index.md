---
layout: home

hero:
  name: "ShuDynamics"
  text: "Tech, Magic & Power Progression"
  tagline: "The comprehensive official wiki and progression guide for ShuDynamics."
  image:
    src: /textures/item/infused_heartwood.png
    alt: Infused Heartwood
  actions:
    - theme: brand
      text: 🚀 Getting Started
      link: /getting-started/
    - theme: alt
      text: 🔥 CurseForge
      link: https://www.curseforge.com/minecraft/mc-mods/shudynamics
    - theme: alt
      text: ⚙️ Explore Machines
      link: /machines/
    - theme: alt
      text: ⭐ GitHub
      link: https://github.com/shufunk-dev/ShuDynamics

features:
  - icon: ⚡
    title: 3-Tier Energy & Machinery
    details: Harness power with Copper, Aluminum, and Steel Generators, high-capacity Batteries, and low-loss Cable grids.
  - icon: 🛡️
    title: Custom Armor & 3x3 Hammers
    details: Forge specialized armor sets and high-efficiency 3x3 mining hammers across all tiers, from Tin to Enchanted Netherite.
  - icon: 📦
    title: Crystal Storage Network
    details: Expand storage with Enchanted Chests and upgrade to 1k-64k Crystal Drives with remote wireless item access.
  - icon: 🛰️
    title: Wireless & Dimensional Upgrades
    details: Install Chunk Loaders and Interdimensional Cards into your Controller for infinite Overworld and Nether/End access.
---

<style>
.home-quickstart {
  margin-top: 40px;
  background: linear-gradient(145deg, rgba(26, 28, 43, 0.8), rgba(15, 16, 26, 0.95));
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.quickstart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.quick-card {
  background: rgba(30, 34, 53, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 16px;
  text-decoration: none !important;
  color: inherit !important;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-card:hover {
  background: rgba(42, 47, 72, 0.9);
  border-color: #8b5cf6;
  transform: translateY(-3px);
}

.quick-title {
  font-weight: 700;
  font-size: 16px;
  color: #c4b5fd;
}

.quick-desc {
  font-size: 13px;
  color: #9ca3af;
}
</style>

<div class="home-quickstart">
  <h2 style="margin:0 0 8px 0; color:#f3f4f6;">📖 Quick Navigation</h2>
  <p style="color:#9ca3af; margin:0 0 16px 0;">Jump straight into any section of the mod:</p>
  
  <div class="quickstart-grid">
    <a href="/getting-started/" class="quick-card">
      <span class="quick-title">🌱 Getting Started</span>
      <span class="quick-desc">Learn about Infused Heartwood, Enchanted Coal, and your first recipes.</span>
    </a>
    <a href="/materials/" class="quick-card">
      <span class="quick-title">⛏️ Materials & Ores</span>
      <span class="quick-desc">Tin, Bronze, Bauxite/Aluminum, Steel, Titanium, and Enchanted Ingot tiers.</span>
    </a>
    <a href="/machines/" class="quick-card">
      <span class="quick-title">⚡ Tech & Machines</span>
      <span class="quick-desc">Generators, Refiners, Oxygen Generators, Batteries, and Cables.</span>
    </a>
    <a href="/tools-and-armor/" class="quick-card">
      <span class="quick-title">🛡️ Armor & Weapons</span>
      <span class="quick-desc">Tier armor sets, 3x3 hammers, Livingwood swords, and Barkskin tools.</span>
    </a>
    <a href="/storage/" class="quick-card">
      <span class="quick-title">💾 Storage Network</span>
      <span class="quick-desc">Enchanted Chests, Storage Crystals (1k-64k), and Wireless Access.</span>
    </a>
    <a href="/backlog/" class="quick-card">
      <span class="quick-title">🚀 Roadmap & Future Tech</span>
      <span class="quick-desc">New Dimensions, Basalt Metallurgy, Supercomputers, and upcoming tech.</span>
    </a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/shudynamics" target="_blank" rel="noopener noreferrer" class="quick-card" style="border-color: rgba(241, 100, 54, 0.4); background: rgba(45, 25, 15, 0.6);">
      <span class="quick-title" style="color: #fb923c;">🔥 CurseForge Page</span>
      <span class="quick-desc">Download stable releases, install via CurseForge App, and track mod updates.</span>
    </a>
    <a href="https://github.com/shufunk-dev/ShuDynamics" target="_blank" rel="noopener noreferrer" class="quick-card" style="border-color: rgba(139, 92, 246, 0.4); background: rgba(30, 27, 50, 0.6);">
      <span class="quick-title" style="color: #c4b5fd;">⭐ GitHub Repository</span>
      <span class="quick-desc">Explore open-source code, submit bug reports, track development, and contribute.</span>
    </a>
  </div>
</div>
