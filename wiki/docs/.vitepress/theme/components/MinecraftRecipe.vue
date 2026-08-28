<template>
  <div class="mc-recipe-card">
    <div class="mc-recipe-title" v-if="displayTitle">{{ displayTitle }}</div>
    <div class="mc-recipe-container">
      <!-- 3x3 Grid -->
      <div class="mc-grid">
        <div 
          v-for="(slot, index) in normalizedGrid" 
          :key="index" 
          class="mc-slot"
          :title="slot ? slot.name : ''"
        >
          <img v-if="slot && slot.icon" :src="slot.icon" :alt="slot.name" class="mc-item-icon" />
          <span v-if="slot && slot.count && slot.count > 1" class="mc-item-count">{{ slot.count }}</span>
        </div>
      </div>

      <!-- Arrow -->
      <div class="mc-arrow">
        <svg width="32" height="24" viewBox="0 0 32 24" fill="none">
          <path d="M4 10H20V4L30 12L20 20V14H4V10Z" fill="#8B5CF6"/>
        </svg>
      </div>

      <!-- Output Slot -->
      <div class="mc-slot mc-output-slot" :title="displayOutput ? displayOutput.name : ''">
        <img v-if="displayOutput && displayOutput.icon" :src="displayOutput.icon" :alt="displayOutput.name" class="mc-item-icon mc-output-icon" />
        <span v-if="displayOutput && displayOutput.count && displayOutput.count > 1" class="mc-item-count mc-output-count">{{ displayOutput.count }}</span>
      </div>
    </div>
    <div class="mc-recipe-notes" v-if="note">{{ note }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import recipesData from '../recipes.json';

const props = defineProps({
  id: { type: String, default: '' },
  recipeId: { type: String, default: '' },
  title: { type: String, default: '' },
  grid: { type: Array, default: () => [] },
  output: { type: Object, default: () => null },
  note: { type: String, default: '' }
});

const recipeRecord = computed(() => {
  const targetId = props.id || props.recipeId;
  if (targetId && recipesData.recipes && recipesData.recipes[targetId]) {
    return recipesData.recipes[targetId];
  }
  return null;
});

const displayTitle = computed(() => {
  if (props.title) return props.title;
  if (recipeRecord.value) return recipeRecord.value.title;
  return 'Crafting Recipe';
});

const displayOutput = computed(() => {
  if (props.output) return props.output;
  if (recipeRecord.value) return recipeRecord.value.output;
  return { name: '', icon: '', count: 1 };
});

const normalizedGrid = computed(() => {
  const arr = new Array(9).fill(null);
  const sourceGrid = (props.grid && props.grid.length > 0) 
    ? props.grid 
    : (recipeRecord.value ? recipeRecord.value.grid : []);

  for (let i = 0; i < 9; i++) {
    if (sourceGrid && sourceGrid[i]) {
      arr[i] = sourceGrid[i];
    }
  }
  return arr;
});
</script>

<style scoped>
.mc-recipe-card {
  background: rgba(18, 19, 28, 0.7);
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0;
  display: inline-block;
  backdrop-filter: blur(8px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}

.mc-recipe-title {
  font-weight: 600;
  font-size: 14px;
  color: #c4b5fd;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.mc-recipe-container {
  display: flex;
  align-items: center;
  gap: 18px;
}

.mc-grid {
  display: grid;
  grid-template-columns: repeat(3, 44px);
  grid-template-rows: repeat(3, 44px);
  gap: 3px;
  background: #1e1f2b;
  padding: 4px;
  border-radius: 6px;
  border: 2px solid #374151;
}

.mc-slot {
  width: 44px;
  height: 44px;
  background: #2a2d3d;
  border: 2px solid #171822;
  border-right-color: #3b3f54;
  border-bottom-color: #3b3f54;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: transform 0.1s ease, border-color 0.2s ease;
}

.mc-slot:hover {
  border-color: #8b5cf6;
}

.mc-output-slot {
  width: 58px;
  height: 58px;
  background: #2a2d3d;
  border: 2px solid #171822;
  border-right-color: #3b3f54;
  border-bottom-color: #3b3f54;
  border-radius: 6px;
  box-shadow: 0 0 12px rgba(139, 92, 246, 0.25);
}

.mc-item-icon {
  width: 32px;
  height: 32px;
  image-rendering: pixelated;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.5));
}

.mc-output-icon {
  width: 40px;
  height: 40px;
}

.mc-item-count {
  position: absolute;
  bottom: 2px;
  right: 4px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  text-shadow: 1px 1px 0 #000, -1px -1px 0 #000;
  font-family: monospace;
}

.mc-arrow {
  display: flex;
  align-items: center;
  animation: pulseArrow 2s infinite ease-in-out;
}

.mc-recipe-notes {
  margin-top: 10px;
  font-size: 12px;
  color: #9ca3af;
  font-style: italic;
}

@keyframes pulseArrow {
  0%, 100% { transform: translateX(0); opacity: 0.8; }
  50% { transform: translateX(3px); opacity: 1; filter: drop-shadow(0 0 8px #8b5cf6); }
}
</style>
