<template>
  <div class="flex h-full min-h-0 flex-col gap-3 overflow-y-auto">
    <div class="flex shrink-0 items-center justify-end">
      <MfButton size="sm" :variant="panelOpen ? 'primary' : 'secondary'" @click="togglePanel">
        <SlidersHorizontal :size="14"/>
        {{ $t('source.ddl.panel.toggle') }}
      </MfButton>
    </div>

    <section v-for="card in cards" :key="card.action" :ref="bindCard(card.action)" class="shrink-0 scroll-mt-1 rounded-mf-md border border-line p-4">
      <header class="mb-3 flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <h3 class="m-0 text-lg font-semibold text-content">{{ $t(`source.ddl.card.${card.action}`) }}</h3>
          <p class="m-0 mt-1 text-sm text-content-tertiary">{{ $t(`source.ddl.cardHint.${card.action}`) }}</p>
        </div>
        <div class="flex shrink-0 flex-wrap items-center gap-2">
          <MfButton size="sm" :disabled="!cardSql(card)" @click="copy(card)"><Copy :size="14"/>{{ $t('source.executed.copy') }}</MfButton>
          <MfButton size="sm" :loading="card.formatting" :disabled="!cardSql(card)" @click="format(card)"><Scan :size="14"/>{{ $t('source.ddl.format') }}</MfButton>
          <MfButton size="sm" :disabled="!cardSql(card)" @click="download(card)"><Download :size="14"/>{{ $t('source.ddl.download') }}</MfButton>
        </div>
      </header>
      <div class="flex flex-col gap-3">
        <div v-for="block in card.blocks" :key="block.kind">
          <p v-if="card.blocks.length > 1" class="m-0 mb-1.5 text-sm font-medium text-content">{{ $t(`source.ddl.block.${block.kind}`) }}</p>
          <div v-if="block.loading" class="flex h-24 items-center justify-center rounded-mf-md border border-line"><MfSpinner/></div>
          <SqlCodeBlock v-else :code="block.sql" :copyable="false" :placeholder="block.error || $t(`source.ddl.empty.${block.kind}`)" max-height="max-h-[28rem]"/>
        </div>
      </div>
    </section>

    <SourceDdlPanel :open="panelOpen" :container="fieldPanelHost" :width="fieldPanelWidth" :active="active" :recent="recent" :recent-loading="recentLoading"
                    :enabled="actionEnabled" @action="runAction" @history="openHistory" @close="closePanel"/>
    <SourceDdlHistory :open="historyVisible" :table="table" :entries="history.data.value" :loading="history.loading.value" :page="history.pageIndex.value"
                      :page-size="history.pageSize.value" :total="history.dataCount.value" :detail="historyDetail"
                      @close="historyVisible = false" @page="history.onPageChange" @detail="toggleHistoryDetail"/>
  </div>
</template>

<script setup lang="ts">
import { Copy, Download, Scan, SlidersHorizontal } from '@lucide/vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import SourceDdlPanel from './components/SourceDdlPanel.vue'
import SourceDdlHistory from './components/SourceDdlHistory.vue'
import { useSourceTableDdl } from '@/composables/useSourceTableDdl'

defineOptions({ name: 'SourceTableStatement' })
const {
  cards, active, recent, recentLoading, table, fieldPanelHost, fieldPanelWidth, actionEnabled, runAction, bindCard,
  copy, download, format, cardSql, history, historyVisible, historyDetail, openHistory, toggleHistoryDetail,
  panelOpen, togglePanel, closePanel,
} = useSourceTableDdl()
</script>
