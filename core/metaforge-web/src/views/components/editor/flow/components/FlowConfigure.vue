<template>
  <div>
    <MfDrawer v-model:open="visible"
              width="25%"
              :title="`[ ${ data?.name } ] ${ $t('common.configure') }`"
              @close="onCancel">
      <MfForm :model="formState" layout="vertical">
        <div v-if="data" v-for="item in data.configure" :key="item.field">
          <MfFormItem v-if="item.input"
                      :name="item.field"
                      :label="item.field">
            <div v-if="item.type === 'INPUT'" class="flex flex-row items-start font-normal space-x-1">
              <MfInput v-model="item.value" :disabled="item.override && item.tooltip"/>
              <MfTooltip v-if="item.tooltip" :content="item.tooltip">
                <MfCheckbox v-model="item.override"/>
              </MfTooltip>
            </div>

            <div v-if="item.type === 'TEXT'" class="flex flex-row items-start font-normal space-x-1">
              <MfTextarea v-model="item.value" :disabled="item.override && item.tooltip"/>
              <MfTooltip v-if="item.tooltip" :content="item.tooltip">
                <MfCheckbox v-model="item.override"/>
              </MfTooltip>
            </div>

            <div v-if="item.type === 'SWITCH'" class="flex flex-row items-start font-normal space-x-1">
              <MfSwitch v-model="item.value" :disabled="item.override && item.tooltip"/>
              <MfTooltip v-if="item.tooltip" :content="item.tooltip">
                <MfCheckbox v-model="item.override" class="ml-2"/>
              </MfTooltip>
            </div>

            <div v-if="item.type === 'SELECT'" class="flex flex-row items-start font-normal space-x-1">
              <MfSelect v-model="item.value" :disabled="item.override && item.tooltip" :style="{ width: '100%' }"
                        :options="(item.defaultValues || []).map((option: any) => ({ label: option, value: option }))"/>
              <MfTooltip v-if="item.tooltip" :content="item.tooltip">
                <MfCheckbox v-model="item.override" class="ml-2"/>
              </MfTooltip>
            </div>

            <div v-if="item.type === 'NUMBER'" class="flex flex-row items-start font-normal space-x-1">
              <MfInputNumber v-model="item.value" :disabled="item.override && item.tooltip" :style="{width: item.width + 'px'}"/>
              <MfTooltip v-if="item.tooltip" :content="item.tooltip">
                <MfCheckbox v-model="item.override" class="ml-2"/>
              </MfTooltip>
            </div>

            <div v-if="item.description" class="text-xs text-content-tertiary mt-1">{{ item.description }}</div>
          </MfFormItem>
        </div>
      </MfForm>

      <template #footer>
        <MfButton variant="primary" @click="onSubmit()">
          <Save :size="16" />
          <span>{{ $t('common.save') }}</span>
        </MfButton>
      </template>
    </MfDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Configuration } from '@/views/components/editor/flow/Configuration.ts'
import { Save } from '@lucide/vue'

defineOptions({ name: 'FlowConfigure' })

const props = withDefaults(defineProps<{ isVisible?: boolean; data?: Configuration | null }>(), {
  isVisible: false
})
const emit = defineEmits<{
  (e: 'close', value: boolean): void
  (e: 'onChange', value: any): void
}>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const formState = ref({
  items: props.data?.configure || []
})

const filterConfigure = (meta: any) => {
  const result = {} as any
  meta.filter((item: { input: any }) => item.input)
      .forEach((item: { origin: any; value: any }) => {
        if (item.value !== null) {
          result[item.origin] = item.value
        }
      })
  return result
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.data) {
    const configure = {
      code: props.data.code,
      protocol: props.data.protocol,
      configures: filterConfigure(formState.value.items),
      type: props.data.nodeType
    }
    emit('onChange', configure)
  }
}
</script>
