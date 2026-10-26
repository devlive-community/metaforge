<template>
  <div class="space-y-1">
    <div v-for="(state, index) in states" :key="index" class="flex items-center gap-2 p-1">
      <span class="inline-block w-2 h-2 rounded-full" :style="{ background: dotColor(String(state)) }"/>
      <span class="text-xs">{{ getDatasetStateText(String(state)) }}</span>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { useUtil } from '@/utils/common'

export default defineComponent({
  name: 'DatasetState',
  props: {
    states: {
      type: Array
    }
  },
  setup()
  {
    const { getDatasetStateText } = useUtil()
    return { getDatasetStateText }
  },
  methods: {
    dotColor(code: string): string
    {
      if (code.endsWith('SUCCESS')) return 'hsl(142.1 76.2% 36.3%)'
      if (code.endsWith('FAILED')) return 'hsl(346.8 77.2% 49.8%)'
      if (code.endsWith('START')) return 'hsl(221.2 83.2% 53.3%)'
      return 'hsl(220 8% 50%)'
    }
  }
})
</script>
