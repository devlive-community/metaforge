<template>
  <div class="relative h-full w-full" :style="{ width: width, height: height }">
    <div v-if="loading" class="flex items-center justify-center absolute inset-0">
      <MfSpinner size="lg"/>
    </div>

    <div v-if="localConfiguration && !loading">
      <div v-if="localConfiguration.message" class="p-4">
        <MfAlert variant="danger" :title="localConfiguration.message"/>
      </div>

      <div v-else-if="hasError && message" class="flex items-center justify-center absolute inset-0">
        <MfAlert variant="danger" :title="message"/>
      </div>

        <div v-else>
          <VisualTable v-if="configuration?.type === Type.TABLE"
                       :configuration="localConfiguration as any"
                       :submitted="false"
                       :width="width"
                       :height="height"/>

          <VisualLine v-else-if="configuration?.type === Type.LINE"
                      :configuration="localConfiguration as any"
                      :submitted="false"
                      :width="width"
                      :height="height"/>

          <VisualBar v-else-if="configuration?.type === Type.BAR"
                     :configuration="localConfiguration as any"
                     :submitted="false"
                     :width="width"
                     :height="height"/>

          <VisualArea v-else-if="configuration?.type === Type.AREA"
                      :configuration="localConfiguration as any"
                      :submitted="false"
                      :width="width"
                      :height="height"/>

          <VisualPie v-else-if="configuration?.type === Type.PIE"
                     :configuration="localConfiguration as any"
                     :submitted="false"
                     :width="width"
                     :height="height"/>

          <VisualHistogram v-else-if="configuration?.type === Type.HISTOGRAM"
                           :configuration="localConfiguration as any"
                           :submitted="false"
                           :width="width"
                           :height="height"/>

          <VisualWordCloud v-else-if="configuration?.type === Type.WORDCLOUD"
                           :configuration="localConfiguration as any"
                           :submitted="false"
                           :width="width"
                           :height="height"/>

          <VisualScatter v-else-if="configuration?.type === Type.SCATTER"
                         :configuration="localConfiguration as any"
                         :submitted="false"
                         :width="width"
                         :height="height"/>

          <VisualRadar v-else-if="configuration?.type === Type.RADAR"
                       :configuration="localConfiguration as any"
                       :submitted="false"
                       :width="width"
                       :height="height"/>

          <VisualFunnel v-else-if="configuration?.type === Type.FUNNEL"
                        :configuration="localConfiguration as any"
                        :submitted="false"
                        :width="width"
                        :height="height"/>

          <VisualGauge v-else-if="configuration?.type === Type.GAUGE"
                       :configuration="localConfiguration as any"
                       :submitted="false"
                       :width="width"
                       :height="height"/>

          <VisualRose v-else-if="configuration?.type === Type.ROSE"
                      :configuration="localConfiguration as any"
                      :submitted="false"
                      :width="width"
                      :height="height"/>
        </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { cloneDeep } from 'lodash'
import { Type } from '@/views/components/visual/Type'
import VisualWordCloud from '@/views/components/visual/components/VisualWordCloud.vue'
import VisualHistogram from '@/views/components/visual/components/VisualHistogram.vue'
import VisualPie from '@/views/components/visual/components/VisualPie.vue'
import VisualArea from '@/views/components/visual/components/VisualArea.vue'
import { Configuration } from './Configuration'
import VisualBar from '@/views/components/visual/components/VisualBar.vue'
import VisualLine from '@/views/components/visual/components/VisualLine.vue'
import VisualTable from '@/views/components/visual/components/VisualTable.vue'
import DatasetService from '@/services/dataset'
import VisualRadar from '@/views/components/visual/components/VisualRadar.vue'
import VisualScatter from '@/views/components/visual/components/VisualScatter.vue'
import VisualFunnel from '@/views/components/visual/components/VisualFunnel.vue'
import VisualGauge from '@/views/components/visual/components/VisualGauge.vue'
import VisualRose from '@/views/components/visual/components/VisualRose.vue'
import ExecuteService from '@/services/execute.ts'
import { ExecuteModel } from '@/model/execute.ts'

defineOptions({ name: 'VisualView' })

const props = withDefaults(defineProps<{
  configuration?: Configuration | null
  query?: any
  code?: string
  type?: string
  width?: string
  height?: string
  original?: string
}>(), {
  width: '100%',
  height: '400px'
})

const loading = ref(false)
const hasError = ref<any>(false)
const message = ref<string | null>(null)
const localConfiguration = ref<Configuration | null>(null)

const formatRaw = (response: any) => {
  if (localConfiguration.value) {
    if (response.data.isSuccessful) {
      localConfiguration.value.headers = response.data.headers
      localConfiguration.value.columns = response.data.columns
      localConfiguration.value.message = null
    }
    else {
      localConfiguration.value.headers = []
      localConfiguration.value.columns = []
      localConfiguration.value.message = response.data.message
    }
  }
}

const handleInitialize = () => {
  localConfiguration.value = cloneDeep(props.configuration) as Configuration
  setTimeout(() => {
    loading.value = true
    if (props.type === 'QUERY') {
      const configure: ExecuteModel = { name: props.original as any, content: props.query as any, mode: 'REPORT' }
      ExecuteService.execute(configure, null)
                    .then((response) => {
                      if (response.status && response.data.isSuccessful) {
                        formatRaw(response)
                        message.value = null
                      }
                      else {
                        hasError.value = response.message
                        message.value = response.message
                      }
                    })
                    .finally(() => (loading.value = false))
    }
    else {
      DatasetService.adhoc(props.code!, props.query)
                    .then((response) => {
                      if (response.status) {
                        formatRaw(response)
                      }
                      else {
                        MfMessage.error(response.message)
                      }
                    })
                    .finally(() => (loading.value = false))
    }
  })
}

handleInitialize()
</script>
