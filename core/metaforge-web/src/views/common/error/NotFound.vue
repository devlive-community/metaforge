<template>
  <BaseLayout>
    <div class="h-screen">
      <MfResult status="404"
                :title="$t('common.pageNotFound')"
                :sub-title="$t('common.pageNotFoundTip')">
        <template #extra>
          <MfButton v-if="original" @click="restore">
            {{ $t('common.backTo') }}
          </MfButton>
          <MfButton variant="primary" @click="$router.push('/')">
            {{ $t('common.backToHome') }}
          </MfButton>
        </template>
      </MfResult>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
import { useRouteRedirect } from '@/composables/useRouteRedirect'

defineOptions({ name: 'NotFound' })

// redirect 是跳到 404 之前所在的页面，"返回"回到那里。
const { original, restore } = useRouteRedirect()
</script>
