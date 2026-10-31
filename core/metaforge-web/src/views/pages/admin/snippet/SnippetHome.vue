<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('snippet.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="initialize"
               @search="onSearch"
               @reset="onSearch"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #actions>
        <MfButton variant="primary" @click="visibleInfo(true, null)">
          <Plus :size="16"/>
          {{ $t('snippet.common.create') }}
        </MfButton>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <EllipsisCell :text="record.name"/>
        </template>
        <template v-else-if="column.key === 'description'">
          <EllipsisCell :text="record.description"/>
        </template>
        <template v-else-if="column.key === 'context'">
          <EllipsisCell :text="record.context" class="font-mono text-xs"/>
        </template>
        <template v-else-if="column.key === 'username'">
          <MfAvatar size="sm" :src="record.user.avatarConfigure?.path" :alt="record.user.username"/>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('snippet.common.modify').replace('$VALUE', record.name)">
              <MfButton variant="ghost" size="sm" shape="circle" @click="visibleInfo(true, record)">
                <Pencil :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click">
              <MfButton variant="ghost" size="sm" shape="circle">
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
import { useHeaders } from '@/views/pages/admin/snippet/SnippetUtils'
import { SnippetModel } from '@/model/snippet'
import DataTable from '@/views/components/table/DataTable.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import SnippetService from '@/services/snippet'
import SnippetInfo from '@/views/pages/admin/snippet/SnippetInfo.vue'
import SqlInfo from '@/views/components/sql/SqlInfo.vue'
import SnippetDelete from '@/views/pages/admin/snippet/SnippetDelete.vue'
import { ChevronRight, FileCode2, Pencil, Plus, Settings, Trash2 } from '@lucide/vue'

defineOptions({ name: 'SnippetHome' })

const tableFilters = useTableFilters('snippet')
const { headers } = useHeaders()

const dataInfoVisible = ref(false)
const dataInfo = ref<SnippetModel | null>(null)
const contentVisible = ref(false)
const content = ref<string | null>(null)
const dataDeleteVisible = ref(false)

const {
    loading,
    data,
    pageIndex,
    pageSize,
    dataCount,
    initialize,
    onSearch,
    onPageChange,
    onSizeChange
  } = usePagedList<any>(filter => SnippetService.getAll(filter))

const visibleInfo = (opened: boolean, value: SnippetModel | null) => {
  dataInfoVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    initialize()
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
    initialize()
  }
}

onMounted(() => initialize())
</script>
