<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Clock3, GitBranch, Workflow } from '@lucide/vue'
import { workflowStateVariant } from '@/model/workflow'
/**
 * Layout of one workflow's pages. The full-screen editor (`studio`) takes the whole viewport;
 * other pages sit beside a side menu leading to the orchestration and the run records, laid out
 * like the account settings.
 */
const props = defineProps<{ code?: string; name?: string; state?: string; studio?: boolean }>()
const route = useRoute()
const items = computed(() => [
  { key: 'design', href: `/workflow/info/${props.code}`, icon: GitBranch },
  { key: 'runs', href: `/workflow/monitor/${props.code}`, icon: Clock3 },
])
const selected = computed(() => items.value.find(item => route.path === item.href)?.href || '')
</script>
<template>
  <div v-if="studio" class="flex h-full min-h-0 w-full overflow-hidden bg-surface-hover">
    <div class="flex min-h-0 min-w-0 flex-1 flex-col"><slot/></div>
  </div>
  <div v-else class="grid min-w-0 w-full grid-cols-1 gap-4 lg:grid-cols-[264px_minmax(0,1fr)]">
    <aside class="flex min-w-0 flex-col self-start rounded-mf-lg border border-line bg-surface px-3 py-4 shadow-mf-sm lg:min-h-[calc(100vh-220px)]">
      <router-link to="/workflow" class="mb-3 flex items-center gap-1 px-2 text-xs text-content-tertiary hover:text-primary"><ArrowLeft :size="12"/>{{ $t('workflow.studio.list') }}</router-link>
      <div class="flex items-center gap-2.5 px-2 pb-3.5">
        <span class="flex size-9 shrink-0 items-center justify-center rounded-mf-md bg-primary-light text-primary"><Workflow :size="18"/></span>
        <div class="min-w-0">
          <div class="truncate text-sm font-semibold text-content" :title="name">{{ name || $t('common.workflow') }}</div>
          <MfTag v-if="state" class="mt-1" size="sm" :variant="workflowStateVariant(state)">{{ $t('dataTable.state' + state) }}</MfTag>
        </div>
      </div>
      <MfMenu mode="inline" :selected-keys="[selected]">
        <MfMenuItem v-for="item in items" :key="item.href" :key_="item.href">
          <router-link :to="item.href" class="flex w-full items-center gap-2.5"><component :is="item.icon" :size="15"/><span>{{ $t('workflow.studio.' + item.key) }}</span></router-link>
        </MfMenuItem>
      </MfMenu>
    </aside>
    <div class="min-w-0"><slot/></div>
  </div>
</template>
