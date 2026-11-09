<template>
  <div class="flex min-h-0 flex-col" :style="{ width, height }">
    <ResultTable v-if="configuration" fill :headers="configuration.headers || []" :rows="configuration.columns || []"/>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { cloneDeep } from 'lodash'
import ResultTable from '@/views/components/table/ResultTable.vue'
import { Configuration } from '../Configuration'

/** The table chart of a report: the dataset's rows in the same table as query results. */
const props = withDefaults(defineProps<{ configuration?: Configuration | null; submitted?: boolean; width?: string; height?: string }>(), {
  configuration: null,
  submitted: true,
  width: '100%',
  height: '400px',
})
const emit = defineEmits<{ change: [configuration: Configuration] }>()

// A submitted table keeps no field settings: the chart is the rows themselves.
onMounted(() => {
  if (props.submitted && props.configuration) {
    const settings = cloneDeep(props.configuration)
    settings.headers = []
    settings.columns = []
    emit('change', settings)
  }
})
</script>
