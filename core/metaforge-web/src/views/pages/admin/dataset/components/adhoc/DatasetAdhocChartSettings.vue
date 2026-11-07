<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { chartFieldKind, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { chartDataSpec } from '@/model/query-chart-data'
import QueryChartTypeSelect from '@/views/components/visual/query/QueryChartTypeSelect.vue'
import QueryChartDataFields from '@/views/components/visual/query/QueryChartDataFields.vue'
import QueryChartSettings from '@/views/components/visual/query/QueryChartSettings.vue'
import DashboardInspectorGroup from '@/views/pages/admin/dashboard/components/DashboardInspectorGroup.vue'

const props = defineProps<{ options: QueryChartOptions; headers: string[]; rows: QueryChartRow[] }>()
const emit = defineEmits<{ change: [value: QueryChartOptions] }>()
const { t } = useI18n()
const section = ref('axes')
const fields = computed(() => props.headers.map(value => ({ value, label: value })))
const numbers = computed(() => fields.value.filter(field => chartFieldKind(props.rows, field.value) === 'number'))
const specialized = computed(() => chartDataSpec(props.options.type))
const timeCategory = computed(() => chartFieldKind(props.rows, props.options.category) === 'date')
function patch(value: Partial<QueryChartOptions>) { emit('change', { ...props.options, ...value }) }
</script>

<template>
  <MfForm :model="options" class="gap-3!">
    <DashboardInspectorGroup :title="$t('queryChart.chartType')">
      <MfFormItem name="type">
        <QueryChartTypeSelect :model-value="options.type" @update:model-value="patch({ type: $event })"/>
      </MfFormItem>
    </DashboardInspectorGroup>
    <template v-if="options.type !== 'table'">
      <DashboardInspectorGroup icon="data" :title="$t('queryChart.dimensions')">
        <QueryChartDataFields v-if="specialized" :options="options" :fields="fields" :numeric-fields="numbers" @change="patch"/>
        <template v-else>
          <MfFormItem name="category" :label="$t('queryChart.category')"><MfSelect :model-value="options.category" :options="fields" searchable @update:model-value="patch({ category: String($event) })"/></MfFormItem>
          <MfFormItem name="metric" :label="$t('queryChart.metric')"><MfSelect :model-value="options.metric" :options="numbers" searchable @update:model-value="patch({ metric: String($event), aggregation: 'sum' })"/></MfFormItem>
          <MfFormItem name="series" :label="$t('queryChart.series')"><MfSelect :model-value="options.series" :options="[{ value: '', label: $t('queryChart.ungrouped') }, ...fields]" searchable @update:model-value="patch({ series: String($event) })"/></MfFormItem>
        </template>
      </DashboardInspectorGroup>
      <DashboardInspectorGroup icon="appearance" :title="$t('dataset.adhoc.chartSettings')">
        <MfFormItem name="settingsSection"><MfSelect v-model="section" :aria-label="$t('dataset.adhoc.chartSettings')" :options="['axes', 'legend', 'style', 'interaction'].map(value => ({ value, label: t('queryChart.panel.' + value) }))"/></MfFormItem>
      </DashboardInspectorGroup>
      <QueryChartSettings collapsible :options="options" :panel="section" :time-category="timeCategory" @change="(key, value) => patch({ [key]: value })"/>
    </template>
  </MfForm>
</template>
