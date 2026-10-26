<template>
  <div style="height: 1080px;">
    <DataBuilderEditor show-guidelines :panelWidth="150" :items="panels">
      <template #panel-label="{ item }">
        {{ item.label }}
      </template>

      <template #text="{ configure, isSelected }">
        <div type="h1" :class="isSelected ? 'text-[var(--dc-primary)]' : 'text-gray-900'">
          {{ getConfigValue(configure, 'text', $t('builder.component.basic.text')) }}
        </div>
      </template>
    </DataBuilderEditor>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import DataBuilderEditor from '@/views/components/editor/builder/DataBuilderEditor.vue'

const { t } = useI18n()

const getConfigValue = (configure: any, groupName: string, label: string) => {
  if (!configure) {
    return null
  }

  const group = configure.find(g => g.key === groupName)
  if (!group) {
    return null
  }

  const item = group.items?.find(item => item.label === label)
  return item?.value
}

const panels = computed(() => ([
  {
    group: t('builder.component.basic'),
    children: [
      {
        type: 'text', label: t('builder.component.basic.text'), configure: [
          {
            group: t('builder.component.textGroup'),
            key: 'text',
            items: [
              { type: 'textarea', label: t('builder.component.basic.text'), placeholder: t('builder.component.basic.text.placeholder'), value: null }
            ]
          },
          {
            group: t('builder.component.styleGroup'),
            key: 'style',
            items: [
              { type: 'text', label: t('builder.component.basic.text.backgroundColor'), key: 'backgroundColor', value: '#FFFFFF' },
              { type: 'number', label: t('builder.component.basic.text.borderRadius'), key: 'borderRadius', 'value': 12, min: 0, max: 100, formatter: (value) => `${ value }px` }
            ]
          }
        ]
      },
    ]
  },
  {
    group: t('builder.component.chart'),
    children: [
      {
        type: 'text', label: t('builder.component.chartLine'), configure: [
          {
            group: t('builder.component.textGroup'),
            key: 'text',
            items: [
              { type: 'textarea', label: t('builder.component.basic.text'), placeholder: t('builder.component.basic.text.placeholder'), value: null }
            ]
          },
          {
            group: t('builder.component.styleGroup'),
            key: 'style',
            items: [
              { type: 'text', label: t('builder.component.basic.text.backgroundColor'), key: 'backgroundColor', value: '#FFFFFF' },
              { type: 'number', label: t('builder.component.basic.text.borderRadius'), key: 'borderRadius', 'value': 12, min: 0, max: 100, formatter: (value) => `${ value }px` }
            ]
          }
        ]
      },
    ]
  }
]))
</script>
