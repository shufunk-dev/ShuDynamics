<template>
  <div class="video-container">
    <!-- If YouTube URL or ID provided -->
    <div v-if="isYouTube" class="responsive-iframe-wrap">
      <iframe 
        :src="youtubeEmbedUrl" 
        title="Machine Video Demonstration"
        frameborder="0" 
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
        allowfullscreen
      ></iframe>
    </div>

    <!-- If Direct Video File (MP4/WebM) provided -->
    <div v-else-if="videoSrc" class="direct-video-wrap">
      <video :src="videoSrc" controls preload="metadata" class="native-video"></video>
    </div>

    <!-- If No Video Provided Yet: Clean Placeholder Box -->
    <div v-else class="video-placeholder">
      <div class="placeholder-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke="#8B5CF6" stroke-width="1.5" fill="#1C1E2D"/>
          <polygon points="10,8 16,12 10,16" fill="#06B6D4"/>
        </svg>
      </div>
      <span class="placeholder-title">{{ title || 'Video Walkthrough Coming Soon' }}</span>
      <span class="placeholder-sub">A video explanation for this machine will be featured right here!</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  url: { type: String, default: '' },
  title: { type: String, default: '' }
});

const isYouTube = computed(() => {
  if (!props.url) return false;
  return props.url.includes('youtube.com') || props.url.includes('youtu.be');
});

const youtubeEmbedUrl = computed(() => {
  if (!props.url) return '';
  if (props.url.includes('embed/')) return props.url;
  
  // Format short URL: youtu.be/xyz
  const shortMatch = props.url.match(/youtu\.be\/([^?&]+)/);
  if (shortMatch) return `https://www.youtube.com/embed/${shortMatch[1]}`;
  
  // Format watch URL: youtube.com/watch?v=xyz
  const watchMatch = props.url.match(/[?&]v=([^?&]+)/);
  if (watchMatch) return `https://www.youtube.com/embed/${watchMatch[1]}`;
  
  return props.url;
});

const videoSrc = computed(() => {
  if (!props.url || isYouTube.value) return '';
  return props.url;
});
</script>

<style scoped>
.video-container {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(139, 92, 246, 0.3);
  background: #0d0e17;
}

.responsive-iframe-wrap {
  position: relative;
  padding-bottom: 56.25%; /* 16:9 */
  height: 0;
}

.responsive-iframe-wrap iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.direct-video-wrap {
  width: 100%;
  display: flex;
}

.native-video {
  width: 100%;
  max-height: 480px;
  background: #000;
}

.video-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 36px 20px;
  background: linear-gradient(135deg, rgba(24, 26, 40, 0.9), rgba(14, 15, 24, 0.9));
  text-align: center;
  gap: 8px;
}

.placeholder-icon {
  margin-bottom: 4px;
  filter: drop-shadow(0 0 12px rgba(6, 182, 212, 0.4));
}

.placeholder-title {
  font-size: 15px;
  font-weight: 700;
  color: #f3f4f6;
}

.placeholder-sub {
  font-size: 13px;
  color: #9ca3af;
  max-width: 420px;
}
</style>
