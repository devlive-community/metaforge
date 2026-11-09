<template>
  <div v-if="widget.kind === 'imageText'" class="image-text-widget h-full min-w-0 overflow-auto">
    <img v-if="imageUrl" :src="imageUrl" :alt="widget.name" class="w-full rounded-mf-sm object-contain"/>
    <MfEmpty v-else size="sm" :title="$t('dashboard.studio.chooseImage')"/>
    <div class="flex min-w-0 flex-col justify-center gap-2">
      <p class="whitespace-pre-wrap break-words text-sm leading-relaxed">{{ widget.text }}</p>
      <a v-if="linkUrl" :href="linkUrl" target="_blank" rel="noopener noreferrer" class="inline-flex items-center gap-1 text-sm text-primary hover:underline">
        <ExternalLink :size="14"/>{{ $t('dashboard.studio.openLink') }}
      </a>
    </div>
  </div>
  <template v-else-if="widget.kind === 'bulletList'">
    <MfEmpty v-if="!lines.length" class="h-full" size="sm" :title="$t('dashboard.studio.enterListItems')"/>
    <ul v-else class="h-full list-inside list-disc space-y-2 overflow-auto px-1 py-1 text-sm marker:text-primary">
      <li v-for="(line, index) in lines" :key="index" class="break-words leading-relaxed">{{ line }}</li>
    </ul>
  </template>
  <template v-else-if="widget.kind === 'linkList'">
    <MfEmpty v-if="!links.length" class="h-full" size="sm" :title="$t('dashboard.studio.addLinks')"/>
    <nav v-else class="flex h-full flex-col gap-1 overflow-auto" :aria-label="widget.name">
      <template v-for="link in links" :key="link.renderKey">
        <a v-if="link.url" :href="link.url" target="_blank" rel="noopener noreferrer"
           class="flex min-w-0 items-center gap-2 rounded-mf-md px-2 py-2 text-sm text-primary hover:bg-surface-hover">
          <Link :size="15" class="shrink-0"/><span class="truncate" :title="link.label || link.url">{{ link.label || link.url }}</span><ExternalLink :size="12" class="ml-auto shrink-0"/>
        </a>
        <span v-else class="flex items-center gap-2 px-2 py-2 text-sm text-content-tertiary"><Link :size="15"/>{{ link.label || $t('dashboard.studio.chooseUrl') }}</span>
      </template>
    </nav>
  </template>
  <CountdownTimer v-else-if="widget.kind === 'countdown'" :deadline="widget.deadline"/>
  <div v-else class="flex size-full items-center justify-center overflow-hidden text-primary" :style="{ color: widget.color || undefined }">
    <component :is="MENU_ICONS[widget.iconName || 'Layers'] || MENU_ICONS.Layers" :size="widget.iconSize ?? 64" :stroke-width="widget.strokeWidth ?? 2" aria-hidden="true"/>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ExternalLink, Link } from '@lucide/vue'
import { MENU_ICONS } from '@/config/menu-icons'
import { dashboardResourceUrl, type DashboardWidget } from '@/model/dashboard-layout'
import CountdownTimer from '@/views/components/content/CountdownTimer.vue'
const props = defineProps<{ widget: Pick<DashboardWidget, 'color' | 'deadline' | 'iconName' | 'iconSize' | 'image' | 'kind' | 'links' | 'name' | 'strokeWidth' | 'text' | 'url'> }>()
const lines = computed(() => props.widget.text.split('\n').map(line => line.trim()).filter(Boolean).slice(0, 100))
const links = computed(() => (props.widget.links ?? []).map((link, index) => ({ ...link, renderKey: `${link.id}:${index}`, url: dashboardResourceUrl(link.url) })))
const imageUrl = computed(() => dashboardResourceUrl(props.widget.image))
const linkUrl = computed(() => dashboardResourceUrl(props.widget.url))
</script>

<style scoped>
.image-text-widget { display: flex; flex-direction: column; gap: 12px; }
.image-text-widget > img { min-height: 0; max-height: 55%; }
</style>
