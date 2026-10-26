<template>
  <div>
    <div ref="content" :style="{width: width, height: height}"/>
  </div>
</template>
<script lang="ts">
import VChart from '@visactor/vchart'
import { cloneDeep } from 'lodash'
import { Configuration } from '../Configuration'
import { defineComponent } from 'vue'

let instance: VChart

export default defineComponent({
  name: 'VisualArea',
  props: {
    configuration: {
      type: Object as () => Configuration | null
    },
    submitted: {
      type: Boolean,
      default: true
    },
    width: {
      type: String,
      default: () => '100%'
    },
    height: {
      type: String,
      default: () => '400px'
    }
  },
  watch: {
    configuration: {
      handler: 'handlerReset',
      deep: true
    }
  },
  created()
  {
    this.handlerInitialize(false)
  },
  methods: {
    handlerInitialize(reset: boolean)
    {
      setTimeout(() => {
        try {
          if (this.configuration) {
            const options = {
              type: 'area',
              data: { values: this.configuration.columns },
              xField: this.configuration.chartConfigure?.xAxis!,
              yField: this.configuration.chartConfigure?.yAxis!
            }
            if (!reset) {
              instance = new VChart(options, { dom: this.$refs.content as HTMLElement })
              instance.renderAsync()
            }
            else {
              instance.updateSpec(options, true)
            }
            if (this.submitted) {
              const cloneOptions = cloneDeep(this.configuration)
              cloneOptions.headers = []
              cloneOptions.columns = []
              this.$emit('change', cloneOptions)
            }
          }
        }
        catch (e) {
          console.warn(e)
        }
      })
    },
    handlerReset()
    {
      this.handlerInitialize(true)
    }
  }
})
</script>
