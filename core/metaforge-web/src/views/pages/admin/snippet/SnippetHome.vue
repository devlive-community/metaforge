<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('snippet.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handleInitialize"
               @search="onSearch"
               @reset="onSearch"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'">
          <MfAvatar size="sm" :src="record.user.avatarConfigure?.path" :alt="record.user.username"/>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace size="xs">
            <MfTooltip :content="$t('snippet.common.modify').replace('$VALUE', record.name)">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="visibleInfo(true, record)">
                <Pencil :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click">
              <MfButton variant="ghost" size="sm" class="rounded-full!">
                <Settings :size="15" />
              </MfButton>

              <template #overlay>
                <MfMenu>
                  <MfMenuItem>
                    <router-link :to="`/query/snippet/${ record?.code }`" target="_blank">
                      <div class="flex items-center space-x-2">
                        <FileCode2 :size="15" />
                        <span>{{ $t('query.common.quoteRecord') }}</span>
                      </div>
                    </router-link>
                  </MfMenuItem>

                  <MfMenuItem @click="visibleContent(true, record?.context)">
                    <div class="flex items-center space-x-2">
                      <ChevronRight :size="15" />
                      <span>{{ $t('query.common.showSql') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem @click="visibleDelete(true, record)">
                    <div class="flex items-center space-x-2">
                      <Trash2 :size="15" />
                      <span>{{ $t('snippet.common.delete') }}</span>
                    </div>
                  </MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <SnippetInfo v-if="dataInfoVisible"
               :is-visible="dataInfoVisible"
               :info="dataInfo"
               @close="visibleInfo(false, null)"/>

  <SnippetDelete v-if="dataDeleteVisible"
                 :is-visible="dataDeleteVisible"
                 :info="dataInfo"
                 @close="visibleDelete(false, null)"/>

  <SqlInfo v-if="contentVisible && content"
           :is-visible="contentVisible"
           :content="content"
           @close="visibleContent(false, null)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { FilterModel, type TableFilters } from '@/model/filter.ts'
import { useHeaders } from '@/views/pages/admin/snippet/SnippetUtils'
import { SnippetModel } from '@/model/snippet'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import SnippetService from '@/services/snippet'
import SnippetInfo from '@/views/pages/admin/snippet/SnippetInfo.vue'
import SqlInfo from '@/views/components/sql/SqlInfo.vue'
import SnippetDelete from '@/views/pages/admin/snippet/SnippetDelete.vue'
import { ChevronRight, FileCode2, Pencil, Settings, Trash2 } from '@lucide/vue'

defineOptions({ name: 'SnippetHome' })

const tableFilters = useTableFilters('snippet')
const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const dataInfoVisible = ref(false)
const dataInfo = ref<SnippetModel | null>(null)
const contentVisible = ref(false)
const content = ref<string | null>(null)
const dataDeleteVisible = ref(false)

const onSearch = (values: TableFilters) => {
  Object.assign(filter, values, { page: 1 })
  pageIndex.value = 1
  handleInitialize()
}

let latestRequest = 0
const handleInitialize = () => {
  const request = ++latestRequest
  loading.value = true
  SnippetService.getAll({ ...filter })
                .then(response => {
                 if (request !== latestRequest) return
                  if (response.status) {
                    data.value = response.data.content
                    dataCount.value = response.data.total
                    pageSize.value = response.data.size
                    pageIndex.value = response.data.page
                  }
                  else {
                    MfMessage.error(response.message)
                  }
                })
                .finally(() => { if (request === latestRequest) loading.value = false })
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

const visibleInfo = (opened: boolean, value: SnippetModel | null) => {
  dataInfoVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    handleInitialize()
  }
}

const visibleContent = (opened: boolean, value: string | null) => {
  contentVisible.value = opened
  content.value = value
}

const visibleDelete = (opened: boolean, value: SnippetModel | null) => {
  dataDeleteVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    handleInitialize()
  }
}

onMounted(() => handleInitialize())
</script>
