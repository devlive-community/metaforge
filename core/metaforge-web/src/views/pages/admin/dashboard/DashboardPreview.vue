<template>
  <div v-if="loading" class="flex flex-1 items-center justify-center"><MfSpinner size="lg"/></div>
  <div v-else-if="failed || !design" class="flex flex-1 items-center justify-center p-4">
    <MfAlert :variant="failed ? 'danger' : 'info'" :title="$t(failed ? 'dashboard.studio.loadFailed' : 'dashboard.studio.unpublished')">
      <MfButton v-if="failed" size="sm" @click="load">{{ $t('common.refresh') }}</MfButton>
      <MfButton v-else size="sm" @click="edit">{{ $t('dashboard.studio.backToEditor') }}</MfButton>
    </MfAlert>
  </div>
  <div v-else class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden bg-surface">
    <header class="flex shrink-0 flex-wrap items-center justify-between gap-2 border-b border-line bg-surface px-3 py-2 sm:px-4">
      <div class="flex min-w-0 items-center gap-2">
        <StudioBackLink to="/dashboard" :label="$t('dashboard.studio.backToList')"/>
        <div class="min-w-0">
          <h1 class="truncate text-base font-semibold" :title="design.name">{{ design.name }}</h1>
          <p class="text-xs text-content-tertiary">{{ $t(design.mode === 'wall' ? 'dashboard.studio.wallPreviewTitle' : 'dashboard.studio.previewTitle') }}</p>
        </div>
      </div>
      <div class="flex flex-wrap items-center gap-1.5">
        <MfButton size="sm" @click="edit"><Pencil :size="14"/>{{ $t('dashboard.studio.backToEditor') }}</MfButton>
        <MfButton v-if="fullscreenAvailable" size="sm" @click="fullscreen">
          <Minimize v-if="fullscreenActive" :size="14"/><Maximize v-else :size="14"/>
          {{ $t(fullscreenActive ? 'dashboard.studio.exitFullscreen' : 'dashboard.studio.fullscreen') }}
        </MfButton>
        <MfButton size="sm" variant="primary" @click="share"><ArrowUpRight :size="15"/>{{ $t('dashboard.studio.share') }}</MfButton>
      </div>
    </header>
    <DashboardPresentationCanvas :design="design" :reports="data?.reports || []"/>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onScopeDispose, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { ArrowUpRight, Maximize, Minimize, Pencil } from '@lucide/vue'
import StudioBackLink from '@/views/components/studio/StudioBackLink.vue'
import DashboardService from '@/services/dashboard'
import type { DashboardModel } from '@/model/dashboard'
import { readDashboardDocument } from '@/model/dashboard-layout'
import DashboardPresentationCanvas from './components/DashboardPresentationCanvas.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const code = computed(() => String(route.params.code || ''))
const data = shallowRef<DashboardModel>()
const loading = ref(false)
const failed = ref(false)
const fullscreenActive = ref(false)
const fullscreenAvailable = document.fullscreenEnabled
let enteredFullscreen = false
const design = computed(() => data.value ? readDashboardDocument(data.value.configure, data.value.name, data.value.description, t).published : undefined)
let request = 0

async function load() {
  const id = ++request
  loading.value = true
  failed.value = false
  data.value = undefined
  try {
    const response = await DashboardService.getByCode(code.value)
    if (id !== request) return
    if (!response.status || !response.data) throw new Error('Dashboard unavailable')
    data.value = response.data
  }
  catch { if (id === request) failed.value = true }
  finally { if (id === request) loading.value = false }
}
function edit() { void router.push('/dashboard/info/' + encodeURIComponent(code.value)) }
async function share() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    MfMessage.success(t('dashboard.studio.linkCopied'))
  }
  catch { MfMessage.error(t('dashboard.studio.linkFailed')) }
}
async function fullscreen() {
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else {
      await document.documentElement.requestFullscreen()
      enteredFullscreen = true
    }
    syncFullscreen()
  }
  catch { MfMessage.error(t('dashboard.studio.fullscreenFailed')) }
}
function syncFullscreen() {
  fullscreenActive.value = enteredFullscreen && document.fullscreenElement === document.documentElement
  if (!fullscreenActive.value) enteredFullscreen = false
}
watch(code, load, { immediate: true })
onMounted(() => document.addEventListener('fullscreenchange', syncFullscreen))
onScopeDispose(() => {
  request++
  document.removeEventListener('fullscreenchange', syncFullscreen)
  if (enteredFullscreen && document.fullscreenElement === document.documentElement) void document.exitFullscreen().catch(() => {})
})
</script>
