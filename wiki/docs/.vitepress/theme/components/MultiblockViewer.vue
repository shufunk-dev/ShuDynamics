<template>
  <div class="multiblock-card">
    <div class="mb-header">
      <h3 class="mb-title">🖥️ {{ title }}</h3>
      <span class="mb-dim-badge">{{ dimensions }}</span>
    </div>
    <p class="mb-desc">{{ description }}</p>

    <!-- Layer Switcher Tabs -->
    <div class="layer-tabs">
      <button 
        v-for="layer in layers" 
        :key="layer.id"
        class="layer-tab-btn"
        :class="{ active: currentLayerId === layer.id }"
        @click="currentLayerId = layer.id"
      >
        {{ layer.name }}
      </button>
    </div>

    <!-- Active Layer Content -->
    <div class="layer-content-grid" v-if="activeLayer">
      <div class="layer-diagram">
        <img 
          :src="layerImgSrc" 
          :alt="activeLayer.name" 
          class="layer-img" 
          @error="handleLayerImgError"
        />
        <span class="layer-view-tag">{{ activeLayer.name }} View</span>
      </div>

      <div class="layer-materials">
        <h4 class="materials-heading">🧱 Required Layer Blocks</h4>
        <ul class="block-list">
          <li v-for="b in activeLayer.blocks" :key="b.name" class="block-item">
            <img v-if="b.icon" :src="b.icon" :alt="b.name" class="block-icon" />
            <span class="block-name">{{ b.name }}</span>
            <span class="block-count">x{{ b.count }}</span>
          </li>
        </ul>
        <div class="layer-tips" v-if="activeLayer.tip">
          💡 <strong>Tip:</strong> {{ activeLayer.tip }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  title: { type: String, default: 'Supercomputer Multiblock Blueprint' },
  dimensions: { type: String, default: '3x3x3 Structure' },
  description: { type: String, default: 'Layer-by-layer assembly guide for constructing and activating the supercomputer.' },
  layers: { type: Array, default: () => [] }
});

const currentLayerId = ref(props.layers && props.layers[0] ? props.layers[0].id : 'layer1');

const activeLayer = computed(() => {
  return props.layers.find(l => l.id === currentLayerId.value) || props.layers[0];
});

const layerImg = ref('');
const layerImgSrc = computed(() => {
  if (activeLayer.value) {
    return layerImg.value || activeLayer.value.image;
  }
  return '';
});

const handleLayerImgError = () => {
  if (activeLayer.value && activeLayer.value.image.endsWith('.png')) {
    layerImg.value = activeLayer.value.image.replace('.png', '.svg');
  }
};
</script>

<style scoped>
.multiblock-card {
  background: linear-gradient(145deg, rgba(24, 20, 38, 0.9), rgba(12, 11, 20, 0.95));
  border: 1px solid rgba(168, 85, 247, 0.4);
  border-radius: 16px;
  padding: 24px;
  margin: 28px 0;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
}

.mb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.mb-title {
  margin: 0;
  font-size: 22px;
  color: #f3f4f6;
}

.mb-dim-badge {
  background: rgba(168, 85, 247, 0.2);
  color: #c084fc;
  border: 1px solid #a855f7;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}

.mb-desc {
  margin: 0 0 18px 0;
  font-size: 14px;
  color: #9ca3af;
}

.layer-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.layer-tab-btn {
  background: rgba(30, 26, 48, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #9ca3af;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.layer-tab-btn.active {
  background: rgba(168, 85, 247, 0.25);
  border-color: #a855f7;
  color: #e9d5ff;
}

.layer-content-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 20px;
}

@media (max-width: 768px) {
  .layer-content-grid {
    grid-template-columns: 1fr;
  }
}

.layer-diagram {
  position: relative;
  background: #09090f;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

.layer-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.layer-view-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(168, 85, 247, 0.5);
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  color: #e9d5ff;
  font-weight: 600;
}

.layer-materials {
  background: rgba(19, 17, 30, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.materials-heading {
  margin: 0 0 12px 0;
  font-size: 13px;
  text-transform: uppercase;
  color: #c084fc;
  letter-spacing: 0.5px;
}

.block-list {
  list-style: none;
  padding: 0;
  margin: 0 0 12px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.block-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(29, 25, 45, 0.6);
  padding: 8px 12px;
  border-radius: 8px;
  border-left: 3px solid #a855f7;
}

.block-icon {
  width: 24px;
  height: 24px;
  image-rendering: pixelated;
  margin-right: 10px;
}

.block-name {
  font-size: 13px;
  color: #e5e7eb;
  flex-grow: 1;
}

.block-count {
  font-size: 13px;
  font-weight: 700;
  color: #38bdf8;
  font-family: monospace;
}

.layer-tips {
  margin-top: auto;
  font-size: 12px;
  color: #d1d5db;
  background: rgba(168, 85, 247, 0.1);
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid rgba(168, 85, 247, 0.2);
}
</style>
