<template>
  <!-- Install / Update confirm -->
  <MfModal v-if="plugin && isInstallOrUpdate"
           :open="true"
           :title="isInstall ? $t('store.dialog.installTitle') : $t('store.dialog.updateTitle')"
           :subtitle="isInstall ? $t('store.dialog.installSubtitle') : $t('store.dialog.updateSubtitle')"
           width="535px"
           trap-focus
           @close="store.closeConfirm()">
    <div class="flex items-center gap-3">
      <PluginSymbol :plugin="plugin" :label="label" :official="official" size="md"/>
      <div>
        <b class="text-content">{{ label }}</b>
        <div class="mt-1 text-xs text-content-tertiary">
          {{ isInstall
              ? $t('store.dialog.installMeta', { version: plugin.version ?? '', size: store.sizeLabel(plugin), author: store.authorLabel(plugin) })
              : `v${ plugin.installVersion } → v${ plugin.version }` }}
        </div>
      </div>
    </div>

    <div class="mt-5 rounded-xl border border-line bg-surface-hover p-4">
      <h4 class="mb-3 text-xs font-semibold text-content">
        {{ isInstall ? $t('store.dialog.installPerms') : $t('store.dialog.updateScope') }}
      </h4>
      <div class="space-y-2">
        <div v-for="item in confirmList"
             :key="item"
             class="flex items-center gap-2 text-xs text-content-secondary">
          <Check :size="15" class="text-primary"/>
          {{ item }}
        </div>
      </div>
    </div>

    <MfAlert class="mt-5" variant="warning" :title="$t('store.dialog.operationNote')"/>

    <template #footer>
      <div class="flex justify-end gap-2">
        <MfButton @click="store.closeConfirm()">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="primary"
                  :loading="store.operating.value"
                  :disabled="!store.isAdmin.value"
                  @click="store.commit()">
          <template v-if="isInstall">
            <Download :size="15"/>
            {{ $t('store.dialog.confirmInstall') }}
          </template>
          <template v-else>
            <RefreshCw :size="15"/>
            {{ $t('store.dialog.confirmUpdate') }}
          </template>
        </MfButton>
      </div>
    </template>
  </MfModal>

  <!-- Uninstall -->
  <MfModal v-else-if="store.confirmAction.value === 'uninstall' && plugin"
           :open="true"
           :title="$t('store.dialog.uninstallTitle', { name: label })"
           :subtitle="$t('store.dialog.uninstallSubtitle')"
           width="570px"
           trap-focus
           @close="store.closeConfirm()">
    <template #title>
      <span class="flex items-center gap-3">
        <span class="flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-danger-subtle text-danger"><Trash2 :size="20"/></span>
        {{ $t('store.dialog.uninstallTitle', { name: label }) }}
      </span>
    </template>
    <div class="flex items-center gap-2 text-[13px] font-semibold"
         :class="store.uninstallBlocked.value ? 'text-danger' : plugin.refs ? 'text-primary' : 'text-warning'">
      <AlertTriangle v-if="store.uninstallBlocked.value" :size="19"/>
      <ShieldCheck v-else-if="plugin.refs" :size="19"/>
      <Info v-else :size="19"/>
      {{ store.uninstallBlocked.value
          ? $t('store.dialog.uninstallBlocked', { count: plugin.refs?.length ?? 0 })
          : $t(plugin.refs ? 'store.dialog.uninstallReady' : 'store.dialog.uninstallUnknown') }}
    </div>
    <p class="mt-2 text-xs text-content-secondary">
      {{ store.uninstallBlocked.value
          ? $t('store.dialog.uninstallBlockedBody')
          : $t('store.dialog.uninstallReadyBody') }}
    </p>

    <div v-if="store.uninstallBlocked.value"
         class="mt-4 overflow-hidden rounded-xl border border-danger/20">
      <div v-for="ref in plugin.refs"
           :key="`${ref.kind}-${ref.name}`"
           class="flex items-center justify-between border-b border-danger/10 px-4 py-3 text-xs last:border-b-0">
        <span class="font-semibold text-content">{{ ref.name }}</span>
        <MfTag variant="warning" size="sm">{{ store.referenceKind(ref.kind) }}</MfTag>
      </div>
    </div>
    <label v-else class="mt-5 block text-xs font-semibold text-content">
      {{ $t('store.dialog.confirmName', { name: label }) }}
      <MfInput :model-value="store.confirmName.value"
               :placeholder="$t('store.dialog.confirmNamePlaceholder')"
               class="mt-2"
               @update:model-value="store.setConfirmName"/>
    </label>

    <div class="mt-4 rounded-xl bg-surface-hover p-3 text-xs text-content-secondary">
      {{ $t('store.dialog.uninstallFooter') }}
    </div>

    <template #footer>
      <div class="flex items-center justify-end gap-2">
        <MfButton @click="store.closeConfirm()">{{ $t('common.cancel') }}</MfButton>
        <MfButton v-if="store.uninstallBlocked.value"
                  variant="secondary"
                  :disabled="!store.isAdmin.value"
                  @click="store.openConfirm('toggle', plugin, false)">
          <Power :size="15"/>
          {{ $t('store.dialog.changeToDisable') }}
        </MfButton>
        <MfButton v-else
                  variant="danger"
                  :disabled="store.confirmName.value !== label || !store.isAdmin.value"
                  :loading="store.operating.value"
                  @click="store.commit()">
          <Trash2 :size="15"/>
          {{ $t('store.dialog.confirmUninstall') }}
        </MfButton>
      </div>
    </template>
  </MfModal>

  <!-- Enable / Disable -->
  <MfModal v-else-if="store.confirmAction.value === 'toggle' && plugin"
           :open="true"
           :title="$t(store.confirmEnable.value ? 'store.dialog.enableTitle' : 'store.dialog.disableTitle', { name: label })"
           width="480px"
           trap-focus
           @close="store.closeConfirm()">
    <template #title>
      <span class="flex items-center gap-3">
        <Power :size="20" class="text-warning"/>
        {{ $t(store.confirmEnable.value ? 'store.dialog.enableTitle' : 'store.dialog.disableTitle', { name: label }) }}
      </span>
    </template>
    <p class="mt-4 text-sm leading-6 text-content-secondary">
      {{ store.confirmEnable.value
          ? $t('store.dialog.enableBody')
          : plugin.refs ? $t('store.dialog.disableBody', { count: plugin.refs.length }) : $t('store.dialog.disableUnknown') }}
    </p>

    <template #footer>
      <div class="flex justify-end gap-2">
        <MfButton @click="store.closeConfirm()">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="primary"
                  :loading="store.operating.value"
                  :disabled="!store.isAdmin.value"
                  @click="store.commit()">
          <Power :size="15"/>
          {{ $t(store.confirmEnable.value ? 'store.dialog.confirmEnable' : 'store.dialog.confirmDisable') }}
        </MfButton>
      </div>
    </template>
  </MfModal>

  <!-- Progress -->
  <MfModal v-else-if="store.pending.value"
           :open="true"
           :title="progressTitle"
           width="480px"
           trap-focus
           close-disabled
           :close-on-overlay="false"
           :close-on-esc="false">
    <div class="flex min-h-36 flex-col items-center justify-center gap-4" role="status">
      <MfSpinner size="lg" :label="progressTitle"/>
      <p class="text-center text-xs text-content-tertiary">{{ $t('store.dialog.progressBody') }}</p>
    </div>
  </MfModal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  AlertTriangle,
  Check,
  Download,
  Info,
  Power,
  RefreshCw,
  ShieldCheck,
  Trash2,
} from '@lucide/vue'
import type { PluginStoreContext } from '@/composables/usePluginStore'
import PluginSymbol from './PluginSymbol.vue'

defineOptions({ name: 'StoreDialogs' })

const props = defineProps<{ store: PluginStoreContext }>()
const store = props.store
const { t } = useI18n()

const plugin = computed(() => store.confirmPlugin.value)
const label = computed(() => (plugin.value ? store.pluginLabel(plugin.value) : ''))
const official = computed(() => (plugin.value ? store.official(plugin.value) : false))

const isInstallOrUpdate = computed(() =>
  (store.confirmAction.value === 'install' || store.confirmAction.value === 'update') && !!plugin.value)
const isInstall = computed(() => store.confirmAction.value === 'install')

const confirmList = computed(() => {
  const item = plugin.value
  if (!item) return []
  if (isInstall.value) return item.permissions?.length ? item.permissions : [t('store.drawer.noPermissions')]
  return [
    item.refs ? t('store.dialog.updateRef', { count: item.refs.length }) : t('store.reference.unavailable'),
    t('store.dialog.updateVerify'),
  ]
})

const progressTitle = computed(() => {
  const pending = store.pending.value
  if (!pending) return ''
  const key = {
    install: 'store.dialog.progressInstall',
    update: 'store.dialog.progressUpdate',
    uninstall: 'store.dialog.progressUninstall',
    toggle: 'store.dialog.progressToggle',
  }[pending.action]
  return t(key, { name: pending.label })
})
</script>
