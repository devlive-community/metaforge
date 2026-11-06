<script setup lang="ts">
import { computed, onScopeDispose, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import DashboardService from '@/services/dashboard'
import type { DashboardModel } from '@/model/dashboard'
import { readDashboardDocument } from '@/model/dashboard-layout'
import { useUserStore } from '@/stores/user'
import DashboardPresentationCanvas from '@/views/pages/admin/dashboard/components/DashboardPresentationCanvas.vue'

/**
 * The user's home page when they chose a data screen for it: only the published screen, under the
 * usual navigation; editing and fullscreen stay on the data screen pages. An unavailable screen
 * offers the default home page instead.
 */
const route = useRoute(), router = useRouter(), { t } = useI18n(), userStore = useUserStore()
const code = computed(() => String(route.params.code || ''))
const data = shallowRef<DashboardModel>()
const loading = ref(false), failed = ref(false), busy = ref(false)
const design = computed(() => data.value ? readDashboardDocument(data.value.configure, data.value.name, data.value.description, t).published : undefined)
let request = 0

async function load() {
  const id = ++request
  loading.value = true; failed.value = false; data.value = undefined
  try {
    const response = await DashboardService.getByCode(code.value)
    if (id !== request) return
    if (!response.status || !response.data) throw new Error('Dashboard unavailable')
    data.value = response.data
  } catch { if (id === request) failed.value = true }
  finally { if (id === request) loading.value = false }
}
async function unset() {
  if (busy.value) return
  busy.value = true
  try {
    await userStore.setHomePage(null)
    MfMessage.success(t('dashboard.home.cleared'))
    await router.replace('/home')
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dashboard.home.failed')) }
  finally { busy.value = false }
}
watch(code, load, { immediate: true })
onScopeDispose(() => { request++ })
</script>
<template>
  <div class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
    <div v-if="loading" class="flex flex-1 items-center justify-center"><MfSpinner size="lg"/></div>
    <div v-else-if="failed || !design" class="flex flex-1 items-center justify-center p-4">
      <MfEmpty :title="$t('dashboard.home.unavailable')" :description="$t('dashboard.home.unavailableHint')">
        <div class="flex flex-wrap justify-center gap-2">
          <MfButton @click="router.push({ path: '/home', query: { default: '1' } })">{{ $t('dashboard.home.openDefault') }}</MfButton>
          <MfButton variant="primary" :loading="busy" @click="unset">{{ $t('dashboard.home.unset') }}</MfButton>
        </div>
      </MfEmpty>
    </div>
    <template v-else>
      <DashboardPresentationCanvas :design="design" :reports="data?.reports || []"/>
    </template>
  </div>
</template>
