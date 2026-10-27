<template>
  <div class="dc-rt">
    <!-- 页头：标题 + 描述 + 配置说明 -->
    <div class="dc-rt__pagehead">
      <div>
        <div class="dc-rt__title">{{ $t('configure.runtime.title') }}</div>
        <div class="dc-rt__desc">{{ $t('configure.runtime.desc') }}</div>
      </div>
      <MfPopover placement="bottomRight">
        <template #content>
          <div class="dc-rt__guide-pop">{{ $t('configure.runtime.bandTip') }}</div>
        </template>
        <MfButton class="dc-rt__guide-btn">
          <CircleHelp :size="14"/>
          {{ $t('configure.runtime.guide') }}
        </MfButton>
      </MfPopover>
    </div>

    <div class="grid grid-cols-3 gap-4">
      <!-- 左侧：分类 tab + 配置项列表 -->
      <div>
        <div class="dc-rt__sidebar">
          <MfTabs v-model:active-key="activeCategory" class="dc-rt__tabs">
            <MfTabPane item-key="EXECUTOR" :tab="$t('configure.runtime.categoryExecutor')"/>
            <MfTabPane item-key="DATASET" :tab="$t('configure.runtime.categoryDataset')"/>
          </MfTabs>

          <div v-if="loading" class="flex items-center justify-center py-8">
            <MfSpinner size="lg"/>
          </div>
          <div v-else class="dc-rt__rows">
              <div v-for="row in (groupedRows[activeCategory] || [])"
                   :key="row.id"
                   class="dc-rt__row"
                   :class="{ 'dc-rt__row--active': selected && selected.category === activeCategory && selected.name === row.name }"
                   @click="onSelect(activeCategory, row.name)">
                <span class="dc-rt__row-name">{{ row.name }}</span>
                <MfButton variant="ghost" size="sm" class="dc-rt__row-test" @click.stop="onTestConfig(activeCategory, row.name)">
                  {{ $t('configure.runtime.testConfig') }}
                </MfButton>
              </div>
              <div v-if="!(groupedRows[activeCategory] || []).length" class="dc-rt__empty">
                {{ $t('configure.runtime.empty') }}
              </div>
          </div>
        </div>
      </div>

      <!-- 右侧：选中项参数 -->
      <div class="col-span-2">
        <div class="dc-rt__content">
          <div v-if="loading" class="flex items-center justify-center py-8">
            <MfSpinner size="lg"/>
          </div>
          <template v-else>
            <div v-if="!selected" class="dc-rt__placeholder">
              {{ $t('configure.runtime.selectHint') }}
            </div>

            <template v-else>
              <div class="dc-rt__content-title">
                {{ selected.category }} / {{ selected.name }}
              </div>

              <div class="dc-rt__fields">
                <div v-for="field in schema" :key="field.name" class="dc-rt__field">
                  <div class="dc-rt__field-label">
                    <span>{{ field.name }}</span>
                    <MfTooltip v-if="field.description" :content="field.description">
                      <CircleHelp :size="14" class="dc-rt__field-q"/>
                    </MfTooltip>
                    <span v-if="!field.tunable" class="dc-rt__field-admin">[{{ $t('configure.runtime.adminOnly') }}]</span>
                  </div>

                  <MfInput v-if="field.type === 'STRING'" v-model="form[field.name]"/>
                  <MfInputNumber v-else-if="field.type === 'NUMBER'" v-model="form[field.name]" class="w-full"/>
                  <MfSwitch v-else-if="field.type === 'BOOLEAN'"
                            :model-value="booleanProxies[field.name]"
                            @update:model-value="(v: boolean) => { booleanProxies[field.name] = v; onBoolChange(field.name, v) }"/>
                  <MfInput v-else-if="field.type === 'PASSWORD'" v-model="form[field.name]" type="password" show-password/>
                </div>
              </div>

              <div class="dc-rt__actions">
                <MfButton variant="primary" :loading="saving" @click="onSave">
                  {{ $t('configure.runtime.save') }}
                </MfButton>
              </div>
            </template>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { CircleHelp } from '@lucide/vue'
import RuntimeConfigureService from '@/services/runtimeConfigure'

interface PluginConfigureField
{
  name: string
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'PASSWORD'
  defaultValue: string
  description: string
  tunable: boolean
}

interface ConfigureRow
{
  id: number
  name: string
  category: string
}

defineOptions({ name: 'ConfigureHome' })

const { t } = useI18n()

const CATEGORY_KEYS = ['EXECUTOR', 'DATASET']

const loading = ref(false)
const saving = ref(false)
const activeCategory = ref('EXECUTOR')
const groupedRows = ref<Record<string, ConfigureRow[]>>({})
const selected = ref<{ category: string; name: string } | null>(null)
const schema = ref<PluginConfigureField[]>([])
const form = ref<Record<string, string>>({})
const booleanProxies = ref<Record<string, boolean>>({})

const loadAll = async () => {
  loading.value = true
  try {
    for (const c of CATEGORY_KEYS) {
      const response = await RuntimeConfigureService.list(c)
      groupedRows.value[c] = response.status ? (response.data || []) : []
    }
    // 默认选中当前分类的第一项
    const first = (groupedRows.value[activeCategory.value] || [])[0]
    if (first && (!selected.value || selected.value.category !== activeCategory.value)) {
      await onSelect(activeCategory.value, first.name)
    }
  }
  finally {
    loading.value = false
  }
}

const onSelect = async (category: string, name: string) => {
  selected.value = { category, name }
  loading.value = true
  await RuntimeConfigureService.detail(category, name)
                         .then(response => {
                           if (response.status && response.data) {
                             schema.value = response.data.schema || []
                             form.value = {}
                             booleanProxies.value = {}
                             const values = response.data.values || {}
                             for (const f of schema.value) {
                               form.value[f.name] = values[f.name] ?? f.defaultValue ?? ''
                               if (f.type === 'BOOLEAN') {
                                 booleanProxies.value[f.name] = (form.value[f.name] + '').toLowerCase() === 'true'
                               }
                             }
                           }
                           else {
                             MfMessage.error(response.message)
                           }
                         })
                         .finally(() => (loading.value = false))
}

const onBoolChange = (name: string, value: boolean) => {
  form.value[name] = value ? 'true' : 'false'
}

const onTestConfig = (category: string, name: string) => {
  loading.value = true
  RuntimeConfigureService.detail(category, name)
                         .then(response => {
                           if (response.status && response.data) {
                             const values = response.data.values || {}
                             const missing = (response.data.schema || [])
                                 .filter((f: PluginConfigureField) => {
                                   const v = values[f.name] ?? f.defaultValue ?? ''
                                   return v === '' || v === null || v === undefined
                                 })
                                 .map((f: PluginConfigureField) => f.name)
                             if (missing.length === 0) {
                               MfMessage.success(t('configure.runtime.testOk'))
                             }
                             else {
                               MfMessage.error(`${ t('configure.runtime.testMissing', { count: missing.length }) }: ${ missing.join(', ') }`)
                             }
                           }
                           else {
                             MfMessage.error(response.message)
                           }
                         })
                         .finally(() => (loading.value = false))
}

const onSave = () => {
  if (!selected.value) {
    return
  }
  for (const f of schema.value) {
    if (f.type === 'BOOLEAN') {
      form.value[f.name] = booleanProxies.value[f.name] ? 'true' : 'false'
    }
  }
  saving.value = true
  RuntimeConfigureService.save(selected.value.category, selected.value.name, form.value)
                         .then(response => {
                           if (response.status) {
                             MfMessage.success(t('common.successfully'))
                           }
                           else {
                             MfMessage.error(response.message)
                           }
                         })
                         .finally(() => (saving.value = false))
}

// 切换左侧 tab 时自动选中该分类的第一项
watch(activeCategory, (category) => {
  const first = (groupedRows.value[category] || [])[0]
  if (first) {
    onSelect(category, first.name)
  }
})

loadAll()
</script>

<style scoped>
.dc-rt__pagehead {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    padding: 18px 20px;
    border-radius: var(--dc-radius-lg);
    background: var(--dc-gradient-hero);
    border: 1px solid var(--dc-border-light);
    margin-bottom: 16px;
}

.dc-rt__title {
    font-size: 22px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-rt__desc {
    font-size: 13px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-rt__guide-btn {
    border-radius: var(--dc-radius-md);
    color: var(--dc-text-primary);
}

.dc-rt__sidebar {
    background: var(--dc-sidebar);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    padding: 12px;
    min-height: calc(100vh - 260px);
}

.dc-rt__tabs {
    margin-bottom: 8px;
}

.dc-rt__rows {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.dc-rt__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 9px 12px;
    border-radius: var(--dc-radius-md);
    cursor: pointer;
    transition: background-color 0.2s;
}

.dc-rt__row:hover {
    background: var(--dc-menu-hover);
}

.dc-rt__row--active {
    background: var(--dc-primary-light);
}

.dc-rt__row--active .dc-rt__row-name {
    color: var(--dc-primary-700);
    font-weight: 600;
}

.dc-rt__row-name {
    font-size: 13px;
    color: var(--dc-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.dc-rt__row-test {
    flex-shrink: 0;
    font-size: 12px;
}

.dc-rt__empty {
    font-size: 12px;
    color: var(--dc-text-placeholder);
    padding: 8px;
}

.dc-rt__content {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
    min-height: 300px;
}

.dc-rt__content-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
    margin-bottom: 16px;
}

.dc-rt__fields {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.dc-rt__field-label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--dc-text-title);
    margin-bottom: 6px;
}

.dc-rt__field-q {
    color: var(--dc-text-placeholder);
    cursor: help;
}

.dc-rt__field-admin {
    font-size: 11px;
    color: var(--dc-warning);
}

.dc-rt__actions {
    display: flex;
    gap: 12px;
    margin-top: 8px;
}

.dc-rt__guide-pop {
    max-width: 320px;
    font-size: 12px;
    color: var(--dc-text-secondary);
}
</style>
