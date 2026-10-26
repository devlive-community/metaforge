<template>
  <BaseLayout>
    <div class="h-screen">
      <MfResult status="warning"
                :title="$t('common.pageNotNetwork')"
                :sub-title="$t('common.tip.pageNotNetwork')">
        <template #extra>
          <MfButton @click="$router.push(String($route.query.redirect || '/'))">
            {{ $t('common.backTo') }}
          </MfButton>
          <MfButton variant="primary" @click="$router.push(String($route.query.redirect || '/'))">
            {{ $t('common.backToHome') }}
          </MfButton>
        </template>
      </MfResult>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'

defineOptions({ name: 'NotNetwork' })
</script>
