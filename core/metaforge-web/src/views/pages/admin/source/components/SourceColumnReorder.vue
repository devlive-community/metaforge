<template>
  <MfModal :open="open" width="960px" height="min(720px, 100%)" body-class="p-0" :close-disabled="saving" :close-on-overlay="!saving" :close-on-esc="!saving"
           @update:open="value => !value && emit('update:open', false)">
    <template #title>
      <span class="flex items-center gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-primary"><ListOrdered :size="20"/></span>
        <span class="flex min-w-0 flex-col">
          <span class="truncate">{{ $t('source.structure.reorderTitle') }}</span>
          <span class="mt-0.5 truncate text-xs font-normal text-content-tertiary">{{ $t('source.structure.reorderSubtitle') }}</span>
        </span>
      </span>
    </template>
    <div class="grid h-full min-h-0 grid-cols-[minmax(220px,300px)_minmax(0,1fr)]">
      <aside class="flex min-h-0 flex-col border-r border-line bg-surface-hover/40 p-4">
        <MfInput v-model="keyword" size="sm" clearable :placeholder="$t('source.structure.reorderSearch')">
          <template #prefix><Search :size="14"/></template>
        </MfInput>
        <div class="mb-1 mt-4 px-2 text-xs font-medium text-content-secondary">{{ $t('source.structure.reorderAll', { count: draft.length }) }}</div>
        <div class="min-h-0 flex-1 overflow-y-auto" @dragleave="sort.leave">
          <MfEmpty v-if="!filtered.length" size="sm" :title="$t('common.noData')"/>
          <div v-for="column in filtered" :key="column.name" role="button" tabindex="0" :draggable="!saving"
               class="flex cursor-pointer items-center gap-2 rounded-mf-md px-2 py-1.5 outline-none hover:bg-surface-hover focus-visible:ring-1 focus-visible:ring-primary"
               :class="[dropClass(column.name) || (column.name === focused && 'bg-primary/10'), column.name === sort.dragging.value && 'opacity-50']"
               @click="focus(column.name)" @keydown.enter.prevent="focus(column.name)"
               @dragstart="sort.start(column.name, $event)" @dragend="sort.end" @dragover.prevent="sort.enter(column.name)" @drop.prevent="onDrop(column.name)">
            <GripVertical :size="14" class="shrink-0 cursor-grab text-content-tertiary"/>
            <span class="min-w-0 flex-1 truncate text-sm" :title="column.name">{{ column.name }}</span>
            <MfTag size="sm" :variant="typeTone(column.type)" :color="typeColor(column.type)">{{ column.type }}</MfTag>
          </div>
        </div>
      </aside>
      <section class="flex min-h-0 flex-col p-4">
        <div class="mb-3 flex shrink-0 items-center justify-between gap-3">
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold text-content">{{ $t('source.structure.reorderOrder') }}</span>
            <MfTag size="sm" variant="success">{{ $t('source.structure.reorderCount', { count: draft.length }) }}</MfTag>
          </div>
          <MfButton size="sm" :disabled="saving || !changed" @click="reset"><RotateCcw :size="14"/>{{ $t('source.structure.reorderReset') }}</MfButton>
        </div>
        <ol ref="list" class="m-0 min-h-0 flex-1 list-none overflow-y-auto p-0" @dragleave="sort.leave">
          <li v-for="(column, index) in draft" :key="column.name" :data-name="column.name" :draggable="!saving"
              class="flex items-center gap-3 border-b border-line px-2 py-2 last:border-b-0"
              :class="[dropClass(column.name) || (column.name === focused ? 'bg-primary/10' : 'hover:bg-surface-hover'), column.name === sort.dragging.value && 'opacity-50']"
              @click="focused = column.name" @dragstart="sort.start(column.name, $event)" @dragend="sort.end" @dragover.prevent="sort.enter(column.name)" @drop.prevent="onDrop(column.name)">
            <GripVertical :size="14" class="shrink-0 cursor-grab text-content-tertiary"/>
            <span class="inline-flex h-7 w-10 shrink-0 items-center justify-center rounded-mf-sm border border-line bg-surface text-xs tabular-nums">{{ index + 1 }}</span>
            <span class="min-w-0 flex-1 truncate text-sm" :title="column.name">{{ column.name }}</span>
            <span class="w-28 shrink-0"><MfTag size="sm" :variant="typeTone(column.type)" :color="typeColor(column.type)">{{ column.type }}</MfTag></span>
            <span class="flex shrink-0 gap-2">
              <MfButton size="sm" :aria-label="$t('source.structure.moveUp')" :disabled="saving || index === 0" @click.stop="move(index, -1)"><ArrowUp :size="13"/></MfButton>
              <MfButton size="sm" :aria-label="$t('source.structure.moveDown')" :disabled="saving || index === draft.length - 1" @click.stop="move(index, 1)"><ArrowDown :size="13"/></MfButton>
            </span>
          </li>
        </ol>
      </section>
    </div>
    <template #footer>
      <span class="mr-auto self-center text-xs text-content-tertiary">{{ $t('source.structure.reorderHint') }}</span>
      <MfButton :disabled="saving" @click="emit('update:open', false)">{{ $t('common.cancel') }}</MfButton>
      <MfButton variant="primary" :loading="saving" :disabled="!changed" @click="emit('apply', draft.map(column => column.name))">{{ $t('source.structure.reorderApply') }}</MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ArrowDown, ArrowUp, GripVertical, ListOrdered, RotateCcw, Search } from '@lucide/vue'
import type { SourceColumn } from '@/model/source-column'
import { DROP_SIDE_CLASS, useDragSort } from '@/composables/useDragSort'
const props = defineProps<{
  open: boolean; columns: SourceColumn[]; saving: boolean
  typeTone: (type: string) => 'info' | 'success' | 'warning' | 'default'; typeColor: (type: string) => string | undefined
}>()
const emit = defineEmits<{ 'update:open': [value: boolean]; apply: [names: string[]] }>()
const draft = ref<SourceColumn[]>([])
const keyword = ref('')
const focused = ref<string | null>(null)
const list = ref<HTMLElement | null>(null)
const filtered = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return text ? draft.value.filter(column => column.name.toLowerCase().includes(text)) : draft.value
})
const changed = computed(() => draft.value.some((column, index) => column.name !== props.columns[index]?.name))
function reset() {
  draft.value = [...props.columns]
}
// The draft is rebuilt from the table every time the modal opens or the table reloads behind it.
watch(() => [props.open, props.columns] as const, ([open]) => {
  if (!open) return
  reset()
  keyword.value = ''
  focused.value = draft.value[0]?.name || null
}, { immediate: true })
function moveTo(name: string, target: number) {
  const from = draft.value.findIndex(column => column.name === name)
  if (from < 0 || target < 0 || target >= draft.value.length || from === target) return
  const next = [...draft.value]
  const [column] = next.splice(from, 1)
  next.splice(target, 0, column)
  draft.value = next
  focused.value = name
}
const move = (index: number, direction: -1 | 1) => moveTo(draft.value[index].name, index + direction)
function focus(name: string) {
  focused.value = name
  nextTick(() => list.value?.querySelector(`[data-name="${CSS.escape(name)}"]`)?.scrollIntoView({ block: 'nearest' }))
}
const sort = useDragSort(() => draft.value.map(column => column.name))
function dropClass(name: string) {
  const side = sort.dropSide(name)
  return side ? DROP_SIDE_CLASS[side] : ''
}
function onDrop(target: string) {
  const name = sort.drop(target)
  if (name) moveTo(name, draft.value.findIndex(column => column.name === target))
}
</script>
