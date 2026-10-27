<template>
  <BaseLayout>
    <div class="flex min-h-screen w-full flex-col">
      <div class="relative z-30 flex w-full flex-col">
        <LayoutHeader/>
      </div>
      <main class="container isolate min-h-screen py-6">
        <div class="flex w-full gap-4">
          <div class="shrink-0">
            <LayoutSidebar/>
          </div>
          <div class="ml-6 min-w-0 flex-1">
            <RouterView/>
          </div>
        </div>
      </main>
      <LayoutFooter/>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import LayoutHeader from '@/views/layouts/common/components/LayoutHeader.vue'
import LayoutSidebar from '@/views/layouts/profile/components/LayoutSidebar.vue'
import LayoutFooter from '@/views/layouts/common/components/LayoutFooter.vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
</script>
