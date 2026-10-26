<template>
  <div @scroll.capture="onPopupScroll">
    <MfSelect v-model="applySource"
              :options="options as any"
              :loading="loading"
              searchable
              :style="{ width: '100%' }"
              :placeholder="$t('source.tip.selectSource')"
              @change="onChange">
    </MfSelect>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SourceService from '@/services/source'
import { SourceModel } from '@/model/source'
import { FilterModel } from '@/model/filter.ts'

defineOptions({ name: 'SourceSelect' })

const props = defineProps<{ value?: string }>()
const emit = defineEmits<{ (e: 'on-change', value: any): void }>()

const filter: FilterModel = new FilterModel()

const options = ref<SourceModel[]>([])
const loading = ref(false)
const applySource = ref<any>(undefined)
const pageIndex = ref(1)
const pageTotal = ref(10)
const dataCount = ref(0)

const handleInitialize = () => {
  loading.value = true
  SourceService.getAll(filter)
               .then((response) => {
                 if (response.status) {
                   options.value = response.data.content.map((item: any) => ({ ...item, label: item.name, value: `${ item.id }:${ item.type }:${ item.code }` }))
                   dataCount.value = response.data.total
                   pageTotal.value = response.data.totalPage
                   pageIndex.value = response.data.page
                   if (props.value) {
                     applySource.value = props.value as any
                   }
                 }
               })
               .finally(() => (loading.value = false))
}

const loadMoreData = async () => {
  if (pageIndex.value < pageTotal.value) {
    filter.page = pageIndex.value + 1
    const response = await SourceService.getAll(filter)

    if (response.status) {
      const more = response.data.content.map((item: any) => ({ ...item, label: item.name, value: `${ item.id }:${ item.type }:${ item.code }` }))
      dataCount.value = response.data.total
      pageTotal.value = response.data.totalPage
      pageIndex.value = response.data.page
      options.value = [...options.value, ...more]
    }
  }
}

const onPopupScroll = (e: any) => {
  const target = e.target
  if (target.scrollTop + target.offsetHeight >= target.scrollHeight - 10) {
    loadMoreData()
  }
}

const onChange = () => {
  emit('on-change', applySource.value)
}

handleInitialize()
</script>
