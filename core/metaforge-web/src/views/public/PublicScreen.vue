<template>
  <div class="flex h-dvh min-h-0 flex-col bg-surface">
    <div v-if="loading" class="flex min-h-0 flex-1 items-center justify-center"><MfSpinner size="lg"/></div>
    <div v-else-if="!design" class="flex min-h-0 flex-1 items-center justify-center p-4">
      <MfEmpty :title="$t('dashboard.share.public.unavailable')" :description="$t('dashboard.share.public.unavailableHint')"/>
    </div>
    <!-- The published screen only, as the owner chose to show it; no toolbar or navigation. -->
    <DashboardPresentationCanvas v-else :design="design" :reports="screen?.reports || []"/>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, provide, ref, shallowRef } from 'vue'
import { useI18n } from 'vue-i18n'
import PublicScreenService from '@/services/public-screen'
import { readDashboardDocument } from '@/model/dashboard-layout'
import type { ReportModel } from '@/model/report'
import { dashboardRefreshKey } from '@/composables/useDashboardReports'
import DashboardPresentationCanvas from '@/views/pages/admin/dashboard/components/DashboardPresentationCanvas.vue'

defineOptions({ name: 'PublicScreen' })

const props = defineProps<{ token: string; embed: boolean }>()

interface PublicScreenModel
{
  name: string
  configure: string
  reports: ReportModel[]
  embed: boolean
}

const { t } = useI18n()
const loading = ref(true)
const screen = shallowRef<PublicScreenModel | null>(null)
const design = computed(() => screen.value ? readDashboardDocument(screen.value.configure, screen.value.name, '', t).published : undefined)

// Charts refresh through the link itself; the visitor is not signed in.
provide(dashboardRefreshKey, report => PublicScreenService.refresh(props.token, report))

onMounted(async () => {
  try {
    const response = await PublicScreenService.view(props.token)
    screen.value = response.status && response.data ? response.data : null
    if (screen.value) document.title = `${ screen.value.name } - MetaForge`
  }
  catch { screen.value = null }
  finally { loading.value = false }
})
</script>
