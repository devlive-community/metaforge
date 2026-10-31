<template>
  <MfModal v-model:open="visible"
           :width="currentStep === 0 ? '1180px' : '720px'"
           :close-on-overlay="false"
           class="dc-src-modal">
    <!-- 头部 -->
    <div class="dc-src__head">
      <div class="dc-src__head-title" :title="wizardTitle">{{ wizardTitle }}</div>
      <div class="dc-src__head-desc">{{ $t('source.wizard.desc') }}</div>
    </div>

    <!-- 步骤条 -->
    <div class="dc-src__steps flex items-center gap-3">
      <template v-for="(step, index) in stepDefs" :key="step.titleKey">
        <div class="flex items-center gap-2" :class="index <= currentStep ? 'text-primary' : 'text-content-tertiary'">
          <span class="inline-flex size-6 shrink-0 items-center justify-center rounded-full border text-xs font-medium"
                :class="index <= currentStep ? 'border-primary bg-primary text-on-primary' : 'border-line'">
            {{ index + 1 }}
          </span>
          <span class="text-xs leading-tight">
            <span class="block whitespace-nowrap font-medium" :class="index <= currentStep ? 'text-primary' : 'text-content-secondary'">{{ $t(step.titleKey) }}</span>
            <span class="block whitespace-nowrap text-content-tertiary">{{ $t(step.descKey) }}</span>
          </span>
        </div>
        <span v-if="index < stepDefs.length - 1" class="h-px w-8 shrink-0 bg-line"></span>
      </template>
    </div>

    <MfAlert v-if="testInfo && testInfo.message" variant="danger" :title="testInfo.message" class="mb-3"/>

    <!-- 步骤 1：选择数据源类型 -->
    <div v-if="currentStep === 0" class="dc-src__type-step">
      <!-- 左侧分类 -->
      <div class="dc-src__catalog flex flex-col gap-1">
        <div v-for="cat in categoryList"
             :key="cat.key"
             class="dc-src__cat-item"
             :class="{ 'dc-src__cat-item--active': activeCategory === cat.key }"
             @click="activeCategory = cat.key">
          <component :is="cat.icon" class="dc-src__cat-icon"/>
          <span class="dc-src__cat-label">{{ $t(cat.labelKey) }}</span>
          <span class="dc-src__cat-count">{{ cat.count }}</span>
        </div>
      </div>

      <!-- 中间：搜索 + 筛选 + 网格 -->
      <div class="dc-src__middle">
        <MfInput v-model="searchText"
                 class="dc-src__search"
                 :placeholder="$t('source.search.placeholder')">
          <template #prefix>
            <SearchOutlined :style="{ fontSize: '14px', color: 'var(--dc-text-secondary)' }"/>
          </template>
        </MfInput>

        <div class="dc-src__chips">
          <span v-for="chip in chips"
                :key="chip.key"
                class="dc-src__chip"
                :class="{ 'dc-src__chip--active': activeChip === chip.key }"
                @click="activeChip = chip.key">
            {{ $t(chip.labelKey) }}
          </span>
        </div>

        <div class="dc-src__grid">
          <div v-for="src in filteredSources"
               :key="src.name"
               class="dc-src__grid-item"
               :class="{ 'dc-src__grid-item--selected': formState?.type === src.name }"
               @click="selectSource(src.name)">
            <span v-if="formState?.type === src.name" class="dc-src__grid-check">
              <CircleCheck :size="14"/>
            </span>
            <img class="dc-src__grid-logo" :src="'/static/images/plugin/' + src.name.toLowerCase() + '.svg'" :alt="src.name">
            <span class="dc-src__grid-name">{{ src.name }}</span>
          </div>
          <div v-if="filteredSources.length === 0" class="dc-src__grid-empty">
            {{ $t('common.noData') }}
          </div>
        </div>
      </div>

      <!-- 右侧详情面板 -->
      <div class="dc-src__detail">
        <template v-if="selectedSource">
          <div class="dc-src__detail-logo">
            <img :src="'/static/images/plugin/' + selectedSource.name.toLowerCase() + '.svg'" :alt="selectedSource.name">
            <MfBadge v-if="catalogEntry.popular" variant="success" :count="$t('source.badge.popular')"/>
          </div>
          <div class="dc-src__detail-name">{{ selectedSource.name }}</div>
          <p class="dc-src__detail-desc">{{ catalogEntry.desc[detailLocale] }}</p>

          <template v-if="catalogEntry.features">
            <div class="dc-src__detail-sub">{{ $t('source.common.features') }}</div>
            <div v-for="(feature, index) in catalogEntry.features[detailLocale] || catalogEntry.features.zh"
                 :key="index"
                 class="dc-src__detail-feature">
              <CircleCheck :size="12" class="dc-src__detail-feature-icon"/>
              <span>{{ feature }}</span>
            </div>
          </template>

          <template v-if="catalogEntry.scenes">
            <div class="dc-src__detail-sub">{{ $t('source.common.scenes') }}</div>
            <div class="dc-src__detail-scenes">
              <span v-for="(scene, index) in catalogEntry.scenes[detailLocale] || catalogEntry.scenes.zh"
                    :key="index"
                    class="dc-src__detail-scene">{{ scene }}</span>
            </div>
          </template>

          <a class="dc-src__detail-docs" href="https://metaforge.devlive.org/" target="_blank" rel="noopener noreferrer">
            {{ $t('source.common.viewDocs') }}
            <ArrowRight :size="14"/>
          </a>
        </template>
      </div>
    </div>

    <!-- 步骤 2-4：连接参数 -->
    <div v-else class="dc-src__config-step">
      <div class="relative">
        <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit">
          <div class="dc-src__field-grid">
            <template v-for="configure in stepFields" :key="configure.field">
              <div class="dc-src__field" :class="{ 'dc-src__field--full': ['Boolean', 'Array', 'File'].includes(configure.type) }">
                <MfFormItem :name="configure.field"
                            :label="$t('source.common.' + configure.field)">
                  <MfInput v-if="configure.type === 'String'" v-model="configure.value" :disabled="configure.disabled"/>

                  <MfInputNumber v-else-if="configure.type === 'Number'"
                                 v-model="configure.value"
                                 :disabled="configure.disabled"
                                 :controls="false"
                                 :max="configure.max"
                                 :min="configure.min"
                                 :style="{ width: '100%' }"/>

                  <MfSwitch v-else-if="configure.type === 'Boolean'" v-model="configure.value" :disabled="configure.disabled"/>

                  <MfUpload v-else-if="configure.type === 'File'"
                            action="http://localhost:9096/api/v1/source/uploadFile"
                            multiple
                            accept=".xml"
                            :headers="{ 'Authorization': auth?.type + ' ' + auth?.token, 'PluginType': (formState.type as string).split(' ')[0] }"
                            @change="onUploadChange">
                    {{ $t('common.upload') }}
                  </MfUpload>

                  <div v-else class="dc-src__kv">
                    <div class="dc-src__kv-tabs">
                      <button type="button"
                              class="dc-src__kv-tab"
                              :class="{ 'dc-src__kv-tab--active': !kvTextModes[configure.field] }"
                              title="Form"
                              @click="toggleKvMode(configure, false)">
                        <List :size="14"/>
                      </button>
                      <button type="button"
                              class="dc-src__kv-tab"
                              :class="{ 'dc-src__kv-tab--active': kvTextModes[configure.field] }"
                              title="Text"
                              @click="toggleKvMode(configure, true)">
                        <FileText :size="14"/>
                      </button>
                    </div>

                    <template v-if="!kvTextModes[configure.field]">
                      <div v-for="(element, index) in configure.value || []" :key="index" class="dc-src__kv-row">
                        <MfInput v-model="element.field" :placeholder="$t('common.field')"/>
                        <MfInput v-model="element.value" :placeholder="$t('common.value')"/>
                        <button type="button" class="dc-src__kv-remove" @click="onMinusConfigure(element, configure.value)">
                          <Minus :size="14"/>
                        </button>
                      </div>
                      <button type="button" class="dc-src__kv-add" @click="onPlusConfigure(configure)">
                        <Plus :size="14" />
                      </button>
                    </template>

                    <MfTextarea v-else
                                :model-value="kvTextValues[configure.field] || ''"
                                :rows="8"
                                placeholder="key=value"
                                @update:model-value="(value: string) => onKvTextChange(configure, value)"/>
                  </div>
                </MfFormItem>

                <div v-if="configure.description" class="-mt-1 text-xs text-content-tertiary">
                  {{ configure.description }}
                </div>
              </div>
            </template>
          </div>
        </MfForm>

        <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center">
          <MfSpinner size="lg" label="Loading..."/>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <template #footer>
      <div class="dc-src__footer">
        <div v-if="currentStep === 0" class="dc-src__footer-left">
          <Info :size="12" class="dc-src__footer-info-icon"/>
          <span>{{ $t('source.wizard.notFound') }}</span>
          <a href="https://github.com/devlive-community/metaforge/issues" target="_blank" rel="noopener noreferrer" class="dc-src__footer-link">
            {{ $t('source.common.requestNew') }}
          </a>
        </div>
        <div class="dc-src__footer-actions">
          <MfButton v-if="currentStep > 0" @click="currentStep--">{{ $t('source.wizard.previous') }}</MfButton>
          <MfButton variant="danger" @click="onCancel">{{ $t('common.cancel') }}</MfButton>
          <MfButton v-if="currentStep < 3"
                    variant="primary"
                    :disabled="!formState?.type"
                    @click="currentStep++">
            {{ $t('source.wizard.next') }}
          </MfButton>
          <MfButton v-if="currentStep === 3"
                    variant="ghost"
                    :loading="testing"
                    :disabled="testing || !formState.type"
                    @click="onTest()">
            {{ $t('common.test') }}
          </MfButton>
          <MfButton v-if="currentStep === 3"
                    variant="primary"
                    type="submit"
                    :loading="saving"
                    :disabled="!testInfo.connected || saving"
                    @click="onSubmit">
            {{ $t('common.save') }}
          </MfButton>
        </div>
      </div>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { ArrowRight, CircleCheck, Database, FileText, Info, List, Minus, Plus } from '@lucide/vue'
import { cloneDeep, pick } from 'lodash'
import { SourceModel, SourceRequest } from '@/model/source'
import SourceService from '@/services/source'
import PluginService from '@/services/plugin'
import { TokenUtils } from '@/utils/token'
import { ResponseModel } from '@/model/response'
import { findCatalogEntry, SourceCategoryKey } from '@/views/pages/admin/source/sourceCatalog'

interface TestInfo
{
  connected: boolean
  percent: number
  successful: boolean
  message?: null | string
}

defineOptions({ name: 'SourceInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: SourceModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t, locale } = useI18n()
const auth = TokenUtils.getAuthUser()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const formState = ref<SourceModel>(null as unknown as SourceModel)
const loading = ref(false)
const saving = ref(false)
const title = ref<string | null>(null)
const testing = ref(false)
const testInfo = ref<TestInfo>({ connected: false, percent: 0, successful: false })
const plugins = ref<any[]>([])
const pluginConfigure = ref<any>(null)
const applyConfigure = ref<any>(null)
const originalSchema = ref<any>(null)
const hasJsonConvert = ref(false)

const currentStep = ref(0)

const stepDefs = [
  { titleKey: 'source.common.source', descKey: 'source.wizard.stepTypeDesc' },
  { titleKey: 'source.common.configure', descKey: 'source.wizard.stepConfigureDesc' },
  { titleKey: 'source.common.authorization', descKey: 'source.wizard.stepAuthDesc' },
  { titleKey: 'source.common.advanced', descKey: 'source.wizard.stepAdvancedDesc' }
]

const wizardTitle = computed(() => props.info
  ? `${ t('source.common.modify').replace('$NAME', String(props.info.name || '...')) }`
  : t('source.common.create'))

/** 步骤对应的字段分组（与后端 IConfigureFieldGroup 枚举一致：configure/authorization/advanced/custom） */
const stepGroup = computed(() => ['configure', 'authorization', 'advanced'][currentStep.value - 1] || '')

const stepFields = computed(() => {
  if (currentStep.value === 0 || !pluginConfigure.value) {
    return []
  }
  if (currentStep.value < 3) {
    return pluginConfigure.value.filter((field: { group: string }) => field.group === stepGroup.value)
  }
  // 高级：除 configure / authorization 之外的所有分组
  return pluginConfigure.value.filter((field: { group: string }) => field.group !== 'configure' && field.group !== 'authorization')
})

// ---- 步骤 1：数据源类型选择 ----
const searchText = ref('')
const activeCategory = ref<SourceCategoryKey | 'all'>('all')
const activeChip = ref('all')

const chips = [
  { key: 'all', labelKey: 'source.catalog.all' },
  { key: 'popular', labelKey: 'source.filter.popular' },
  { key: 'rdb', labelKey: 'source.category.rdb' },
  { key: 'cloud', labelKey: 'source.category.cloud' },
  { key: 'other', labelKey: 'source.category.other' }
]

const categoryList = computed(() => {
  const defs: Array<{ key: SourceCategoryKey | 'all'; labelKey: string; icon?: any; count: number }> = [
    { key: 'all', labelKey: 'source.catalog.all', icon: Database, count: 0 }
  ]
  const categoryDefs: Array<{ key: SourceCategoryKey; labelKey: string }> = [
    { key: 'rdb', labelKey: 'source.category.rdb' },
    { key: 'bigdata', labelKey: 'source.category.bigdata' },
    { key: 'cloud', labelKey: 'source.category.cloud' },
    { key: 'nosql', labelKey: 'source.category.nosql' },
    { key: 'file', labelKey: 'source.category.file' },
    { key: 'api', labelKey: 'source.category.api' },
    { key: 'mq', labelKey: 'source.category.mq' },
    { key: 'other', labelKey: 'source.category.other' }
  ]
  for (const def of categoryDefs) {
    defs.push({ ...def, count: plugins.value.filter(p => pluginCategory(p) === def.key).length })
  }
  defs[0].count = plugins.value.length
  return defs
})

/** 分类：优先插件 describe() 声明的 category，前端目录仅兜底 */
const pluginCategory = (p: { name?: string, configure?: { category?: string } }): SourceCategoryKey => {
  const declared = p.configure?.category
  if (declared) {
    return String(declared).toLowerCase() as SourceCategoryKey
  }
  return findCatalogEntry(p.name || '').category
}

const catalogEntryFor = (name: string) => findCatalogEntry(name)

const selectedCatalogKey = ref('')

const selectedSource = computed(() => plugins.value.find(p => p.name === selectedCatalogKey.value) || null)
const detailLocale = computed(() => (locale.value.startsWith('zh') ? 'zh' : 'en') as 'zh' | 'en')

const hasContent = (value: unknown) => !!value && Object.keys(value as object).length > 0

/**
 * 右侧详情面板数据：优先用插件 describe() 声明的双语文案
 * （descriptions/features/scenes/popular），前端目录仅作未声明插件的兜底。
 */
const catalogEntry = computed(() => {
  const base = catalogEntryFor(selectedCatalogKey.value)
  const declared = selectedSource.value?.configure
  if (!declared) {
    return base
  }
  return {
    ...base,
    popular: declared.popular ?? base.popular,
    desc: hasContent(declared.descriptions) ? declared.descriptions : base.desc,
    features: hasContent(declared.features) ? declared.features : base.features,
    scenes: hasContent(declared.scenes) ? declared.scenes : base.scenes
  }
})

const filteredSources = computed(() => {
  let list = plugins.value
  const keyword = searchText.value?.toLowerCase() || ''
  if (keyword) {
    list = list.filter(p => p.name?.toLowerCase().includes(keyword))
  }
  if (activeCategory.value !== 'all') {
    list = list.filter(p => pluginCategory(p) === activeCategory.value)
  }
  if (activeChip.value === 'popular') {
    list = list.filter(p => p.configure?.popular ?? catalogEntryFor(p.name).popular)
  }
  else if (activeChip.value !== 'all') {
    list = list.filter(p => pluginCategory(p) === activeChip.value)
  }
  return list
})

const selectSource = (name: string) => {
  selectedCatalogKey.value = name
  if (formState.value) {
    formState.value.type = name
  }
}

const handleInitialize = async () => {
  try {
    loading.value = true
    testInfo.value = { connected: false, percent: 0, successful: false }
    title.value = `${ t('source.common.create') }`
    currentStep.value = 0

    const pluginsResponse = await PluginService.getPlugins(true)
    hasJsonConvert.value = pluginsResponse.data.some((value: any) => value.type?.toLowerCase() === 'convert' && value.name === 'JsonConvert')

    if (!hasJsonConvert.value) {
      testInfo.value.message = t('plugin.text.requiredJsonConvert')
    }

    if (pluginsResponse.status) {
      plugins.value = pluginsResponse.data.filter((plugin: { type: string }) => plugin.type?.toLowerCase() === 'connector')
      // 默认选中第一个数据源（原型中默认高亮 MySQL）
      if (!selectedCatalogKey.value && plugins.value.length > 0) {
        selectSource(plugins.value[0].name)
      }
    }

    if (props.info) {
      title.value = `${ t('source.common.modify').replace('$NAME', String(props.info.name || '...')) }`
      const sourceResponse = await SourceService.getByCode(props.info.code)
      if (sourceResponse.status) {
        formState.value = cloneDeep(sourceResponse.data)
        applyConfigure.value = sourceResponse.data?.schema
        pluginConfigure.value = applyConfigure.value?.configures
        originalSchema.value = cloneDeep(sourceResponse.data?.schema)
        selectedCatalogKey.value = String(formState.value.type || '')
      }
    }
    else {
      formState.value = SourceRequest.of()
    }
  }
  catch (error) {
    console.error('Failed to initialize:', error)
    MfMessage.error('Failed to initialize the form')
  }
  finally {
    loading.value = false
  }
}

watch(() => formState.value?.type, (newValue: any) => {
  if (!newValue) {
    return
  }
  if (newValue instanceof Array) {
    formState.value.type = newValue[0]
  }
  try {
    const applyPlugin = plugins.value.find((plugin: { name: string }) => plugin.name === formState.value.type) as any
    const isEditingMatchingType = props.info && props.info.type === formState.value.type

    if (isEditingMatchingType && originalSchema.value) {
      applyConfigure.value = cloneDeep(originalSchema.value)
      pluginConfigure.value = applyConfigure.value?.configures
    }
    else {
      applyConfigure.value = cloneDeep(applyPlugin?.configure)
      pluginConfigure.value = applyConfigure.value?.configures
    }

    nextTick(() => {
      // 分组字段在向导步骤 2-4 中按 group 渲染
    })
  }
  catch (error) {
    console.error('Plugin change error:', error)
  }
})

const onCancel = () => {
  visible.value = false
}

const onTest = () => {
  testing.value = true
  const { configures } = applyConfigure.value
  const configure = {
    type: formState.value.type,
    configure: {
      configures: configures.filter((item: any) => item.field).map((item: any) => pick(item, ['field', 'required', 'type', 'min', 'max', 'message', 'value']))
    }
  }
  SourceService.testConnection(configure)
               .then((response) => {
                 testInfo.value.percent = 100
                 if (response.status) {
                   MfMessage.success('Test successful')
                   testInfo.value.connected = true
                   testInfo.value.successful = true
                   testInfo.value.message = null
                   formState.value.version = extractVersion(response.data)
                 }
                 else {
                   testInfo.value.message = response.message
                   testInfo.value.connected = false
                   testInfo.value.successful = false
                 }
               })
               .finally(() => (testing.value = false))
}

const onSubmit = () => {
  saving.value = true
  const { configures } = applyConfigure.value
  const configure = {
    code: formState.value.code,
    name: formState.value.type,
    configure: {
      configures: configures.filter((item: any) => item.field).map((item: any) => pick(item, ['field', 'required', 'type', 'min', 'max', 'message', 'value']))
    },
    type: formState.value.type,
    version: formState.value.version
  }
  SourceService.saveOrUpdate(configure as any)
               .then((response) => {
                 if (response.status) {
                   MfMessage.success('Create successful')
                   onCancel()
                 }
               })
               .finally(() => (saving.value = false))
}

const onUploadChange = (info: any) => {
  if (info.file.status === 'done') {
    handlerUploadSuccess(info.file.response)
  }
}

const handlerUploadSuccess = (response: ResponseModel) => {
  if (response.status) {
    const configure = applyConfigure.value.configures.filter((configure: { field: string }) => configure.field === 'file')
    configure[0].value.push(response.data)
  }
}

const onPlusConfigure = (configure: any) => {
  if (!configure.value) {
    configure.value = []
  }
  configure.value.push({ field: '', value: '' })
}

// Array 字段双视图：false = 逐行表单（默认），true = key=value 文本（每行一条）
const kvTextModes = ref<Record<string, boolean>>({})
const kvTextValues = ref<Record<string, string>>({})

const toggleKvMode = (configure: any, textMode: boolean) => {
  if (textMode) {
    kvTextValues.value[configure.field] = (configure.value || [])
      .filter((element: any) => element.field)
      .map((element: any) => `${element.field}=${element.value ?? ''}`)
      .join('\n')
  }
  kvTextModes.value[configure.field] = textMode
}

const onKvTextChange = (configure: any, text: string) => {
  kvTextValues.value[configure.field] = text
  configure.value = text.split('\n')
                       .map(line => line.trim())
                       .filter(line => line.length > 0)
                       .map(line => {
                         const separator = line.indexOf('=')
                         return separator === -1
                           ? { field: line, value: '' }
                           : { field: line.slice(0, separator).trim(), value: line.slice(separator + 1).trim() }
                       })
}

const onMinusConfigure = (configure: any, array: Array<any>) => {
  const index = array.indexOf(configure)
  if (index !== -1) {
    array.splice(index, 1)
  }
}

const extractVersion = (json: any) => {
  const columnName = json['headers'][0]
  const column = json['columns'][0]
  if (typeof column === 'object' && column !== null && !Array.isArray(column)) {
    const value = column[columnName]
    if (value !== undefined) {
      if (typeof value === 'object' && value.values && Array.isArray(value.values)) {
        return value.values.length === 1 ? value.values[0] : value.values
      }
      if (typeof value === 'object' && value.string) {
        return value.string
      }
      return value
    }
    return column
  }
  if (typeof column === 'string') {
    if (column.startsWith('"') && column.endsWith('"') && column.length > 1) {
      return column.slice(1, -1)
    }
    return column
  }
  return column
}

handleInitialize()
</script>

<style scoped>
.dc-src__head {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    min-width: 0;
    padding-right: 40px;
}

.dc-src__head-title {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 20px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-src__head-desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-src__close {
    margin-left: auto;
}

.dc-src__steps {
    margin: 16px 0 20px;
}

.dc-src__type-step {
    display: flex;
    gap: 14px;
    min-height: 460px;
}

.dc-src__catalog {
    width: 200px;
    flex-shrink: 0;
    border-right: 1px solid var(--dc-border-light);
    padding-right: 10px;
}

.dc-src__cat-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    border-radius: var(--dc-radius-sm);
    cursor: pointer;
    font-size: 13px;
    color: var(--dc-text-primary);
}

.dc-src__cat-item:hover {
    background: var(--dc-menu-hover);
}

.dc-src__cat-item--active {
    background: var(--dc-primary-light);
    color: var(--dc-primary-700);
    font-weight: 600;
}

.dc-src__cat-icon {
    color: var(--dc-text-secondary);
}

.dc-src__cat-item--active .dc-src__cat-icon {
    color: var(--dc-primary);
}

.dc-src__cat-label {
    flex: 1;
}

.dc-src__cat-count {
    font-size: 11px;
    color: var(--dc-text-secondary);
    background: var(--dc-border-light);
    border-radius: 8px;
    padding: 0 6px;
}

.dc-src__middle {
    flex: 1;
    min-width: 0;
    padding: 0 14px;
}

.dc-src__chips {
    display: flex;
    gap: 8px;
    margin: 10px 0 14px;
}

.dc-src__chip {
    padding: 3px 12px;
    border-radius: 999px;
    border: 1px solid var(--dc-border);
    font-size: 12px;
    color: var(--dc-text-secondary);
    cursor: pointer;
}

.dc-src__chip--active {
    background: var(--dc-primary-light);
    border-color: var(--dc-primary);
    color: var(--dc-primary-700);
}

.dc-src__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 10px;
    max-height: 440px;
    overflow-y: auto;
    padding-right: 4px;
}

.dc-src__grid-item {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 14px 8px;
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-md);
    cursor: pointer;
    transition: border-color 0.2s, box-shadow 0.2s;
}

.dc-src__grid-item:hover {
    border-color: var(--dc-primary);
}

.dc-src__grid-item--selected {
    border-color: var(--dc-primary);
    background: var(--dc-primary-surface);
    box-shadow: var(--dc-shadow-focus);
}

.dc-src__grid-check {
    position: absolute;
    top: 6px;
    right: 6px;
    color: var(--dc-primary);
    font-size: 14px;
}

.dc-src__grid-logo {
    height: 40px;
    width: 40px;
    object-fit: contain;
}

.dc-src__grid-name {
    font-size: 12px;
    color: var(--dc-text-primary);
    text-align: center;
}

.dc-src__grid-empty {
    grid-column: 1 / -1;
    text-align: center;
    color: var(--dc-text-placeholder);
    font-size: 12px;
    padding: 20px 0;
}

.dc-src__detail {
    width: 280px;
    flex-shrink: 0;
    border-left: 1px solid var(--dc-border-light);
    padding-left: 16px;
}

.dc-src__detail-logo {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
}

.dc-src__detail-logo img {
    height: 42px;
    object-fit: contain;
}

.dc-src__detail-name {
    font-size: 20px;
    font-weight: 700;
    color: var(--dc-text-title);
    margin-top: 8px;
}

.dc-src__detail-desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
    line-height: 1.7;
    margin-top: 8px;
}

.dc-src__detail-sub {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
    margin: 14px 0 8px;
}

.dc-src__detail-feature {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--dc-text-primary);
    padding: 3px 0;
}

.dc-src__detail-feature-icon {
    color: var(--dc-success);
    font-size: 12px;
}

.dc-src__detail-scenes {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.dc-src__detail-scene {
    padding: 2px 10px;
    border-radius: 999px;
    background: var(--dc-border-light);
    font-size: 11px;
    color: var(--dc-text-secondary);
}

.dc-src__detail-docs {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    margin-top: 14px;
    font-size: 12px;
    color: var(--dc-primary);
}

.dc-src__field-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 20px;
    align-content: start;
}

.dc-src__field {
    min-width: 0;
}

.dc-src__field--full {
    grid-column: 1 / -1;
}

.dc-src__kv {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
}

.dc-src__kv-tabs {
    align-self: flex-end;
    display: inline-flex;
    gap: 2px;
    padding: 2px;
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-sm);
}

.dc-src__kv-tab {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 22px;
    border: none;
    border-radius: 4px;
    background: transparent;
    color: var(--dc-text-secondary);
    cursor: pointer;
    transition: color 0.2s, background 0.2s;
}

.dc-src__kv-tab:hover {
    color: var(--dc-text-primary);
    background: var(--dc-menu-hover);
}

.dc-src__kv-tab--active {
    background: var(--dc-primary-light);
    color: var(--dc-primary);
}

.dc-src__kv-row {
    display: grid;
    grid-template-columns: minmax(0, 5fr) minmax(0, 6fr) 32px;
    gap: 8px;
    align-items: center;
}

.dc-src__kv-remove {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    border: none;
    border-radius: var(--dc-radius-sm);
    background: transparent;
    color: var(--dc-text-secondary);
    cursor: pointer;
    transition: color 0.2s, background 0.2s;
}

.dc-src__kv-remove:hover {
    color: var(--dc-danger);
    background: var(--dc-danger-bg);
}

.dc-src__kv-add {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 32px;
    border: 1px dashed var(--dc-border);
    border-radius: var(--dc-radius-sm);
    background: transparent;
    color: var(--dc-text-secondary);
    cursor: pointer;
    transition: border-color 0.2s, color 0.2s;
}

.dc-src__kv-add:hover {
    border-color: var(--dc-primary);
    color: var(--dc-primary);
}

.dc-src__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    width: 100%;
}

.dc-src__footer-left {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-src__footer-info-icon {
    color: var(--dc-info);
}

.dc-src__footer-link {
    color: var(--dc-primary);
}

.dc-src__footer-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-left: auto;
}
</style>
