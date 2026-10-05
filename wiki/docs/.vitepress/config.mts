import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'ShuDynamics Wiki',
  description: 'Official Documentation and Progression Guide for the ShuDynamics Minecraft Mod',
  cleanUrls: true,
  base: '/',
  appearance: 'force-dark',
  
  head: [
    ['link', { rel: 'icon', type: 'image/png', href: '/textures/item/infused_heartwood.png' }],
    ['meta', { name: 'theme-color', content: '#8B5CF6' }]
  ],

  themeConfig: {
    logo: '/textures/item/infused_heartwood.png',
    siteTitle: 'ShuDynamics Wiki',
    
    nav: [
      { text: 'Getting Started', link: '/getting-started/' },
      { text: '🌌 The Convergence', link: '/convergence/' },
      { text: '🎵 Soundtrack', link: '/convergence/music-discs' },
      { text: 'Materials', link: '/materials/' },
      { text: 'Tools & Armor', link: '/tools-and-armor/' },
      { text: 'Machines & Power', link: '/machines/' },
      { text: 'Vehicles & ATV', link: '/vehicles-and-atv/' },
      { text: 'Fuels & Roads', link: '/petrochemicals-and-fuels/' },
      { text: 'Storage Network', link: '/storage/' },
      { text: 'Roadmap & Future', link: '/backlog/' },
      { text: '🔥 CurseForge', link: 'https://www.curseforge.com/minecraft/mc-mods/shudynamics' }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/shufunk-dev/ShuDynamics' }
    ],

    sidebar: {
      '/': [
        {
          text: 'Overview & Basics',
          items: [
            { text: 'Introduction & Setup', link: '/getting-started/' },
            { text: 'Infused Heartwood & Energy Basics', link: '/getting-started/energy-basics' }
          ]
        },
        {
          text: '🌌 The Convergence (v2.2)',
          items: [
            { text: 'Dimension & Hazards Guide', link: '/convergence/' },
            { text: '🌲 The 7 Lost Biomes', link: '/convergence/lost-biomes' },
            { text: '3-Tier Boss Gauntlet & Relics', link: '/convergence/boss-and-relics' },
            { text: 'Botanicals & Multi-Course Bento', link: '/convergence/botanicals-and-cuisine' },
            { text: '🎵 Harmonic Soundtrack & Discs', link: '/convergence/music-discs' }
          ]
        },
        {
          text: 'Materials & Progression',
          items: [
            { text: 'Ores, Metals & Ingots', link: '/materials/' },
            { text: 'Convergence Minerals & Superalloys', link: '/materials/convergence-minerals' },
            { text: 'Ore Dusts & Processing', link: '/materials/dusts' },
            { text: 'Gears & Mechanical Parts', link: '/materials/gears' },
            { text: 'Health Lockets & Crystals', link: '/materials/crystals' },
            { text: 'Agronomy & Volcanic Soil', link: '/materials/agronomy' },
            { text: 'Dimensional Keystones', link: '/materials/keystones' }
          ]
        },
        {
          text: 'Equipment & Combat',
          items: [
            { text: 'Armor Sets Overview', link: '/tools-and-armor/' },
            { text: 'Modular Power Exosuit Suite', link: '/tools-and-armor/modular-exosuit' },
            { text: 'Diving Gear & Scuba Life Support', link: '/tools-and-armor/diving' },
            { text: 'Tools & 3x3 Mining Hammers', link: '/tools-and-armor/tools' },
            { text: 'Enchanted Wood Tools & Artifacts', link: '/tools-and-armor/special-weapons' }
          ]
        },
        {
          text: 'Tech & Machinery',
          items: [
            { text: 'Machines Overview', link: '/machines/' },
            { text: 'Cleanroom Complex & Medical Lab', link: '/machines/cleanroom-and-medical' },
            { text: 'Induction Smelter & Circuit Chips', link: '/machines/induction-smeltery' },
            { text: 'Power Generation (Generators)', link: '/machines/generators' },
            { text: 'Power Storage (Batteries & Cables)', link: '/machines/batteries-and-cables' },
            { text: 'Crushers & Dust Smelters', link: '/machines/crushers-and-smelters' },
            { text: 'Furnaces & Coke Ovens', link: '/machines/furnaces-and-ovens' },
            { text: 'Refiners & Blast Furnaces', link: '/machines/refiners' },
            { text: 'Gas & Oxygen Systems', link: '/machines/oxygen-and-gas' },
            { text: 'Item Logistics & Industrial Wrench', link: '/machines/item-logistics' },
            { text: 'Digital Laser Quarry & Excavation', link: '/machines/laser-quarry' },
            { text: 'Item Salvager & Recycler', link: '/machines/item-salvager' },
            { text: 'Harmonic Record Press', link: '/machines/harmonic-record-press' },
            { text: 'Cryogenics, Freezers & Kitchen Appliances', link: '/machines/culinary-and-cryogenics' }
          ]
        },
        {
          text: 'Vehicles & Infrastructure',
          items: [
            { text: 'Modular All-Terrain Vehicles (ATV)', link: '/vehicles-and-atv/' },
            { text: 'Petrochemicals, Fuels & Roadways', link: '/petrochemicals-and-fuels/' }
          ]
        },
        {
          text: 'Digital Storage & Logistics',
          items: [
            { text: 'Enchanted Chests & Storage Network', link: '/storage/' }
          ]
        },
        {
          text: 'Milestones & Roadmap',
          items: [
            { text: 'Roadmap & Milestones', link: '/backlog/' }
          ]
        }
      ]
    },

    search: {
      provider: 'local'
    },

    footer: {
      message: 'Built with VitePress',
      copyright: 'Copyright © 2026 Shufelt Designs. All rights reserved.'
    }
  }
});
