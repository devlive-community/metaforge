<template>
  <div class="flex h-full min-h-0 flex-col gap-2 overflow-hidden">
    <MfForm :model="{ keyword }" class="shrink-0" @finish="search">
      <div class="flex flex-wrap items-end gap-2">
        <MfFormItem name="keyword" class="min-w-40 flex-1" :label="$t(storage ? 'source.native.prefix' : 'source.native.matchPattern')" :rules="[{max:4096}]">
          <MfInput v-model="keyword" size="sm" :placeholder="$t(storage ? 'source.native.prefixPlaceholder' : 'source.native.patternPlaceholder')"/>
        </MfFormItem>
        <MfButton type="submit" size="sm" variant="primary" :loading="loading"><Search :size="14"/>{{ $t('common.search') }}</MfButton>
        <MfSelect :model-value="limit" size="sm" style="width: 96px" :options="batchOptions" @update:model-value="setLimit"/>
      </div>
    </MfForm>
    <div v-if="storage" class="flex shrink-0 items-center gap-2 text-xs text-content-secondary">
      <MfButton size="sm" :disabled="!prefix" @click="up"><ArrowUp :size="13"/>{{ $t('source.native.parentDirectory') }}</MfButton>
      <FolderOpen :size="14" class="shrink-0 text-primary"/><span class="shrink-0 font-medium">{{ namespace }}</span><span class="text-content-tertiary">/</span><EllipsisCell v-if="prefix" :text="prefix"/>
    </div>
    <MfAlert v-if="error" variant="danger" :title="error" class="shrink-0"/>
    <MfEmpty v-if="!namespace" class="flex-1" variant="initial" :title="$t(storage ? 'source.native.selectBucket' : 'source.native.selectDatabase')"/>
    <div v-else class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-mf-md border border-line">
      <MfTable fill :columns="headers" :data-source="entries" :loading="loading" row-key="id" density="compact" :empty-text="$t(hasMore ? 'source.native.emptyBatch' : 'common.noData')" @row-click="open">
        <template #cell-name="{ record }"><button type="button" class="flex min-w-0 max-w-full items-center gap-2 text-left hover:text-primary" @click.stop="open(record)"><Folder v-if="record.type === 'folder'" :size="16" class="shrink-0 text-primary"/><File v-else-if="storage" :size="15" class="shrink-0 text-content-tertiary"/><KeyRound v-else :size="15" class="shrink-0 text-primary"/><EllipsisCell :text="record.name || $t('source.native.emptyKey')"/></button></template>
        <template #cell-type="{ record }"><MfTag size="sm">{{ record.type }}</MfTag></template>
        <template #cell-size="{ record }">{{ record.type === 'folder' ? '-' : formatSize(record.size) }}</template>
        <template #cell-modifiedAt="{ record }">{{ formatTime(record.modifiedAt) }}</template>
        <template #cell-ttl="{ record }">{{ formatTtl(record.ttl) }}</template>
      </MfTable>
      <footer class="flex shrink-0 items-center justify-between gap-2 border-t border-line px-3 py-2 text-xs text-content-tertiary">
        <span>{{ $t('source.native.batchSummary', { page, count: entries.length }) }}</span>
        <div class="flex items-center gap-2"><MfButton size="sm" :disabled="loading || !previousCursors.length" @click="previous"><ChevronLeft :size="14"/>{{ $t('source.native.previousBatch') }}</MfButton><MfButton size="sm" :disabled="loading || !hasMore" @click="next">{{ $t('source.native.nextBatch') }}<ChevronRight :size="14"/></MfButton></div>
      </footer>
    </div>
    <MfDrawer v-if="panelOpen && panelHost" :open="panelOpen" inline :container="panelHost || undefined" :width="panelWidth + 'px'" :title="$t(storage ? 'source.native.objectDetail' : 'source.native.keyDetail')" @update:open="value => !value && closeDetail()">
      <div v-if="detailLoading" class="flex h-full items-center justify-center"><MfSpinner/></div>
      <MfAlert v-else-if="detailError" variant="danger" :title="detailError"/>
      <template v-else-if="detail">
        <div class="mb-4 flex min-w-0 items-start gap-2"><File v-if="storage" :size="18" class="mt-0.5 shrink-0 text-primary"/><KeyRound v-else :size="18" class="mt-0.5 shrink-0 text-primary"/><span class="min-w-0 break-all text-sm font-semibold text-content">{{ detail.entry.name || $t('source.native.emptyKey') }}</span></div>
        <dl class="m-0 grid grid-cols-[90px_minmax(0,1fr)] gap-x-3 gap-y-2 text-xs">
          <dt class="text-content-tertiary">{{ $t('source.structure.type') }}</dt><dd class="m-0"><MfTag size="sm">{{ detail.entry.type }}</MfTag></dd>
          <dt class="text-content-tertiary">{{ $t(storage || detail.entry.type === 'string' ? 'source.native.size' : 'source.native.length') }}</dt><dd class="m-0">{{ storage || detail.entry.type === 'string' ? formatSize(detail.entry.size) : detail.entry.size }}</dd>
          <template v-if="!storage"><dt class="text-content-tertiary">TTL</dt><dd class="m-0">{{ formatTtl(detail.entry.ttl) }}</dd></template>
          <template v-else><dt class="text-content-tertiary">{{ $t('common.updateTime') }}</dt><dd class="m-0">{{ formatTime(detail.entry.modifiedAt) }}</dd></template>
          <template v-for="attribute in detailAttributes" :key="attribute.name"><dt class="min-w-0 break-all text-content-tertiary">{{ attribute.label }}</dt><dd class="m-0 min-w-0 break-all text-content-secondary">{{ attribute.value }}</dd></template>
        </dl>
        <div class="mb-2 mt-5 flex items-center justify-between gap-2"><span class="text-sm font-medium">{{ $t('source.native.content') }}</span><MfTag v-if="detail.encoding === 'base64'" size="sm">Base64</MfTag></div>
        <MfAlert v-if="detail.truncated" variant="info" class="mb-2" :title="$t('source.native.previewTruncated')"/>
        <template v-if="detail.content != null"><pre class="max-h-96 overflow-auto whitespace-pre-wrap break-all rounded-mf-md border border-line bg-surface-hover/40 p-3 font-mono text-xs">{{ detail.content === '' ? $t('source.native.emptyString') : detail.content }}</pre></template>
        <template v-else-if="!storage && detail.entry.attributes?.previewSupported === 'true'">
          <MfTable :columns="detailHeaders" :data-source="detailRows" row-key="rowId" density="compact" :empty-text="$t('common.noData')">
            <template #cell-position="{ record }"><EllipsisCell :text="record.position === '' ? $t('source.native.emptyField') : record.position"/></template>
            <template #cell-value="{ record }"><div class="flex min-w-0 items-center gap-1"><EllipsisCell :text="record.value === '' ? $t('source.native.emptyString') : record.value"/><MfTag v-if="record.encoding === 'base64'" size="sm">Base64</MfTag><span v-if="record.truncated === 'true'" class="text-content-tertiary">…</span></div></template>
          </MfTable>
          <div class="mt-2 flex justify-end gap-2"><MfButton size="sm" :disabled="!previousDetailCursors.length" @click="previousDetail">{{ $t('source.native.previousBatch') }}</MfButton><MfButton size="sm" :disabled="!detail.hasMore" @click="nextDetail">{{ $t('source.native.nextBatch') }}</MfButton></div>
        </template>
        <MfEmpty v-else size="sm" variant="initial" :title="$t(storage ? 'source.native.previewOnDemand' : 'source.native.unsupportedValue')"/>
      </template>
      <template #footer>
        <MfButton size="sm" @click="closeDetail">{{ $t('source.native.close') }}</MfButton>
        <MfButton v-if="storage && detail" size="sm" :loading="detailLoading" @click="loadDetail(true)"><Eye :size="14"/>{{ $t('common.preview') }}</MfButton>
        <MfButton v-if="storage && can('object:download') && detail" size="sm" variant="primary" :loading="downloading" @click="download"><Download :size="14"/>{{ $t('source.native.download') }}</MfButton>
      </template>
    </MfDrawer>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUp, ChevronLeft, ChevronRight, Download, Eye, File, Folder, FolderOpen, KeyRound, Search } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { useNativeBrowser } from '@/composables/useNativeBrowser'
const { t } = useI18n()
const batchOptions = computed(() => [50,100,200].map(value => ({value,label:t('source.native.batchSize',{count:value})})))
const { can, storage, namespace, prefix, keyword, limit, entries, loading, error, hasMore, page, previousCursors,
  detail, detailLoading, detailError, detailRows, detailHeaders, detailAttributes, previousDetailCursors, downloading,
  panelOpen, panelHost, panelWidth, headers, formatSize, formatTime, formatTtl, search, open, up, closeDetail,
  loadDetail, download, next, previous, nextDetail, previousDetail, setLimit } = useNativeBrowser()
</script>
