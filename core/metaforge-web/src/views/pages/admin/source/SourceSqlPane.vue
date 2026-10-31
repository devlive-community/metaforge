<template>
  <MfEmpty v-if="!table" class="h-full" variant="initial" :title="$t('source.sqlScope.selectTable')"/>
  <div v-else class="flex h-full min-h-0 flex-col gap-2 overflow-hidden">
    <section class="flex min-h-0 flex-[0_0_42%] flex-col overflow-hidden rounded-mf-md border border-line">
      <div class="flex shrink-0 items-center justify-between gap-2 border-b border-line px-2 py-1.5">
        <div class="flex min-w-0 items-center gap-2">
          <span class="text-sm font-medium text-content-secondary">{{ $t('source.tab.sqlQuery') }}</span>
          <MfTag v-if="table" size="sm" class="min-w-0"><span class="truncate">{{ $t('source.sqlScope.badge', { table: `${database}.${table}` }) }}</span></MfTag>
        </div>
        <MfButton size="sm" variant="primary" :loading="running" :disabled="!content.trim()" @click="run"><Play :size="14"/>{{ $t('query.common.execute') }}</MfButton>
      </div>
      <p v-if="scopeError" role="alert" class="m-0 shrink-0 border-b border-line bg-danger/5 px-3 py-1.5 text-xs text-danger">{{ scopeError }}</p>
      <div ref="editorRoot" class="min-h-0 flex-1"><AceEditor v-model:value="content" height="100%" @created="onEditorCreated"/></div>
    </section>
    <section class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-mf-md border border-line">
      <QueryResultPanel :response="response" :running="running" :query="executedQuery" :source-code="sourceCode" :dataset-available="datasetAvailable"
                        @add-dataset="onCreateFromResult" @existing-dataset="onExistingDataset" @refresh="run"/>
    </section>
    <DatasetCreateMode :open="creationModeVisible" :result="resultEntry" :sql="sqlEntry" @close="creationModeVisible = false" @create="openCreation"/>
    <DatasetWizard v-if="datasetWizardVisible" embedded :entry="datasetEntry" @close="datasetWizardVisible = false" @submitted="onDatasetSubmitted"/>
    <DatasetExistingMapping v-if="existingMappingVisible" :entry="datasetEntry" @close="existingMappingVisible = false" @submitted="onDatasetSubmitted"/>
  </div>
</template>
<script setup lang="ts">
import { Play } from '@lucide/vue'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import QueryResultPanel from '@/views/pages/admin/query/QueryResultPanel.vue'
import DatasetCreateMode from '@/views/pages/admin/dataset/components/DatasetCreateMode.vue'
import DatasetWizard from '@/views/pages/admin/dataset/DatasetWizard.vue'
import DatasetExistingMapping from '@/views/pages/admin/dataset/components/DatasetExistingMapping.vue'
import { useSourceSqlPane } from '@/composables/useSourceSqlPane'
const {
  content, response, running, executedQuery, scopeError, database, table, editorRoot, onEditorCreated, run, sourceCode,
  datasetAvailable, creationModeVisible, datasetWizardVisible, existingMappingVisible, datasetEntry, resultEntry, sqlEntry,
  onCreateFromResult, openCreation, onExistingDataset, onDatasetSubmitted,
} = useSourceSqlPane()
</script>
