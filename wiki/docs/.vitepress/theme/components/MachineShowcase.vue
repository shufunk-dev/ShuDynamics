<template>
  <div class="machine-showcase-card">
    <!-- Header -->
    <div class="machine-header">
      <div class="machine-identity">
        <div class="machine-icon-box">
          <img :src="iconSrc" :alt="name" class="pixel-icon" @error="handleIconError" />
        </div>
        <div>
          <div class="title-row">
            <h2 class="machine-title">{{ name }}</h2>
            <span class="tier-pill" :class="tierClass">{{ tier }}</span>
          </div>
          <p class="machine-category">{{ category }}</p>
        </div>
      </div>
    </div>

    <!-- Description -->
    <p class="machine-summary">{{ description }}</p>

    <!-- Main Content: Specs & Visual Tabs -->
    <div class="machine-body-grid">
      <!-- Left: Specs Box -->
      <div class="specs-panel">
        <h4 class="panel-heading">⚡ Operational Specifications</h4>
        <div class="spec-row" v-for="(val, key) in specs" :key="key">
          <span class="spec-key">{{ key }}</span>
          <span class="spec-val">{{ val }}</span>
        </div>
      </div>

      <!-- Right: Media Tabs (Placed View vs GUI View) -->
      <div class="media-panel">
        <div class="tab-controls">
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'placed' }"
            @click="activeTab = 'placed'"
          >
            📸 In-Game Block View
          </button>
          <button 
            v-if="guiImage"
            class="tab-btn" 
            :class="{ active: activeTab === 'gui' }"
            @click="activeTab = 'gui'"
          >
            🖥️ Interface (GUI)
          </button>
        </div>

        <div class="tab-content">
          <img 
            v-if="activeTab === 'placed'" 
            :src="placedImgSrc" 
            :alt="`${name} In-Game View`"
            class="tab-image"
            @error="handlePlacedError"
          />
          <img 
            v-if="activeTab === 'gui'" 
            :src="guiImgSrc" 
            :alt="`${name} GUI Screen`"
            class="tab-image"
            @error="handleGuiError"
          />
        </div>
      </div>
    </div>

    <!-- Video Demonstration Section (Optional per machine) -->
    <div class="video-section" v-if="showVideo || videoUrl">
      <h4 class="panel-heading">🎥 Video Explanation & Walkthrough</h4>
      <VideoPlayer :url="videoUrl" :title="`How to operate the ${name}`" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import VideoPlayer from './VideoPlayer.vue';

const props = defineProps({
  name: { type: String, required: true },
  icon: { type: String, required: true },
  tier: { type: String, default: 'Tier 1' },
  tierClass: { type: String, default: 'tier-early' },
  category: { type: String, default: 'Machinery & Power' },
  description: { type: String, default: '' },
  specs: { type: Object, default: () => ({}) },
  placedImage: { type: String, required: true },
  guiImage: { type: String, default: '' },
  videoUrl: { type: String, default: '' },
  showVideo: { type: Boolean, default: false }
});

const activeTab = ref('placed');

const currentIcon = ref(props.icon);
const iconSrc = computed(() => currentIcon.value);

const placedImg = ref(props.placedImage);
const guiImg = ref(props.guiImage);

const placedImgSrc = computed(() => placedImg.value);
const guiImgSrc = computed(() => guiImg.value);

const handleIconError = () => {
  if (currentIcon.value.endsWith('.png')) {
    currentIcon.value = currentIcon.value.replace('.png', '.svg');
  }
};

const handlePlacedError = () => {
  if (placedImg.value.endsWith('.png')) {
    placedImg.value = placedImg.value.replace('.png', '.svg');
  }
};

const handleGuiError = () => {
  if (guiImg.value.endsWith('.png')) {
    guiImg.value = guiImg.value.replace('.png', '.svg');
  }
};
</script>

<style scoped>
.machine-showcase-card {
  background: linear-gradient(145deg, rgba(22, 24, 38, 0.9), rgba(13, 14, 23, 0.95));
  border: 1px solid rgba(6, 182, 212, 0.35);
  border-radius: 16px;
  padding: 24px;
  margin: 28px 0;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(12px);
}

.machine-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 16px;
}

.machine-identity {
  display: flex;
  align-items: center;
  gap: 16px;
}

.machine-icon-box {
  width: 52px;
  height: 52px;
  background: #171926;
  border: 2px solid #06b6d4;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 16px rgba(6, 182, 212, 0.2);
}

.pixel-icon {
  width: 36px;
  height: 36px;
  image-rendering: pixelated;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.machine-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #f3f4f6;
}

.tier-pill {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  padding: 3px 10px;
  border-radius: 20px;
}

.tier-early { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid #3b82f6; }
.tier-mid { background: rgba(168, 85, 247, 0.2); color: #c084fc; border: 1px solid #a855f7; }
.tier-high { background: rgba(234, 179, 8, 0.2); color: #facc15; border: 1px solid #eab308; }
.tier-end { background: rgba(236, 72, 153, 0.2); color: #f472b6; border: 1px solid #ec4899; }

.machine-category {
  margin: 2px 0 0 0;
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
}

.machine-summary {
  margin: 16px 0;
  font-size: 14px;
  color: #d1d5db;
  line-height: 1.6;
}

.machine-body-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .machine-body-grid {
    grid-template-columns: 1fr;
  }
}

.specs-panel {
  background: rgba(17, 19, 31, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-heading {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 700;
  color: #38bdf8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.spec-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  background: rgba(26, 29, 46, 0.6);
  border-radius: 8px;
  border-left: 3px solid #8b5cf6;
}

.spec-key {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
}

.spec-val {
  font-size: 13px;
  font-weight: 700;
  color: #f3f4f6;
  font-family: monospace;
}

.media-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tab-controls {
  display: flex;
  gap: 8px;
}

.tab-btn {
  background: rgba(26, 29, 46, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #9ca3af;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: rgba(6, 182, 212, 0.2);
  border-color: #06b6d4;
  color: #38bdf8;
}

.tab-content {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #0b0c14;
  aspect-ratio: 16 / 10;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-section {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 20px;
}
</style>
