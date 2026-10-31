<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'" :title="$t('source.data.sortTitle')" @update:open="value => !value && emit('close')">
    <div class="flex flex-col gap-5">
      <section class="flex flex-col gap-3">
        <div class="flex items-center justify-between gap-2">
          <h4 class="m-0 text-sm font-semibold text-content">{{ $t('source.data.sortRules') }} <span class="text-xs font-normal text-content-tertiary">{{ $t('source.data.sortLimit', { count: MAX_SORT_RULES }) }}</span></h4>
          <MfButton size="sm" :disabled="!draft.length" @click="draft = []">{{ $t('source.data.clear') }}</MfButton>
        </div>
        <div class="flex flex-col gap-2" @dragleave="sort.leave">
          <div v-for="(rule, index) in draft" :key="rule.id" :draggable="true" class="flex items-center gap-2 rounded-mf-sm py-0.5"
               :class="[dropClass(rule.id), rule.id === sort.dragging.value && 'opacity-50']"
               @dragstart="sort.start(rule.id, $event)" @dragend="sort.end" @dragover.prevent="sort.enter(rule.id)" @drop.prevent="onDrop(rule.id)">
            <GripVertical :size="14" class="shrink-0 cursor-grab text-content-tertiary"/>
            <span class="w-4 shrink-0 text-center text-sm tabular-nums text-content-secondary">{{ index + 1 }}</span>
            <div class="min-w-0 flex-1"><MfSelect v-model="rule.name" size="sm" searchable :options="fieldOptions(rule)" :aria-label="$t('source.data.sortField')"/></div>
            <div class="w-28 shrink-0"><MfSelect v-model="rule.order" size="sm" :options="directionOptions" :aria-label="$t('source.data.sortDirection')"/></div>
            <button type="button" class="inline-flex size-7 shrink-0 items-center justify-center rounded-mf-md text-danger hover:bg-danger/10" :aria-label="$t('source.data.removeSort')" @click="remove(rule.id)"><Trash2 :size="15"/></button>
          </div>
        </div>
        <MfButton size="sm" class="ml-10" :disabled="draft.length >= MAX_SORT_RULES || draft.length >= fields.length" @click="add"><Plus :size="14"/>{{ $t('source.data.addSort') }}</MfButton>
      </section>
      <section v-if="quick.length" class="flex flex-col gap-2">
        <h4 class="m-0 border-b border-line pb-2 text-sm font-semibold text-content">{{ $t('source.data.quickSort') }}</h4>
        <div class="overflow-hidden rounded-mf-md border border-line">
          <button v-for="rule in quick" :key="rule.name + rule.order" type="button"
                  class="flex w-full items-center gap-2.5 border-b border-line px-3 py-2.5 text-left text-sm last:border-b-0"
                  :class="isQuickActive(rule) ? 'bg-primary/10 text-primary' : 'text-content hover:bg-surface-hover'" @click="pickQuick(rule)">
            <CircleCheck v-if="isQuickActive(rule)" :size="16" class="shrink-0"/>
            <ArrowDownWideNarrow v-else-if="rule.order === 'desc'" :size="16" class="shrink-0 text-content-secondary"/>
            <ArrowUpNarrowWide v-else :size="16" class="shrink-0 text-content-secondary"/>
            {{ $t(rule.order === 'desc' ? 'source.data.quickDesc' : 'source.data.quickAsc', { name: rule.name }) }}
          </button>
        </div>
      </section>
    </div>
    <template #footer>
      <MfButton size="sm" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" @click="confirm">{{ $t('source.prototype.confirm') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDownWideNarrow, ArrowUpNarrowWide, CircleCheck, GripVertical, Plus, Trash2 } from '@lucide/vue'
import { MAX_SORT_RULES, sameSort, type SortRule } from '@/model/source-data-view'
import { DROP_SIDE_CLASS, useDragSort } from '@/composables/useDragSort'

type DraftRule = SortRule & { id: string }
const props = defineProps<{ open: boolean; rules: SortRule[]; fields: string[]; quick: SortRule[]; width: number; container: HTMLElement | null }>()
const emit = defineEmits<{ close: []; apply: [rules: SortRule[]] }>()
const { t } = useI18n()
let nextId = 0
const toDraft = (rules: SortRule[]): DraftRule[] => rules.map(rule => ({ ...rule, id: String(nextId++) }))
// Rules are edited as a draft and only reach the query on confirm.
const draft = ref<DraftRule[]>(toDraft(props.rules))
watch(() => props.open, open => { if (open) draft.value = toDraft(props.rules) })
const plain = () => draft.value.filter(rule => rule.name).map(({ name, order }) => ({ name, order }))
const directionOptions = computed(() => [{ label: t('source.data.desc'), value: 'desc' }, { label: t('source.data.asc'), value: 'asc' }])
/** A field can be sorted once; each rule offers its own field plus the unused ones. */
const fieldOptions = (rule: DraftRule) => props.fields
  .filter(name => name === rule.name || !draft.value.some(item => item.name === name))
  .map(name => ({ label: name, value: name }))
function add() {
  const name = props.fields.find(field => !draft.value.some(rule => rule.name === field))
  if (name && draft.value.length < MAX_SORT_RULES) draft.value = [...draft.value, ...toDraft([{ name, order: 'asc' }])]
}
const remove = (id: string) => { draft.value = draft.value.filter(rule => rule.id !== id) }
const isQuickActive = (rule: SortRule) => sameSort(plain(), [rule])
const pickQuick = (rule: SortRule) => { draft.value = toDraft([rule]) }
const confirm = () => emit('apply', plain())
const sort = useDragSort(() => draft.value.map(rule => rule.id))
function dropClass(id: string) {
  const side = sort.dropSide(id)
  return side ? DROP_SIDE_CLASS[side] : ''
}
function onDrop(target: string) {
  const id = sort.drop(target)
  if (!id) return
  const list = [...draft.value]
  const targetIndex = list.findIndex(rule => rule.id === target)
  const [moved] = list.splice(list.findIndex(rule => rule.id === id), 1)
  list.splice(targetIndex, 0, moved)
  draft.value = list
}
</script>
