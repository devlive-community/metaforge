<template>
  <div class="relative">
    <div class="relative flex items-center justify-center" v-if="loadingState" style="height: 100vh;">
      <MfSpinner size="lg" :label="loadingText"/>
    </div>

    <div v-show="!loadingState" :class="(workspaceMode || workbenchMode) && 'flex h-dvh flex-col overflow-hidden'">
      <div class="relative z-30 flex w-full shrink-0 flex-col">
        <!-- 数据源管理工作台使用独立头部（原型设计，通栏），其余页面使用默认头部 -->
        <WorkbenchHeader v-if="workbenchMode"/>
        <LayoutHeader v-else/>
      </div>

      <!-- Workspace pages (query console) own a fixed-height IDE layout: no page
           padding, no footer, content fills the remaining viewport height. -->
      <main v-if="workspaceMode" class="isolate min-h-0 flex-1 overflow-hidden p-2">
        <RouterView/>
      </main>

      <!-- 数据源管理工作台：通栏全宽、无页脚、无默认内边距（严格按原型） -->
      <!-- Workbench pages (source workspace, compare) fit the viewport and scroll inside their panels. -->
      <main v-else-if="workbenchMode" class="isolate min-h-0 flex-1 overflow-hidden p-2">
        <RouterView/>
      </main>

      <main v-else class="container isolate min-h-screen py-6">
        <RouterView/>
      </main>

      <LayoutFooter v-if="!workspaceMode && !workbenchMode"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { isSourceWorkbenchRoute } from '@/model/source-manage'
import { computed, onBeforeMount } from 'vue'
import { useRoute } from 'vue-router'
import LayoutHeader from '@/views/layouts/common/components/LayoutHeader.vue'
import WorkbenchHeader from '@/views/layouts/metadata/components/WorkbenchHeader.vue'
import LayoutFooter from '@/views/layouts/common/components/LayoutFooter.vue'
import { provideI18nHandler } from '@/i18n/I18n'

const { loadLocale, loadingState, loadingText } = provideI18nHandler()

const route = useRoute()
const workspaceMode = computed(() => route.meta.workspace === true)
const workbenchMode = computed(() => isSourceWorkbenchRoute(route))

onBeforeMount(async () => {
  try {
    const locale = localStorage.getItem('locale') || 'zh-CN'
    await loadLocale(locale)
  }
  catch (error) {
    console.error('Failed to load locale:', error)
  }
})
</script>
