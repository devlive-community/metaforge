import { dashboardBandBoundaries } from './dashboard-business'
import { dashboardNumericValue, dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import type { QueryChartRow } from './query-chart'

export interface DashboardCoordinate {
  row: QueryChartRow
  index: number
  lon: number
  lat: number
}

export function dashboardCoordinates(rows: readonly QueryChartRow[], latitude: string, longitude: string) {
  const points: DashboardCoordinate[] = [], invalid: QueryChartRow[] = []
  rows.forEach((row, index) => {
    const lat = dashboardNumericValue(row[latitude]), lon = dashboardNumericValue(row[longitude])
    if (lat === undefined || lon === undefined || Math.abs(lat) > 90 || Math.abs(lon) > 180) invalid.push(row)
    else points.push({ row, index, lon, lat })
  })
  return { points, invalid }
}

function gridIndex(value: number, lower: number, upper: number, step: number) {
  const count = Math.ceil((upper - lower) / step)
  // Decimal cell boundaries can fall a few machine units below an integer.
  const offset = (value - lower) / step
  const tolerance = Number.EPSILON * Math.max(1, Math.abs(lower), Math.abs(value)) * 4 / step
  return Math.min(count - 1, Math.floor(offset + tolerance))
}

export function dashboardCoordinateGrid(points: readonly DashboardCoordinate[], degrees: number, limit: number) {
  const cells = new Map<string, { key: string; west: number; east: number; south: number; north: number; rows: QueryChartRow[] }>()
  for (const point of points) {
    const x = gridIndex(point.lon, -180, 180, degrees), y = gridIndex(point.lat, -90, 90, degrees)
    const key = x + ':' + y
    let cell = cells.get(key)
    if (!cell) {
      const west = -180 + x * degrees, south = -90 + y * degrees
      cell = { key, west, east: Math.min(180, west + degrees), south, north: Math.min(90, south + degrees), rows: [] }
      cells.set(key, cell)
    }
    cell.rows.push(point.row)
  }
  return { total: cells.size, cells: [...cells.values()].sort((a, b) => b.rows.length - a.rows.length || a.west - b.west || a.south - b.south).slice(0, limit) }
}

function groupName(value: unknown) {
  return !dashboardValueMissing(value) && (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') ? String(value) : ''
}

export function dashboardCoordinateTracks(points: readonly DashboardCoordinate[], timeField: string, groupField: string, pointLimit: number, trackLimit = 6) {
  const tracks = new Map<string, (DashboardCoordinate & { time: number })[]>()
  for (const point of points) {
    const time = dashboardTimestamp(point.row[timeField])
    if (time === undefined) continue
    const name = groupField ? groupName(point.row[groupField]) : ''
    const track = tracks.get(name) ?? []
    track.push({ ...point, time })
    tracks.set(name, track)
  }
  return {
    total: tracks.size,
    tracks: [...tracks].sort(([a, left], [b, right]) => right.length - left.length || a.localeCompare(b)).slice(0, trackLimit).map(([name, track]) => {
      track.sort((a, b) => a.time - b.time || a.index - b.index)
      const simultaneous = new Set<number>()
      for (let index = 1; index < track.length; index++) if (track[index].time === track[index - 1].time) simultaneous.add(track[index].time)
      const visible = track.slice(-pointLimit)
      const points: (typeof visible[number] | null)[] = []
      visible.forEach((point, index) => {
        const previous = visible[index - 1]
        if (previous && (simultaneous.has(point.time) || simultaneous.has(previous.time) || Math.abs(point.lon - previous.lon) > 180)) points.push(null)
        points.push(point)
      })
      return { name, shownPoints: visible.length, points }
    }),
  }
}

export function dashboardCoordinateBounds(points: readonly DashboardCoordinate[], west: number, east: number, south: number, north: number) {
  const inside: QueryChartRow[] = [], outside: QueryChartRow[] = []
  const matchesLongitude = (lon: number) => west <= east ? lon >= west && lon <= east : lon >= west || lon <= east
  for (const point of points) {
    const longitudeMatches = matchesLongitude(point.lon) || Math.abs(point.lon) === 180 && matchesLongitude(-point.lon)
    const target = longitudeMatches && point.lat >= south && point.lat <= north ? inside : outside
    target.push(point.row)
  }
  return { inside, outside }
}

export function dashboardCoordinateHotspots(points: readonly DashboardCoordinate[], precision: number, limit: number) {
  const groups = new Map<string, { key: string; lon: number; lat: number; rows: QueryChartRow[] }>()
  for (const point of points) {
    const lon = Number(point.lon.toFixed(precision)) || 0, lat = Number(point.lat.toFixed(precision)) || 0
    const longitude = lon === 180 ? -180 : lon
    const key = longitude + ':' + lat
    const group = groups.get(key) ?? { key, lon: longitude, lat, rows: [] }
    group.rows.push(point.row)
    groups.set(key, group)
  }
  return { total: groups.size, groups: [...groups.values()].sort((a, b) => b.rows.length - a.rows.length || a.lon - b.lon || a.lat - b.lat).slice(0, limit) }
}

export function dashboardDistanceBoundaries(input: string) {
  const bounds = dashboardBandBoundaries(input)
  return bounds?.every(value => value > 0) ? bounds : undefined
}

const radians = (degrees: number) => degrees * Math.PI / 180
const EARTH_RADIUS_KM = 6371.0088

function distanceKm(point: DashboardCoordinate, referenceLat: number, referenceLon: number) {
  const deltaLat = radians(point.lat - referenceLat)
  const longitude = ((point.lon - referenceLon + 540) % 360) - 180
  const deltaLon = radians(longitude)
  const haversine = Math.sin(deltaLat / 2) ** 2 + Math.cos(radians(referenceLat)) * Math.cos(radians(point.lat)) * Math.sin(deltaLon / 2) ** 2
  const safe = Math.max(0, Math.min(1, haversine))
  return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(safe), Math.sqrt(1 - safe))
}

export function dashboardDistanceBands(points: readonly DashboardCoordinate[], referenceLat: number, referenceLon: number, boundaries: readonly number[]) {
  const bands = Array.from({ length: boundaries.length + 1 }, (_, index) => ({
    lower: boundaries[index - 1],
    upper: boundaries[index],
    rows: [] as QueryChartRow[],
  }))
  for (const point of points) {
    const distance = distanceKm(point, referenceLat, referenceLon)
    const index = boundaries.findIndex(upper => distance < upper)
    bands[index < 0 ? boundaries.length : index].rows.push(point.row)
  }
  return bands
}
