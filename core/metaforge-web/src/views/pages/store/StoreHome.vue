<template>
  <div class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden md:flex-row">
    <StoreSidebar :store="store"/>

    <section class="flex min-h-0 min-w-0 flex-1 flex-col overflow-y-auto overscroll-contain bg-surface-hover px-2 py-4 md:py-6" :aria-label="$t('store.title')">
      <MfAlert v-if="store.installedFailed.value" variant="warning" class="mb-4 shrink-0" :title="$t('store.market.installedFailed')">
        <MfButton size="sm" :loading="store.loading.value" @click="store.refresh()">{{ $t('common.refresh') }}</MfButton>
      </MfAlert>
      <div v-if="store.loading.value && !store.hasCatalog.value" class="flex h-full min-h-96 items-center justify-center">
        <MfSpinner size="lg" :label="$t('common.loading')"/>
      </div>

      <template v-else>
        <MarketView v-if="store.view.value === 'market'" :store="store"/>
        <InstalledView v-else-if="store.view.value === 'installed'" :store="store"/>
        <UpdatesView v-else-if="store.view.value === 'updates'" :store="store"/>
        <HistoryView v-else-if="store.view.value === 'history'" :store="store"/>
      </template>
    </section>

    <PluginDetailDrawer v-if="store.drawerPlugin.value" :store="store"/>

    <StoreDialogs :store="store"/>
  </div>
</template>

<script setup lang="ts">
import { usePluginStore } from '@/composables/usePluginStore'
import StoreSidebar from './components/StoreSidebar.vue'
import MarketView from './components/MarketView.vue'
import InstalledView from './components/InstalledView.vue'
import UpdatesView from './components/UpdatesView.vue'
import HistoryView from './components/HistoryView.vue'
import PluginDetailDrawer from './components/PluginDetailDrawer.vue'
import StoreDialogs from './components/StoreDialogs.vue'

defineOptions({ name: 'StoreHome' })

const store = usePluginStore()
</script>
