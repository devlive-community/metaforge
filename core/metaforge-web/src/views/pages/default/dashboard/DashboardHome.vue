<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ArrowRight, ArrowUpRight, ChartColumn, Database, History, LayoutDashboard, Plus, RefreshCw, Search, Table2, Terminal, Workflow } from '@lucide/vue'
import { useUserStore } from '@/stores/user'
import { useHomeWorkspace, type HomeResourceKind } from '@/composables/useHomeWorkspace'
import HomeResourceList from './components/HomeResourceList.vue'

defineOptions({ name: 'DashboardHome' })
const { locale } = useI18n(), router = useRouter(), user = useUserStore()
const { sources, queries, datasets, visualizations, loading, refresh, loadResources } = useHomeWorkspace()
const resource = ref<HomeResourceKind>('datasets')
const name = computed(() => user.userInfo?.username || '')
const metrics = computed(() => [
  { key: 'sources', label: 'common.source', icon: Database, value: sources.value.value, loading: sources.value.loading, failed: sources.value.failed, path: '/source' },
  { key: 'queries', label: 'homeWorkspace.queryCount', icon: Terminal, value: queries.value.value, loading: queries.value.loading, failed: queries.value.failed, path: '/history' },
  { key: 'datasets', label: 'common.dataset', icon: Table2, value: datasets.value.total, loading: datasets.value.loading, failed: datasets.value.failed, path: '/dataset' },
  { key: 'visualizations', label: 'homeWorkspace.visualizations', icon: LayoutDashboard, value: visualizations.value.total, loading: visualizations.value.loading, failed: visualizations.value.failed, path: '/dashboard' },
])
const shortcuts = [
  { path: '/query', icon: Terminal, label: 'homeWorkspace.openQuery' },
  { path: '/source', icon: Database, label: 'common.source' },
  { path: '/dataset', icon: Table2, label: 'common.dataset' },
  { path: '/workflow', icon: Workflow, label: 'common.workflow' },
  { path: '/dashboard', icon: LayoutDashboard, label: 'homeWorkspace.workspace' },
  { path: '/report', icon: ChartColumn, label: 'homeWorkspace.reports' },
]
const listPath = computed(() => resource.value === 'datasets' ? '/dataset' : '/dashboard')
const formatCount = (value: number | null) => value === null ? '--' : value.toLocaleString(locale.value)
</script>

<template>
  <section class="flex h-full min-h-0 min-w-0 flex-col gap-5 overflow-y-auto px-2 py-4 sm:px-4">
    <header class="flex min-w-0 flex-wrap items-center justify-between gap-3">
      <h1 class="min-w-0 truncate text-xl font-semibold text-content" :title="name ? $t('homeWorkspace.greeting', { name }) : $t('homeWorkspace.welcome')">{{ name ? $t('homeWorkspace.greeting', { name }) : $t('homeWorkspace.welcome') }}</h1>
      <div class="flex shrink-0 items-center gap-2">
        <MfButton size="sm" :disabled="loading" :aria-label="$t('common.refresh')" @click="refresh"><RefreshCw :size="14"/></MfButton>
        <MfButton size="sm" @click="router.push('/source')"><Plus :size="14"/>{{ $t('homeWorkspace.connectSource') }}</MfButton>
        <MfButton size="sm" variant="primary" @click="router.push('/query')"><Terminal :size="14"/>{{ $t('homeWorkspace.openQuery') }}</MfButton>
      </div>
    </header>

    <div class="grid grid-cols-2 gap-3 xl:grid-cols-4">
      <RouterLink v-for="metric in metrics" :key="metric.key" :to="metric.path" class="group min-w-0 rounded-mf-lg outline-none focus-visible:ring-2 focus-visible:ring-primary/40">
        <MfCard padding="sm" hoverable>
          <div class="flex items-center justify-between gap-3">
            <span class="text-xs text-content-secondary sm:text-sm">{{ $t(metric.label) }}</span>
            <span class="grid size-9 place-items-center rounded-mf-md bg-primary/8 text-primary"><component :is="metric.icon" :size="18"/></span>
          </div>
          <div class="mt-2 flex h-10 items-end justify-between gap-2">
            <MfSpinner v-if="metric.loading" size="sm"/>
            <span v-else class="text-2xl font-semibold tabular-nums tracking-tight text-content sm:text-3xl">{{ formatCount(metric.value) }}</span>
            <span v-if="metric.failed" class="text-xs text-danger">{{ $t('homeWorkspace.unavailable') }}</span>
            <ArrowUpRight v-else :size="16" class="mb-1 text-content-tertiary group-hover:text-primary"/>
          </div>
        </MfCard>
      </RouterLink>
    </div>

    <div class="grid min-w-0 grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.6fr)_minmax(300px,1fr)]">
      <MfCard padding="sm" class="min-w-0">
        <template #header><h2 class="flex items-center gap-2 text-base font-semibold"><History :size="17" class="text-primary"/>{{ $t('homeWorkspace.recent') }}</h2></template>
        <template #extra><RouterLink :to="listPath" class="inline-flex items-center gap-1 text-xs text-content-secondary hover:text-primary">{{ $t('homeWorkspace.viewAll') }}<ArrowRight :size="13"/></RouterLink></template>
        <MfTabs v-model:active-key="resource" class="home-resource-tabs">
          <MfTabPane item-key="datasets" :tab="$t('common.dataset')"><HomeResourceList kind="datasets" :items="datasets.items" :loading="datasets.loading" :failed="datasets.failed" @retry="loadResources('datasets')"/></MfTabPane>
          <MfTabPane item-key="visualizations" :tab="$t('homeWorkspace.visualizations')"><HomeResourceList kind="visualizations" :items="visualizations.items" :loading="visualizations.loading" :failed="visualizations.failed" @retry="loadResources('visualizations')"/></MfTabPane>
        </MfTabs>
      </MfCard>

      <MfCard padding="md" class="flex min-w-0 flex-col [&>div]:flex [&>div]:flex-1">
        <template #header><h2 class="flex items-center gap-2 text-base font-semibold"><Search :size="17" class="text-primary"/>{{ $t('homeWorkspace.shortcuts') }}</h2></template>
        <nav class="grid w-full grid-cols-2 gap-3" :aria-label="$t('homeWorkspace.shortcuts')">
          <RouterLink v-for="item in shortcuts" :key="item.path" :to="item.path" class="group flex min-h-22 min-w-0 items-center gap-3 rounded-mf-md border border-line px-3 py-4 outline-none transition-colors hover:border-primary/30 hover:bg-primary/5 focus-visible:ring-2 focus-visible:ring-primary/40">
            <span class="grid size-9 shrink-0 place-items-center rounded-mf-md bg-surface-hover text-primary group-hover:bg-primary/10"><component :is="item.icon" :size="19"/></span>
            <span class="min-w-0 break-words text-sm font-medium text-content">{{ $t(item.label) }}</span>
          </RouterLink>
        </nav>
      </MfCard>
    </div>
  </section>
</template>

<style scoped>
.home-resource-tabs :deep([role="tab"]) { padding-inline: 12px; }
</style>
