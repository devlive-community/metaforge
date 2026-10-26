<template>
  <div class="dc-workflow-configure flex flex-col h-full bg-white border-l" :style="{ width: `${ width }px` }">
    <template v-if="selectedNode">
      <div class="p-3 border-b">
        <div class="text-sm font-semibold">{{ selectedNode.label }}</div>
        <div class="text-xs text-gray-400 mt-1">{{ selectedNode.description }}</div>
      </div>

      <div class="flex-1 overflow-y-auto p-3">
        <div v-for="item in selectedNode.configure || []"
             :key="item.field"
             class="mb-3">
          <div class="text-xs text-gray-600 mb-1">
            <span v-if="item.required" class="text-red-500">*</span>
            {{ item.label }}
          </div>

          <MfInput v-if="item.type === 'input'"
                   :model-value="item.value"
                   :disabled="item.disabled"
                   :placeholder="item.placeholder"
                   :invalid="item.validated && !item.validated.valid"
                   @update:model-value="(value) => onChange(item, value)"/>

          <MfInput v-else-if="item.type === 'password'"
                   type="password"
                   show-password
                   :model-value="item.value"
                   :disabled="item.disabled"
                   :placeholder="item.placeholder"
                   :invalid="item.validated && !item.validated.valid"
                   @update:model-value="(value) => onChange(item, value)"/>

          <MfTextarea v-else-if="item.type === 'textarea'"
                      :model-value="item.value"
                      :disabled="item.disabled"
                      :rows="3"
                      :placeholder="item.placeholder"
                      :invalid="item.validated && !item.validated.valid"
                      @update:model-value="(value) => onChange(item, value)"/>

          <MfInputNumber v-else-if="item.type === 'number'"
                         :model-value="item.value"
                         :disabled="item.disabled"
                         :placeholder="item.placeholder"
                         class="w-full"
                         :invalid="item.validated && !item.validated.valid"
                         @change="(value: any) => onChange(item, value)"/>

          <MfSwitch v-else-if="item.type === 'switch'"
                    :model-value="item.value === true || item.value === 'true'"
                    :disabled="item.disabled"
                    @update:model-value="(value: any) => onChange(item, value)"/>

          <MfSelect v-else-if="item.type === 'select'"
                    :model-value="item.value"
                    :disabled="item.disabled"
                    :placeholder="item.placeholder"
                    clearable
                    class="w-full"
                    :options="(item.options || []) as any"
                    @change="(value: any) => onChange(item, value)"/>

          <MfSelect v-else-if="item.type === 'array'"
                    :model-value="toArrayValue(item.value)"
                    :disabled="item.disabled"
                    tags
                    searchable
                    :placeholder="item.placeholder"
                    class="w-full"
                    :options="toArrayValue(item.value).map((v) => ({ label: v, value: v }))"
                    @change="(value: any) => onChange(item, value)"/>

          <MfTextarea v-else-if="item.type === 'map'"
                      :model-value="toMapText(item.value)"
                      :disabled="item.disabled"
                      :rows="3"
                      placeholder="key=value"
                      :invalid="item.validated && !item.validated.valid"
                      @update:model-value="(value) => onChange(item, fromMapText(value))"/>

          <MfInput v-else
                   :model-value="item.value"
                   :disabled="item.disabled"
                   :placeholder="item.placeholder"
                   @update:model-value="(value) => onChange(item, value)"/>

          <div v-if="item.description" class="text-[10px] text-gray-400 mt-1">{{ item.description }}</div>
          <div v-if="item.validated && !item.validated.valid" class="text-xs text-red-500 mt-1">
            {{ item.validated.message }}
          </div>
        </div>
      </div>
    </template>

    <div v-else class="flex-1 flex items-center justify-center text-sm text-gray-400 p-4 text-center">
      {{ $t('workflow.placeholder.selectNode') }}
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from 'vue'
import { ConfigureItem, NodeInstance } from './types'

export default defineComponent({
  name: 'WorkflowConfigure',
  props: {
    selectedNode: {
      type: Object as PropType<NodeInstance | null>,
      default: null
    },
    width: {
      type: Number,
      default: 300
    }
  },
  emits: ['update:node'],
  setup(props, { emit })
  {
    const validateItem = (item: ConfigureItem): { valid: boolean; message?: string } => {
      if (!item.required) {
        return { valid: true }
      }
      const empty = item.value === undefined || item.value === null || item.value === ''
          || (Array.isArray(item.value) && item.value.length === 0)
      return empty ? { valid: false, message: item.message || `${ item.label } is required` } : { valid: true }
    }

    const onChange = (item: ConfigureItem, value: any) => {
      item.value = value
      item.validated = validateItem(item)
      if (props.selectedNode) {
        emit('update:node', props.selectedNode)
      }
    }

    const toArrayValue = (value: any): string[] => {
      if (Array.isArray(value)) {
        return value.map((v) => String(v))
      }
      if (value !== undefined && value !== null && value !== '') {
        return [String(value)]
      }
      return []
    }

    const toMapText = (value: any): string => {
      if (!value || typeof value !== 'object') {
        return ''
      }
      return Object.entries(value)
          .map(([key, val]) => `${ key }=${ val }`)
          .join('\n')
    }

    const fromMapText = (text: string): Record<string, string> => {
      const result: Record<string, string> = {}
      text.split('\n')
          .map((line) => line.trim())
          .filter((line) => line.includes('='))
          .forEach((line) => {
            const index = line.indexOf('=')
            result[line.slice(0, index).trim()] = line.slice(index + 1).trim()
          })
      return result
    }

    return { onChange, toArrayValue, toMapText, fromMapText }
  }
})
</script>
