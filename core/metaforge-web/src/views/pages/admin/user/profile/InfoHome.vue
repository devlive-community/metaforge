<template>
  <MfCard :bordered="false" :title="$t('user.common.info')">
    <div class="text-sm text-gray-500">{{ $t('user.tip.info') }}</div>
    <MfDivider class="my-2"/>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <MfCard :title="$t('user.common.contribution')">
        <MfSkeleton :loading="loading">
          <div class="text-sm text-gray-500">{{ $t('user.tip.contribution') }}</div>
          <div class="p-2">
            <Contribution :data="heatmap.data"/>
          </div>
        </MfSkeleton>
      </MfCard>
      <MfCard :title="$t('user.common.radar7Days')">
        <MfSkeleton :loading="loading">
          <div class="text-sm text-gray-500">{{ $t('user.tip.radar7Days') }}</div>
          <div class="p-2">
            <VisualPie v-if="radar.configuration" :configuration="radar.configuration as any" height="200px" :submitted="false"/>
          </div>
        </MfSkeleton>
      </MfCard>
    </div>
  </MfCard>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { HttpUtils } from '@/utils/http'
import UserService from '@/services/user'
import { Configuration } from '@/views/components/visual/Configuration'
import VisualPie from '@/views/components/visual/components/VisualPie.vue'
import { DateUtils } from '@/utils/date'
import Contribution from '@/views/components/contribution/Contribution.vue'

defineOptions({ name: 'InfoHome' })

const loading = ref(false)
const heatmap = reactive<{ data: any[]; endDate?: string }>({ data: [] })
const radar = reactive<{ configuration: Configuration | null }>({ configuration: null })

const handlerInitialize = () => {
  loading.value = true
  const axios = new HttpUtils().getAxios()
  axios.all([UserService.getUserContribution(), UserService.getUserContributionRadar()])
       .then(axios.spread((fetchContribution: any, fetchRadar: any) => {
         if (fetchContribution.status) {
           heatmap.data = fetchContribution.data
           if (fetchContribution.data.length > 0) {
             const item = heatmap.data[heatmap.data.length - 1] as any
             heatmap.endDate = item.date
           }
           else {
             heatmap.endDate = DateUtils.formatTime(new Date(), 'YYYY-MM-DD')
           }
         }
         if (fetchRadar.status) {
           const configuration = new Configuration()
           configuration.columns = fetchRadar.data
           configuration.chartConfigure = { yAxis: 'count', xAxis: 'label', outerRadius: [1.2] }
           radar.configuration = configuration
         }
       }))
       .finally(() => (loading.value = false))
}

handlerInitialize()
</script>
