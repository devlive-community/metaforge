<template>
  <div class="relative min-h-screen">
    <!-- Loading State -->
    <div class="relative flex items-center justify-center" v-if="loadingState" style="height: 100vh;">
      <MfSpinner size="lg" :label="loadingText"/>
    </div>

    <!-- Error State -->
    <div v-if="errorState && !loadingState" class="fixed inset-0 flex items-center justify-center bg-background/80 backdrop-blur-sm">
      <div class="flex flex-col items-center space-y-4">
        <h3 class="text-xl font-semibold">{{ errorMessage }}</h3>
        <MfButton @click="handleRetry">Retry 重试</MfButton>
      </div>
    </div>

    <!-- Content -->
    <div v-show="!loadingState && !errorState">
      <slot/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeMount } from 'vue'
import { provideI18nHandler } from '@/i18n/I18n'

const {
  loadLocale,
  loadingState,
  loadingText,
  errorState,
  errorMessage
} = provideI18nHandler()

const handleRetry = async () => {
  try {
    const locale = localStorage.getItem('locale') || 'zh_cn'
    await loadLocale(locale)
  }
  catch (error) {
    console.error('Failed to load locale:', error)
  }
}

onBeforeMount(async () => {
  await handleRetry()
})
</script>
