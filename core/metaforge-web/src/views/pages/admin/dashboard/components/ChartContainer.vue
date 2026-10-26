<template>
  <MfModal v-model:open="visible"
           width="60%"
           :title="$t('report.common.list')"
           @close="onCancel">
    <div class="relative w-full h-full">
      <div v-if="loading" class="flex items-center justify-center py-16">
        <MfSpinner size="lg"/>
      </div>
      <template v-else>
        <div class="p-2">
          <!-- a-checkbox-group 数组用法：MfCheckbox(value + :model-value) -->
          <div :style="{ width: '100%' }">
            <div class="grid grid-cols-3 gap-2">
              <div v-for="item of data" :key="item.code">
                <MfCheckbox class="px-1 py-1 dc-chart-checkbox w-full! items-start!"
                            :value="item.code"
                            :model-value="report"
                            @update:model-value="(value: any[]) => (report = value)">
                  <div class="flex-1 w-full">
                    <MfCard :title="item.name">
                      <template #extra>
                        <MfTooltip v-if="item.description" :content="item.description">
                          <InfoCircleOutlined :style="{ fontSize: '20px' }"/>
                        </MfTooltip>
                      </template>

                      <VisualView width="300px" height="250px"
                                  :code="item.dataset?.code as string"
                                  :configuration="JSON.parse(item.configure as string)"
                                  :type="item.type"
                                  :query="item.type === 'DATASET' ? JSON.parse(item.query as string) : item.query"
                                  :original="item?.source?.code"/>
                    </MfCard>
                  </div>
                </MfCheckbox>
              </div>
            </div>
          </div>

          <div v-if="data.length === 0" class="flex w-full items-center">
            {{ $t('common.noData') }}
          </div>
        </div>

        <div v-if="data.length > 0" class="py-2 mt-2 flex justify-end">
          <MfPagination :current="pageIndex"
                        :page-size="pageSize"
                        :total="dataCount"
                        show-size
                        :page-sizes="[12, 24, 36]"
                        @change="(page: number, size: number) => size !== pageSize ? onSizeChange(page, size) : onPageChange(page)"/>
        </div>
      </template>
    </div>

    <template #footer>
      <MfSpace>
        <MfButton @click="onCancel">
          {{ $t('common.cancel') }}
        </MfButton>

        <MfButton variant="primary" :disabled="!report || report.length === 0" @click="onSubmit()">
          {{ $t('common.save') }}
        </MfButton>
      </MfSpace>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import ReportService from '@/services/report.ts'
import { FilterModel } from '@/model/filter.ts'
import { ReportModel } from '@/model/report.ts'
import VisualView from '@/views/components/visual/VisualView.vue'
import { InfoCircleOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'ChartContainer' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
const emit = defineEmits<{
  (e: 'close', value: boolean): void
  (e: 'change', value: ReportModel[]): void
}>()

const filter: FilterModel = new FilterModel()
filter.size = 12

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const data = ref<ReportModel[]>([])
const report = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(12)
const dataCount = ref(0)

const handleInitialize = () => {
  loading.value = true
  ReportService.getAll(filter)
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

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  const nodes = data.value.filter((item) =>
      report.value.some((reportId: string) => item.code === reportId)
  )
  emit('change', nodes)
  onCancel()
}

handleInitialize()
</script>

<style scoped>
/* 让 checkbox 内容区占满整列，卡片撑满 */
.dc-chart-checkbox {
    display: flex;
    width: 100%;
    align-items: flex-start;
}
</style>
