<template>
  <MfDrawer :open="true"
            width="min(485px, 100vw)"
            :title="label"
            body-class="flex flex-col overflow-hidden"
            placement="right"
            @close="store.closeDrawer()">
    <template #title>
      <div class="flex items-center gap-3">
        <PluginSymbol :plugin="plugin" :label="label" :official="official" size="md"/>
        <div class="min-w-0">
          <div class="truncate text-[19px] font-bold text-content">{{ label }}</div>
          <div class="mt-1 truncate text-xs text-content-tertiary">{{ plugin.en || plugin.key }} · {{ store.authorLabel(plugin) }}</div>
        </div>
      </div>
    </template>

    <div class="flex min-h-0 flex-1 flex-col">
      <!-- Status + stats + actions -->
      <div class="shrink-0 border-b border-line px-4 py-5 sm:px-6">
        <div class="flex flex-wrap items-center gap-2">
          <MfTag variant="info" size="sm">{{ store.typeLabel(plugin.type || 'Other') }}</MfTag>
          <MfTag v-if="plugin.installed"
                 :variant="plugin.enabled ? 'success' : 'default'"
                 size="sm">
            {{ plugin.state === 'FAILED' ? $t('store.status.failed') : $t(plugin.enabled ? 'store.drawer.installedEnabled' : 'store.drawer.installedDisabled') }}
          </MfTag>
          <MfTag v-else size="sm">{{ $t('store.drawer.notInstalled') }}</MfTag>
          <MfTag v-if="plugin.hasUpdate" variant="warning" size="sm">{{ $t('store.badge.updatable') }}</MfTag>
        </div>

        <div class="mt-4 grid grid-cols-3 gap-2 text-center">
          <div class="rounded-xl bg-surface-hover p-3">
            <div class="flex items-center justify-center gap-1 font-bold text-content">{{ plugin.rating ?? '—' }}<Star v-if="plugin.rating != null" :size="14" aria-hidden="true"/></div>
            <div class="text-[11px] text-content-tertiary">{{ $t('store.drawer.rating') }}</div>
          </div>
          <div class="rounded-xl bg-surface-hover p-3">
            <div class="font-bold text-content">{{ store.downloadsLabel(plugin) }}</div>
            <div class="text-[11px] text-content-tertiary">{{ $t('store.drawer.downloads') }}</div>
          </div>
          <div class="rounded-xl bg-surface-hover p-3">
            <div class="font-bold text-content">{{ displayedVersion ? 'v' + displayedVersion : '—' }}</div>
            <div class="text-[11px] text-content-tertiary">
              {{ plugin.installed ? $t('store.drawer.installedVersion') : $t('store.drawer.latestVersion') }}
            </div>
          </div>
        </div>

        <div class="mt-5 flex flex-wrap gap-2">
          <template v-if="plugin.installed">
            <MfButton v-if="plugin.hasUpdate"
                      variant="primary"
                      :loading="plugin.loading"
                      :disabled="!store.canInstall(plugin)"
                      @click="store.openConfirm('update', plugin)">
              <RefreshCw :size="15"/>
              {{ $t('store.drawer.updatePlugin') }}
            </MfButton>
            <MfButton v-else
                      variant="primary"
                      :disabled="!store.canToggle(plugin)"
                      @click="store.openConfirm('toggle', plugin, !plugin.enabled)">
              <Power :size="15"/>
              {{ plugin.enabled ? $t('store.action.disable') : $t('store.action.enable') }}
            </MfButton>
            <MfButton variant="secondary"
                      :loading="plugin.loading"
                      :disabled="!store.isAdmin.value || store.operating.value"
                      @click="store.openConfirm('uninstall', plugin)">
              <Trash2 :size="15"/>
              {{ $t('common.uninstall') }}
            </MfButton>
          </template>
          <MfButton v-else
                    variant="primary"
                    :loading="plugin.loading"
                    :disabled="!store.canInstall(plugin)"
                    @click="store.openConfirm('install', plugin)">
            <Download :size="15"/>
            {{ $t('store.drawer.installPlugin') }}
          </MfButton>
        </div>
      </div>

      <MfTabs fill :active-key="store.drawerTab.value" class="[&_[role=tablist]]:px-4 sm:[&_[role=tablist]]:px-6" @update:active-key="store.setDrawerTab">
        <MfTabPane item-key="overview" :tab="$t('store.tab.overview')">
          <div class="px-4 py-5 text-[13px] sm:px-6">
            <h4 class="mb-3 font-bold text-content">{{ $t('store.drawer.intro') }}</h4>
            <p class="leading-6 text-content-secondary">{{ store.pluginDescription(plugin) }}</p>

            <h4 class="mb-3 mt-6 font-bold text-content">{{ $t('store.drawer.features') }}</h4>
            <div v-if="plugin.features?.length" class="space-y-3">
              <div v-for="feature in plugin.features" :key="feature" class="flex items-center gap-2 text-content-secondary">
                <Check :size="17" class="shrink-0 text-primary"/>
                {{ store.featureLabel(feature) }}
              </div>
            </div>
            <MfEmpty v-else variant="data" size="sm" :title="$t('store.drawer.noFeatures')"/>

            <h4 class="mb-3 mt-7 font-bold text-content">{{ $t('store.drawer.basicInfo') }}</h4>
            <div class="flex justify-between gap-3 border-b border-line py-2.5">
              <span class="text-content-tertiary">{{ $t('store.field.author') }}</span>
              <span class="text-content">{{ store.authorLabel(plugin) }}</span>
            </div>
            <div class="flex justify-between gap-3 border-b border-line py-2.5">
              <span class="text-content-tertiary">{{ $t('store.field.category') }}</span>
              <span class="text-content">{{ store.typeLabel(plugin.type || 'Other') }}</span>
            </div>
            <div class="flex justify-between gap-3 border-b border-line py-2.5">
              <span class="text-content-tertiary">{{ $t('store.field.size') }}</span>
              <span class="text-content">{{ store.sizeLabel(plugin) }}</span>
            </div>
            <div class="flex justify-between gap-3 border-b border-line py-2.5">
              <span class="text-content-tertiary">{{ $t('store.field.requires') }}</span>
              <span class="text-content">{{ store.requiresLabel(plugin) }}</span>
            </div>
            <div class="flex justify-between gap-3 border-b border-line py-2.5">
              <span class="text-content-tertiary">{{ $t('store.field.latest') }}</span>
              <span class="text-content">{{ plugin.version ? 'v' + plugin.version : '—' }}</span>
            </div>
            <div v-if="plugin.supportVersion?.length" class="flex justify-between gap-3 border-b border-line py-2.5">
              <span class="text-content-tertiary">{{ $t('common.plugin.list.supportVersion') }}</span>
              <span class="flex flex-wrap justify-end gap-1">
                <MfTag v-for="ver in plugin.supportVersion" :key="ver" variant="success" size="sm">{{ ver }}</MfTag>
              </span>
            </div>
          </div>
        </MfTabPane>

        <MfTabPane item-key="versions" :tab="$t('store.tab.versions')">
          <div class="px-4 py-5 text-[13px] sm:px-6">
            <h4 class="mb-3 font-bold text-content">{{ $t('store.drawer.versionHistory') }}</h4>
            <div class="space-y-5 border-l-2 border-primary/20 pl-4">
              <div v-if="plugin.version">
                <b class="text-content">v{{ plugin.version }}
                  <MfTag variant="success" size="sm">{{ $t('store.drawer.latest') }}</MfTag>
                </b>
                <ul v-if="plugin.changelog?.length" class="mt-2 list-disc space-y-1 pl-4 text-xs text-content-secondary">
                  <li v-for="(change, index) in plugin.changelog" :key="index">{{ change }}</li>
                </ul>
                <p v-else class="mt-2 text-xs text-content-tertiary">{{ $t('store.updates.noChangelog') }}</p>
              </div>
              <div v-if="plugin.installed && plugin.installVersion && plugin.installVersion !== plugin.version">
                <b class="text-content">v{{ plugin.installVersion }}</b>
                <p class="mt-2 text-xs text-content-tertiary">{{ $t('store.drawer.installedVersion') }}</p>
              </div>
              <div v-if="plugin.released">
                <b class="text-content">{{ $t('common.releasedTime') }}</b>
                <p class="mt-2 text-xs text-content-tertiary">{{ plugin.released }}</p>
              </div>

            </div>
          </div>
        </MfTabPane>

        <MfTabPane item-key="perms" :tab="$t('store.tab.perms')">
          <div class="px-4 py-5 text-[13px] sm:px-6">
            <h4 class="mb-3 font-bold text-content">{{ $t('store.drawer.permissions') }}</h4>
            <div v-if="plugin.permissions?.length" class="space-y-2">
              <div v-for="perm in plugin.permissions"
                   :key="perm"
                   class="flex items-center gap-2 rounded-lg border border-line bg-surface-hover p-3">
                <ShieldCheck :size="16" class="text-primary"/>
                {{ perm }}
              </div>
            </div>
            <MfEmpty v-else variant="data" size="sm" :title="$t('store.drawer.noPermissions')"/>

            <h4 class="mb-3 mt-6 font-bold text-content">{{ $t('store.drawer.deps') }}</h4>
            <div v-if="plugin.deps?.length" class="space-y-2">
              <div v-for="dep in plugin.deps" :key="dep" class="rounded-lg border border-line p-3 text-content-secondary">
                <Puzzle :size="15" class="mr-2 inline text-content-tertiary"/>
                {{ dep }}
              </div>
            </div>
            <p v-else class="text-content-tertiary">{{ $t('store.drawer.noDeps') }}</p>

            <h4 class="mb-3 mt-6 font-bold text-content">
              {{ $t('store.drawer.refs') }}<template v-if="plugin.refs"> ({{ plugin.refs.length }})</template>
            </h4>
            <div v-if="plugin.refs?.length">
              <div v-for="ref in plugin.refs"
                   :key="`${ref.kind}-${ref.name}`"
                   class="flex justify-between border-b border-line py-2">
                <b class="text-content">{{ ref.name }}</b>
                <MfTag size="sm">{{ store.referenceKind(ref.kind) }}</MfTag>
              </div>
            </div>
            <p v-else class="text-content-tertiary">{{ $t(plugin.refs ? 'store.drawer.noRefs' : 'store.reference.unavailable') }}</p>
          </div>
        </MfTabPane>

      </MfTabs>
    </div>

    <template #footer>
      <div class="flex w-full min-w-0 items-center justify-between gap-3 text-xs text-content-tertiary">
        <span class="flex min-w-0 items-center gap-1 truncate">
          <ShieldCheck :size="14" class="shrink-0"/>
          {{ $t('store.drawer.footerHint') }}
        </span>
        <MfButton variant="secondary" size="sm" class="shrink-0" @click="store.closeDrawer()">
          {{ $t('common.close') }}
        </MfButton>
      </div>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Check,
  Download,
  Power,
  Puzzle,
  RefreshCw,
  ShieldCheck,
  Star,
  Trash2,
} from '@lucide/vue'
import type { PluginStoreContext } from '@/composables/usePluginStore'
import PluginSymbol from './PluginSymbol.vue'

defineOptions({ name: 'PluginDetailDrawer' })

const props = defineProps<{ store: PluginStoreContext }>()
const store = props.store

const plugin = computed(() => store.drawerPlugin.value!)
const label = computed(() => store.pluginLabel(plugin.value))
const official = computed(() => store.official(plugin.value))

const displayedVersion = computed(() => plugin.value.installed ? plugin.value.installVersion : plugin.value.version)
</script>
