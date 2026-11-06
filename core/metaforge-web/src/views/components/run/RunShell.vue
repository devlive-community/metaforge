<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Workflow } from '@lucide/vue'
import { workflowStateVariant } from '@/model/workflow'
/**
 * Layout of run pages (workflow runs, dataset syncs): a side menu that stays in place and a
 * content area whose header stays fixed while the body scrolls; stacked on narrow screens.
 * `studio` gives the whole viewport to a full-screen editor instead.
 */
const props = defineProps<{
  name?: string; state?: string; studio?: boolean; icon?: Component
  back?: { to: string; label: string }
  items?: { key: string; label: string; href: string; icon: Component }[]
}>()
const route = useRoute()
const selected = computed(() => props.items?.find(item => route.path === item.href)?.href || '')
</script>
<template>
  <div v-if="studio" class="flex h-full min-h-0 w-full overflow-hidden bg-surface-hover">
    <div class="flex min-h-0 min-w-0 flex-1 flex-col"><slot/></div>
  </div>
  <div v-else class="flex h-full min-h-0 w-full flex-col gap-4 overflow-y-auto px-2 pt-6 lg:flex-row lg:overflow-hidden">
    <aside class="flex min-w-0 shrink-0 flex-col rounded-mf-lg border border-line bg-surface px-3 py-4 shadow-mf-sm lg:mb-6 lg:w-[264px] lg:overflow-y-auto">
      <router-link v-if="back" :to="back.to" class="mb-3 flex items-center gap-1 px-2 text-xs text-content-tertiary hover:text-primary"><ArrowLeft :size="12"/>{{ back.label }}</router-link>
      <div class="flex items-center gap-2.5 px-2 pb-3.5">
        <span class="flex size-9 shrink-0 items-center justify-center rounded-mf-md bg-primary-light text-primary"><component :is="icon || Workflow" :size="18"/></span>
        <div class="min-w-0">
          <div class="truncate text-sm font-semibold text-content" :title="name">{{ name || '—' }}</div>
          <MfTag v-if="state" class="mt-1" size="sm" :variant="workflowStateVariant(state)">{{ $t('dataTable.state' + state) }}</MfTag>
        </div>
      </div>
      <MfMenu mode="inline" :selected-keys="[selected]">
        <MfMenuItem v-for="item in items || []" :key="item.href" :key_="item.href">
          <router-link :to="item.href" class="flex w-full items-center gap-2.5"><component :is="item.icon" :size="15"/><span>{{ item.label }}</span></router-link>
        </MfMenuItem>
      </MfMenu>
    </aside>
    <div class="flex min-w-0 flex-1 flex-col lg:min-h-0">
      <!-- The page header stays in place; only the body below it scrolls. -->
      <div v-if="$slots.header" class="shrink-0 pb-4"><slot name="header"/></div>
      <div class="min-w-0 flex-1 pb-6 lg:min-h-0 lg:overflow-y-auto"><slot/></div>
    </div>
  </div>
</template>
