import DefaultTheme from 'vitepress/theme';
import './custom.css';

import MinecraftRecipe from './components/MinecraftRecipe.vue';
import ArmorShowcase from './components/ArmorShowcase.vue';
import MachineShowcase from './components/MachineShowcase.vue';
import VideoPlayer from './components/VideoPlayer.vue';
import MultiblockViewer from './components/MultiblockViewer.vue';

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('MinecraftRecipe', MinecraftRecipe);
    app.component('ArmorShowcase', ArmorShowcase);
    app.component('MachineShowcase', MachineShowcase);
    app.component('VideoPlayer', VideoPlayer);
    app.component('MultiblockViewer', MultiblockViewer);
  }
};
