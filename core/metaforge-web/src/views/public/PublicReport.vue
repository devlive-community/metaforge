<template>
  <div class="flex h-dvh min-h-0 flex-col" :class="embed ? 'p-2' : 'px-4 py-6 sm:px-8'">
    <div v-if="loading" class="flex min-h-0 flex-1 items-center justify-center">
      <MfSpinner size="lg"/>
    </div>

    <div v-else-if="!report" class="flex min-h-0 flex-1 items-center justify-center">
      <MfEmpty :title="$t('report.share.unavailable')" :description="$t('report.share.unavailableHint')"/>
    </div>

    <template v-else>
      <header v-if="showHeader" class="mb-3 shrink-0">
        <h1 v-if="report.showTitle" class="truncate font-semibold text-content" :class="embed ? 'text-sm' : 'text-xl'">{{ report.name }}</h1>
        <p v-if="showDescription" class="mt-1 text-content-secondary" :class="embed ? 'text-xs' : 'text-sm'">{{ report.description }}</p>
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
  showTitle: boolean
  showDescription: boolean
}

const route = useRoute()
const token = computed(() => String(route.params.token ?? ''))
// Embedded pages are compact; what the page shows is the owner's choice in the share settings.
const embed = computed(() => route.query.embed === '1')

const loading = ref(true)
const report = ref<PublicReportModel | null>(null)
const showDescription = computed(() => !!report.value?.showDescription && !!report.value.description)
const showHeader = computed(() => !!report.value?.showTitle || showDescription.value)
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
