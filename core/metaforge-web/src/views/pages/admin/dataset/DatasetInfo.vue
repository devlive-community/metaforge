<template>
  <MfCard :title="$t('common.dataset')" :bordered="false">
    <template #extra>
      <MfButton :disabled="!data?.data.columns" @click="configureVisible = true">
        {{ $t('common.configure') }}
      </MfButton>
    </template>

    <div class="relative h-full space-y-4" style="min-height: 200px;">
      <div class="relative">
        <MfCard v-if="sourceInfo" class="w-full">
          <template #header>
            <MfButton variant="primary" :loading="running" :disabled="running" @click="onRun()">
              {{ $t('query.common.execute') }}
            </MfButton>
          </template>

          <div class="relative">
            <AceEditor :value="value" @update:value="value = $event"/>
            <div v-if="running" class="absolute inset-0 z-10 flex items-center justify-center">
              <MfSpinner size="lg" label="Loading..."/>
            </div>
          </div>
        </MfCard>

        <div v-if="data || code" class="relative">
          <AgGridVue v-if="data?.data.columns"
                     :style="{height: '300px'}"
                     class="ag-theme-metaforge"
                     :pagination="true"
                     :columnDefs="columnDefs"
                     :rowData="data.data.columns"
                     :gridOptions="gridOptions as any"/>
          <div v-if="running" class="absolute inset-0 z-10 flex items-center justify-center">
            <MfSpinner size="lg" label="Loading..."/>
          </div>
        </div>

        <div v-if="!sourceInfo" class="mt-3 justify-center items-center">
          <div class="flex flex-col items-center space-y-4">
            <MfAlert :title="i18n.t('dataset.common.onlyPreviewCreate')"/>

            <MfButton>
              <router-link to="/admin/query">
                {{ i18n.t('dataset.common.returnQuery') }}
              </router-link>
            </MfButton>
          </div>
        </div>

        <div v-if="loading" class="absolute inset-0 z-20 flex items-center justify-center">
          <MfSpinner size="lg" label="Loading..."/>
        </div>
      </div>
    </div>
  </MfCard>

  <MfDrawer v-model:open="configureVisible"
            placement="bottom"
            :style="{ height: '70%' }"
            :title="$t('common.configure')">
    <template #extra>
      <div class="flex items-center justify-end space-x-2">
        <MfButton variant="primary" @click="onSubmit">
          {{ code ? $t('dataset.common.modify') : $t('dataset.common.create') }}
        </MfButton>

        <MfButton @click="configureVisible = false">
          {{ $t('common.cancel') }}
        </MfButton>
      </div>
    </template>

    <MfAlert v-if="validator" variant="danger" class="mt-2" :title="validatorMessage"/>

    <MfTabs v-model:active-key="activeTab" class="mt-1">
      <MfTabPane item-key="columns" :tab="$t('dataset.common.dataColumn')" class="space-y-2">
        <div class="relative">
          <div class="grid grid-cols-12 gap-2">
            <div class="text-center">{{ $t('dataset.common.columnName') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnAlias') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnType') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnMode') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnDefaultValue') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnIsNullable') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnIsOrderByKey') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnIsPartitionKey') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnIsPrimaryKey') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnIsSampling') }}</div>
            <div class="text-center">{{ $t('dataset.common.columnLength') }}</div>
            <div class="text-center">{{ $t('common.action') }}</div>
          </div>

          <div v-for="(item, index) in formState.columns" :key="index" class="grid grid-cols-12 gap-2">
            <div class="flex justify-center">
              <MfInput v-model="item.name"/>
            </div>

            <div class="flex justify-center">
              <MfInput v-model="item.aliasName"/>
            </div>

            <div class="flex justify-center">
              <MfSelect v-model="item.type" placeholder="Options" class="w-full"
                        :options="[
                          { label: $t('dataset.common.columnTypeString'), value: 'STRING' },
                          { label: $t('dataset.common.columnTypeNumber'), value: 'NUMBER' },
                          { label: $t('dataset.common.columnTypeNumberSigned'), value: 'NUMBER_SIGNED' },
                          { label: $t('dataset.common.columnTypeBoolean'), value: 'BOOLEAN' },
                          { label: $t('dataset.common.columnTypeDateTime'), value: 'DATETIME' }
                        ]"/>
            </div>

            <div class="flex justify-center items-center">
              <MfSwitch :model-value="item.mode === 'METRIC'"
                        @update:model-value="item.mode = $event ? 'METRIC' : 'DIMENSION'"/>
            </div>

            <div class="flex justify-center">
              <MfInput v-model="item.defaultValue" :disabled="item.virtualColumn"/>
            </div>

            <div class="flex justify-center items-center">
              <MfSwitch v-model="item.nullable" :disabled="item.virtualColumn" @update:model-value="setNullable(item, $event)"/>
            </div>

            <div class="flex justify-center items-center">
              <MfSwitch v-model="item.orderByKey" :disabled="item.virtualColumn" @update:model-value="setOrderByKey(item, $event)"/>
            </div>

            <div class="flex justify-center items-center">
              <MfSwitch v-model="item.partitionKey" :disabled="item.virtualColumn" @update:model-value="setPartitionKey(item, $event)"/>
            </div>

            <div class="flex justify-center items-center">
              <MfSwitch v-model="item.primaryKey" :disabled="item.virtualColumn" @update:model-value="setPrimaryKey(item, $event)"/>
            </div>

            <div class="flex justify-center items-center">
              <MfSwitch v-model="item.samplingKey" :disabled="item.virtualColumn" @update:model-value="setSamplingKey(item, $event)"/>
            </div>

            <div class="flex justify-center">
              <MfInputNumber v-model="item.length" :disabled="item.type === 'BOOLEAN' || item.type === 'DATETIME' || item.virtualColumn"/>
            </div>

            <div class="flex justify-center items-center space-x-1">
              <MfPopover placement="bottom">
                <template #content>
                  <div class="p-2 space-y-2">
                    <div class="text-base font-medium">{{ $t('dataset.common.columnComment') }}</div>
                    <MfTextarea v-model="item.comment"/>
                  </div>
                </template>

                <MfButton variant="ghost" size="sm">
                  <EditOutlined :style="{ fontSize: '10px' }"/>
                </MfButton>
              </MfPopover>

              <MfButton variant="ghost"
                        size="sm"
                        class="text-danger"
                        :disabled="!item.customColumn"
                        @click="onRemoveColumn(index)">
                <DeleteOutlined :style="{ fontSize: '10px' }"/>
              </MfButton>

              <MfButton variant="ghost" size="sm" @click="onAddColumn(index)">
                <PlusOutlined :style="{ fontSize: '10px' }"/>
              </MfButton>
            </div>
          </div>

          <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center">
            <MfSpinner size="lg" label="Loading..."/>
          </div>
        </div>
      </MfTabPane>

      <MfTabPane item-key="configure" :tab="$t('dataset.common.dataConfigure')">
        <MfForm class="w-[40%] mx-auto items-center" :model="formState" layout="vertical">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <MfFormItem name="name" :label="$t('common.name')" :rules="[{ required: true, message: $t('common.name') }]">
              <MfInput v-model="formState.name"/>
            </MfFormItem>

            <MfFormItem name="executor" :label="$t('common.executor')">
              <MfSelect v-model="formState.executor"
                        :options="executors.map((item: any) => ({ label: item.name, value: item.name }))"/>
            </MfFormItem>

            <MfFormItem name="syncMode" :label="$t('dataset.common.syncMode')">
              <MfSelect v-model="formState.syncMode"
                        :options="[
                          { label: $t('dataset.common.syncModeManual'), value: 'MANUAL' },
                          { label: $t('dataset.common.syncModeTiming'), value: 'TIMING' },
                          { label: $t('dataset.common.syncModeOutSync'), value: 'OUT_SYNC' }
                        ]"/>
            </MfFormItem>

            <MfFormItem name="expression" :label="$t('dataset.common.columnExpression')">
              <MfInput v-model="formState.expression" :disabled="formState.syncMode !== 'TIMING'"/>
            </MfFormItem>

            <MfFormItem name="scheduler" :label="$t('common.scheduler')">
              <MfSelect v-model="formState.scheduler"
                        :disabled="formState.syncMode !== 'TIMING'"
                        :options="schedulers.map((item: any) => ({ label: item.name, value: item.name }))"/>
            </MfFormItem>
          </div>

          <MfDivider/>

          <MfAlert v-if="formState.columns.filter(item => item.type === 'DATETIME').length === 0"
                   variant="danger"
                   :title="$t('dataset.tip.lifeCycleMustDateColumn')"/>

          <MfFormItem name="lifeCycleColumn" :label="$t('dataset.common.lifeCycleColumn')">
            <MfSelect v-model="formState.lifeCycleColumn"
                      :disabled="formState.columns.filter(item => item.type === 'DATETIME').length === 0"
                      :options="formState.columns.filter(v => v.type === 'DATETIME').map((v: any) => ({ label: v.name, value: v.name }))"/>
            <div class="text-xs text-content-tertiary mt-1">{{ $t('dataset.tip.lifeCycle') }}</div>
          </MfFormItem>

          <MfFormItem name="lifeCycleType" :label="$t('dataset.common.lifeCycleType')">
            <MfSelect v-model="formState.lifeCycleType"
                      :disabled="!formState.lifeCycleColumn"
                      :options="[
                        { label: $t('dataset.common.lifeCycleMonth'), value: 'MONTH' },
                        { label: $t('dataset.common.lifeCycleWeek'), value: 'WEEK' },
                        { label: $t('dataset.common.lifeCycleDay'), value: 'DAY' },
                        { label: $t('dataset.common.lifeCycleHour'), value: 'HOUR' }
                      ]"/>
          </MfFormItem>

          <MfFormItem name="lifeCycle" :label="$t('dataset.common.lifeCycleNumber')">
            <MfInputNumber v-model="formState.lifeCycle" :min="1" :disabled="!formState.lifeCycleColumn" :style="{ width: '100%' }"/>
          </MfFormItem>

          <MfDivider/>

          <MfFormItem name="description" :label="$t('common.description')">
            <MfTextarea v-model="formState.description"/>
          </MfFormItem>
        </MfForm>
      </MfTabPane>
    </MfTabs>
  </MfDrawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import GridOptions from '@/views/components/grid/GridOptions'
import DatasetService from '@/services/dataset'
import { HttpUtils } from '@/utils/http'
import { GridColumn } from '@/views/components/grid/GridColumn'
import PluginService from '@/services/plugin'
import { AgGridVue } from 'ag-grid-vue3'
import 'ag-grid-community/styles/ag-grid.css'
import '@/views/components/grid/ag-theme-metaforge.css'
import { DatasetModel } from '@/model/dataset'
import { ResponseModel } from '@/model/response.ts'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import { SourceModel } from '@/model/source.ts'
import SourceService from '@/services/source'
import ExecuteService from '@/services/execute'
import { ExecuteModel } from '@/model/execute.ts'
import { ArrayUtils } from '@/utils/array.ts'
import { join } from 'lodash'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'DatasetInfo' })

const i18n = useI18n()
const route = useRoute()
const router = useRouter()
const gridOptions = GridOptions.createDefaultOptions(i18n)

const code = ref<string | null>(null)
const loading = ref(false)
const saving = ref(false)
const validator = ref(false)
const validatorMessage = ref<string | null>(null)
const columnDefs = ref<GridColumn[]>([])
const schedulers = ref<any[]>([])
const executors = ref<any[]>([])
const configureVisible = ref(false)
const formState = ref<any>({
  id: null,
  name: null as string | null | undefined,
  description: null as string | null | undefined,
  query: null as string | null,
  syncMode: 'MANUAL',
  columns: [] as any[],
  source: { code: null },
  expression: null as string | null,
  scheduler: 'Default',
  executor: 'Local',
  lifeCycle: null as number | null,
  lifeCycleColumn: null as string | null,
  lifeCycleType: null as string | null
})
const data = ref<ResponseModel | null>(null)
const sourceInfo = ref<SourceModel | null>(null)
const value = ref('')
const running = ref(false)
const activeTab = ref('columns')

const onRun = () => {
  const configure: ExecuteModel = {
    content: value.value,
    name: sourceInfo.value?.code as unknown as string,
    mode: 'DATASET'
  }
  running.value = true
  ExecuteService.execute(configure, null)
                .then((response) => {
                  if (response.status) {
                    data.value = response
                    columnDefs.value = []
                    response.data?.headers.forEach((header: any) => {
                      const columnDef: GridColumn = { headerName: header, field: header }
                      columnDefs.value.push(columnDef)
                    })
                    if (formState.value.columns.length === 0) {
                      response.data?.headers.map((header: any, index: number) => {
                        const column = {
                          id: null,
                          name: `column_${ index + 1 }`,
                          aliasName: header.replace('(', '_').replace(')', ''),
                          type: 'STRING',
                          comment: header,
                          defaultValue: null,
                          position: index,
                          nullable: false,
                          length: 0,
                          original: header,
                          orderByKey: false,
                          partitionKey: false,
                          primaryKey: false,
                          samplingKey: false,
                          mode: 'DIMENSION',
                          virtualColumn: false,
                          customColumn: false
                        }
                        formState.value.columns.push(column)
                      })
                    }
                  }
                  else {
                    MfMessage.error(response.message)
                  }
                })
                .finally(() => (running.value = false))
}

const handleInitialize = () => {
  setTimeout(() => {
    PluginService.getPlugins()
                 .then((response) => {
                   if (response.status) {
                     schedulers.value = response.data.filter((v: { type: string }) => v.type === 'SCHEDULER')
                     executors.value = response.data.filter((v: { type: string }) => v.type === 'EXECUTOR')
                   }
                 })
    const routeCode = route.params.code
    const sourceCode = route.params.sourceCode

    if (routeCode) {
      loading.value = true
      code.value = routeCode as string
      const axios = new HttpUtils().getAxios()
      axios.all([DatasetService.getByCode(code.value), DatasetService.getColumnsByCode(code.value)])
           .then(axios.spread((info: any, column: any) => {
             if (info.status) {
               formState.value = info.data
               formState.value.source.code = info.data.source.code
               sourceInfo.value = info.data.source
               value.value = info.data.query
               onRun()
             }
             if (column.status) {
               formState.value.columns = column.data
             }
           }))
           .finally(() => (loading.value = false))
    }
    else if (sourceCode) {
      const tempId = route.query.tempId
      if (tempId) {
        value.value = localStorage.getItem(`QueryContent_${ tempId }`) || ''
        localStorage.removeItem(`QueryContent_${ tempId }`)
      }
      loading.value = true
      SourceService.getByCode(sourceCode as string)
                   .then((response) => {
                     if (response.status) {
                       sourceInfo.value = response.data
                       formState.value.source.code = response.data.code
                     }
                   })
                   .finally(() => (loading.value = false))
    }
  })
}

const validatorSampling = () => {
  const samplingColumns = formState.value.columns.filter((item: any) => item.samplingKey)
  if (samplingColumns.length === 0) {
    validator.value = false
    validatorMessage.value = null
    return
  }

  const orderByColumns = formState.value.columns.filter((item: any) => item.orderByKey)
  const isNameInOrderByColumns = samplingColumns.every((samplingItem: any) => {
    return orderByColumns.some((orderByItem: any) => orderByItem.name === samplingItem.name)
  })
  if (!isNameInOrderByColumns) {
    validator.value = true
    validatorMessage.value = i18n.t('dataset.tip.validatorSampling') as string
  }
  else {
    validator.value = false
    validatorMessage.value = null
  }
}

const beforeCheck = (): boolean => {
  const duplicateColumns = ArrayUtils.findDuplicates(formState.value.columns)
  if (duplicateColumns.length > 0) {
    validator.value = true
    validatorMessage.value = i18n.t('dataset.validator.duplicateColumn').replace('$VALUE', join(duplicateColumns, ','))
    return true
  }

  const orderByColumns = formState.value.columns.filter((item: any) => item.orderByKey)
  const primaryKeyColumns = formState.value.columns.filter((item: any) => item.primaryKey)
  if (orderByColumns.length === 0 && primaryKeyColumns.length === 0) {
    validator.value = true
    validatorMessage.value = i18n.t('dataset.validator.specifiedColumn')
    return true
  }

  if (!formState.value.name) {
    validator.value = true
    validatorMessage.value = i18n.t('dataset.validator.specifiedName')
    return true
  }
  return false
}

const onSubmit = () => {
  if (!beforeCheck()) {
    saving.value = true
    formState.value.query = value.value
    DatasetService.saveOrUpdate(formState.value as unknown as DatasetModel)
                  .then((response) => {
                    if (response.status) {
                      MfMessage.success(i18n.t('dataset.tip.publishSuccess').replace('$VALUE', formState.value.name as string))
                      router.push('/admin/dataset')
                    }
                  })
                  .finally(() => (saving.value = false))
  }
}

const onAddColumn = (index: number) => {
  formState.value.columns.splice(index + 1, 0, {
    id: null,
    name: null,
    aliasName: null,
    type: 'STRING',
    comment: null,
    defaultValue: null,
    position: index + 1,
    nullable: false,
    length: 0,
    original: null,
    orderByKey: false,
    partitionKey: false,
    primaryKey: false,
    samplingKey: false,
    mode: 'DIMENSION',
    virtualColumn: true,
    customColumn: true
  })
}

const onRemoveColumn = (index: number) => {
  formState.value.columns.splice(index, 1)
}

const setNullable = (item: any, checked: any) => {
  item.nullable = checked
}

const setOrderByKey = (item: any, checked: any) => {
  item.orderByKey = checked
  validatorSampling()
}

const setPartitionKey = (item: any, checked: any) => {
  item.partitionKey = checked
  validatorSampling()
}

const setPrimaryKey = (item: any, checked: any) => {
  item.primaryKey = checked
  validatorSampling()
}

const setSamplingKey = (item: any, checked: any) => {
  item.samplingKey = checked
  validatorSampling()
}

handleInitialize()
</script>
