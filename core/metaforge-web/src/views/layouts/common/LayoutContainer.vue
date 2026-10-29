<template>
  <div class="relative">
    <div class="relative flex items-center justify-center" v-if="loadingState" style="height: 100vh;">
      <MfSpinner size="lg" :label="loadingText"/>
    </div>

    <div v-show="!loadingState">
      <div class="relative z-30 flex w-full flex-col">
        <LayoutHeader/>
      </div>

      <!-- Workspace pages (query console) own a fixed-height IDE layout: no page
           padding, no footer, content fills the remaining viewport height. -->
      <main v-if="workspaceMode" class="container isolate h-[calc(100vh-4rem)] min-h-0 overflow-hidden py-3">
        <RouterView/>
      </main>

      <main v-else class="container isolate min-h-screen py-6">
        <RouterView/>
      </main>

      <LayoutFooter v-if="!workspaceMode"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeMount } from 'vue'
import { useRoute } from 'vue-router'
import LayoutHeader from '@/views/layouts/common/components/LayoutHeader.vue'
import LayoutFooter from '@/views/layouts/common/components/LayoutFooter.vue'
import { provideI18nHandler } from '@/i18n/I18n'

const { loadLocale, loadingState, loadingText } = provideI18nHandler()

const route = useRoute()
const workspaceMode = computed(() => route.meta.workspace === true)

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
