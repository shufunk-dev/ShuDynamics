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
      { text: 'Materials', link: '/materials/' },
      { text: 'Tools & Armor', link: '/tools-and-armor/' },
      { text: 'Machines & Power', link: '/machines/' },
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
          text: 'Materials & Progression',
          items: [
            { text: 'Ores, Metals & Ingots', link: '/materials/' },
            { text: 'Ore Dusts & Processing', link: '/materials/dusts' },
            { text: 'Gears & Mechanical Parts', link: '/materials/gears' },
            { text: 'Health Lockets & Crystals', link: '/materials/crystals' }
          ]
        },
        {
          text: 'Equipment & Combat',
          items: [
            { text: 'Armor Sets Overview', link: '/tools-and-armor/' },
            { text: 'Tools & 3x3 Mining Hammers', link: '/tools-and-armor/tools' },
            { text: 'Livingwood & Barkskin Specials', link: '/tools-and-armor/special-weapons' }
          ]
        },
        {
          text: 'Tech & Machinery',
          items: [
            { text: 'Machines Overview', link: '/machines/' },
            { text: 'Power Generation (Generators)', link: '/machines/generators' },
            { text: 'Power Storage (Batteries & Cables)', link: '/machines/batteries-and-cables' },
            { text: 'Crushers & Dust Smelters', link: '/machines/crushers-and-smelters' },
            { text: 'Furnaces & Coke Ovens', link: '/machines/furnaces-and-ovens' },
            { text: 'Refiners & Blast Furnaces', link: '/machines/refiners' },
            { text: 'Gas & Oxygen Systems', link: '/machines/oxygen-and-gas' }
          ]
        },
        {
          text: 'Digital Storage & Logistics',
          items: [
            { text: 'Enchanted Chests & Storage Network', link: '/storage/' }
          ]
        },
        {
          text: 'Bonus & Upcoming Features',
          items: [
            { text: 'Roadmap & Future Tech', link: '/backlog/' }
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
