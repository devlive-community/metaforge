<template>
  <MfModal v-model:open="visible" width="40%" :title="$t('source.common.newColumn')">
    <div v-if="loading" class="flex h-40 items-center justify-center"><MfSpinner/></div>

    <MfForm v-else-if="formState" :model="formModel" layout="vertical" @finish="onSubmit">
      <div v-for="(_item, index) in formState.columns" :key="index">
        <div class="grid grid-cols-2 gap-4">
          <MfFormItem :name="`columns.${index}.name`"
                      :label="`${$t('source.common.columnName')} ${index + 1}`"
                      :rules="[
                          { required: true, message: $t('source.validator.columnName.required') },
                          { pattern: /^[A-Za-z][A-Za-z0-9_-]*$/, message: $t('source.validator.columnName.pattern') }
                      ]">
            <MfInput v-model="formState.columns[index].name" :placeholder="$t('source.placeholder.columnName')"/>
          </MfFormItem>

          <MfFormItem :name="`columns.${index}.type`"
                      :label="$t('source.common.columnType')"
                      :rules="[{ required: true, message: $t('source.validator.columnType.required') }]">
            <MfSelect v-model="formState.columns[index].type"
                      :options="dataTypeOptions"
                      :placeholder="$t('source.placeholder.columnType')"
                      :loading="loading"/>
          </MfFormItem>

          <MfFormItem :name="`columns.${index}.length`"
                      :label="$t('source.common.columnLength')"
                      :rules="[
                          { required: true, message: $t('source.validator.columnLength.required') },
                          { min: 1, message: $t('source.validator.columnLength.min') }
                      ]">
            <MfInputNumber v-model="formState.columns[index].length"
                           :placeholder="$t('source.placeholder.columnLength')"
                           :style="{ width: '100%' }"
                           :min="1"/>
          </MfFormItem>

          <MfFormItem :name="`columns.${index}.defaultValue`" :label="$t('source.common.columnDefaultValue')">
            <MfInput v-model="formState.columns[index].defaultValue" :placeholder="$t('source.placeholder.columnDefaultValue')"/>
          </MfFormItem>
        </div>

        <div class="grid grid-cols-3 gap-4">
          <MfFormItem :name="`columns.${index}.primaryKey`" :label="$t('source.common.columnPrimaryKey')">
            <MfSwitch v-model="formState.columns[index].primaryKey"/>
          </MfFormItem>

          <MfFormItem :name="`columns.${index}.autoIncrement`" :label="$t('source.common.columnAutoIncrement')">
            <MfSwitch v-model="formState.columns[index].autoIncrement"/>
          </MfFormItem>

          <MfFormItem :name="`columns.${index}.isNullable`" :label="$t('source.common.columnIsNullable')">
            <MfSwitch :model-value="formState.columns[index].isNullable === 'YES'"
                      @update:model-value="(checked: boolean) => formState.columns[index].isNullable = checked ? 'YES' : 'NO'"/>
          </MfFormItem>
        </div>

        <MfFormItem :name="`columns.${index}.comment`" :label="$t('source.common.columnComment')">
          <MfTextarea v-model="formState.columns[index].comment" :placeholder="$t('source.placeholder.columnComment')"/>
        </MfFormItem>
      </div>

      <MfSpace>
        <MfButton @click="onCancel">{{ $t('common.cancel') }}</MfButton>

        <MfButton variant="primary" type="submit" :loading="saving" :disabled="saving">
          {{ $t('common.save') }}
        </MfButton>
      </MfSpace>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import HttpUtils from '@/utils/http'

defineOptions({ name: 'ColumnChange' })

const props = withDefaults(defineProps<{ isVisible?: boolean; column?: string }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const route = useRoute()
const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const saving = ref(false)
const dataTypes = ref<any[]>([])
const formState = ref<any>({ columns: [] })

// MfFormItem 仅支持单层 name，用代理把 `columns.0.name` 等嵌套路径解析到 formState 上，保持原 a-form 嵌套校验
const formModel = new Proxy(formState.value, {
  get(target, prop: string) {
    return prop.split('.').reduce((obj: any, key) => (obj ? obj[key] : undefined), target)
  }
})

const dataTypeOptions = computed(() => dataTypes.value.map((dataType: any) => ({ label: dataType, value: dataType })))

const onCancel = () => {
  visible.value = false
}

const handleInitialize = () => {
  const code = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string

  if (code && database && table) {
    loading.value = true
    HttpUtils.all([MetadataService.getColumn(code, database, table, { columns: [{ name: props.column }] }), MetadataService.getDataTypes(code)])
             .then(HttpUtils.spread((...responses: any[]) => {
               const [columnResponse, dataTypeResponse] = responses

               if (columnResponse.status && columnResponse.data && columnResponse.data.isSuccessful) {
                 const primaryKey = columnResponse.data.columns.find((item: any) => item.type_name === 'primary')

                 columnResponse.data.columns
                               .filter((item: any) => item.type_name === 'column')
                               .forEach((item: any) => {
                                 formState.value.columns.push({
                                   name: item.object_name,
                                   type: item.object_data_type,
                                   length: item.object_length,
                                   comment: item.object_comment,
                                   defaultValue: item.object_default_value,
                                   primaryKey: primaryKey !== undefined,
                                   autoIncrement: item.extra === 'auto_increment',
                                   isNullable: item.object_nullable
                                 })
                               })
               }
               else {
                 MfMessage.error(columnResponse.data.message)
               }

               if (dataTypeResponse.status && dataTypeResponse.data && dataTypeResponse.data.isSuccessful) {
                 dataTypes.value = dataTypeResponse.data.columns
               }
               else {
                 MfMessage.error(dataTypeResponse.data.message)
               }
             }))
             .finally(() => (loading.value = false))
  }
}

const onSubmit = () => {
  const code = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string

  if (code && database && table) {
    saving.value = true
    MetadataService.changeColumn(code, database, table, formState.value)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       const columns = formState.value.columns.map((item: any) => item.name).join(', ') as string
                       MfMessage.success(t('source.tip.changeColumnSuccess').replace('$VALUE', columns))
                       onCancel()
                     }
                     else {
                       MfMessage.error(response.data.message)
                     }
                   })
                   .finally(() => (saving.value = false))
  }
}

handleInitialize()
</script>
