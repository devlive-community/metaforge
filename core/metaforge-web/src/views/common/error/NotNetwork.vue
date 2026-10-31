<template>
  <BaseLayout>
    <div class="flex min-h-screen flex-col" style="min-height: 100vh">
      <!-- 顶部品牌栏：Logo + 离线状态 -->
      <header class="flex h-16 shrink-0 items-center justify-between border-b border-line bg-surface px-6">
        <div class="flex items-center gap-2.5">
          <img src="/static/images/logo.png" alt="MetaForge" class="h-8 w-auto"/>
          <span class="text-xl font-semibold text-content">MetaForge</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="size-2 rounded-full bg-warning"/>
          <span class="text-sm text-content-secondary">{{ $t('common.network.offline') }}</span>
        </div>
      </header>

      <!-- 主体：严格居中 -->
      <main class="relative flex flex-1 items-center justify-center overflow-hidden px-6">
        <!-- 圆弧装饰 -->
        <div aria-hidden="true" class="pointer-events-none absolute -left-44 -top-44 size-[430px] rounded-full border border-primary/10"/>
        <div aria-hidden="true" class="pointer-events-none absolute -left-28 -top-28 size-[270px] rounded-full border border-primary/15"/>
        <div aria-hidden="true" class="pointer-events-none absolute -bottom-48 -right-44 size-[460px] rounded-full border border-primary/10"/>
        <div aria-hidden="true" class="pointer-events-none absolute -bottom-32 -right-28 size-[300px] rounded-full border border-primary/15"/>

        <div class="relative flex flex-col items-center text-center">
          <!-- 品牌 Logo + 断网徽章 + 光晕 -->
          <div class="relative">
            <div aria-hidden="true" class="absolute left-1/2 top-1/2 size-72 -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary/10 blur-3xl"/>
            <img src="/static/images/logo.png" alt="MetaForge" class="relative w-56 select-none" draggable="false"/>
            <span class="absolute -bottom-1 -right-8 inline-flex size-20 items-center justify-center rounded-full border border-line bg-surface shadow-mf-md">
              <WifiOff :size="34" :stroke-width="1.8" class="text-content-tertiary"/>
            </span>
          </div>

          <h1 class="mt-8 text-3xl font-semibold text-content">{{ $t('common.pageNotNetwork') }}</h1>
          <p class="mt-3 text-base text-content-secondary">{{ $t('common.tip.pageNotNetwork') }}</p>

          <MfButton size="lg" variant="primary" class="mt-8 px-10!" :loading="checking" @click="onRetry">
            <RefreshCw :size="15"/>
            {{ $t('common.network.retry') }}
          </MfButton>
        </div>
      </main>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { RefreshCw, WifiOff } from '@lucide/vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
import SetupService from '@/services/setup'
import { useRouteRedirect } from '@/composables/useRouteRedirect'

defineOptions({ name: 'NotNetwork' })

const { t } = useI18n()
const { restore } = useRouteRedirect()

const checking = ref(false)

// HttpUtils resolves transport failures with code 0; any other response means the server answered.
// 恢复后 replace 回断网前的原地址，避免历史里残留中间页。
const reconnect = (silent: boolean) => {
  if (checking.value) return
  checking.value = true
  SetupService.getStatus()
               .then(response => {
                 if (response && response.code !== 0) {
                   restore()
                 }
                 else if (!silent) {
                   MfMessage.error(t('common.network.reconnectFailed'))
                 }
               })
               .finally(() => (checking.value = false))
}

const onRetry = () => reconnect(false)
// 系统网络恢复时自动探测服务端，连通即回原地址；探测失败不打扰用户。
const onOnline = () => reconnect(true)

onMounted(() => window.addEventListener('online', onOnline))
onBeforeUnmount(() => window.removeEventListener('online', onOnline))
</script>
