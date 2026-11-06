<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ArrowRight, ChartColumn, Database, Layers, RefreshCw, Terminal } from '@lucide/vue'
import { SCREEN_LIST_PATH } from '@/model/screen-route'
import { useHomeWorkspace } from '@/composables/useHomeWorkspace'
import { useHomeAnalytics } from '@/composables/useHomeAnalytics'
import HomeActivityChart from './components/HomeActivityChart.vue'
import HomeSourceChart from './components/HomeSourceChart.vue'
import HomeRecentQueries from './components/HomeRecentQueries.vue'
import HomeRecentScreens from './components/HomeRecentScreens.vue'

defineOptions({ name: 'DashboardHome' })
const { t, locale } = useI18n(), router = useRouter()
const workspace = useHomeWorkspace()
const { sources, queries, datasets, visualizations } = workspace
const { period, activity, activityLoading, activityFailed, queries: recentQueries, queriesLoading, queriesFailed, refresh: refreshAnalytics, loadActivity, loadQueries } = useHomeAnalytics()
const loading = computed(() => workspace.loading.value || activityLoading.value || queriesLoading.value)
const periods = computed(() => [7,30].map(value => ({ value, label: t('homeDashboard.recentDays', { days: value }) })))
const metrics = computed(() => [
  { key: 'sources', label: 'common.source', icon: Database, value: sources.value.value, loading: sources.value.loading, failed: sources.value.failed, path: '/source' },
  { key: 'queries', label: 'homeWorkspace.queryCount', icon: Terminal, value: queries.value.value, loading: queries.value.loading, failed: queries.value.failed, path: '/history' },
  { key: 'datasets', label: 'common.dataset', icon: Layers, value: datasets.value.total, loading: datasets.value.loading, failed: datasets.value.failed, path: '/dataset' },
  { key: 'visualizations', label: 'homeWorkspace.visualizations', icon: ChartColumn, value: visualizations.value.total, loading: visualizations.value.loading, failed: visualizations.value.failed, path: SCREEN_LIST_PATH },
])
const format = (value: number | null) => value === null ? '--' : value.toLocaleString(locale.value)
function refresh() { void Promise.allSettled([workspace.refresh(), refreshAnalytics()]) }
</script>

<template>
  <section class="home-dashboard flex h-full min-h-0 min-w-0 flex-col gap-4 overflow-y-auto px-2 py-3 text-content sm:px-4">
    <header class="flex shrink-0 flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-semibold">{{ $t('homeDashboard.title') }}</h1>
      <div class="flex max-w-full items-center gap-2"><MfSelect :model-value="period" class="w-36" :options="periods" :aria-label="$t('homeDashboard.period')" @update:model-value="period = Number($event)"/><MfButton :disabled="loading" :aria-label="$t('common.refresh')" @click="refresh"><RefreshCw :size="15"/></MfButton><MfButton variant="primary" @click="router.push('/query')"><Terminal :size="15"/>{{ $t('homeWorkspace.openQuery') }}</MfButton></div>
    </header>
    <div class="grid shrink-0 grid-cols-2 gap-3 xl:grid-cols-4">
      <RouterLink v-for="metric in metrics" :key="metric.key" :to="metric.path" class="group min-w-0 rounded-mf-lg outline-none focus-visible:ring-2 focus-visible:ring-primary/40">
        <MfCard padding="md" hoverable><div class="flex min-w-0 items-center gap-3"><span class="grid size-11 shrink-0 place-items-center rounded-mf-md bg-primary/8 text-primary"><component :is="metric.icon" :size="23"/></span><div class="min-w-0 flex-1"><span class="block text-xs font-medium text-content-secondary sm:text-sm">{{ $t(metric.label) }}</span><MfSpinner v-if="metric.loading" size="sm" class="mt-2"/><strong v-else class="mt-1 block truncate text-2xl font-semibold tabular-nums tracking-tight sm:text-3xl" :title="format(metric.value)">{{ format(metric.value) }}</strong><span v-if="metric.failed" class="text-xs text-danger">{{ $t('homeWorkspace.unavailable') }}</span></div></div></MfCard>
      </RouterLink>
    </div>
    <div class="grid min-w-0 shrink-0 grid-cols-1 gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      <MfCard padding="sm" class="min-w-0">
        <template #header><h2 class="text-base font-semibold">{{ $t('homeDashboard.queryActivity') }}</h2></template>
        <template #extra><MfRadioGroup v-model="period" variant="button" size="sm" :aria-label="$t('homeDashboard.period')"><MfRadio v-for="item in periods" :key="item.value" :value="item.value" :label="item.label"/></MfRadioGroup></template>
        <div class="flex h-56 min-w-0 flex-col lg:h-[clamp(11rem,calc(100dvh-34rem),16rem)]"><div v-if="activityLoading" class="flex h-full items-center justify-center"><MfSpinner/></div><MfEmpty v-else-if="activityFailed" class="h-full" size="sm" :title="$t('homeWorkspace.loadFailed')"><template #extra><MfButton size="sm" @click="loadActivity">{{ $t('common.refresh') }}</MfButton></template></MfEmpty><HomeActivityChart v-else-if="activity" :activity="activity"/></div>
      </MfCard>
      <MfCard padding="sm" class="min-w-0">
        <template #header><h2 class="text-base font-semibold">{{ $t('homeDashboard.sourceDistribution') }}</h2></template>
        <div class="flex h-56 min-w-0 flex-col lg:h-[clamp(11rem,calc(100dvh-34rem),16rem)]"><div v-if="activityLoading" class="flex h-full items-center justify-center"><MfSpinner/></div><MfEmpty v-else-if="activityFailed || !activity?.total" class="h-full" size="sm" :title="$t(activityFailed ? 'homeWorkspace.loadFailed' : 'homeDashboard.noQueries')"/><HomeSourceChart v-else :activity="activity"/></div>
      </MfCard>
    </div>
    <div class="grid min-w-0 shrink-0 grid-cols-1 gap-4 pb-1 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      <MfCard padding="sm" class="min-w-0"><template #header><h2 class="text-base font-semibold">{{ $t('homeDashboard.recentQueries') }}</h2></template><template #extra><RouterLink to="/history" class="inline-flex items-center gap-1 text-xs text-content-secondary hover:text-primary">{{ $t('homeWorkspace.viewAll') }}<ArrowRight :size="12"/></RouterLink></template>
        <div class="min-h-48"><div v-if="queriesLoading" class="flex min-h-48 items-center justify-center"><MfSpinner/></div><MfEmpty v-else-if="queriesFailed || !recentQueries.length" class="min-h-48" size="sm" :title="$t(queriesFailed ? 'homeWorkspace.loadFailed' : 'homeDashboard.noQueries')"><template v-if="queriesFailed" #extra><MfButton size="sm" @click="loadQueries">{{ $t('common.refresh') }}</MfButton></template></MfEmpty><HomeRecentQueries v-else :queries="recentQueries"/></div>
      </MfCard>
      <MfCard padding="sm" class="min-w-0"><template #header><h2 class="text-base font-semibold">{{ $t('homeDashboard.recentVisualizations') }}</h2></template><template #extra><RouterLink :to="SCREEN_LIST_PATH" class="text-xs text-content-secondary hover:text-primary">{{ $t('homeWorkspace.viewAll') }}</RouterLink></template>
        <div v-if="visualizations.loading" class="flex min-h-48 items-center justify-center"><MfSpinner/></div><MfEmpty v-else-if="visualizations.failed || !visualizations.items.length" class="min-h-48" size="sm" :title="$t(visualizations.failed ? 'homeWorkspace.loadFailed' : 'homeWorkspace.empty.visualizations')"/><HomeRecentScreens v-else :items="visualizations.items"/>
      </MfCard>
    </div>
  </section>
</template>
