<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import DashboardService from '@/services/dashboard'
import type { DashboardModel } from '@/model/dashboard'
import { screenPreview } from '@/model/screen-preview'
import DashboardBoard from './DashboardBoard.vue'

const props = defineProps<{ screen: DashboardModel }>()
const { t } = useI18n()
const host = ref<HTMLElement>(), detail = shallowRef(props.screen)
const visible = ref(false), coverFailed = ref(false), failed = ref(false), pending = ref(false)
const size = ref({ width: 0, height: 0 })
let generation = 0, intersection: IntersectionObserver | undefined, resize: ResizeObserver | undefined
const cover = computed(() => !coverFailed.value ? props.screen.avatar?.path : '')
const preview = computed(() => screenPreview(detail.value))
const scale = computed(() => Math.min(size.value.width / preview.value.width, size.value.height / preview.value.height))
const frame = computed(() => ({ width: preview.value.width + 'px', height: preview.value.height + 'px',
  transform: `translate(-50%, -50%) scale(${scale.value || 0})` }))
async function load() {
  if (!visible.value || cover.value || Array.isArray(props.screen.reports) || !props.screen.code || pending.value) return
  const request = ++generation
  pending.value = true
  try {
    const response = await DashboardService.getByCode(props.screen.code)
    if (request !== generation) return
    if (!response.status || !response.data) throw Error('Preview unavailable')
    detail.value = response.data; failed.value = false
  } catch { if (request === generation) failed.value = true }
  finally { if (request === generation) pending.value = false }
}
watch(() => props.screen, () => {
  generation++; detail.value = props.screen; pending.value = false; failed.value = false; coverFailed.value = false; void load()
})
watch([visible, cover], () => { void load() })
onMounted(() => {
  if (!host.value) return
  resize = new ResizeObserver(([entry]) => { size.value = { width: entry.contentRect.width, height: entry.contentRect.height } })
  resize.observe(host.value)
  intersection = new IntersectionObserver(entries => { if (entries.some(entry => entry.isIntersecting)) { visible.value = true; intersection?.disconnect() } }, { rootMargin: '120px' })
  intersection.observe(host.value)
})
onBeforeUnmount(() => { generation++; intersection?.disconnect(); resize?.disconnect() })
</script>

<template>
  <div ref="host" class="screen-thumbnail relative size-full overflow-hidden bg-surface-hover" aria-hidden="true" inert>
    <img v-if="cover" :src="cover" alt="" loading="lazy" class="size-full object-cover" @error="coverFailed = true"/>
    <MfEmpty v-else-if="failed" class="h-full" size="sm" :title="t('screenLibrary.previewUnavailable')"/>
    <div v-else-if="visible" class="pointer-events-none absolute left-1/2 top-1/2 origin-center" :style="frame">
      <DashboardBoard :design="preview.design" :reports="preview.reports" thumbnail/>
    </div>
    <div v-if="pending" class="absolute inset-0 flex items-center justify-center bg-surface/40"><MfSpinner size="sm"/></div>
  </div>
</template>
