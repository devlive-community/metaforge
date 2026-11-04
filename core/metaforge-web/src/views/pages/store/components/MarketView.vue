<template>
  <div class="flex flex-col gap-5 pb-6">
    <div class="relative flex min-h-[154px] items-center justify-between overflow-hidden rounded-2xl border border-primary/15 bg-surface bg-linear-to-r from-primary/8 to-info/8 p-6 sm:p-7">
      <div class="relative z-10">
        <div class="flex items-center gap-2">
          <MfTag variant="primary" size="sm">PLUGIN MARKET</MfTag>
          <span class="text-[11px] text-primary">{{ $t('store.banner.tagline') }}</span>
        </div>
        <h2 class="mt-2 text-2xl font-bold text-content">{{ $t('store.banner.title') }}</h2>
        <p class="mt-2 text-[13px] text-content-secondary">{{ $t('store.banner.description') }}</p>
        <div class="mt-4 flex flex-wrap items-center gap-3">
          <MfButton variant="primary" @click="store.switchView('installed')">
            <Blocks :size="15"/>
            {{ $t('store.banner.viewInstalled') }}
          </MfButton>
          <MfButton variant="secondary" :loading="store.loading.value" @click="store.checkUpdates()">
            <RefreshCw :size="15"/>
            {{ $t('store.banner.checkUpdates') }}
          </MfButton>
        </div>
      </div>
      <div aria-hidden="true" class="relative mr-12 hidden h-[112px] w-[200px] shrink-0 items-center justify-center lg:flex">
        <div class="absolute size-24 rotate-12 rounded-3xl bg-primary/10"></div>
        <div class="relative flex size-20 items-center justify-center rounded-2xl bg-primary text-on-primary shadow-mf-md">
          <Puzzle :size="42"/>
        </div>
        <div class="absolute -left-4 top-2 rounded-xl border border-line bg-surface/80 p-3 text-primary shadow-mf-sm">
          <Database :size="22"/>
        </div>
        <div class="absolute -right-4 bottom-1 rounded-xl border border-line bg-surface/80 p-3 text-primary shadow-mf-sm">
          <ChartLine :size="22"/>
        </div>
      </div>
    </div>

    <MfAlert v-if="store.catalogFailed.value" variant="warning" :title="$t('store.market.catalogFailed')">
      <MfButton size="sm" :loading="store.loading.value" @click="store.refresh()">{{ $t('common.refresh') }}</MfButton>
    </MfAlert>

    <div class="overflow-hidden rounded-2xl border border-line bg-surface">
      <!-- Category tabs -->
      <div role="group" :aria-label="$t('store.sidebar.types')" class="flex min-h-[62px] items-center gap-1 overflow-x-auto border-b border-line px-5">
        <button v-for="cat in categoryTabs"
                :key="cat.id"
                type="button"
                class="relative flex h-[62px] shrink-0 items-center gap-2 whitespace-nowrap px-4 text-[13px] font-medium text-content-secondary transition-colors hover:text-content"
                :aria-pressed="store.category.value === cat.id"
                :class="store.category.value === cat.id && 'font-bold text-primary'"
                @click="store.pickCategory(cat.id)">
          <component :is="cat.icon" :size="16"/>
          {{ cat.name }}
          <span v-if="store.category.value === cat.id"
                class="absolute bottom-0 left-4 right-4 h-[3px] rounded-t bg-primary"></span>
        </button>
      </div>

      <div class="px-5 py-5">
        <!-- Filters -->
        <div class="flex flex-wrap items-center gap-3">
          <div class="relative min-w-0 basis-full sm:min-w-[200px] sm:max-w-[480px] sm:flex-1">
            <MfInput :model-value="store.query.value"
                     :placeholder="$t('store.market.searchPlaceholder')"
                     class="h-10"
                     @update:model-value="store.setQuery">
              <template #prefix>
                <Search :size="17"/>
              </template>
            </MfInput>
          </div>

          <MfSelect :model-value="store.status.value"
                    :options="statusOptions"
                    class="w-32"
                    size="md"
                    @update:model-value="store.setStatus"/>

          <MfSelect :model-value="store.sort.value"
                    :options="sortOptions"
                    class="w-32"
                    size="md"
                    @update:model-value="store.setSort"/>

          <div class="ml-auto flex gap-1">
            <MfButton :variant="store.layout.value === 'grid' ? 'outline' : 'secondary'"
                      size="sm"
                      :aria-pressed="store.layout.value === 'grid'"
                      :aria-label="$t('store.market.layoutGrid')"
                      @click="store.setLayout('grid')">
              <LayoutGrid :size="15"/>
            </MfButton>
            <MfButton :variant="store.layout.value === 'list' ? 'outline' : 'secondary'"
                      size="sm"
                      :aria-pressed="store.layout.value === 'list'"
                      :aria-label="$t('store.market.layoutList')"
                      @click="store.setLayout('list')">
              <Menu :size="15"/>
            </MfButton>
          </div>
        </div>

        <div class="mb-4 mt-6 flex flex-wrap items-center justify-between gap-2">
          <div>
            <h3 class="text-[17px] font-bold text-content">
              {{ activeCategoryName }}
              <span class="font-semibold text-content-tertiary">({{ store.marketPlugins.value.length }})</span>
            </h3>
            <p class="mt-1 text-xs text-content-tertiary">{{ $t('store.market.dynamicHint') }}</p>
          </div>
          <MfTag variant="primary" size="sm">
            {{ $t('store.market.countBadge', { installed: store.installedCount.value, updates: store.updateCount.value }) }}
          </MfTag>
        </div>

        <div v-if="store.marketPlugins.value.length === 0" class="flex h-full min-h-64 items-center justify-center">
          <MfEmpty variant="search"
                   :title="$t('store.market.emptyTitle')"
                   :description="$t('store.market.emptyDescription')">
            <template #extra>
              <MfButton variant="secondary" @click="store.clearFilters()">
                {{ $t('store.market.clearFilters') }}
              </MfButton>
            </template>
          </MfEmpty>
        </div>

        <div v-else-if="store.layout.value === 'grid'" class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          <PluginCard v-for="plugin in store.marketPlugins.value"
                      :key="plugin.key"
                      :store="store"
                      :plugin="plugin"/>
        </div>

        <div v-else class="space-y-3">
          <PluginCard v-for="plugin in store.marketPlugins.value"
                      :key="plugin.key"
                      :store="store"
                      :plugin="plugin"
                      layout="list"/>
        </div>

        <div class="mt-6 flex items-center justify-between text-xs text-content-tertiary">
          <span>{{ $t('store.market.footerCount', { count: store.marketPlugins.value.length }) }}</span>
          <span v-if="store.marketPlugins.value.length">{{ $t('store.market.singlePage') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Blocks, ChartLine, Database, LayoutGrid, Menu, Puzzle, RefreshCw, Search } from '@lucide/vue'
import type { PluginStoreContext } from '@/composables/usePluginStore'
import PluginCard from './PluginCard.vue'
import { Box, Code, Layers, Puzzle as PuzzleIcon, Terminal, Bell, Clock, LayoutGrid as GridIcon } from '@lucide/vue'

defineOptions({ name: 'MarketView' })

const props = defineProps<{ store: PluginStoreContext }>()
const store = props.store
const { t } = useI18n()

const ICONS: Record<string, any> = {
  all: GridIcon,
  Connector: Database,
  Convert: Code,
  Executor: Layers,
  FileSystem: Box,
  Notify: Bell,
  Scheduler: Clock,
  Parser: Terminal,
}

const categoryTabs = computed(() => [
  { id: 'all', name: t('store.category.all'), icon: GridIcon },
  ...store.categories.value.map(cat => ({
    id: cat.id,
    name: cat.label,
    icon: ICONS[cat.id] ?? PuzzleIcon,
  })),
])

const activeCategoryName = computed(() => {
  if (store.category.value === 'all') {
    return t('store.category.all')
  }
  return store.typeLabel(store.category.value)
})

const statusOptions = computed(() => [
  { label: t('store.filter.allStatus'), value: 'all' },
  { label: t('store.filter.installed'), value: 'installed' },
  { label: t('store.filter.uninstalled'), value: 'uninstalled' },
  { label: t('store.filter.updates'), value: 'updates' },
])

const sortOptions = computed(() => [
  { label: t('store.sort.popular'), value: 'popular' },
  { label: t('store.sort.rating'), value: 'rating', disabled: !store.hasRatings.value },
  { label: t('store.sort.az'), value: 'az' },
])
</script>
