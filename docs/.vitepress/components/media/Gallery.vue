<script setup>
import { withBase } from 'vitepress'

// A row of screenshots with captions. Pass `items` as [{ src, caption }] where `src` is a path
// under public/, for example '/media/showcase.jpeg'.
defineProps({
  items: {
    type: Array,
    required: true
  },
  columns: {
    type: Number,
    default: 2
  }
})
</script>

<template>
  <div class="gallery" :style="{ '--gallery-columns': columns }">
    <figure v-for="item in items" :key="item.src" class="gallery-item">
      <img :src="withBase(item.src)" :alt="item.caption || ''" loading="lazy" />
      <figcaption v-if="item.caption">{{ item.caption }}</figcaption>
    </figure>
  </div>
</template>

<style scoped>
.gallery {
  display: grid;
  grid-template-columns: repeat(var(--gallery-columns), minmax(0, 1fr));
  gap: 16px;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .gallery {
    grid-template-columns: 1fr;
  }
}

.gallery-item {
  margin: 0;
  display: flex;
  flex-direction: column;
}

.gallery-item img {
  width: 100%;
  height: auto;
  border-radius: 12px;
  border: 1px solid var(--vp-c-border);
  background-color: var(--vp-c-bg-soft);
  display: block;
}

.gallery-item figcaption {
  margin-top: 8px;
  font-size: 0.85rem;
  line-height: 1.5;
  color: var(--vp-c-text-2);
  text-align: center;
}
</style>
