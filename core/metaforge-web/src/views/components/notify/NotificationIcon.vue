<template>
  <span class="dc-notice-icon" :class="`dc-notice-icon--${ tone }`" :style="{ width: `${ size }px`, height: `${ size }px` }">
    <component :is="icon" :size="Math.round(size * 0.45)"/>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowLeftRight, Bell, ChartColumn, Database, FlaskConical, Plug, Puzzle, ShieldCheck, TriangleAlert } from '@lucide/vue'
import { NotificationItem, toneOf } from '@/model/notification'

defineOptions({ name: 'NotificationIcon' })

const props = withDefaults(defineProps<{ item: Pick<NotificationItem, 'scene' | 'category'>, size?: number }>(), { size: 36 })

const ICONS: Record<string, any> = {
  SYSTEM: Bell,
  DATASET: Database,
  SOURCE: Plug,
  REPORT: ChartColumn,
  TRANSFER: ArrowLeftRight,
  ACCOUNT: ShieldCheck,
  PLUGIN: Puzzle
}

const tone = computed(() => toneOf(props.item))
const icon = computed(() => {
  if (tone.value === 'danger') {
    return TriangleAlert
  }
  return props.item.scene === 'TEST' ? FlaskConical : ICONS[props.item.category] || Bell
})
</script>

<style scoped>
.dc-notice-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    border-radius: var(--dc-radius-md);
}

.dc-notice-icon--primary { background: var(--dc-primary-light); color: var(--dc-primary); }
.dc-notice-icon--info { background: var(--dc-info-bg); color: var(--dc-info); }
.dc-notice-icon--success { background: var(--dc-success-bg); color: var(--dc-success); }
.dc-notice-icon--warning { background: var(--dc-warning-bg); color: var(--dc-warning); }
.dc-notice-icon--danger { background: var(--dc-danger-bg); color: var(--dc-danger); }
.dc-notice-icon--violet { background: color-mix(in srgb, #7c5cff 12%, transparent); color: #7c5cff; }
</style>
