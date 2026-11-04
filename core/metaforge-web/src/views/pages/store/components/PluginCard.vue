<template>
  <article class="relative flex min-w-0 rounded-2xl border border-line bg-surface p-4 transition-colors hover:border-primary/40 focus-within:border-primary/40"
           :class="layout === 'list' ? 'items-center gap-4' : 'min-h-[205px] flex-col hover:shadow-mf-md'">
    <button type="button"
            class="absolute inset-0 rounded-2xl focus-visible:outline-2 focus-visible:outline-primary"
            :aria-label="$t('store.action.openDetails', { name: label })"
            @click="store.openDrawer(plugin)"/>
    <div class="pointer-events-none flex min-w-0 items-start gap-3" :class="layout === 'list' && 'flex-1'">
      <PluginSymbol :plugin="plugin" :label="label" :official="official" :size="layout === 'list' ? 'sm' : 'md'"/>
      <div class="min-w-0 flex-1">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="min-w-0 truncate text-sm font-bold text-content">{{ label }}</h3>
          <MfTag v-if="layout === 'grid' && plugin.author" :variant="official ? 'success' : 'info'" size="sm">
            {{ official ? $t('store.badge.official') : $t('store.badge.community') }}
          </MfTag>
        </div>
        <p v-if="layout === 'list'" class="mt-1 truncate text-xs text-content-tertiary">{{ description }}</p>
        <p v-else class="mt-1 text-[11px] text-content-tertiary">
          {{ typeLabel }} · {{ plugin.version ? 'v' + plugin.version : '—' }}
        </p>
      </div>
      <MfTag v-if="layout === 'grid' && plugin.installed" :variant="plugin.enabled ? 'success' : 'default'" size="sm" class="shrink-0">
        {{ statusLabel }}
      </MfTag>
    </div>

    <template v-if="layout === 'grid'">
      <p class="pointer-events-none mt-3 line-clamp-2 min-h-10 text-xs leading-5 text-content-secondary">{{ description }}</p>
      <div class="pointer-events-none mt-auto flex min-h-8 items-center gap-3 border-t border-line pt-3 text-[11px] text-content-tertiary">
        <span :class="plugin.rating != null && 'font-semibold text-warning'">★ {{ plugin.rating ?? '—' }}</span>
        <span class="flex items-center gap-1"><Download :size="12"/>{{ store.downloadsLabel(plugin) }}</span>
        <MfTag v-if="plugin.hasUpdate" variant="warning" size="sm" class="ml-auto">{{ $t('store.badge.updatable') }}</MfTag>
      </div>
    </template>
    <template v-else>
      <MfTag variant="info" size="sm" class="pointer-events-none hidden shrink-0 sm:inline-flex">{{ typeLabel }}</MfTag>
      <span class="pointer-events-none hidden shrink-0 text-xs text-content-tertiary sm:inline">{{ plugin.version ? 'v' + plugin.version : '—' }}</span>
    </template>

    <div class="flex shrink-0 items-center gap-2" :class="layout === 'grid' && 'justify-between pt-3'">
      <span v-if="layout === 'grid'" class="pointer-events-none min-w-0 truncate text-[11px] text-content-tertiary">{{ store.authorLabel(plugin) }}</span>
      <MfButton v-if="!plugin.installed || plugin.hasUpdate"
                :variant="plugin.installed ? 'outline' : 'primary'"
                size="sm"
                class="relative"
                :loading="plugin.loading"
                :disabled="!store.canInstall(plugin)"
                @click="store.openConfirm(plugin.installed ? 'update' : 'install', plugin)">
        <RefreshCw v-if="plugin.installed" :size="14"/>
        <Download v-else :size="14"/>
        {{ $t(plugin.installed ? 'store.action.update' : 'common.install') }}
      </MfButton>
      <MfButton v-else-if="layout === 'grid'" variant="secondary" size="sm" class="relative" @click="store.openDrawer(plugin)">
        {{ $t('store.action.detail') }}
      </MfButton>
      <MfTag v-else :variant="plugin.enabled ? 'success' : 'default'" size="sm" class="pointer-events-none">{{ statusLabel }}</MfTag>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Download, RefreshCw } from '@lucide/vue'
import type { StoreLayout, StorePlugin } from '@/model/store'
import type { PluginStoreContext } from '@/composables/usePluginStore'
import PluginSymbol from './PluginSymbol.vue'

const { store, plugin, layout = 'grid' } = defineProps<{ store: PluginStoreContext; plugin: StorePlugin; layout?: StoreLayout }>()
const { t } = useI18n()
const label = computed(() => store.pluginLabel(plugin))
const description = computed(() => store.pluginDescription(plugin))
const official = computed(() => store.official(plugin))
const typeLabel = computed(() => store.typeLabel(plugin.type || 'Other'))
const statusLabel = computed(() => t(store.statusKey(plugin, 'installed')))
</script>
