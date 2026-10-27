<template>
  <div class="relative">
    <div class="relative flex items-center justify-center" v-if="loadingState" style="height: 100vh;">
      <MfSpinner size="lg" :label="loadingText"/>
    </div>

    <div v-show="!loadingState">
      <div class="relative z-30 flex w-full flex-col">
        <LayoutHeader/>
      </div>

      <main class="container isolate min-h-screen py-6">
        <RouterView/>
      </main>

      <LayoutFooter/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeMount } from 'vue'
import LayoutHeader from '@/views/layouts/common/components/LayoutHeader.vue'
import LayoutFooter from '@/views/layouts/common/components/LayoutFooter.vue'
import { provideI18nHandler } from '@/i18n/I18n'

const { loadLocale, loadingState, loadingText } = provideI18nHandler()

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
