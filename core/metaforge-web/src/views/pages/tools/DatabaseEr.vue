<template>
  <div class="er-page flex h-full min-h-0 flex-col gap-2">
    <header class="flex shrink-0 flex-wrap items-center justify-between gap-3 rounded-mf-lg bg-surface/95 px-5 py-3">
      <div class="flex min-w-0 items-baseline gap-3">
        <h1 class="m-0 truncate text-xl font-semibold text-content">{{ $t('navigation.erDiagram') }}</h1>
        <span class="truncate text-sm text-content-tertiary">{{ $t('er.summary', { database: context.database || '-', tables: tables.length, relations: relations.length }) }}</span>
      </div>
      <div class="flex shrink-0 items-center gap-2">
        <MfInput v-model="keyword" class="w-56" :placeholder="$t('er.find')" :aria-label="$t('er.find')" @keydown.enter="locate(keyword)">
          <template #prefix><Search :size="14"/></template>
        </MfInput>
        <div class="flex items-center overflow-hidden rounded-mf-md border border-line bg-surface">
          <button type="button" class="inline-flex size-9 items-center justify-center text-content hover:bg-surface-hover disabled:opacity-40" :disabled="!canZoomOut"
                  :aria-label="$t('source.er.zoomOut')" @click="zoomOut"><Minus :size="16"/></button>
          <span class="w-16 border-x border-line text-center text-sm tabular-nums text-content">{{ zoomLabel }}</span>
          <button type="button" class="inline-flex size-9 items-center justify-center text-content hover:bg-surface-hover disabled:opacity-40" :disabled="!canZoomIn"
                  :aria-label="$t('source.er.zoomIn')" @click="zoomIn"><Plus :size="16"/></button>
        </div>
        <MfButton :disabled="!tables.length" @click="fit"><Maximize :size="15"/>{{ $t('er.fit') }}</MfButton>
      </div>
    </header>

    <section class="relative min-h-0 flex-1 overflow-hidden rounded-mf-lg bg-surface/95">
      <div v-if="loading" class="flex h-full items-center justify-center"><MfSpinner/></div>
      <MfAlert v-else-if="error" variant="danger" class="m-5" :title="error"/>
      <MfEmpty v-else-if="!tables.length" class="h-full" variant="initial" :title="$t(context.database ? 'er.empty' : 'er.chooseDatabase')"/>
      <div v-else ref="viewport" class="er-grid absolute inset-0 touch-none select-none" :class="panning ? 'cursor-grabbing' : 'cursor-grab'"
           @wheel="onWheel" @pointerdown="onPointerDown" @pointermove="onPointerMove" @pointerup="onPointerUp" @pointercancel="onPointerUp" @click.self="focused = ''">
        <div class="absolute left-0 top-0 origin-top-left" :style="{ transform, width: size.width + 'px', height: size.height + 'px' }">
          <svg class="pointer-events-none absolute left-0 top-0 overflow-visible" :width="size.width" :height="size.height" aria-hidden="true">
            <path v-for="edge in edges" :key="edge.relation.table + edge.relation.name + edge.relation.column" :d="edge.path" fill="none"
                  :class="related(edge.relation) ? 'stroke-primary' : focused ? 'stroke-line' : 'stroke-content-tertiary'" :stroke-width="related(edge.relation) ? 2 : 1.25"/>
          </svg>
          <article v-for="box in boxes" :key="box.table.name" class="absolute overflow-hidden rounded-mf-md border bg-surface shadow-mf-sm"
                   :class="focused === box.table.name ? 'border-primary ring-2 ring-primary/20' : 'border-line'"
                   :style="{ left: box.x + 'px', top: box.y + 'px', width: box.width + 'px', height: box.height + 'px' }">
            <!-- The header spans the card; a long name is cut with an ellipsis and shown whole in the tooltip. -->
            <MfTooltip class="flex w-full">
              <button type="button" class="flex w-full min-w-0 items-center gap-2 border-b border-line bg-primary/10 px-3 text-left text-sm font-semibold text-content hover:bg-primary/15"
                      :style="{ height: ER_CARD.header + 'px' }" @click="openTable(box.table.name)" @mouseenter="focused = box.table.name">
                <Table2 :size="16" class="shrink-0 text-primary"/><span class="min-w-0 truncate">{{ box.table.name }}</span>
              </button>
              <template #content>
                <span class="block font-semibold">{{ box.table.name }}</span>
                <span v-if="box.table.comment" class="block">{{ box.table.comment }}</span>
              </template>
            </MfTooltip>
            <div v-for="column in box.table.columns" :key="column.name" class="flex items-center gap-2 border-b border-line/60 px-3 text-xs last:border-b-0"
                 :style="{ height: ER_CARD.row + 'px' }">
              <KeyRound v-if="column.primary" :size="13" class="shrink-0 text-warning"/>
              <Link2 v-else-if="foreignColumns.has(box.table.name + '.' + column.name)" :size="13" class="shrink-0 text-info"/>
              <span v-else class="w-[13px] shrink-0"/>
              <span class="min-w-0 flex-1 truncate text-content" :class="column.primary && 'font-semibold'">{{ column.name }}</span>
              <span class="shrink-0 truncate text-content-tertiary">{{ column.type }}</span>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { KeyRound, Link2, Maximize, Minus, Plus, Search, Table2 } from '@lucide/vue'
import { ER_CARD } from '@/model/database-er'
import { useDatabaseEr } from '@/composables/useDatabaseEr'

defineOptions({ name: 'DatabaseEr' })
const {
  context, loading, error, tables, relations, boxes, edges, viewport, transform, zoomLabel, panning, focused, keyword, size,
  zoomIn, zoomOut, fit, locate, related, onWheel, onPointerDown, onPointerMove, onPointerUp, openTable, canZoomIn, canZoomOut,
} = useDatabaseEr()
// Columns that are part of a foreign key carry the link mark.
const foreignColumns = computed(() => new Set(relations.value.map(relation => relation.table + '.' + relation.column)))
</script>

<style scoped>
.er-grid {
  background-image: linear-gradient(to right, var(--mf-color-line) 1px, transparent 1px), linear-gradient(to bottom, var(--mf-color-line) 1px, transparent 1px);
  background-size: 24px 24px;
}
</style>
