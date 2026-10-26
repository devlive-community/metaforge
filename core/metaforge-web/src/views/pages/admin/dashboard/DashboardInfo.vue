<template>
  <div class="relative min-h-screen">
    <div v-if="loading" class="flex items-center justify-center py-16">
      <MfSpinner size="lg"/>
    </div>
    <div v-else>
      <DashboardEditor v-if="version === '1.0'" :info="dataInfo"/>

      <DashboardEditorV2 v-else :info="dataInfo"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import DashboardService from '@/services/dashboard'
import { DashboardModel } from '@/model/dashboard'
import DashboardEditor from '@/views/pages/admin/dashboard/components/DashboardEditor.vue'
import DashboardEditorV2 from '@/views/pages/admin/dashboard/components/DashboardEditorV2.vue'

defineOptions({ name: 'DashboardInfo' })

const route = useRoute()
const loading = ref(false)
const dataInfo = ref<DashboardModel | null>(null)
const version = ref('1.0')

const handleInitialize = async () => {
  const code = route.params.code as string

  if (!code) {
    dataInfo.value = null
    return
  }

  try {
    loading.value = true
    const response = await DashboardService.getByCode(code)
    if (response.status) {
      dataInfo.value = response.data
      version.value = response.data.version
    }
    else {
      MfMessage.error(response.message)
    }
  }
  catch (error) {
    console.error('Failed to fetch dashboard:', error)
    MfMessage.error('Failed to load dashboard data')
  }
  finally {
    loading.value = false
  }
}

onMounted(() => {
  handleInitialize()
})
</script>
