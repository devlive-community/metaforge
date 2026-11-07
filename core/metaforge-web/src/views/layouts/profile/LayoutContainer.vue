<template>
  <BaseLayout>
    <div class="flex min-h-screen w-full flex-col">
      <div class="relative z-30 flex w-full flex-col">
        <LayoutHeader/>
      </div>
      <main class="isolate min-h-screen px-2 py-6">
        <div class="grid min-w-0 w-full grid-cols-1 gap-4 lg:grid-cols-[264px_minmax(0,1fr)]">
          <div class="min-w-0">
            <LayoutSidebar/>
          </div>
          <div class="min-w-0">
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
