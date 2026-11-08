<template>
  <MfEmpty v-if="emptyKey" class="h-full" size="sm" :title="$t(emptyKey)"/>
  <div v-else-if="kind === 'geoBounds'" class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <p class="shrink-0 text-xs tabular-nums text-content-secondary">{{ boxLabel }}</p>
    <div class="grid min-h-0 flex-1 grid-cols-[repeat(auto-fit,minmax(min(100%,8rem),1fr))] content-start gap-2 overflow-auto">
      <section v-for="item in boundsCards" :key="item.label" class="min-w-0 rounded-mf-md border border-line bg-surface-hover px-2.5 py-1">
        <p class="text-xs font-medium text-content-secondary">{{ $t(key + item.label) }}</p>
        <div class="flex flex-wrap items-center justify-between gap-1">
          <MfButton v-if="interactive && item.rows.length" size="sm" variant="ghost" class="h-auto! py-0.5! text-base font-semibold tabular-nums text-primary"
                    :aria-label="linkLabel($t(key + item.label), item.rows.length)" @click="link(item.rows)">{{ count(item.rows.length) }}</MfButton>
          <span v-else class="py-1 text-base font-semibold tabular-nums">{{ count(item.rows.length) }}</span>
          <span class="text-xs tabular-nums text-content-secondary">{{ percent(item.rows.length / source.length) }}</span>
        </div>
      </section>
    </div>
  </div>
  <div v-else-if="tableKind" class="flex h-full min-h-0 min-w-0 flex-col gap-2 overflow-auto">
    <div class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs">
      <MfTag size="sm" variant="primary">{{ table.summary }}</MfTag>
      <span v-if="coordinates.invalid.length" class="text-content-secondary">{{ $t(key + 'invalidCount', { count: count(coordinates.invalid.length) }) }}</span>
    </div>
    <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="table.columns" :data-source="table.rows" row-key="key">
      <template #cell-name="{ record }"><EllipsisCell :text="record.name" class="font-medium tabular-nums"/></template>
      <template #cell-count="{ record }">
        <MfButton v-if="interactive && record.rows.length" size="sm" variant="ghost" class="min-w-8 tabular-nums text-primary"
                  :aria-label="linkLabel(record.name, record.rows.length)" @click="link(record.rows)">{{ count(record.rows.length) }}</MfButton>
        <span v-else class="tabular-nums">{{ count(record.rows.length) }}</span>
      </template>
      <template #cell-share="{ record }"><span class="tabular-nums">{{ percent(record.rows.length / coordinates.points.length) }}</span></template>
    </MfTable>
  </div>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-1">
    <div class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs">
      <MfTag size="sm" variant="primary">{{ plot.summary }}</MfTag>
      <span v-if="coordinates.invalid.length" class="text-content-secondary">{{ $t(key + 'invalidCount', { count: count(coordinates.invalid.length) }) }}</span>
    </div>
    <EChart class="min-h-0 flex-1" :option="plot.option" @point="selectPoint"/>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CustomSeriesRenderItem, EChartsOption, LineSeriesOption } from 'echarts'
import type { MfTableColumn } from 'metaforge-ui'
import { useChartColors } from '@/composables/useChartColors'
import { dashboardDataSettings, dashboardSpatialSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardCoordinateBounds, dashboardCoordinateGrid, dashboardCoordinateHotspots, dashboardCoordinates, dashboardCoordinateTracks,
  dashboardDistanceBands, dashboardDistanceBoundaries, type DashboardCoordinate } from '@/model/dashboard-spatial'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { formatDateTime } from '@/utils/date'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import EChart from '@/views/components/visual/EChart.vue'

const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'color' | 'dataOptions' | 'kind' | 'spatialOptions' | 'timeField'>; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.spatial.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardSpatialSettings(props.widget.spatialOptions))
const limit = computed(() => Math.min(30, dashboardDataSettings(props.widget.dataOptions).limit))
const source = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const coordinates = computed(() => dashboardCoordinates(source.value, settings.value.latitudeField, settings.value.longitudeField))
const interactive = computed(() => !props.editable && props.widget.action === 'filter')
const tableKind = computed(() => kind.value === 'geoHotspots' || kind.value === 'geoDistanceBands')
const integers = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const degrees = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 6, useGrouping: false }))
const percentages = computed(() => new Intl.NumberFormat(locale.value, { style: 'percent', maximumFractionDigits: 1 }))
const count = (value: number) => integers.value.format(value)
const percent = (value: number) => percentages.value.format(value)
const location = (lon: number, lat: number) => degrees.value.format(lon) + '°, ' + degrees.value.format(lat) + '°'
const linkLabel = (name: string, value: number) => t('dashboard.studio.relation.filterCell', { name, value: count(value) })
function link(rows: QueryChartRow[]) { if (rows.length) emit('filter', rows) }
const colors = useChartColors()

const boundsCards = computed(() => {
  const { west, east, south, north } = settings.value
  const { inside, outside } = dashboardCoordinateBounds(coordinates.value.points, west, east, south, north)
  return [{ label: 'inside', rows: inside }, { label: 'outside', rows: outside }, { label: 'invalid', rows: coordinates.value.invalid }]
})
const boxLabel = computed(() => {
  const { west, east, south, north } = settings.value
  return t(key + 'boundsRange', { west, east, south, north }) + (west > east ? ' · ' + t(key + 'wraps') : '')
})
const boundaries = computed(() => dashboardDistanceBoundaries(settings.value.distanceBands))
function bandLabel(band: { lower?: number; upper?: number }) {
  if (band.lower === undefined) return band.upper === undefined ? t(key + 'allDistances') : t(key + 'underKm', { value: band.upper })
  return band.upper === undefined ? t(key + 'atLeastKm', { value: band.lower }) : t(key + 'betweenKm', { lower: band.lower, upper: band.upper })
}
const table = computed(() => {
  const hotspot = kind.value === 'geoHotspots'
  const columns: MfTableColumn[] = [
    { title: t(key + (hotspot ? 'longitudeLatitude' : 'distance')), dataIndex: 'name' },
    { title: t(key + 'count'), dataIndex: 'count', align: 'right' },
    { title: t(key + 'share'), dataIndex: 'share', align: 'right' },
  ]
  if (hotspot) {
    const result = dashboardCoordinateHotspots(coordinates.value.points, settings.value.precision, limit.value)
    return { columns, rows: result.groups.map(group => ({ key: group.key, name: location(group.lon, group.lat), rows: group.rows })),
      summary: t(key + 'hotspotCount', { shown: count(result.groups.length), total: count(result.total) }) }
  }
  const { referenceLat, referenceLon } = settings.value
  const result = dashboardDistanceBands(coordinates.value.points, referenceLat ?? 0, referenceLon ?? 0, boundaries.value ?? [])
  return { columns, rows: result.map((band, index) => ({ key: String(index), name: bandLabel(band), rows: band.rows })),
    summary: t(key + 'referenceSummary', { coordinates: location(referenceLon ?? 0, referenceLat ?? 0) }) }
})

type Plot = { option: EChartsOption; samples: Map<string, QueryChartRow[]>; summary: string; empty?: boolean }
function axisOption(name: string, nameGap = 25) {
  const theme = colors.value
  return { type: 'value' as const, name, nameLocation: 'middle' as const, nameGap, scale: true,
    nameTextStyle: { color: theme.text }, axisLabel: { color: theme.text, hideOverlap: true }, splitLine: { lineStyle: { color: theme.line } } }
}
function baseOption(): EChartsOption {
  const theme = colors.value
  return {
    animation: false,
    color: [props.widget.color || theme.primary, ...theme.colors.slice(2)],
    grid: { top: 15, left: 30, right: 28, bottom: 30, containLabel: true },
    xAxis: axisOption(t(key + 'longitude')),
    yAxis: axisOption(t(key + 'latitude'), 48),
    tooltip: { trigger: 'item', renderMode: 'richText', confine: true },
  }
}
function pointDatum(point: DashboardCoordinate, time?: number) {
  return { id: String(point.index), name: location(point.lon, point.lat) + (time === undefined ? '' : ' · ' + formatDateTime(time)), value: [point.lon, point.lat] }
}
const drawCell: CustomSeriesRenderItem = (_params, api) => {
  const [left, bottom] = api.coord([Number(api.value(0)), Number(api.value(1))])
  const [right, top] = api.coord([Number(api.value(2)), Number(api.value(3))])
  return { type: 'rect', shape: { x: left, y: top, width: right - left, height: bottom - top }, style: { fill: api.visual('color') as string } }
}
const plot = computed<Plot>(() => {
  const points = coordinates.value.points, option = baseOption(), settingsValue = settings.value
  const samples = new Map<string, QueryChartRow[]>()
  if (kind.value === 'geoGrid') {
    const result = dashboardCoordinateGrid(points, settingsValue.gridDegrees, settingsValue.pointLimit)
    const theme = colors.value
    const maximum = Math.max(1, ...result.cells.map(cell => cell.rows.length))
    const data = result.cells.map(cell => {
      samples.set(cell.key, cell.rows)
      return { id: cell.key, name: location(cell.west, cell.south) + ' – ' + location(cell.east, cell.north),
        value: [cell.west, cell.south, cell.east, cell.north, cell.rows.length] }
    })
    return { samples, summary: t(key + 'gridCount', { shown: count(data.length), total: count(result.total), degrees: settingsValue.gridDegrees }),
      option: { ...option, grid: { top: 12, left: 30, right: 28, bottom: 51, containLabel: true },
        visualMap: { min: 0, max: maximum, text: [count(maximum), '0'], dimension: 4, orient: 'horizontal', left: 'center', bottom: 0,
          itemWidth: 8, itemHeight: 80, textStyle: { color: theme.text }, inRange: { color: [theme.line, props.widget.color || theme.primary] } },
        series: [{ type: 'custom', renderItem: drawCell, clip: true, data, dimensions: ['west', 'south', 'east', 'north', t(key + 'count')],
          encode: { x: [0, 2], y: [1, 3], tooltip: [4] } }],
      },
    }
  }
  if (kind.value === 'geoTrack') {
    const result = dashboardCoordinateTracks(points, props.widget.timeField || '', settingsValue.groupField, settingsValue.pointLimit)
    const series: LineSeriesOption[] = result.tracks.map(track => ({
      name: track.name || t(key + (settingsValue.groupField ? 'ungrouped' : 'allTrack')),
      type: 'line',
      showSymbol: true,
      symbolSize: 6,
      connectNulls: false,
      data: track.points.map(point => {
        if (!point) return { value: [null, null] }
        samples.set(String(point.index), [point.row])
        return pointDatum(point, point.time)
      }),
    }))
    return { samples, empty: !series.length,
      summary: t(key + 'trackCount', { shown: count(series.length), total: count(result.total), points: count(result.tracks.reduce((total, track) => total + track.shownPoints, 0)) }),
      option: { ...option, grid: { top: 34, left: 30, right: 28, bottom: 30, containLabel: true },
        legend: { top: 0, type: 'scroll', textStyle: { color: colors.value.text } }, series },
    }
  }
  const data = points.slice(0, settingsValue.pointLimit).map(point => {
    samples.set(String(point.index), [point.row])
    return pointDatum(point)
  })
  return { samples, summary: t(key + 'pointCount', { shown: count(data.length), total: count(points.length) }),
    option: { ...option, series: [{ type: 'scatter', data, symbolSize: 8, itemStyle: { opacity: .8 } }] },
  }
})
function selectPoint(value: unknown) {
  if (!interactive.value || !value || typeof value !== 'object' || !('id' in value) || typeof value.id !== 'string') return
  const rows = plot.value.samples.get(value.id)
  if (rows) link(rows)
}

const emptyKey = computed(() => {
  const fields = props.options.fields, config = settings.value
  if (config.longitudeField === config.latitudeField || !fields.includes(config.longitudeField) || !fields.includes(config.latitudeField)) return key + 'chooseCoordinates'
  if (!source.value.length) return 'common.noData'
  if (kind.value === 'geoBounds') return config.south > config.north ? key + 'invalidBounds' : ''
  if (!coordinates.value.points.length) return key + 'noCoordinates'
  if (kind.value === 'geoTrack') {
    if (!fields.includes(props.widget.timeField ?? '')) return key + 'chooseTime'
    if (plot.value.empty) return key + 'noTrack'
  }
  if (kind.value === 'geoDistanceBands') {
    if (config.referenceLat === null || config.referenceLon === null) return key + 'chooseReference'
    if (!boundaries.value) return key + 'bandHint'
  }
  return ''
})
</script>
