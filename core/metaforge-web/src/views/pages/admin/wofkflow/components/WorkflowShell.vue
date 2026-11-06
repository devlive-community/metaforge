<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Clock3, GitBranch, LayoutList, Settings2 } from '@lucide/vue'
const props = defineProps<{ code?: string; studio?: boolean }>()
const route = useRoute()
const links = computed(() => [
  { key: 'list', path: '/workflow', icon: LayoutList, disabled: false },
  { key: 'design', path: props.code ? `/workflow/info/${props.code}` : '/workflow/info', icon: GitBranch, disabled: false },
  { key: 'nodeConfig', path: `/workflow/info/${props.code}?view=config`, icon: Settings2, disabled: !props.code },
  { key: 'runs', path: `/workflow/monitor/${props.code}`, icon: Clock3, disabled: !props.code },
])
const active = computed(() => route.path.includes('/monitor/') ? 'runs' : route.path.includes('/info') ? route.query.view === 'config' ? 'nodeConfig' : 'design' : 'list')
</script>
<template>
  <div class="flex h-full min-h-0 w-full overflow-hidden bg-surface-hover">
    <aside v-if="!studio" class="hidden w-[196px] shrink-0 flex-col border-r border-line bg-surface lg:flex">
      <div class="px-5 pb-3 pt-6 text-[10px] font-semibold tracking-widest text-content-tertiary">WORKSPACE / {{ $t('common.workflow') }}</div>
      <nav class="flex flex-col gap-2 p-3"><template v-for="link in links" :key="link.key"><span v-if="link.disabled" class="flex items-center gap-3 rounded-mf-md px-3 py-3 text-xs text-content-tertiary opacity-50"><component :is="link.icon" :size="16"/>{{ $t('workflow.studio.' + link.key) }}</span><router-link v-else :to="link.path" class="flex items-center gap-3 rounded-mf-md px-3 py-3 text-xs font-medium transition-colors" :class="active === link.key ? 'bg-primary-light text-primary' : 'text-content-secondary hover:bg-surface-hover'"><component :is="link.icon" :size="16"/>{{ $t('workflow.studio.' + link.key) }}</router-link></template></nav>
      <div class="mt-auto border-t border-line p-4"><router-link to="/" class="flex items-center gap-2 text-xs text-content-tertiary hover:text-primary"><ArrowLeft :size="14"/>{{ $t('common.home') }}</router-link></div>
    </aside>
    <div class="flex min-h-0 min-w-0 flex-1 flex-col"><slot/></div>
  </div>
</template>
