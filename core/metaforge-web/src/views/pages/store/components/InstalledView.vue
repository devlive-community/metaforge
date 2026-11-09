<template>
  <div class="flex flex-col gap-5 pb-6">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-content">{{ $t('store.installed.title') }}</h1>
        <p class="mt-2 text-[13px] text-content-secondary">{{ $t('store.installed.description') }}</p>
      </div>
      <MfButton variant="primary" :loading="store.loading.value" @click="store.checkUpdates()">
        <RefreshCw :size="15"/>
        {{ $t('store.installed.checkUpdates') }}
      </MfButton>
    </div>

    <div class="grid grid-cols-3 gap-2 sm:gap-4">
      <div class="rounded-xl border border-line bg-surface p-3 sm:p-5">
        <span class="text-xs text-content-secondary">{{ $t('store.installed.statInstalled') }}</span>
        <div class="mt-2 text-3xl font-bold text-content">{{ store.installedCount.value }}</div>
      </div>
      <div class="rounded-xl border border-line bg-surface p-3 sm:p-5">
        <span class="text-xs text-content-secondary">{{ $t('store.installed.statRunning') }}</span>
        <div class="mt-2 text-3xl font-bold text-content">{{ store.runningCount.value }}</div>
      </div>
      <div class="rounded-xl border border-line bg-surface p-3 sm:p-5">
        <span class="text-xs text-content-secondary">{{ $t('store.installed.statUpdates') }}</span>
        <div class="mt-2 text-3xl font-bold text-warning">{{ store.updateCount.value }}</div>
      </div>
    </div>

    <div class="overflow-hidden rounded-2xl border border-line bg-surface">
      <div class="flex items-center justify-between border-b border-line p-4">
        <MfInput :model-value="store.query.value" :placeholder="$t('store.installed.searchPlaceholder')"
                 class="w-64 max-w-full" size="sm" @update:model-value="store.setQuery">
          <template #prefix><Search :size="16"/></template>
        </MfInput>
      </div>

      <MfTable :columns="columns"
               :data-source="store.installedPlugins.value"
               row-key="key"
               :loading="store.loading.value"
               :empty-text="$t(store.query.value.trim() ? 'store.market.emptyTitle' : 'store.installed.empty')"
               empty-variant="data">
        <template #cell-name="{ record }">
            <button type="button" class="flex w-full min-w-0 items-center gap-3 rounded-mf-md text-left focus-visible:outline-2 focus-visible:outline-primary"
                    @click="store.openDrawer(record)">
              <PluginSymbol :plugin="record" :label="store.pluginLabel(record)" :official="store.official(record)" size="sm"/>
              <div class="min-w-0">
                <b class="block truncate text-content">{{ store.pluginLabel(record) }}</b>
                <div class="truncate text-[11px] text-content-tertiary">{{ store.authorLabel(record) }}</div>
              </div>
            </button>
          </template>

          <template #cell-category="{ record }">
            {{ store.typeLabel(record.type || 'Other') }}
          </template>

          <template #cell-currentVersion="{ record }">
            <span class="font-semibold">{{ record.installVersion ? 'v' + record.installVersion : '—' }}</span>
          </template>

          <template #cell-latestVersion="{ record }">
            {{ record.version ? 'v' + record.version : '—' }}
            <span v-if="record.hasUpdate" class="text-warning">↑</span>
          </template>

          <template #cell-status="{ record }">
            <MfTag :variant="record.state === 'FAILED' ? 'danger' : record.enabled ? 'success' : 'default'" size="sm">
              {{ $t(store.statusKey(record, 'enabled')) }}
            </MfTag>
          </template>

          <template #cell-refs="{ record }">
            {{ record.refs ? record.refs.length + ' ' + $t('store.installed.refUnit') : '—' }}
          </template>

          <template #cell-action="{ record }">
            <div class="flex items-center gap-2">
              <MfButton v-if="record.hasUpdate"
                        variant="outline"
                        size="sm"
                        :loading="record.loading"
                        :disabled="!store.canInstall(record)"
                        @click="store.openConfirm('update', record)">
                {{ $t('store.action.update') }}
              </MfButton>
              <MfButton variant="secondary"
                        size="sm"
                        :disabled="!store.canToggle(record)"
                        @click="store.openConfirm('toggle', record, !record.enabled)">
                {{ record.enabled ? $t('store.action.disable') : $t('store.action.enable') }}
              </MfButton>
              <MfButton variant="secondary"
                        size="sm"
                        :loading="record.loading"
                        :disabled="!store.isAdmin.value || store.operating.value"
                        @click="store.openConfirm('uninstall', record)">
                <Trash2 :size="14"/>
                {{ $t('common.uninstall') }}
              </MfButton>
            </div>
          </template>
      </MfTable>

      <div class="border-t border-line p-4 text-xs text-content-tertiary">
        {{ $t('store.installed.footer', { count: store.installedPlugins.value.length }) }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw, Search, Trash2 } from '@lucide/vue'
import type { PluginStoreContext } from '@/composables/usePluginStore'
import type { MfTableColumn } from 'metaforge-ui'
import PluginSymbol from './PluginSymbol.vue'

defineOptions({ name: 'InstalledView' })

const props = defineProps<{ store: PluginStoreContext }>()
const store = props.store
const { t } = useI18n()

const columns = computed<MfTableColumn[]>(() => [
  { title: t('store.column.name'), dataIndex: 'label', key: 'name', width: 220, ellipsis: true },
  { title: t('store.column.category'), dataIndex: 'type', key: 'category', width: 120 },
  { title: t('store.column.currentVersion'), dataIndex: 'installVersion', key: 'currentVersion', width: 110 },
  { title: t('store.column.latestVersion'), dataIndex: 'version', key: 'latestVersion', width: 120 },
  { title: t('store.column.status'), dataIndex: 'enabled', key: 'status', width: 100 },
  { title: t('store.column.refs'), dataIndex: 'refs', key: 'refs', width: 90, align: 'center' },
  { title: t('store.column.action'), dataIndex: 'action', key: 'action', width: 280 },
])
</script>
