<template>
  <div class="source-metadata-sidebar flex h-full min-h-0 flex-col px-4 py-3">

    <div class="flex shrink-0 items-center gap-1.5 px-1 text-sm">
      <Database :size="14" class="shrink-0 text-primary"/>
      <span class="min-w-0 truncate font-medium text-content">{{ tableEngine || (originalSource ?? '') }}</span>
      <span class="shrink-0 text-content-tertiary">· {{ $t('source.sidebar.tableCount', { count: tableCount }) }}</span>
    </div>

    <div class="mt-3 flex shrink-0 items-center gap-1.5">
      <MfInput v-model="searchKeyword" size="sm" class="min-w-0 flex-1" :placeholder="$t('source.sidebar.searchTable')">
        <template #prefix>
          <Search :size="13" class="text-content-tertiary"/>
        </template>
      </MfInput>
      <MfTooltip v-if="can('table:create')" :content="$t('source.common.menuNewTable')">
        <MfButton size="sm" :aria-label="$t('source.common.menuNewTable')" @click="visibleCreateTable(true)">
          <Plus :size="15"/>
        </MfButton>
      </MfTooltip>
    </div>

    <div class="relative mt-3 min-h-0 flex-1 overflow-x-hidden overflow-y-auto">
      <div v-if="loading" class="flex justify-center py-10"><MfSpinner/></div>

      <MfTree v-else-if="!loading && filteredTreeArray.length > 0"
              v-model:selected-keys="selectedKeys"
              :data="filteredTreeArray"
              default-expand-all
              :load-data="onLoadData"
              @select="onSelect">
        <template #title="{ node }">
          <div class="flex items-center space-x-1" @contextmenu.prevent="visibleContextMenu($event, node)">
            <Table :size="16" v-if="node.level === StructureEnum.TABLE || (node.level === StructureEnum.TYPE && node.type === 'table')" class="shrink-0 text-content-secondary" />
            <Eye :size="16" v-else-if="node.level === StructureEnum.TYPE && node.type === 'view'" class="shrink-0 text-content-secondary" />
            <SquareFunction :size="16" v-else-if="node.level === StructureEnum.TYPE && node.type === 'function'" class="shrink-0 text-content-secondary" />
            <Plug :size="16" v-else-if="node.level === StructureEnum.TYPE && node.type === 'procedure'" class="shrink-0 text-content-secondary" />
            <Columns3 :size="16" v-else-if="node.level === StructureEnum.TYPE && node.type === 'column'" class="shrink-0 text-content-secondary" />
            <Layers :size="16" v-else-if="node.level === StructureEnum.TYPE && node.type === 'index'" class="shrink-0 text-content-secondary" />
            <Zap :size="16" v-else-if="node.level === StructureEnum.TYPE && node.type === 'trigger'" class="shrink-0 text-content-secondary" />
            <Key :size="16" v-else-if="node.level === StructureEnum.TYPE && node.type === 'primary'" class="shrink-0 text-content-secondary" />

            <span class="min-w-0 truncate text-[13px]">
              {{ node.title }}
            </span>

            <span v-if="node.level === StructureEnum.COLUMN" class="ml-1 text-xs text-content-tertiary">
              {{ node.typeName === 'column' ? node.type : node.definition }}
            </span>
          </div>
        </template>
      </MfTree>

      <MfEmpty v-if="!loading && filteredTreeArray.length === 0" class="h-full" size="sm" :title="$t('common.noData')"/>

      <div v-if="contextmenu.visible && dataInfo"
           ref="contextMenuRoot"
           class="dc-ctxmenu"
           :style="{ left: contextmenu.position.x + 'px', top: contextmenu.position.y + 'px' }"
           @click.stop>
        <MfMenu mode="vertical" style="min-width: 180px;">
          <MfSubMenu v-if="dataInfo.level === StructureEnum.TABLE || dataInfo.level === StructureEnum.COLUMN || dataInfo.type === 'table' || dataInfo.type === 'column'"
                     key_="new"
                     :title="$t('source.common.menuNew')">
            <MfMenuItem v-if="can('table:create') && (dataInfo.level === StructureEnum.TABLE || dataInfo.type === 'table')" key_="new-table" @click="visibleCreateTable(true)">
              <div class="flex items-center space-x-1">
                <Table :size="16" />
                <span>{{ $t('source.common.menuNewTable') }}</span>
              </div>
            </MfMenuItem>

            <MfMenuItem v-if="can('column:create') && (dataInfo.level === StructureEnum.COLUMN || dataInfo.type === 'column' || dataInfo.level === StructureEnum.TABLE)" key_="new-column" @click="visibleCreateColumn(true)">
              <div class="flex items-center space-x-1">
                <Columns3 :size="16" />
                <span>{{ $t('source.common.newColumn') }}</span>
              </div>
            </MfMenuItem>
          </MfSubMenu>

          <MfSubMenu v-if="dataInfo.level === StructureEnum.TABLE" key_="export" :title="$t('source.common.menuExport')">
            <MfMenuItem v-if="can('table:export')" key_="export-data" @click="visibleExportData(true)">
              <div class="flex items-center space-x-1">
                <ArrowUpToLine :size="16" />
                <span>{{ $t('source.common.exportData') }}</span>
              </div>
            </MfMenuItem>
          </MfSubMenu>

          <MfMenuItem v-if="can('table:truncate') && (dataInfo.level === StructureEnum.TABLE)" key_="truncate" @click="visibleTruncateTable(true)">
            <div class="flex items-center space-x-1">
              <Trash2 :size="16" />
              <span>{{ $t('source.common.truncateTable') }}</span>
            </div>
          </MfMenuItem>

          <MfMenuItem v-if="can('table:drop') && (dataInfo.level === StructureEnum.TABLE)" key_="drop-table" @click="visibleDropTable(true)">
            <div class="flex items-center space-x-1">
              <Trash2 :size="16" />
              <span>{{ $t('source.common.dropTable') }}</span>
            </div>
          </MfMenuItem>

          <MfMenuItem v-if="can('column:modify') && (dataInfo.level === StructureEnum.COLUMN)" key_="change-column" @click="visibleChangeColumn(true)">
            <div class="flex items-center space-x-1">
              <Pencil :size="16" />
              <span>{{ $t('source.common.changeColumn') }}</span>
            </div>
          </MfMenuItem>

          <MfMenuItem v-if="can('column:drop') && (dataInfo.level === StructureEnum.COLUMN)" key_="drop-column" @click="visibleDropColumn(true)">
            <div class="flex items-center space-x-1">
              <Trash2 :size="16" />
              <span>{{ $t('source.common.dropColumn') }}</span>
            </div>
          </MfMenuItem>
        </MfMenu>
      </div>
    </div>
  </div>

  <TableCreate v-if="tableCreateVisible" :is-visible="tableCreateVisible" @close="visibleCreateTable(false)"/>

  <ColumnCreate v-if="columnCreateVisible" :is-visible="columnCreateVisible" @close="visibleCreateColumn(false)"/>

  <TableExport v-if="tableExportVisible"
               :isVisible="tableExportVisible"
               :info="dataInfo as any"
               @close="visibleExportData(false)"/>

  <TableTruncate v-if="tableTruncateVisible" :is-visible="tableTruncateVisible" @close="visibleTruncateTable(false)"/>

  <TableDrop v-if="tableDropVisible" :is-visible="tableDropVisible" @close="visibleDropTable(false)"/>

  <ColumnChange v-if="columnChangeVisible && dataInfo"
                :is-visible="columnChangeVisible"
                :column="dataInfo.code"
                @close="visibleChangeColumn(false)">
  </ColumnChange>

  <ColumnDrop v-if="columnDropVisible && dataInfo"
              :is-visible="columnDropVisible"
              :column="dataInfo.code"
              @close="visibleDropColumn(false)">
  </ColumnDrop>
</template>

<script lang="ts" setup>
import { Database, Plus, Search } from '@lucide/vue'
import ColumnCreate from '@/views/pages/admin/source/components/ColumnCreate.vue'
import ColumnDrop from '@/views/pages/admin/source/components/ColumnDrop.vue'
import TableExport from '@/views/pages/admin/source/components/TableExport.vue'
import ColumnChange from '@/views/pages/admin/source/components/ColumnChange.vue'
import TableTruncate from '@/views/pages/admin/source/components/TableTruncate.vue'
import TableDrop from '@/views/pages/admin/source/components/TableDrop.vue'
import TableCreate from '@/views/pages/admin/source/components/TableCreate.vue'
import { ArrowUpToLine, Columns3, Eye, Key, Layers, Pencil, Plug, SquareFunction, Table, Trash2, Zap } from '@lucide/vue'
import { StructureEnum } from '@/model/structure'
import { useMetadataSidebar } from '@/composables/useMetadataSidebar'

defineOptions({ name: 'MetadataSidebar' })

const { can, loading, selectedKeys, originalSource, dataInfo, contextMenuRoot, tableCreateVisible, tableExportVisible, tableTruncateVisible, tableDropVisible, columnCreateVisible, columnChangeVisible, columnDropVisible, contextmenu, tableEngine, searchKeyword, tableCount, filteredTreeArray, onSelect, onLoadData, visibleContextMenu, visibleCreateTable, visibleCreateColumn, visibleExportData, visibleTruncateTable, visibleDropTable, visibleChangeColumn, visibleDropColumn } = useMetadataSidebar()
</script>

<style scoped>
.source-metadata-sidebar :deep([role='treeitem'] > div:first-child) { padding-top: 3px; padding-bottom: 3px; }
.source-metadata-sidebar :deep([role='treeitem'] > .flex-col) { margin-left: 0; padding-left: 12px; }
.source-metadata-sidebar :deep([aria-selected='true'] > div:first-child svg) { color: var(--mf-color-primary); }

.dc-ctxmenu {
    position: fixed;
    z-index: 1050;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
    border-radius: 8px;
    background: var(--dc-bg, #fff);
    padding: 4px;
}
</style>
