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
  name: 'VisualLine',
  props: {
    configuration: {
      type: Object as () => Configuration
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
              type: 'line',
              data: { values: this.configuration.columns },
              xField: this.configuration.chartConfigure?.xAxis,
              yField: this.configuration.chartConfigure?.yAxis,
              seriesField: this.configuration.chartConfigure?.series,
              invalidType: this.configuration.chartConfigure?.invalidType
            } as any

            if (this.configuration.chartConfigure) {
              options.title = {
                visible: this.configuration.chartConfigure?.titleVisible,
                text: this.configuration.chartConfigure?.titleText,
                subtext: this.configuration.chartConfigure?.titleSubText,
                orient: this.configuration.chartConfigure?.titlePosition,
                align: this.configuration.chartConfigure?.titleAlign
              }
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
