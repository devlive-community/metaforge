<template>
  <BaseLayout>
    <div class='h-screen'>
      <MfResult status="403"
                :title="$t('common.pageNotAuthorized')"
                :sub-title="$t('common.pageNotAuthorizedTip')">
        <template #extra>
          <MfButton @click="$router.push(String($route.query.redirect || '/'))">
            {{ $t('common.backTo') }}
          </MfButton>
          <MfButton variant="primary" @click="$router.push('/')">
            {{ $t('common.backToHome') }}
          </MfButton>
          <MfButton @click="$router.push('/auth/signin')">
            {{ $t('common.backToSignin') }}
          </MfButton>
        </template>
      </MfResult>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'

defineOptions({ name: 'NotAuthorized' })
</script>
