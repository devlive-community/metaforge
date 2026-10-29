<template>
  <div class="p-3 pb-2">
    <MfDropdown v-model:open="open" trigger="click" placement="bottomLeft" class="w-full">
      <button ref="triggerRef"
              type="button"
              class="flex w-full min-w-0 items-center gap-2.5 rounded-mf-md border border-line bg-surface px-2.5 py-2 text-left transition-[border-color,background-color] duration-(--mf-duration-base) ease-(--mf-easing) hover:bg-surface-hover"
              :class="open && 'border-primary'">
        <MfAvatar v-if="current"
                  size="sm"
                  :src="`/static/images/plugin/${ String(current.type).toLowerCase() }.svg`"
                  :alt="current.type"
                  class="shrink-0"/>
        <span v-else class="flex size-8 shrink-0 items-center justify-center rounded-mf-full bg-primary/10">
          <Database :size="15" class="text-primary"/>
        </span>

        <span class="flex min-w-0 flex-1 flex-col gap-0.5">
          <span class="truncate text-sm font-medium text-content">
            {{ current?.name ?? $t('source.tip.selectSource') }}
          </span>
          <span v-if="current" class="truncate text-xs text-content-tertiary">
            {{ currentMeta }}
          </span>
        </span>

        <ChevronDown :size="15" class="shrink-0 text-content-tertiary transition-transform duration-(--mf-duration-base)" :class="open && 'rotate-180'"/>
      </button>

      <template #overlay>
        <div class="flex flex-col gap-1.5" :style="{ width: overlayWidth + 'px' }">
          <div class="px-1 pt-1">
            <MfInput v-model="keyword" size="sm" :placeholder="$t('common.search')">
              <template #prefix>
                <Search :size="13"/>
              </template>
            </MfInput>
          </div>

          <div class="flex max-h-64 flex-col gap-1 overflow-y-auto px-1 pb-1" @scroll="onListScroll">
            <template v-if="loading && options.length === 0">
              <div class="flex items-center justify-center py-4">
                <MfSpinner size="sm"/>
              </div>
            </template>
            <template v-else-if="filteredOptions.length === 0">
              <MfEmpty size="sm" :variant="keyword.trim() ? 'search' : 'data'" :title="$t('query.tip.noSource')"/>
            </template>
            <button v-for="item in filteredOptions"
                    :key="item.value"
                    type="button"
                    class="flex items-center gap-2 rounded-mf-md px-2 py-1.5 text-left transition-[background-color] duration-(--mf-duration-fast)"
                    :class="item.value === modelValue ? 'bg-surface-active' : 'hover:bg-surface-hover'"
                    @click="onSelect(item)">
              <MfAvatar size="sm" :src="`/static/images/plugin/${ String(item.type).toLowerCase() }.svg`" :alt="item.type"/>
              <span class="flex min-w-0 flex-1 flex-col">
                <span class="truncate text-sm" :class="item.value === modelValue ? 'font-medium text-primary' : 'text-content'">{{ item.label }}</span>
                <span class="truncate text-xs text-content-tertiary">{{ itemMeta(item) }}</span>
              </span>
              <Check v-if="item.value === modelValue" :size="14" class="shrink-0 text-primary"/>
            </button>
          </div>
        </div>
      </template>
    </MfDropdown>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { Check, ChevronDown, Database, Search } from '@lucide/vue'
import SourceService from '@/services/source'
import { FilterModel } from '@/model/filter.ts'
import type { SourceModel } from '@/model/source'

defineOptions({ name: 'QuerySourcePicker' })

const props = defineProps<{ modelValue?: string | null, selectedSource?: SourceModel | null }>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}>()

interface SourceOption
{
  label: string
  value: string
  name: string
  type: string
  version?: string
  host?: string
  port?: number
}

const open = ref(false)
const keyword = ref('')
const options = ref<SourceOption[]>([])
const loading = ref(false)
const pageIndex = ref(1)
const pageTotal = ref(1)
const triggerRef = ref<HTMLElement | null>(null)
const overlayWidth = ref(256)

const itemMeta = (item: SourceOption) =>
  `${ item.type }${ item.version ? ' ' + item.version : '' } · ${ item.host ?? '' }:${ item.port ?? '' }`

const current = computed(() => options.value.find(item => item.value === props.modelValue)
  || (props.selectedSource?.code ? mapOption(props.selectedSource) : undefined))
const currentMeta = computed(() => (current.value ? itemMeta(current.value) : ''))

/** The floating menu follows the trigger width (minus the dropdown's own p-1 padding). */
watch(open, (value) => {
  if (value) {
    nextTick(() => {
      const width = triggerRef.value?.offsetWidth ?? 0
      if (width > 8) {
        overlayWidth.value = width - 8
      }
    })
  }
})

const filteredOptions = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) {
    return options.value
  }
  return options.value.filter(item => item.label.toLowerCase().includes(kw))
})

const filter: FilterModel = new FilterModel()
filter.size = 100

const mapOption = (item: any): SourceOption => ({
  ...item,
  label: item.name,
  value: `${ item.id }:${ item.type }:${ item.code }`
})

const handleInitialize = () => {
  loading.value = true
  SourceService.getAll(filter)
               .then((response) => {
                 if (response.status) {
                   options.value = response.data.content.map(mapOption)
                   pageTotal.value = response.data.totalPage
                   pageIndex.value = response.data.page
                 }
               })
               .finally(() => (loading.value = false))
}

const loadMoreData = async () => {
  if (loading.value || pageIndex.value >= pageTotal.value) {
    return
  }
  loading.value = true
  filter.page = pageIndex.value + 1
  try {
    const response = await SourceService.getAll(filter)
    if (response.status) {
      const more = response.data.content.map(mapOption)
      options.value = [...options.value, ...more]
      pageTotal.value = response.data.totalPage
      pageIndex.value = response.data.page
    }
  }
  finally {
    loading.value = false
  }
}

const onListScroll = (event: Event) => {
  const target = event.target as HTMLElement
  if (target.scrollTop + target.offsetHeight >= target.scrollHeight - 10) {
    loadMoreData()
  }
}

const onSelect = (item: SourceOption) => {
  open.value = false
  keyword.value = ''
  emit('update:modelValue', item.value)
  emit('change', item.value)
}

handleInitialize()
</script>
