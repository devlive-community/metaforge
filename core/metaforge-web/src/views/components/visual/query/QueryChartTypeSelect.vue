<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronDown, Search } from '@lucide/vue'
import { queryChartTypes, type QueryChartType } from '@/model/query-chart'
import QueryChartTypeIcon from './QueryChartTypeIcon.vue'

const props = defineProps<{ modelValue: QueryChartType; types?: readonly QueryChartType[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: QueryChartType] }>()
const { t } = useI18n(), keyword = ref('')
const open = ref(false)
watch(open, visible => { if (!visible) keyword.value = '' })
const choices = computed(() => (props.types ?? queryChartTypes).filter(type => t('queryChart.type.' + type).toLowerCase().includes(keyword.value.trim().toLowerCase())))
</script>

<template>
  <MfDropdown v-model:open="open" trigger="click" placement="bottomRight" class="w-full">
    <slot>
      <MfButton class="w-full justify-between!" :aria-label="$t('queryChart.chartType')"><span class="flex min-w-0 items-center gap-2"><QueryChartTypeIcon :type="modelValue" :size="16" class="shrink-0 text-primary"/><span class="truncate">{{ $t('queryChart.type.' + modelValue) }}</span></span><ChevronDown :size="14" class="shrink-0"/></MfButton>
    </slot>
    <template #overlay>
      <div class="w-60 max-w-[80vw] space-y-2 p-1">
        <div @click.stop>
          <MfInput v-model="keyword" size="sm" :aria-label="$t('queryChart.chartType')" :placeholder="$t('common.search')" clearable><template #prefix><Search :size="13"/></template></MfInput>
        </div>
        <div class="flex max-h-64 flex-col gap-1 overflow-y-auto">
          <MfButton v-for="type in choices" :key="type" size="sm" class="shrink-0 justify-start!" :variant="modelValue === type ? 'secondary' : 'ghost'" :aria-pressed="modelValue === type" @click="emit('update:modelValue', type)"><QueryChartTypeIcon :type="type" :size="16" class="shrink-0 text-primary"/>{{ $t('queryChart.type.' + type) }}</MfButton>
          <MfEmpty v-if="!choices.length" size="sm" :title="$t('common.noData')"/>
        </div>
      </div>
    </template>
  </MfDropdown>
</template>
