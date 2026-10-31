<template>
  <div class="relative min-h-screen">
    <MfAlert variant="warning" class="mb-2">
      {{ $t('common.plugin.systemVersion') }}
      <MfTag variant="danger" class="text-red-400">{{ version }}</MfTag>
    </MfAlert>

    <div v-if="loading" class="flex items-center justify-center py-16">
      <MfSpinner size="lg"/>
    </div>

    <!-- 原 a-tabs #rightExtra（MfTabs 暂无该插槽）：设置按钮放页签右上方 -->
    <div v-show="!loading" class="flex justify-end">
      <MfPopover trigger="click" placement="bottom">
        <template #content>
          <div class="p-2 space-y-2 flex flex-col">
            <div>
              <label class="mb-1 block text-sm text-content-secondary">{{ $t('common.plugin.metadata.url') }}</label>
              <MfInput v-model="applyMetadataUrl"/>
              <div class="mt-1 text-xs text-content-tertiary">{{ $t('common.plugin.metadata.description') }}</div>
            </div>

            <MfButton variant="primary" @click="onSave">{{ $t('common.save') }}</MfButton>
          </div>
        </template>

        <MfButton variant="ghost" size="sm" shape="circle">
          <Settings :size="15" />
        </MfButton>
      </MfPopover>
    </div>

    <MfTabs v-show="!loading" v-model:active-key="activeTab" class="mt-2">
      <MfTabPane v-for="type in pluginTypes" :key="type" :item-key="type" :tab="type">
        <div class="relative">
          <MfSpace direction="vertical" size="lg" fill :style="{ width: '100%' }">
            <MfAlert v-if="typeDescription(type)" :title="typeDescription(type)"/>

            <MfCard v-for="plugin in pluginsByType(type)" :key="plugin.key" class="w-full">
              <div class="grid grid-cols-24 p-3 px-6 items-center">
                <div class="col-span-4">
                  <!-- Logo and Name -->
                  <div class="flex flex-col items-center space-y-2 justify-between">
                    <MfAvatar class="bg-transparent cursor-pointer"
                              :size="64"
                              :src="plugin.logo"
                              :alt="plugin.i18nFormat ? $t(plugin.label) : plugin.label"
                              @click="onVisibleInfo(plugin, true)">
                    </MfAvatar>

                    <span class="text-base font-medium">
                      {{ plugin.i18nFormat ? $t(plugin.label) : plugin.label }}
                    </span>
                  </div>
                </div>

                <div class="col-span-18">
                  <!-- Plugin -->
                  <div class="flex items-center space-x-4 justify-between">
                    <div class="pl-8 flex flex-wrap">
                      <!-- Description -->
                      <div class="flex flex-col space-y-4">
                          <span class="text-sm text-gray-500">
                            {{ plugin.i18nFormat ? $t(plugin.description) : plugin.description }}
                          </span>

                        <!-- Support Version -->
                        <div class="flex space-x-2 text-sm text-gray-500">
                          <div class="flex items-center space-x-2">
                            {{ $t('common.plugin.list.supportVersion') }} :
                          </div>

                          <MfTag v-for="ver in plugin.supportVersion" :key="ver" variant="success">
                            {{ ver }}
                          </MfTag>
                        </div>

                        <!-- Version -->
                        <div class="flex space-x-2 text-sm text-gray-500">
                          <div class="space-x-1">
                            {{ $t('common.plugin.version') }} :
                            <MfTag>{{ plugin.version }}</MfTag>
                          </div>

                          <span v-if="plugin.installed" class="self-stretch w-px bg-line mx-1"/>

                          <div v-if="plugin.installed" class="space-x-1">
                            {{ $t('common.installVersion') }}:
                            <MfTag variant="info">{{ plugin.installVersion }}</MfTag>
                          </div>
                        </div>

                        <!-- Time -->
                        <div class="flex space-x-2 text-sm text-gray-500">
                          <div class="space-x-1">
                            {{ $t('common.releasedTime') }}：
                            <MfTag variant="warning">{{ plugin.released }}</MfTag>
                          </div>

                          <span v-if="plugin.installed" class="self-stretch w-px bg-line mx-1"/>

                          <div v-if="plugin.installed" class="space-x-1">
                            {{ $t('common.installTime') }}：
                            <MfTag variant="info">{{ plugin.installTime }}</MfTag>
                          </div>
                        </div>

                        <!-- Other -->
                        <div class="flex space-x-2 text-sm text-gray-500">
                          <div class="space-x-1">
                            {{ $t('common.author') }}： {{ plugin.author }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="col-span-2">
                  <!-- Action -->
                  <MfButton :variant="plugin.installed ? 'danger' : 'primary'"
                            :loading="plugin.loading"
                            @click="plugin.installed ? onUninstall(plugin) : onInstall(plugin)">
                    <component :size="15" :is="plugin.installed ? Trash2 : Plus" />
                    {{ plugin.installed ? $t('common.uninstall') : $t('common.install') }}
                  </MfButton>
                </div>
              </div>
            </MfCard>
          </MfSpace>
        </div>
      </MfTabPane>
    </MfTabs>
  </div>

  <PluginInfo v-if="infoVisible && info"
              :info="info"
              :is-visible="infoVisible"
              @close="onVisibleInfo(null, false)">
  </PluginInfo>
</template>

<script setup lang="ts">
import { computed, onBeforeMount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { useI18nHandler } from '@/i18n/I18n'
import { PackageUtils } from '@/utils/package.ts'
import PluginService from '@/services/plugin.ts'
import PluginInfo from '@/views/pages/store/components/PluginInfo.vue'
import { Plus, Settings, Trash2 } from '@lucide/vue'

interface MetadataItem
{
  key: string
  label: string
  description: string
  logo: string
  type: string
  released: string
  i18nFormat: boolean
  version: string
  supportVersion: string[]
  author: string
  installed: boolean
  installTime: string
  installVersion: string
  url: string
  loading: boolean
}

interface Metadata
{
  key: string
  label: string
  description: string
  i18nFormat: boolean
  children: MetadataItem[]
}

defineOptions({ name: 'StoreHome' })

const DEFAULT_METADATA_URL = 'https://cdn.north.devlive.org/applications/metaforge/metadata/metadata.json'

// URL validation helper
const isValidUrl = (urlString: string): boolean => {
  try {
    new URL(urlString)
    return true
  }
  catch {
    return false
  }
}

const { t } = useI18n()
// @ts-ignore
const { loadingState } = useI18nHandler()
const loading = ref(false)
const metadata = ref<Metadata | null>(null)
const version = ref(PackageUtils.get('version'))
const info = ref<MetadataItem | null>(null)
const infoVisible = ref(false)

// Default to first plugin type found
const activeTab = ref('')

// Get all unique plugin types
const pluginTypes = computed(() => {
  if (!metadata.value?.children?.length) {
    return []
  }

  const types = new Set(metadata.value.children.map(plugin => plugin.type))
  return Array.from(types)
})

// Group plugins by type
const pluginsByType = (type: string) => {
  if (!metadata.value?.children?.length) {
    return []
  }
  return metadata.value.children.filter(plugin => plugin.type === type)
}

// Get description for a type (could be extended to add custom descriptions per type)
const typeDescription = (type: string) => {
  const defaultDescriptions: Record<string, string> = {
    'FileSystem': 'File system plugins provide storage integrations',
    'Connector': 'Connector plugins enable data source connections',
    'Visualization': 'Visualization plugins provide data display capabilities'
    // Add more defaults for other types
  }

  return defaultDescriptions[type] || `${ type } plugins for MetaForge`
}

const metadataUrl = computed({
  get: () => {
    const savedUrl = localStorage.getItem('metadataUrl')
    return savedUrl && isValidUrl(savedUrl) ? savedUrl : DEFAULT_METADATA_URL
  },
  set: (value: string) => {
    const trimmedUrl = value?.trim()
    if (trimmedUrl && isValidUrl(trimmedUrl)) {
      localStorage.setItem('metadataUrl', trimmedUrl)
    }
    else {
      localStorage.removeItem('metadataUrl')
    }
  }
})
const applyMetadataUrl = ref(metadataUrl.value)

watch(loadingState, async (newVal) => {
  if (!newVal && !metadata.value) {
    await loadMetadata()
  }
})

watch(pluginTypes, (newTypes) => {
  // Set active tab to first type when types are loaded
  if (newTypes.length && !activeTab.value) {
    activeTab.value = newTypes[0]
  }
})

const loadMetadata = async () => {
  loading.value = true
  try {
    const url = metadataUrl.value
    if (!isValidUrl(url)) {
      throw new Error('Invalid metadata URL')
    }

    const response = await fetch(url)
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${ response.status }`)
    }

    const data = await response.json()
    metadata.value = data

    const installResponse = await PluginService.getPlugins()
    if (!installResponse.status) {
      MfMessage.error(installResponse.message)
      return
    }

    // Bind installation information
    metadata.value!.children.forEach(item => {
      item.loading = false
      installResponse.data.some((installedPlugin: { key: string, loadTime: string, version: string }) => {
        if (installedPlugin.key === item.key) {
          item.installed = true
          item.installTime = installedPlugin.loadTime
          item.installVersion = installedPlugin.version
          return true
        }
        return false
      })
    })
  }
  catch (error: any) {
    if (error instanceof TypeError) {
      MfMessage.error(t('common.tip.pageNotNetwork'))
    }
    else {
      MfMessage.error(`${ t('common.pageNotFoundTip') }: ${ error.message }`)
    }

    // Reset to default URL if current URL is invalid
    if (!isValidUrl(metadataUrl.value)) {
      metadataUrl.value = DEFAULT_METADATA_URL
      await loadMetadata()
    }
  }
  finally {
    loading.value = false
  }
}

// Install plugin
const onInstall = async (item: MetadataItem) => {
  if (item.loading) {
    return
  }
  try {
    item.loading = true
    const installResponse = await PluginService.install({ name: item.key, url: item.url })
    if (installResponse.status) {
      MfMessage.success(t('common.installSuccess'))
    }
    else {
      MfMessage.error(installResponse.message)
    }

    await loadMetadata()
  }
  finally {
    item.loading = false
  }
}

// Uninstall plugin
const onUninstall = async (item: MetadataItem) => {
  if (item.loading) {
    return
  }
  try {
    item.loading = true
    const unInstallResponse = await PluginService.uninstall(item.label)
    if (unInstallResponse.status) {
      MfMessage.success(t('common.uninstallSuccess'))
    }
    else {
      MfMessage.error(unInstallResponse.message)
    }

    await loadMetadata()
  }
  finally {
    item.loading = false
  }
}

const onSave = async () => {
  try {
    const url = applyMetadataUrl.value.trim()
    if (!isValidUrl(url)) {
      MfMessage.error('Invalid Url')
      return
    }

    metadataUrl.value = url
    MfMessage.success(t('common.successfully'))

    await loadMetadata()
  }
  catch (error) {
    MfMessage.error(t('common.error'))
  }
}

const onVisibleInfo = (item: MetadataItem | null, opened?: boolean) => {
  infoVisible.value = !!opened
  if (opened) {
    info.value = item
  }
}

onBeforeMount(() => {
  if (!loadingState.value) {
    loadMetadata()
  }
})
</script>
