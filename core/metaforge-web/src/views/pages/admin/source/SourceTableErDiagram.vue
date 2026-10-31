<template>
  <div ref="root" class="relative h-full min-h-0 overflow-hidden rounded-mf-md border border-line bg-surface">
    <div class="absolute left-4 top-4 z-10 flex items-center gap-2">
      <div class="flex items-center overflow-hidden rounded-mf-md border border-line bg-surface shadow-mf-sm">
        <button type="button" class="inline-flex size-9 items-center justify-center text-content hover:bg-surface-hover disabled:opacity-40" :disabled="!canZoomOut"
                :aria-label="$t('source.er.zoomOut')" @click="zoomOut"><Minus :size="16"/></button>
        <span class="w-16 border-x border-line text-center text-sm tabular-nums text-content">{{ zoomLabel }}</span>
        <button type="button" class="inline-flex size-9 items-center justify-center text-content hover:bg-surface-hover disabled:opacity-40" :disabled="!canZoomIn"
                :aria-label="$t('source.er.zoomIn')" @click="zoomIn"><Plus :size="16"/></button>
      </div>
      <MfTooltip :content="$t(fullscreen ? 'source.er.exitFullscreen' : 'source.er.fullscreen')">
        <button type="button" class="inline-flex size-9 items-center justify-center rounded-mf-md border border-line bg-surface text-content shadow-mf-sm hover:bg-surface-hover"
                :aria-label="$t(fullscreen ? 'source.er.exitFullscreen' : 'source.er.fullscreen')" @click="toggleFullscreen">
          <Minimize :size="16" v-if="fullscreen"/><Maximize :size="16" v-else/>
        </button>
      </MfTooltip>
      <MfButton @click="center">{{ $t('source.er.center') }}</MfButton>
    </div>

    <div ref="viewport" class="absolute inset-0 touch-none select-none" :class="[settings.grid && 'er-grid', panning ? 'cursor-grabbing' : 'cursor-grab']"
         @wheel="onWheel" @pointerdown="onPointerDown" @pointermove="onPointerMove" @pointerup="onPointerUp" @pointercancel="onPointerUp">
      <div v-if="loading && !columns.length" class="flex h-full items-center justify-center"><MfSpinner/></div>
      <MfEmpty v-else-if="!columns.length" class="h-full" :title="$t('common.noData')"/>
      <article v-else ref="card" class="absolute left-0 top-0 w-max min-w-96 origin-top-left overflow-hidden rounded-mf-lg border border-primary/30 bg-surface shadow-mf-md"
               :style="{ transform }">
        <header class="flex items-center gap-3 border-b border-line bg-primary/10 px-5 py-3.5">
          <Table2 :size="22" class="text-primary"/>
          <h3 class="m-0 text-lg font-semibold text-content">{{ table }}</h3>
        </header>
        <table class="border-collapse text-sm">
          <tbody>
            <tr v-for="column in columns" :key="column.name" class="border-b border-line last:border-b-0">
              <td class="w-8 py-2.5 pl-4 pr-1 align-middle">
                <KeyRound v-if="column.primary" :size="16" class="text-warning"/>
                <Link2 v-else-if="column.reference !== null" :size="16" class="text-info"/>
              </td>
              <td class="whitespace-nowrap py-2.5 pr-6 text-right font-medium text-content">{{ column.name }}</td>
              <td v-if="settings.types" class="whitespace-nowrap border-l border-line px-4 py-2.5 text-content-secondary">{{ column.type }}</td>
              <td v-if="settings.tags || settings.notNull" class="border-l border-line px-4 py-2.5">
                <div class="flex max-w-60 flex-wrap items-center gap-1.5">
                  <template v-if="settings.tags">
                    <span v-if="column.primary" class="rounded-mf-sm bg-info/10 px-1.5 py-0.5 text-xs font-medium text-info">PK</span>
                    <span v-if="column.auto" class="rounded-mf-sm bg-info/10 px-1.5 py-0.5 text-xs font-medium text-info">AUTO</span>
                    <span v-if="column.unique" class="rounded-mf-sm bg-success/10 px-1.5 py-0.5 text-xs font-medium text-success">UK</span>
                    <MfTooltip v-if="column.reference !== null" :content="column.reference || $t('source.er.foreignKey')">
                      <span class="rounded-mf-sm bg-primary/10 px-1.5 py-0.5 text-xs font-medium text-primary">FK</span>
                    </MfTooltip>
                  </template>
                  <span v-if="settings.notNull && !column.nullable" class="rounded-mf-sm bg-danger/10 px-1.5 py-0.5 text-xs font-medium text-danger">NN</span>
                  <span v-if="settings.tags && column.defaultValue !== null" class="rounded-mf-sm bg-surface-hover px-1.5 py-0.5 text-xs text-content-secondary">{{ column.defaultValue }}</span>
                </div>
              </td>
              <td v-if="settings.comments" class="max-w-64 border-l border-line px-4 py-2.5 text-content-secondary">{{ column.comment }}</td>
            </tr>
          </tbody>
        </table>
      </article>
    </div>

    <SourceErPanel v-model:tab="panelTab" v-model:settings="settings" :open="true" :container="fieldPanelHost" :width="fieldPanelWidth" :table="table" :database="database" :info="tableInfo"/>
  </div>
</template>

<script setup lang="ts">
import { KeyRound, Link2, Maximize, Minimize, Minus, Plus, Table2 } from '@lucide/vue'
import SourceErPanel from './components/SourceErPanel.vue'
import { useSourceTableEr } from '@/composables/useSourceTableEr'

defineOptions({ name: 'SourceTableErDiagram' })
const {
  root, viewport, card, table, database, tableInfo, columns, loading, zoomLabel, transform, panning, fullscreen, settings, panelTab, fieldPanelHost, fieldPanelWidth,
  zoomIn, zoomOut, center, toggleFullscreen, onWheel, onPointerDown, onPointerMove, onPointerUp, canZoomIn, canZoomOut,
} = useSourceTableEr()
</script>

<style scoped>
.er-grid {
  background-image: linear-gradient(to right, var(--mf-color-line) 1px, transparent 1px), linear-gradient(to bottom, var(--mf-color-line) 1px, transparent 1px);
  background-size: 24px 24px;
}
</style>
