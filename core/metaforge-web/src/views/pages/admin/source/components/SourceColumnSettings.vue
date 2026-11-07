<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'" :title="$t('source.data.columnSettings')" @update:open="value => !value && emit('close')">
    <div class="flex flex-col gap-4">
      <MfInput v-model="keyword" size="sm" clearable :placeholder="$t('source.data.searchField')"><template #prefix><Search :size="14"/></template></MfInput>
      <MfTable :columns="headers" :data-source="filtered" row-key="name" density="compact" :row-class="rowClass" :empty-text="$t('common.noData')"
               @dragover="onRowDragOver" @drop="onRowDrop" @dragleave="sort.leave">
        <template #cell-__drag__="{ record }">
          <button type="button" draggable="true" class="flex size-4 cursor-grab items-center justify-center text-content-tertiary active:cursor-grabbing" :aria-label="$t('source.data.dragColumn', { name: label(record) })"
                  @dragstart="sort.start(record.name, $event)" @dragend="sort.end"><GripVertical :size="14"/></button>
        </template>
        <template #cell-visible="{ record }"><MfCheckbox v-model="record.visible" :disabled="record.visible && visibleCount === 1" :aria-label="$t('source.data.colShow')"/></template>
        <template #cell-name="{ record }"><span class="block truncate" :title="label(record)">{{ label(record) }}</span></template>
        <template #cell-width="{ record }">
          <MfInput :model-value="String(record.width)" size="sm" inputmode="numeric" :disabled="draft.autoWidth" :aria-label="$t('source.data.colWidth')" @update:model-value="value => setWidth(record, String(value))"/>
        </template>
        <template #cell-align="{ record }"><MfSelect v-model="record.align" size="sm" :options="alignOptions" :aria-label="$t('source.data.colAlign')"/></template>
        <template #cell-fixed="{ record }"><MfCheckbox v-model="record.fixed" :aria-label="$t('source.data.colFixed')"/></template>
      </MfTable>
      <div class="flex flex-col gap-2">
        <span class="text-sm font-medium text-content">{{ $t('source.data.columnMode') }}</span>
        <MfRadioGroup :model-value="isDefault ? 'default' : 'custom'" @update:model-value="value => value === 'default' && reset()">
          <MfRadio value="custom" :label="$t('source.data.customColumns')"/>
          <MfRadio value="default" :label="$t('source.data.restoreDefault')"/>
        </MfRadioGroup>
      </div>
      <div class="border-t border-line pt-3">
        <button type="button" class="flex w-full items-center justify-between text-sm font-medium text-content" :aria-expanded="moreOpen" @click="moreOpen = !moreOpen">
          {{ $t('source.data.moreOptions') }}<ChevronDown :size="14" :class="moreOpen && 'rotate-180'"/>
        </button>
        <div v-show="moreOpen" class="mt-3 flex flex-col gap-2">
          <MfCheckbox :model-value="indexColumn?.visible ?? false" @update:model-value="value => indexColumn && (indexColumn.visible = !!value)">{{ $t('source.data.showIndex') }}</MfCheckbox>
          <MfCheckbox v-model="draft.bordered">{{ $t('source.data.showBorder') }}</MfCheckbox>
          <MfCheckbox v-model="draft.stickyHeader">{{ $t('source.data.stickyHeader') }}</MfCheckbox>
          <MfCheckbox v-model="draft.autoWidth">{{ $t('source.data.autoWidth') }}</MfCheckbox>
        </div>
      </div>
    </div>
    <template #footer>
      <MfButton size="sm" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" @click="emit('apply', clone(draft))">{{ $t('source.data.apply') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronDown, GripVertical, Search } from '@lucide/vue'
import { INDEX_COLUMN, sameColumnView, type ColumnSetting, type ColumnView } from '@/model/source-data-view'
import { DROP_SIDE_CLASS, useDragSort } from '@/composables/useDragSort'

const props = defineProps<{ open: boolean; view: ColumnView; defaults: ColumnView; width: number; container: HTMLElement | null }>()
const emit = defineEmits<{ close: []; apply: [view: ColumnView] }>()
const { t } = useI18n()
const clone = (view: ColumnView): ColumnView => JSON.parse(JSON.stringify(view))
// The panel edits a copy; nothing changes on the table until "apply".
const draft = ref<ColumnView>(clone(props.view))
watch(() => props.open, open => { if (open) draft.value = clone(props.view) })
const keyword = ref('')
const moreOpen = ref(true)
const label = (column: ColumnSetting) => column.name === INDEX_COLUMN ? '#' : column.name
const filtered = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return text ? draft.value.columns.filter(column => label(column).toLowerCase().includes(text)) : draft.value.columns
})
const indexColumn = computed(() => draft.value.columns.find(column => column.name === INDEX_COLUMN))
const visibleCount = computed(() => draft.value.columns.filter(column => column.visible && column.name !== INDEX_COLUMN).length)
const isDefault = computed(() => sameColumnView(draft.value, props.defaults))
const alignOptions = computed(() => [
  { label: t('source.data.alignLeft'), value: 'left' }, { label: t('source.data.alignCenter'), value: 'center' }, { label: t('source.data.alignRight'), value: 'right' },
])
const reset = () => { draft.value = clone(props.defaults) }
function setWidth(column: ColumnSetting, value: string) {
  const width = Math.trunc(Number(value))
  if (Number.isFinite(width) && width > 0) column.width = Math.min(800, Math.max(40, width))
}
const sort = useDragSort(() => draft.value.columns.map(column => column.name))
const headers = computed(() => [
  { title: '', dataIndex: 'name', key: '__drag__', width: 28 },
  { title: t('source.data.colShow'), dataIndex: 'visible', key: 'visible', width: 48 },
  { title: t('source.data.colName'), dataIndex: 'name', key: 'name', ellipsis: true },
  { title: t('source.data.colWidth'), dataIndex: 'width', key: 'width', width: 80 },
  { title: t('source.data.colAlign'), dataIndex: 'align', key: 'align', width: 92 },
  { title: t('source.data.colFixed'), dataIndex: 'fixed', key: 'fixed', width: 48 },
])
// Rows are found by MfTable's data-row-key, as on the table structure page.
const rowName = (event: DragEvent) => (event.target as HTMLElement | null)?.closest<HTMLElement>('tr[data-row-key]')?.dataset.rowKey
function rowClass(column: ColumnSetting) {
  const side = sort.dropSide(column.name)
  return [side ? DROP_SIDE_CLASS[side] : '', column.name === sort.dragging.value ? 'opacity-50' : ''].join(' ')
}
function onRowDragOver(event: DragEvent) {
  const name = rowName(event)
  if (!name || !sort.dragging.value) return
  event.preventDefault()
  sort.enter(name)
}
function onRowDrop(event: DragEvent) {
  event.preventDefault()
  const target = rowName(event)
  if (target) onDrop(target)
  else sort.end()
}
function onDrop(target: string) {
  const name = sort.drop(target)
  if (!name) return
  // Taking the target's index before removal lands the row after the target when dragging down
  // and before it when dragging up, matching the insertion line shown during the drag.
  const columns = [...draft.value.columns]
  const targetIndex = columns.findIndex(column => column.name === target)
  const [moved] = columns.splice(columns.findIndex(column => column.name === name), 1)
  columns.splice(targetIndex, 0, moved)
  draft.value.columns = columns
}
</script>
