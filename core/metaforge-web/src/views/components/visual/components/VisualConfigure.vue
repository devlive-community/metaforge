<template>
  <MfDrawer v-model:open="visible"
            width="40%"
            style="margin-right: -1rem;"
            :title="$t('common.configure')"
            @close="onCancel">
    <MfForm v-if="formState" :model="formState" class="mt-2" layout="vertical">
      <MfTabs v-model:active-key="activeGroup" tab-position="right">
        <MfTabPane v-for="group in fieldGroup"
                   :key="group.label"
                   :item-key="group.label"
                   :tab="group.label">
          <MfFormItem v-for="item in fieldGroup.find(value => value.label === activeGroup)?.fields"
                      :key="item.field"
                      :name="item.field"
                      :label="item.label">
            <MfSwitch v-if="item.type === 'SWITCH'" v-model="formState[Object(item.field)]"/>

            <input v-else-if="item.type === 'SLIDER'"
                   v-model.number="formState[Object(item.field)]"
                   type="range"
                   class="w-full accent-primary"
                   :min="item.min"
                   :max="item.max"
                   :step="item.step"/>

            <MfInput v-else-if="item.type === 'TEXT'"
                     v-model="formState[Object(item.field)]"
                     :placeholder="item.label"
                     :disabled="item.disabled?.field ? formState[Object(item.disabled?.field)] === item.disabled?.value : false"/>

            <MfSelect v-else
                      v-model="formState[Object(item.field)]"
                      :disabled="item.disabled?.field ? formState[Object(item.disabled?.field)] === item.disabled?.value : false"
                      :options="item.values ? item.values : (configuration.headers || []).map((header: any) => ({ label: header, value: header }))"/>
          </MfFormItem>
        </MfTabPane>
      </MfTabs>
    </MfForm>

    <template #footer>
      <MfSpace>
        <MfButton @click="onCancel">
          {{ $t('common.cancel') }}
        </MfButton>
        <MfButton variant="primary" @click="onSubmit">
          {{ $t('common.apply') }}
        </MfButton>
      </MfSpace>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChartFieldGroup, Configuration, IChart } from '@/views/components/visual/Configuration.ts'
import { cloneDeep, keys } from 'lodash'

defineOptions({ name: 'VisualConfigure' })

const props = withDefaults(defineProps<{
  configuration?: Configuration
  fieldGroup?: ChartFieldGroup[]
  isVisible?: boolean
}>(), {
  fieldGroup: () => [],
  isVisible: false
})
const emit = defineEmits<{
  (e: 'close', value: boolean): void
  (e: 'change', value: IChart | null): void
}>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const activeGroup = ref(props.fieldGroup[0]?.label)
const formState = ref<IChart | null>(null)

if (props.configuration && keys(props.configuration.chartConfigure).length > 0) {
  formState.value = cloneDeep(props.configuration.chartConfigure) as IChart
}
else {
  const obj = {} as any
  props.fieldGroup.forEach(group => {
    group?.fields?.forEach(field => {
      if (field.field) {
        obj[field.field] = undefined
      }
    })
  })
  formState.value = obj
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  emit('change', formState.value)
  onCancel()
}
</script>
