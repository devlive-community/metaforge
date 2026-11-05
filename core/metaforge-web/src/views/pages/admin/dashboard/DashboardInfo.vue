<template>
  <div v-if="loading" class="flex min-h-0 flex-1 items-center justify-center"><MfSpinner size="lg"/></div>
  <div v-else-if="failed" class="flex min-h-0 flex-1 items-center justify-center p-4">
    <MfAlert variant="danger" :title="$t('dashboard.studio.loadFailed')">
      <MfButton size="sm" @click="load">{{ $t('common.refresh') }}</MfButton>
    </MfAlert>
  </div>
  <DashboardEditor v-else :key="code" :info="data"/>
</template>

<script setup lang="ts">
import { computed, onScopeDispose, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'
import DashboardService from '@/services/dashboard'
import type { DashboardModel } from '@/model/dashboard'
import DashboardEditor from './components/DashboardEditor.vue'

const route = useRoute()
const code = computed(() => String(route.params.code || ''))
const loading = ref(false)
const failed = ref(false)
const data = shallowRef<DashboardModel | null>(null)
let request = 0

async function load() {
  const id = ++request
  const dashboardCode = code.value
  failed.value = false
  data.value = null
  loading.value = !!dashboardCode
  if (!dashboardCode) return
  try {
    const response = await DashboardService.getByCode(dashboardCode)
    if (id !== request) return
    if (!response.status || !response.data) throw new Error('Dashboard unavailable')
    data.value = response.data
  }
  catch { if (id === request) failed.value = true }
  finally { if (id === request) loading.value = false }
}
watch(code, load, { immediate: true })
onScopeDispose(() => { request++ })
</script>
