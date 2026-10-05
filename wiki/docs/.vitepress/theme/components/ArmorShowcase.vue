<template>
  <div class="armor-showcase-card">
    <div class="armor-showcase-header">
      <div class="armor-title-wrap">
        <h3 class="armor-title">{{ name }}</h3>
        <span class="armor-tier-badge" :class="tierClass">{{ tier }}</span>
      </div>
      <p class="armor-desc">{{ description }}</p>
    </div>

    <div class="armor-main-grid">
      <!-- Left Column: Individual Armor Pieces -->
      <div class="armor-pieces-col">
        <div class="armor-piece-row" v-for="piece in pieces" :key="piece.slot">
          <div class="armor-slot-icon">
            <img :src="piece.icon" :alt="piece.name" class="pixel-icon" />
          </div>
          <div class="armor-piece-info">
            <span class="piece-name">{{ piece.name }}</span>
            <div class="piece-stats">
              <span class="stat-pill defense">🛡️ +{{ piece.defense }} Defense</span>
              <span v-if="piece.durability" class="stat-pill durability">🔨 {{ piece.durability }} Durability</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: In-Game Showcase / Worn Screenshot Placeholder -->
      <div class="armor-preview-col">
        <div class="preview-container">
          <img 
            :src="imageSrc" 
            :alt="`${name} In-Game Showcase`"
            class="showcase-img"
            @error="handleImageError"
          />
          <div class="preview-badge">In-Game Preview</div>
        </div>
      </div>
    </div>

    <!-- Bottom Set Bonus & Full Stats -->
    <div class="armor-footer-stats">
      <div class="stat-box">
        <span class="stat-label">Total Set Defense</span>
        <span class="stat-value highlight-cyan">{{ totalDefense }} 🛡️</span>
      </div>
      <div class="stat-box" v-if="toughness > 0">
        <span class="stat-label">Armor Toughness</span>
        <span class="stat-value highlight-purple">+{{ toughness }} 💎</span>
      </div>
      <div class="stat-box" v-if="knockbackResistance > 0">
        <span class="stat-label">Knockback Res</span>
        <span class="stat-value highlight-gold">+{{ knockbackResistance * 100 }}% 🛡️</span>
      </div>
      <div class="stat-box bonus-box" v-if="setBonus">
        <span class="stat-label">Full Set Perk / Special</span>
        <span class="stat-value perk-text">✨ {{ setBonus }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  name: { type: String, required: true },
  tier: { type: String, default: 'Tier 1' },
  tierClass: { type: String, default: 'tier-early' },
  description: { type: String, default: '' },
  image: { type: String, required: true },
  pieces: { type: Array, default: () => [] },
  toughness: { type: Number, default: 0 },
  knockbackResistance: { type: Number, default: 0 },
  setBonus: { type: String, default: '' }
});

const currentImage = ref(props.image);
const imageSrc = computed(() => currentImage.value);

const handleImageError = () => {
  // If PNG fails to load, fallback to SVG placeholder
  if (currentImage.value.endsWith('.png')) {
    currentImage.value = currentImage.value.replace('.png', '.svg');
  }
};

const totalDefense = computed(() => {
  return props.pieces.reduce((acc, p) => acc + (p.defense || 0), 0);
});
</script>

<style scoped>
.armor-showcase-card {
  background: linear-gradient(145deg, rgba(26, 28, 43, 0.8), rgba(15, 16, 26, 0.95));
  border: 1px solid rgba(139, 92, 246, 0.35);
  border-radius: 16px;
  padding: 24px;
  margin: 24px 0;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(12px);
}

.armor-showcase-header {
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 14px;
}

.armor-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.armor-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #f3f4f6;
}

.armor-tier-badge {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  padding: 3px 10px;
  border-radius: 20px;
  letter-spacing: 0.5px;
}

.tier-early { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid #3b82f6; }
.tier-mid { background: rgba(168, 85, 247, 0.2); color: #c084fc; border: 1px solid #a855f7; }
.tier-high { background: rgba(234, 179, 8, 0.2); color: #facc15; border: 1px solid #eab308; }
.tier-end { background: rgba(236, 72, 153, 0.2); color: #f472b6; border: 1px solid #ec4899; }

.armor-desc {
  margin: 6px 0 0 0;
  font-size: 14px;
  color: #9ca3af;
}

.armor-main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 768px) {
  .armor-main-grid {
    grid-template-columns: 1fr;
  }
}

.armor-pieces-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.armor-piece-row {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(30, 34, 53, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 10px 14px;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.armor-piece-row:hover {
  background: rgba(42, 47, 72, 0.8);
  border-color: rgba(139, 92, 246, 0.4);
  transform: translateX(4px);
}

.armor-slot-icon {
  width: 44px;
  height: 44px;
  background: #191b28;
  border: 2px solid #2d324d;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pixel-icon {
  width: 32px;
  height: 32px;
  image-rendering: pixelated;
}

.armor-piece-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.piece-name {
  font-weight: 600;
  font-size: 14px;
  color: #e5e7eb;
}

.piece-stats {
  display: flex;
  gap: 8px;
}

.stat-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  font-family: monospace;
}

.stat-pill.defense {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.stat-pill.durability {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.armor-preview-col {
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-container {
  width: 100%;
  height: 100%;
  min-height: 220px;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  border: 1px solid rgba(139, 92, 246, 0.25);
  background: #0f1019;
  display: flex;
  align-items: center;
  justify-content: center;
}

.showcase-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.preview-container:hover .showcase-img {
  transform: scale(1.02);
}

.preview-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(139, 92, 246, 0.5);
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  color: #c4b5fd;
  text-transform: uppercase;
}

.armor-footer-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.stat-box {
  background: rgba(22, 24, 38, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 8px 14px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 11px;
  color: #9ca3af;
  text-transform: uppercase;
  font-weight: 600;
}

.stat-value {
  font-size: 15px;
  font-weight: 700;
  margin-top: 2px;
}

.highlight-cyan { color: #38bdf8; }
.highlight-purple { color: #c084fc; }
.highlight-gold { color: #fbbf24; }
.bonus-box { flex-grow: 1; }
.perk-text { color: #a7f3d0; font-weight: 600; }
</style>
