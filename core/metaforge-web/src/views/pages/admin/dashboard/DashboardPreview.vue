<template>
  <div v-if="loading" class="flex flex-1 items-center justify-center"><MfSpinner size="lg"/></div>
  <div v-else-if="failed || !design" class="flex flex-1 items-center justify-center p-4">
    <MfAlert :variant="failed ? 'danger' : 'info'" :title="$t(failed ? 'dashboard.studio.loadFailed' : 'dashboard.studio.unpublished')">
      <div class="flex flex-wrap gap-2">
        <MfButton v-if="failed" size="sm" @click="load">{{ $t('common.refresh') }}</MfButton>
        <MfButton v-else size="sm" @click="router.push(screenDesignPath(code))">{{ $t('dashboard.studio.backToEditor') }}</MfButton>
        <MfButton size="sm" @click="router.push(SCREEN_LIST_PATH)">{{ $t('dashboard.studio.backToList') }}</MfButton>
      </div>
    </MfAlert>
  </div>
  <!-- Viewing shows the published screen only; editing, sharing and the home page live on the list. -->
  <div v-else class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden bg-surface">
    <ScreenPresentation :design="design" :reports="data?.reports || []"/>
  </div>
</template>

<script setup lang="ts">
import { computed, onScopeDispose, provide, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import DashboardService from '@/services/dashboard'
import type { DashboardModel } from '@/model/dashboard'
import { readDashboardDocument } from '@/model/dashboard-layout'
import { SCREEN_LIST_PATH, screenDesignPath } from '@/model/screen-route'
import ScreenPresentation from '@/views/pages/admin/dashboard/components/ScreenPresentation.vue'
import { dashboardScreenKey } from '@/composables/useDashboardReports'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const code = computed(() => String(route.params.code || ''))
provide(dashboardScreenKey, code)
const data = shallowRef<DashboardModel>()
const loading = ref(false)
const failed = ref(false)
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
watch(code, load, { immediate: true })
onScopeDispose(() => { request++ })
</script>
