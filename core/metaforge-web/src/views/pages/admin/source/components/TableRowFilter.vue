<template>
  <MfModal v-model:open="visible"
           width="50%"
           :title="$t('source.common.filterData')">
    <MfForm :model="formState" layout="vertical">
      <div v-for="(item, index) in formState.filters" :key="index" class="flex items-center space-x-2">
        <MfFormItem name="" class="w-full">
          <MfSelect v-model="item.index"
                    :options="columns.map((column: any) => ({ label: column, value: column }))"
                    @change="onFetchOperations(item.index, item)"/>
        </MfFormItem>

        <MfFormItem name="" class="w-full">
          <MfSelect v-model="item.operator"
                    :options="(item.operations ?? []).map((operation: string) => ({ label: operation, value: operation }))"
                    :disabled="!item.index"/>
        </MfFormItem>

        <MfFormItem name="" class="w-full">
          <MfInput v-model="item.value" :disabled="!item.operator"/>
        </MfFormItem>

        <MfFormItem name="">
          <MfButton variant="ghost" size="sm" class="text-danger" @click="onRemoveFilter(index)">
            <Minus :size="16" />
          </MfButton>
        </MfFormItem>
      </div>

      <MfFormItem name="" class="space-y-1" :label="$t('source.common.filterCondition')">
        <MfButton size="sm" @click="onAddFilter">
          {{ $t('source.common.addFilter') }}
        </MfButton>
      </MfFormItem>
    </MfForm>

    <template #footer>
      <MfSpace>
        <MfButton @click="onCancel">
          {{ $t('common.cancel') }}
        </MfButton>
        <MfButton variant="primary" @click="onFilter">
          {{ $t('common.apply') }}
        </MfButton>
      </MfSpace>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { cloneDeep } from 'lodash'
import { Minus } from '@lucide/vue'

defineOptions({ name: 'TableRowFilter' })

const props = withDefaults(defineProps<{
  isVisible?: boolean
  columns?: any[]
  types?: any[]
  configure?: any
}>(), {
  isVisible: false,
  columns: () => [],
  types: () => []
})
const emit = defineEmits<{
  (e: 'close', value: boolean): void
  (e: 'apply', value: any[]): void
}>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const formState = ref<{ filters: any[] }>({
  filters: props.configure ? cloneDeep(props.configure) : []
})

const onAddFilter = () => {
  formState.value.filters.push({})
}

const onRemoveFilter = (index: number) => {
  formState.value.filters.splice(index, 1)
}

const onFetchOperations = (value: any, filter: any) => {
  filter.name = value
  filter.operations = ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE', 'LIKE', 'IN', 'NOT IN', 'IS NULL', 'IS NOT NULL']
}

const onCancel = () => {
  visible.value = false
}

const onFilter = () => {
  emit('apply', formState.value.filters)
  onCancel()
}
</script>
