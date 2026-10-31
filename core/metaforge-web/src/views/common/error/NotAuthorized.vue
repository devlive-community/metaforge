<template>
  <BaseLayout>
    <div class='h-screen'>
      <MfResult status="403"
                :title="$t('common.pageNotAuthorized')"
                :sub-title="$t('common.pageNotAuthorizedTip')">
        <template #extra>
          <MfButton v-if="original" @click="restore">
            {{ $t('common.backTo') }}
          </MfButton>
          <MfButton variant="primary" @click="$router.push('/')">
            {{ $t('common.backToHome') }}
          </MfButton>
          <MfButton @click="$router.push({ path: '/auth/signin', query })">
            {{ $t('common.backToSignin') }}
          </MfButton>
        </template>
      </MfResult>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
import { useRouteRedirect } from '@/composables/useRouteRedirect'

defineOptions({ name: 'NotAuthorized' })

const { original, query, restore } = useRouteRedirect()
</script>
