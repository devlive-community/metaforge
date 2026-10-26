<template>
  <div class="relative min-h-screen">
    <div v-if="loading" class="flex items-center justify-center py-16">
      <MfSpinner size="lg"/>
    </div>

    <DashboardView v-else-if="data" :layouts="JSON.parse(String(data.configure))"/>

    <MfAlert v-else variant="danger" :title="$t('dashboard.tip.notFound').replace('$VALUE', String(route.currentRoute?.value?.params['code']))"/>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DashboardService from '@/services/dashboard'
import { useRouter } from 'vue-router'
import DashboardView from '@/views/pages/admin/dashboard/components/DashboardView.vue'
import { DashboardModel } from '@/model/dashboard'

defineOptions({ name: 'DashboardPreview' })

const route = useRouter()

const loading = ref(true)
const data = ref<DashboardModel | null>(null)

const handlerInitialize = () => {
  loading.value = true
  const params = route.currentRoute.value.params
  DashboardService.getByCode(params['code'] as string)
                  .then((response) => {
                    if (response.status) {
                      data.value = response.data
                    }
                  })
                  .finally(() => (loading.value = false))
}

handlerInitialize()
</script>
