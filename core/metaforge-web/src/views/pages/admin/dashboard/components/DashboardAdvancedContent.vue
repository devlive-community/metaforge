<template>
  <div v-if="widget.kind === 'quote'" class="flex h-full flex-col justify-center overflow-auto border-l-4 border-primary/40 pl-4" :style="textStyle">
    <blockquote class="whitespace-pre-wrap break-words italic">{{ widget.text || widget.name }}</blockquote><cite v-if="settings.attribution" class="mt-3 text-sm not-italic text-content-tertiary">{{ settings.attribution }}</cite>
  </div>
  <div v-else-if="widget.kind === 'textTicker'" class="flex h-full items-center overflow-hidden [container-type:inline-size]" :style="textStyle">
    <span class="whitespace-nowrap" :class="{ 'content-ticker': !editable && settings.autoplay, 'pause-on-hover': settings.pauseOnHover }"
          :style="{ '--ticker-duration': settings.interval + 's' }">{{ widget.text || widget.name }}</span>
  </div>
  <div v-else-if="widget.kind === 'tagCloud'" class="flex h-full content-center items-center justify-center gap-2 overflow-auto flex-wrap p-2">
    <MfTag v-for="(line, index) in lines" :key="index" :style="{ fontSize: settings.fontSize * (.8 + index % 4 / 10) + 'px' }">{{ line }}</MfTag>
    <MfEmpty v-if="!lines.length" size="sm" :title="$t('dashboard.studio.enterListItems')"/>
  </div>
  <div v-else-if="widget.kind === 'actionButton'" class="flex h-full items-center justify-center">
    <MfButton variant="primary" :disabled="!linkUrl" @click="openLink"><ExternalLink :size="15"/>{{ widget.text || widget.name }}</MfButton>
  </div>
  <template v-else-if="widget.kind === 'embed'">
    <iframe v-if="linkUrl" :src="linkUrl" :title="widget.name" class="size-full border-0 bg-surface" loading="lazy" referrerpolicy="no-referrer" sandbox="allow-scripts allow-forms allow-popups"/>
    <MfEmpty v-else class="h-full" size="sm" :title="$t('dashboard.studio.chooseUrl')"/>
  </template>
  <MfEmpty v-else-if="!items.length" class="h-full" size="sm" :title="$t('dashboard.studio.addContentItems')"/>
  <MfTabs v-else-if="widget.kind === 'tabsContent'" fill class="h-full">
    <MfTabPane v-for="item in items" :key="item.renderKey" :item-key="item.renderKey" :tab="item.label">
      <div class="h-full overflow-auto whitespace-pre-wrap break-words p-2" :style="textStyle">{{ item.text }}</div>
    </MfTabPane>
  </MfTabs>
  <div v-else-if="widget.kind === 'accordion'" class="h-full space-y-2 overflow-auto">
    <details v-for="(item, index) in items" :key="item.renderKey" :open="index === 0" class="rounded-mf-md border border-line">
      <summary class="cursor-pointer rounded-mf-md bg-surface-hover px-3 py-2 text-sm font-medium">{{ item.label }}</summary>
      <p class="whitespace-pre-wrap break-words p-3" :style="textStyle">{{ item.text }}</p>
    </details>
  </div>
  <div v-else-if="widget.kind === 'imageGallery'" class="grid h-full grid-cols-2 content-start gap-2 overflow-auto">
    <MfButton v-for="item in items" :key="item.renderKey" variant="ghost" class="h-auto! min-w-0 flex-col! gap-1! p-1!" @click="zoomed = item">
      <img v-if="item.image" :src="item.image" :alt="item.label" class="aspect-video w-full rounded-mf-sm" :style="{ objectFit: settings.fit }"/>
      <span class="max-w-full truncate text-xs">{{ item.label }}</span>
    </MfButton>
  </div>
  <div v-else class="flex h-full min-h-0 flex-col gap-2" @mouseenter="hovered = true" @mouseleave="hovered = false">
    <div class="min-h-0 flex-1 overflow-auto">
      <template v-if="widget.kind === 'imageCarousel'">
        <MfButton variant="ghost" class="size-full! p-0!" @click="zoomed = current">
          <img v-if="current.image" :src="current.image" :alt="current.label" class="size-full rounded-mf-sm" :style="{ objectFit: settings.fit }"/>
          <MfEmpty v-else size="sm" :title="$t('dashboard.studio.chooseImage')"/>
        </MfButton>
      </template>
      <div v-else class="flex h-full flex-col justify-center gap-2" :style="textStyle"><strong class="break-words">{{ current.label }}</strong><p class="whitespace-pre-wrap break-words text-content-secondary">{{ current.text }}</p></div>
    </div>
    <div class="flex shrink-0 items-center justify-between gap-2">
      <MfButton variant="ghost" size="sm" :aria-label="$t('dashboard.studio.previousItem')" @click="advance(-1)"><ChevronLeft :size="14"/></MfButton>
      <span class="min-w-0 truncate text-xs text-content-tertiary">{{ widget.kind === 'imageCarousel' ? current.label : active + 1 }} · {{ active + 1 }} / {{ items.length }}</span>
      <MfButton variant="ghost" size="sm" :aria-label="$t('dashboard.studio.nextItem')" @click="advance(1)"><ChevronRight :size="14"/></MfButton>
    </div>
  </div>
  <MfModal v-if="zoomed" :open="true" :title="zoomed.label" width="min(900px, 95vw)" height="min(700px, 90dvh)" trap-focus @close="zoomed = undefined">
    <div class="flex h-full min-h-0 flex-col gap-3"><img v-if="zoomed.image" :src="zoomed.image" :alt="zoomed.label" class="min-h-0 flex-1 object-contain"/><p v-if="zoomed.text" class="shrink-0 whitespace-pre-wrap text-sm text-content-secondary">{{ zoomed.text }}</p></div>
  </MfModal>
</template>
<script setup lang="ts">
import { computed, onScopeDispose, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, ExternalLink } from '@lucide/vue'
import { dashboardContentSettings, dashboardResourceUrl, type DashboardWidget } from '@/model/dashboard-layout'
const props = defineProps<{ widget: DashboardWidget; editable?: boolean }>()
const settings = computed(() => dashboardContentSettings(props.widget.contentOptions))
const items = computed(() => (props.widget.links ?? []).map((item, index) => ({ ...item, renderKey: item.id + ':' + index, image: dashboardResourceUrl(item.image ?? ''), url: dashboardResourceUrl(item.url), text: item.text ?? '' })))
const lines = computed(() => props.widget.text.split('\n').map(line => line.trim()).filter(Boolean).slice(0, 100))
const textStyle = computed(() => ({ fontSize: settings.value.fontSize + 'px', fontWeight: settings.value.fontWeight === 'bold' ? 700 : settings.value.fontWeight === 'medium' ? 500 : 400,
  lineHeight: settings.value.lineHeight, textAlign: props.widget.alignment, color: props.widget.color || undefined }))
const active = ref(0)
const hovered = ref(false)
const current = computed(() => items.value[active.value] ?? items.value[0])
const zoomed = ref<(typeof items.value)[number]>()
const linkUrl = computed(() => dashboardResourceUrl(props.widget.url))
function openLink() { if (linkUrl.value) window.open(linkUrl.value, '_blank', 'noopener,noreferrer') }
function advance(direction: number) { if (items.value.length) active.value = (active.value + direction + items.value.length) % items.value.length }
watch(items, () => { if (active.value >= items.value.length) active.value = 0 })
let timer: ReturnType<typeof setInterval> | undefined
watch([() => settings.value.interval, () => settings.value.autoplay, () => props.editable, () => props.widget.kind], () => {
  clearInterval(timer)
  if (props.editable || !settings.value.autoplay || !['imageCarousel', 'announcement'].includes(props.widget.kind)) return
  timer = setInterval(() => {
    if (!document.hidden && !(hovered.value && settings.value.pauseOnHover) && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) advance(1)
  }, settings.value.interval * 1000)
}, { immediate: true })
onScopeDispose(() => clearInterval(timer))
</script>
<style scoped>
.content-ticker { animation: content-marquee var(--ticker-duration) linear infinite; }
.content-ticker.pause-on-hover:hover { animation-play-state: paused; }
@keyframes content-marquee { from { transform: translateX(100cqw); } to { transform: translateX(-100%); } }
@media (prefers-reduced-motion: reduce) { .content-ticker { animation: none; white-space: normal; } }
</style>
