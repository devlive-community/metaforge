<script setup lang="ts">
import { computed } from 'vue'
import { ChevronRight } from '@lucide/vue'
import type { HomeResource } from '@/composables/useHomeWorkspace'
import { screenEntry } from '@/model/screen-entry'
import ScreenThumbnail from '@/views/pages/admin/dashboard/components/ScreenThumbnail.vue'
const props = defineProps<{ items: HomeResource[] }>()
const entries = computed(() => props.items.slice(0, 3).map(screenEntry))
</script>
<template>
  <MfMenu>
    <MfMenuItem v-for="item in entries" :key="item.code" :key_="item.code" class="px-1! py-1.5!">
      <RouterLink :to="item.link" class="flex min-w-0 items-center gap-3">
        <span class="block aspect-[2/1] w-24 shrink-0 overflow-hidden rounded-mf-md border border-line"><ScreenThumbnail :screen="item"/></span>
        <span class="min-w-0 flex-1"><span class="mb-1 block truncate font-semibold text-content" :title="item.name">{{ item.name }}</span><MfTag size="sm" :variant="item.published ? 'success' : 'warning'">{{ $t(item.published ? 'dashboard.studio.status.published' : 'dashboard.studio.status.draft') }}</MfTag></span>
        <ChevronRight :size="14" class="shrink-0 text-content-tertiary"/>
      </RouterLink>
    </MfMenuItem>
  </MfMenu>
</template>
