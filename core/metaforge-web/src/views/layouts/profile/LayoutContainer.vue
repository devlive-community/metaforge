<template>
  <BaseLayout>
    <div class="flex min-h-screen w-full flex-col">
      <LayoutHeader/>
      <div class="container min-h-screen" style="margin-top: 20px; margin-bottom: 24px;">
        <div class="flex w-full gap-4">
          <div class="shrink-0">
            <LayoutSidebar/>
          </div>
          <div class="ml-6 min-w-0 flex-1">
            <LayoutBreadcrumb style="margin-bottom: 16px;"/>
            <RouterView/>
          </div>
        </div>
      </div>
      <LayoutFooter/>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import LayoutHeader from '@/views/layouts/common/components/LayoutHeader.vue'
import LayoutSidebar from '@/views/layouts/profile/components/LayoutSidebar.vue'
import LayoutFooter from '@/views/layouts/common/components/LayoutFooter.vue'
import LayoutBreadcrumb from '@/views/layouts/common/components/LayoutBreadcrumb.vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
</script>
