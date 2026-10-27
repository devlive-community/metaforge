<template>
  <MfModal v-model:open="visible"
           width="60%"
           :title="$t('source.common.syncHistory')">
    <MfTable :columns="headers as any"
             :data-source="data"
             :loading="loading"
             row-key="id"
             :empty-text="$t('common.noData')">
      <template #cell-elapsed="{ record }">
        {{ (getTime(record.updateTime) - getTime(record.createTime)) / 1000 }}
      </template>

      <template #cell-state="{ record }">
        <MfTooltip v-if="record?.state === 'FAILURE'" :content="record.message">
          <span class="inline-flex items-center gap-2">
            <span class="size-2.5 rounded-full" :style="{ backgroundColor: Common.getColor(record?.state) }"></span>
            <span class="text-sm">{{ getStateText(record.state) }}</span>
          </span>
        </MfTooltip>
        <span v-else class="inline-flex items-center gap-2">
          <span class="size-2.5 rounded-full" :style="{ backgroundColor: Common.getColor(record?.state) }"></span>
          <span class="text-sm">{{ getStateText(record.state) }}</span>
        </span>
      </template>

      <template #cell-result="{ record }">
        <MfTooltip>
          <template #content>
            <MdPreview :modelValue="toMarkdown(record.info)" style="padding: 0"/>
          </template>
          <MfButton variant="ghost" size="sm">
            <Eye :size="20" />
          </MfButton>
        </MfTooltip>
      </template>
    </MfTable>

    <div v-if="data.length > 0" class="py-2 flex justify-end">
      <MfPagination :current="pageIndex"
                    :page-size="pageSize"
                    :total="dataCount"
                    show-size
                    :page-sizes="[10, 20, 50]"
                    @change="onPageChange"
                    @update:page-size="(size: number) => onSizeChange(1, size)"/>
    </div>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { SourceModel } from '@/model/source'
import { FilterModel } from '@/model/filter'
import { useHeaders } from '@/views/pages/admin/source/SourceUtils'
import Common, { useUtil } from '@/utils/common'
import SourceService from '@/services/source'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { Eye } from '@lucide/vue'

defineOptions({ name: 'SourceHistory' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: SourceModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const filter: FilterModel = new FilterModel()
const { historyHeaders: headers } = useHeaders()
const { getText } = useUtil()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)

const handleInitialize = () => {
  loading.value = true
  SourceService.getHistory(props.info?.code as string, filter)
               .then((response) => {
                 if (response.status) {
                   data.value = response.data.content
                   dataCount.value = response.data.total
                   pageSize.value = response.data.size
                   pageIndex.value = response.data.page
                 }
               })
               .finally(() => (loading.value = false))
}

const fetchData = (value: number) => {
  filter.page = value
  filter.size = pageSize.value
  handleInitialize()
}

const onPageChange = (value: number) => fetchData(value)

const onSizeChange = (_current: number, size: number) => {
  pageSize.value = size
  fetchData(pageIndex.value)
}

const getTime = (time: any) => (time ? new Date(time).getTime() : 0)

const getStateText = (origin: string): string => getText(origin)

const toMarkdown = (content: string) => '```json\n' + JSON.stringify(content, null, 4) + '\n```'

handleInitialize()
</script>
