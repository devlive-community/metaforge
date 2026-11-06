<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Clock3, GitBranch, Workflow } from '@lucide/vue'
import RunShell from '@/views/components/run/RunShell.vue'
/** Workflow pages: the full-screen editor (`studio`), or the run pages beside the orchestration / run records menu. */
const props = defineProps<{ code?: string; name?: string; state?: string; studio?: boolean }>()
const { t } = useI18n()
const items = computed(() => [
  { key: 'design', label: t('workflow.studio.design'), href: `/workflow/info/${props.code}`, icon: GitBranch },
  { key: 'runs', label: t('workflow.studio.runs'), href: `/workflow/monitor/${props.code}`, icon: Clock3 },
])
</script>
<template>
  <RunShell :studio="studio" :name="name || $t('common.workflow')" :state="state" :icon="Workflow" :items="items" :back="{ to: '/workflow', label: $t('workflow.studio.list') }">
    <template v-if="$slots.header" #header><slot name="header"/></template>
    <slot/>
  </RunShell>
</template>
