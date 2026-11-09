<template>
  <aside class="flex min-h-0 shrink-0 flex-col border-b border-line bg-surface md:w-[234px] md:border-r md:border-b-0">
    <div class="hidden border-b border-line p-5 md:block">
      <div class="flex items-center gap-2 text-lg font-bold text-content">
        <Store :size="20" class="text-primary"/>
        {{ $t('store.title') }}
      </div>
    </div>

    <nav :aria-label="$t('store.title')" class="flex gap-1 overflow-x-auto px-2 py-2 md:flex-col md:overflow-visible md:px-3 md:py-4">
      <button v-for="item in views"
              :key="item.id"
              type="button"
              class="flex shrink-0 items-center gap-2 whitespace-nowrap rounded-mf-lg px-3.5 py-2.5 text-left md:w-full md:gap-3 md:py-3 text-[13px] font-semibold text-content-secondary transition-colors hover:bg-surface-hover"
              :aria-current="store.view.value === item.id ? 'page' : undefined"
              :class="store.view.value === item.id && 'bg-primary/10 text-primary'"
              @click="store.switchView(item.id)">
        <component :is="item.icon" :size="17"/>
        <span class="flex-1">{{ item.name }}</span>
        <span v-if="item.count !== null"
              class="rounded-mf-full px-2 py-0.5 text-[11px]"
              :class="store.view.value === item.id ? 'bg-primary/15 text-primary' : 'bg-surface-hover text-content-tertiary'">
          {{ item.count }}
        </span>
      </button>
    </nav>

    <div class="mx-5 hidden border-t border-line md:block"></div>

    <div class="hidden min-h-0 flex-1 overflow-y-auto p-5 pt-4 md:block">
      <div class="mb-4 text-xs font-bold uppercase tracking-wider text-content-tertiary">{{ $t('store.sidebar.types') }}</div>
      <div class="space-y-1">
        <button v-for="cat in categoryItems"
                :key="cat.id"
                type="button"
                class="flex w-full items-center gap-2 rounded-mf-md px-2 py-2 text-left text-xs text-content-secondary transition-colors hover:bg-surface-hover"
                :class="store.view.value === 'market' && store.category.value === cat.id && 'bg-primary/10 text-primary'"
                @click="store.pickCategory(cat.id)">
          <component :is="cat.icon" :size="15" class="shrink-0"/>
          <span class="flex-1 truncate">{{ cat.label }}</span>
          <span class="text-content-tertiary">{{ cat.count }}</span>
        </button>
      </div>
    </div>

    <div class="m-4 hidden rounded-mf-lg border border-primary/20 bg-primary/5 p-4 md:block">
      <div class="flex items-center gap-2 font-semibold text-primary">
        <ShieldCheck :size="16"/>
        {{ $t('store.sidebar.safetyTitle') }}
      </div>
      <p class="mt-2 text-xs leading-5 text-primary/80">{{ $t('store.sidebar.safetyBody') }}</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Blocks, Box, Clock, Code, Database, History, Layers, Puzzle, RefreshCw, ShieldCheck, Store, Terminal, Bell } from '@lucide/vue'
import type { PluginStoreContext } from '@/composables/usePluginStore'

defineOptions({ name: 'StoreSidebar' })

const props = defineProps<{ store: PluginStoreContext }>()
const store = props.store
const { t } = useI18n()

const CATEGORY_ICONS: Record<string, any> = {
  Connector: Database,
  Convert: Code,
  Executor: Layers,
  FileSystem: Box,
  Notify: Bell,
  Scheduler: Clock,
  Parser: Terminal,
  Other: Puzzle,
}

const views = computed(() => [
  { id: 'market', name: t('store.nav.market'), icon: Store, count: null },
  { id: 'installed', name: t('store.nav.installed'), icon: Blocks, count: store.installedCount.value },
  { id: 'updates', name: t('store.nav.updates'), icon: RefreshCw, count: store.updateCount.value },
  { id: 'history', name: t('store.nav.history'), icon: History, count: null },
] as const)

const categoryItems = computed(() => store.categories.value.map(cat => ({
  ...cat,
  icon: CATEGORY_ICONS[cat.id] ?? Puzzle,
})))
</script>
