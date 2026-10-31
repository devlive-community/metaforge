<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'" :title="$t('source.ddl.panel.title')"
            @update:open="value => !value && emit('close')">
    <template #title>
      <span class="flex flex-col">
        <span class="text-base font-semibold text-content">{{ $t('source.ddl.panel.title') }}</span>
        <span class="mt-0.5 text-xs font-normal text-content-tertiary">{{ $t('source.ddl.panel.subtitle') }}</span>
      </span>
    </template>
    <div class="flex flex-col gap-5">
      <nav class="flex flex-col gap-1" :aria-label="$t('source.ddl.panel.title')">
        <button v-for="action in DDL_ACTIONS" :key="action" type="button" :disabled="!enabled(action)"
                class="flex w-full items-center gap-3 rounded-mf-md border px-3 py-2.5 text-left text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-50"
                :class="action === active ? 'border-primary/30 bg-primary/10 font-medium text-primary' : 'border-line text-content hover:bg-surface-hover'"
                :aria-current="action === active ? 'true' : undefined" @click="emit('action', action)">
          <component :is="ACTION_ICONS[action]" :size="16" class="shrink-0"/>
          <span class="min-w-0 flex-1 truncate">{{ $t(`source.ddl.action.${action}`) }}</span>
          <ChevronRight :size="15" class="shrink-0" :class="action === active ? 'text-primary' : 'text-content-tertiary'"/>
        </button>
      </nav>

      <section class="border-t border-line pt-4">
        <div class="mb-3 flex items-center justify-between gap-2">
          <h4 class="m-0 text-base font-semibold text-content">{{ $t('source.ddl.history.title') }}</h4>
          <button type="button" class="text-xs text-primary hover:underline disabled:opacity-50" :disabled="!recent.length" @click="emit('history')">{{ $t('source.ddl.history.all') }}</button>
        </div>
        <div v-if="recentLoading && !recent.length" class="flex h-24 items-center justify-center"><MfSpinner size="sm"/></div>
        <MfEmpty v-else-if="!recent.length" :title="$t('source.ddl.history.empty')"/>
        <ol v-else class="m-0 flex list-none flex-col gap-3 p-0">
          <li v-for="entry in recent" :key="entry.id" class="flex gap-3">
            <span class="inline-flex size-9 shrink-0 items-center justify-center rounded-mf-md" :class="KIND_TONES[entry.kind]">
              <component :is="ACTION_ICONS[ddlActionOf(entry.kind)]" :size="16"/>
            </span>
            <span class="flex min-w-0 flex-col text-xs">
              <span class="tabular-nums text-content-tertiary">{{ entry.createTime }}</span>
              <span class="truncate text-sm text-content">{{ $t(`source.ddl.history.kind.${entry.kind}`) }}</span>
              <span class="truncate text-content-tertiary">{{ $t('source.ddl.history.user', { user: entry.username }) }}</span>
            </span>
          </li>
        </ol>
      </section>

      <section class="border-t border-line pt-4">
        <h4 class="m-0 mb-2 flex items-center gap-2 text-base font-semibold text-content"><Info :size="16" class="text-primary"/>{{ $t('source.ddl.note.title') }}</h4>
        <ul class="m-0 flex list-disc flex-col gap-1 pl-5 text-xs text-content-secondary">
          <li v-for="index in 4" :key="index">{{ $t(`source.ddl.note.item${index}`) }}</li>
        </ul>
      </section>
    </div>
  </MfDrawer>
</template>

<script setup lang="ts">
import { ChevronRight, Columns3, FileInput, GitCompare, Info, Link, PencilRuler, ShieldCheck, Table } from '@lucide/vue'
import { DDL_ACTIONS, ddlActionOf, type DdlAction, type DdlHistoryEntry, type DdlKind } from '@/model/source-table-ddl'

defineProps<{
  open: boolean; container: HTMLElement | null; width: number; active: DdlAction; recent: DdlHistoryEntry[]; recentLoading: boolean
  enabled: (action: DdlAction) => boolean
}>()
const emit = defineEmits<{ action: [action: DdlAction]; history: []; close: [] }>()
const ACTION_ICONS: Record<DdlAction, unknown> = {
  create: Table, structure: Columns3, indexes: Link, constraints: ShieldCheck, 'sample-data': FileInput, alter: PencilRuler, compare: GitCompare,
}
const KIND_TONES: Record<DdlKind, string> = {
  create: 'bg-success/10 text-success', structure: 'bg-warning/10 text-warning', indexes: 'bg-success/10 text-success', constraints: 'bg-info/10 text-info',
  'sample-data': 'bg-info/10 text-info', alter: 'bg-primary/10 text-primary', 'alter-table': 'bg-primary/10 text-primary',
}
</script>
