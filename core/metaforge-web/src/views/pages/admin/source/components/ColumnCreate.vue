<template>
  <MfModal v-model:open="visible" width="40%" :title="$t('source.common.newColumn')">
    <MfForm v-if="formState" :model="formModel" layout="vertical" @finish="onSubmit">
      <div class="mb-2">
        <MfButton variant="ghost" size="sm" @click="onAdd">
          <Plus :size="15" />
        </MfButton>
      </div>

      <div v-for="(item, index) in formState.columns" :key="index">
        <MfDivider>{{ item.name }}</MfDivider>

        <div class="mb-2">
          <MfButton variant="ghost" size="sm" class="text-danger" :disabled="item.removed" @click="onRemove(index)">
            <Minus :size="15" />
          </MfButton>
        </div>

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
            <MfSwitch v-model="formState.columns[index].isNullable"/>
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
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { Minus, Plus } from '@lucide/vue'

defineOptions({ name: 'ColumnCreate' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
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

const onAdd = () => {
  const newColumn = { removed: false }
  if (formState.value.columns.length === 0) {
    newColumn.removed = true
  }
  formState.value.columns.push(newColumn)
}

const onRemove = (index: number) => {
  if (formState.value.columns) {
    formState.value.columns.splice(index, 1)
  }
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  const code = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string

  if (code && database && table) {
    saving.value = true
    MetadataService.createColumn(code, database, table, formState.value)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       const columns = String(formState.value?.columns?.map((item: any) => item.name).join(', '))
                       MfMessage.success(t('source.tip.createColumnSuccess').replace('$VALUE', columns))
                       onCancel()
                     }
                     else {
                       MfMessage.error(response.data.message)
                     }
                   })
                   .finally(() => (saving.value = false))
  }
}

const code = route.params.source as string
loading.value = true
MetadataService.getDataTypes(code)
               .then((response) => {
                 if (response.status && response.data && response.data.isSuccessful) {
                   dataTypes.value = response.data.columns
                 }
               })
               .finally(() => (loading.value = false))

onAdd()
</script>
