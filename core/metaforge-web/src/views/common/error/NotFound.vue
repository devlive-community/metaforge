<template>
  <BaseLayout>
    <div class="h-screen">
      <MfResult status="404"
                :title="$t('common.pageNotFound')"
                :sub-title="$t('common.pageNotFoundTip')">
        <template #extra>
          <MfButton v-if="$route.redirectedFrom"
                    @click="$router.push($route.redirectedFrom.fullPath)">
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

defineOptions({ name: 'NotFound' })
</script>
