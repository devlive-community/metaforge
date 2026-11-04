<template>
  <div class="flex h-dvh min-h-0 flex-col" :class="embed ? 'p-2' : 'px-4 py-6 sm:px-8'">
    <div v-if="loading" class="flex min-h-0 flex-1 items-center justify-center">
      <MfSpinner size="lg"/>
    </div>

    <div v-else-if="!report" class="flex min-h-0 flex-1 items-center justify-center">
      <MfEmpty :title="$t('report.share.unavailable')" :description="$t('report.share.unavailableHint')"/>
    </div>

    <template v-else>
      <header v-if="showTitle" class="mb-3 shrink-0">
        <h1 class="truncate font-semibold text-content" :class="embed ? 'text-sm' : 'text-xl'">{{ report.name }}</h1>
        <p v-if="report.description && !embed" class="mt-1 text-sm text-content-secondary">{{ report.description }}</p>
      </header>
      <main class="min-h-0 flex-1">
        <MfAlert v-if="!configuration" variant="danger" :title="$t('queryChart.renderError')"/>
        <VisualView v-else
                    :configuration="configuration"
                    :type="report.type"
                    :realtime="report.realtime"
                    :share-token="token"
                    width="100%"
                    height="100%"/>
      </main>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PublicReportService from '@/services/public-report'
import VisualView from '@/views/components/visual/VisualView.vue'

defineOptions({ name: 'PublicReport' })

interface PublicReportModel
{
  name: string
  description?: string
  type: string
  realtime: boolean
  configure: string
  embed: boolean
}

const route = useRoute()
const token = computed(() => String(route.params.token ?? ''))
// Embedded pages are compact; ?title=0 hides the report name inside the host page.
const embed = computed(() => route.query.embed === '1')
const showTitle = computed(() => route.query.title !== '0')

const loading = ref(true)
const report = ref<PublicReportModel | null>(null)
const configuration = computed(() => {
  try {
    return report.value ? JSON.parse(report.value.configure) : null
  }
  catch {
    return null
  }
})

onMounted(async () => {
  try {
    const response = await PublicReportService.view(token.value)
    report.value = response.status && response.data ? response.data : null
    if (report.value) document.title = `${ report.value.name } - MetaForge`
  }
  finally {
    loading.value = false
  }
})
</script>
