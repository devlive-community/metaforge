<template>
  <MfModal v-model:open="visible" :title="title" @close="onCancel">
    <MfForm :model="formState" layout="vertical" @finish="onCommit">
      <div v-if="String(columnType) === 'FILTER'">
        <MfFormItem name="expression" :label="$t('common.expression')">
          <MfSelect v-model="formState.expression"
                    :placeholder="$t('dataset.tip.selectExpression')"
                    :options="[
                      { label: $t('dataset.common.columnExpressionIsNull'), value: Expression.IS_NULL },
                      { label: $t('dataset.common.columnExpressionIsNotNull'), value: Expression.IS_NOT_NULL },
                      { label: $t('dataset.common.columnExpressionIsLike'), value: Expression.IS_LIKE },
                      { label: $t('dataset.common.columnExpressionIsNotLike'), value: Expression.IS_NOT_LIKE },
                      { label: $t('dataset.common.columnExpressionEquals'), value: Expression.EQ },
                      { label: $t('dataset.common.columnExpressionNotEquals'), value: Expression.NE },
                      { label: $t('dataset.common.columnExpressionGreaterThan'), value: Expression.GT },
                      { label: $t('dataset.common.columnExpressionGreaterThanOrEquals'), value: Expression.GTE },
                      { label: $t('dataset.common.columnExpressionLessThan'), value: Expression.LT },
                      { label: $t('dataset.common.columnExpressionLessThanOrEquals'), value: Expression.LTE }
                    ]"/>
        </MfFormItem>

        <MfFormItem v-if="formState.expression && formState.expression !== Expression.IS_NULL && formState.expression !== Expression.IS_NOT_NULL"
                    name="value"
                    :label="$t('common.value')">
          <MfInput v-model="formState.value"/>
        </MfFormItem>
      </div>

      <div v-else>
        <MfSpace direction="vertical" fill>
          <MfFormItem v-if="String(columnType) === 'METRIC'"
                       class="w-full"
                       name="expression"
                       :label="$t('common.expression')">
            <MfSelect v-model="formState.expression"
                      :placeholder="$t('dataset.tip.selectExpression')"
                      :options="[
                        ...(formState.type === ColumnType.NUMBER ? [{ label: $t('dataset.common.columnExpressionSum'), value: Expression.SUM }] : []),
                        { label: $t('dataset.common.columnExpressionCount'), value: Expression.COUNT },
                        { label: $t('dataset.common.columnExpressionMax'), value: Expression.MAX },
                        { label: $t('dataset.common.columnExpressionMin'), value: Expression.MIN },
                        ...(formState.type === ColumnType.NUMBER ? [{ label: $t('dataset.common.columnExpressionAvg'), value: Expression.AVG }] : [])
                      ]"
                      @change="onUpdateAlias"/>
          </MfFormItem>

          <MfFormItem class="w-full" name="alias" :label="$t('common.alias')">
            <MfInput v-model="formState.alias"/>
          </MfFormItem>

          <MfFormItem class="w-full" name="order" :label="$t('common.sort')">
            <MfRadioGroup v-model="formState.order" variant="button" @change="onChangeToggle">
              <MfRadio value="">{{ $t('dataset.common.columnSortNone') }}</MfRadio>
              <MfRadio value="ASC">{{ $t('dataset.common.columnOrderAsc') }}</MfRadio>
              <MfRadio value="DESC">{{ $t('dataset.common.columnOrderDesc') }}</MfRadio>
            </MfRadioGroup>
          </MfFormItem>

          <MfFormItem v-if="String(columnType) === 'DIMENSION'"
                       class="w-full"
                       name="function"
                       :label="$t('dataset.common.customFunction')">
            <MfInput v-model="formState.function"/>
          </MfFormItem>
        </MfSpace>
      </div>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>

          <MfButton variant="primary" type="submit">
            {{ $t('common.save') }}
          </MfButton>
        </MfSpace>
      </div>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { cloneDeep } from 'lodash'
import { ColumnType, Type } from '@/views/pages/admin/dataset/Type'
import { Expression } from '@/views/pages/admin/dataset/Expression'
import { Model } from '../../model/model'

defineOptions({ name: 'DatasetColumnConfigure' })

const props = withDefaults(defineProps<{
  isVisible?: boolean
  columnType?: Type | null
  content?: any
  configure?: any
}>(), {
  isVisible: false,
  columnType: () => Type.DIMENSION,
  content: () => null
})
const emit = defineEmits<{
  (e: 'close', value: boolean): void
  (e: 'commit', value: Model): void
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const title = ref('')
const formState = ref<Model>({} as Model)

const prefixText = () => {
  let prefix = `${ t('dataset.common.columnModeMetric') }`
  if (props.columnType === Type.DIMENSION) {
    prefix = `${ t('dataset.common.columnModeDimension') }`
  }
  else if (props.columnType === Type.FILTER) {
    prefix = `${ t('dataset.common.columnModeFilter') }`
  }
  return prefix
}

const handleInitialize = () => {
  formState.value = {} as Model
  const prefix = prefixText()
  title.value = `${ prefix } [ ${ props.content.aliasName ? props.content.aliasName : props.content.name } ] ${ t('common.configure') }`
  if (props.configure) {
    formState.value = cloneDeep(props.configure) as Model
  }
  else {
    formState.value.id = props.content.id
  }
}

const onUpdateAlias = () => {
  const startWith = `${ prefixText() } [ ${ props.content.aliasName ? props.content.aliasName : props.content.name } ]`
  if (formState.value) {
    if (formState.value.expression === Expression.SUM) {
      formState.value.alias = `${ startWith } ${ t('dataset.common.columnExpressionSum') }`
    }
    else if (formState.value.expression === Expression.COUNT) {
      formState.value.alias = `${ startWith } ${ t('dataset.common.columnExpressionCount') }`
    }
    else if (formState.value.expression === Expression.MAX) {
      formState.value.alias = `${ startWith } ${ t('dataset.common.columnExpressionMax') }`
    }
    else if (formState.value.expression === Expression.MIN) {
      formState.value.alias = `${ startWith } ${ t('dataset.common.columnExpressionMin') }`
    }
    else if (formState.value.expression === Expression.AVG) {
      formState.value.alias = `${ startWith } ${ t('dataset.common.columnExpressionAvg') }`
    }
    else {
      formState.value.alias = startWith
    }
  }
}

const onChangeToggle = (event: any) => {
  formState.value.order = event?.target ? event.target.value : event
}

const onCancel = () => {
  visible.value = false
}

const onCommit = () => {
  if (formState.value) {
    formState.value.mode = String(props.columnType)
    emit('commit', formState.value)
    onCancel()
  }
}

handleInitialize()
</script>
