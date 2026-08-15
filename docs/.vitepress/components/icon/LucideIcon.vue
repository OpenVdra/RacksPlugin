<script setup>
import { computed } from 'vue'
import {
  Package, Download, Hammer, Wrench, Settings, Terminal, ShieldCheck,
  Languages, Globe, Check, Sparkles, TreePine, Axe, Sword, RotateCw,
  Layers, BookOpen, Github, MessageCircle, Heart, ArrowUpRight, Bug,
  Tag, FileText, Box, MousePointer2, Eye, RefreshCw, Database, Blocks,
  Frame, Palette, Hand, PanelTop, Boxes, Server, Pickaxe, Shield,
  TriangleAlert, Trash2, ArrowRightLeft
} from 'lucide-vue-next'

const ICONS = {
  Package, Download, Hammer, Wrench, Settings, Terminal, ShieldCheck,
  Languages, Globe, Check, Sparkles, TreePine, Axe, Sword, RotateCw,
  Layers, BookOpen, Github, MessageCircle, Heart, ArrowUpRight, Bug,
  Tag, FileText, Box, MousePointer2, Eye, RefreshCw, Database, Blocks,
  Frame, Palette, Hand, PanelTop, Boxes, Server, Pickaxe, Shield,
  TriangleAlert, Trash2, ArrowRightLeft
}

const props = defineProps({
  name: String,
  size: { type: Number, default: 22 }
})

const icon = computed(() => props.name ? (ICONS[props.name] ?? null) : null)
</script>

<template>
  <component :is="icon" v-if="icon" :size="size" :stroke-width="1.75" />
</template>
