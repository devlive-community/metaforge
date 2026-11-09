<template>
  <div class="flex flex-col gap-5 pb-6">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-content">{{ $t('store.updates.title') }}</h1>
        <p class="mt-2 text-[13px] text-content-secondary">{{ $t('store.updates.description') }}</p>
      </div>
      <MfButton variant="secondary" @click="store.switchView('market')">
        <Store :size="15"/>
        {{ $t('store.updates.backToMarket') }}
      </MfButton>
    </div>

    <MfAlert v-if="store.updatePlugins.value.length" variant="warning">
      {{ $t('store.updates.banner', { count: store.updatePlugins.value.length }) }}
    </MfAlert>

    <div v-if="store.updatePlugins.value.length === 0" class="flex min-h-64 items-center justify-center">
      <MfEmpty variant="data"
               :title="$t('store.updates.empty')"
               :description="$t('store.updates.emptyDescription')"/>
    </div>

    <div v-else class="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <div v-for="plugin in store.updatePlugins.value"
           :key="plugin.key"
           class="rounded-2xl border border-line bg-surface p-5">
        <div class="flex items-start gap-4">
          <PluginSymbol :plugin="plugin" :label="store.pluginLabel(plugin)" :official="store.official(plugin)" size="md"/>
          <div class="min-w-0 flex-1">
            <div class="flex items-center justify-between">
              <b class="text-base text-content">{{ store.pluginLabel(plugin) }}</b>
              <MfTag variant="warning" size="sm">{{ $t('store.updates.newVersion') }}</MfTag>
            </div>
            <p class="mt-1 text-xs text-content-secondary">
              v{{ plugin.installVersion }} →
              <span class="font-semibold text-primary">v{{ plugin.version }}</span>
            </p>
            <p class="mt-3 text-xs text-content-secondary">{{ store.pluginDescription(plugin) }}</p>
          </div>
        </div>

        <div class="mt-4 space-y-2 rounded-lg bg-surface-hover p-3 text-xs text-content-secondary">
          <ul v-if="plugin.changelog?.length" class="list-disc space-y-1 pl-4">
            <li v-for="(change, index) in plugin.changelog" :key="index">{{ change }}</li>
          </ul>
          <p v-else>{{ $t('store.updates.noChangelog') }}</p>
        </div>

        <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
          <span class="text-xs text-content-tertiary">
            {{ plugin.refs ? $t('store.updates.refCount', { count: plugin.refs.length }) : $t('store.reference.unavailable') }}
          </span>
          <MfButton variant="primary"
                    :loading="plugin.loading"
                    :disabled="!store.canInstall(plugin)"
                    @click="store.openConfirm('update', plugin)">
            <RefreshCw :size="15"/>
            {{ $t('store.updates.updateTo', { version: plugin.version }) }}
          </MfButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RefreshCw, Store } from '@lucide/vue'
import type { PluginStoreContext } from '@/composables/usePluginStore'
import PluginSymbol from './PluginSymbol.vue'

defineOptions({ name: 'UpdatesView' })

const props = defineProps<{ store: PluginStoreContext }>()
const store = props.store
</script>
