<template>
  <BaseLayout>
    <div class="flex min-h-screen w-full flex-col">
      <div class="container my-2 min-h-screen">
        <div class="flex w-full">
          <div class="mr-4 w-[250px] shrink-0 bg-surface">
            <MetadataSidebar/>
          </div>
          <div class="ml-2 min-w-0 flex-1">
            <MetadataContent/>
          </div>
        </div>
      </div>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import MetadataSidebar from '@/views/layouts/metadata/components/MetadataSidebar.vue'
import MetadataContent from '@/views/layouts/metadata/components/MetadataContent.vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
</script>
