<template>
  <div class="grid grid-cols-24 gap-2.5">
    <div class="col-span-4">
      <MfCard>
        <template #header>
          <div class="text-center w-full">
            {{ $t('user.common.sourceCount') }}
          </div>
        </template>
        <MfSkeleton :loading="loading">
          <div class="flex flex-col items-center justify-center py-8">
            <div class="text-2xl font-bold">
              {{ summary.sourceCount }}
            </div>
            <div class="text-xs text-muted-foreground mt-2">
              {{ $t('user.tip.sourceCountTip') }}
            </div>
          </div>
        </MfSkeleton>
      </MfCard>
    </div>

    <div class="col-span-4">
      <MfCard>
        <template #header>
          <div class="text-center w-full">
            {{ $t('user.common.queryCount') }}
          </div>
        </template>
        <MfSkeleton :loading="loading">
          <div class="flex flex-col items-center justify-center py-8">
            <div class="text-2xl font-bold">
              {{ summary.queryCount }}
            </div>
            <div class="text-xs text-muted-foreground mt-2">
              {{ $t('user.tip.queryCountTip') }}
            </div>
          </div>
        </MfSkeleton>
      </MfCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { HttpUtils } from '@/utils/http'
import UserService from '@/services/user'

defineOptions({ name: 'DashboardHome' })

const loading = ref(false)
const summary = reactive({
  sourceCount: 0,
  queryCount: 0
})

const handlerInitialize = () => {
  loading.value = true
  const axios = new HttpUtils().getAxios()
  axios.all([UserService.getSourceCount(), UserService.getQueryCount()])
       .then(axios.spread((source: any, query: any) => {
         if (source.status) {
           summary.sourceCount = source.data
         }
         if (query.status) {
           summary.queryCount = query.data
         }
       }))
       .finally(() => (loading.value = false))
}

handlerInitialize()
</script>
