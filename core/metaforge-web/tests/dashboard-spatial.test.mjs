import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardCoordinateBounds, dashboardCoordinateGrid, dashboardCoordinateHotspots, dashboardCoordinates, dashboardCoordinateTracks,
  dashboardDistanceBands, dashboardDistanceBoundaries } = await model('dashboard-spatial')
const { dashboardSpatialTools, dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')

test('coordinate validation retains zero and endpoint coordinates while preserving original invalid rows', () => {
  const rows = [{ lat: '0', lon: 0 }, { lat: 90, lon: -180 }, { lat: -90, lon: 180 }, { lat: '', lon: 20 },
    { lat: 90.1, lon: 20 }, { lat: 1, lon: 180.1 }, { lat: true, lon: 0 }]
  const { points, invalid } = dashboardCoordinates(rows, 'lat', 'lon')
  assert.deepEqual(points.map(point => [point.lat, point.lon]), [[0, 0], [90, -180], [-90, 180]])
  assert.equal(points[0].row, rows[0])
  assert.deepEqual(invalid, rows.slice(3))
})

test('grid cells include their lower boundaries and keep exact global endpoints in the final cell', () => {
  const rows = [{ lat: -89.99, lon: -179.99 }, { lat: -89.989, lon: -179.988 }, { lat: 90, lon: 180 }]
  const { points } = dashboardCoordinates(rows, 'lat', 'lon')
  const grid = dashboardCoordinateGrid(points, .01, 20)
  assert.equal(grid.total, 2)
  assert.deepEqual(grid.cells[0].rows, rows.slice(0, 2))
  assert.equal(grid.cells[0].west.toFixed(2), '-179.99')
  assert.equal(grid.cells[0].south.toFixed(2), '-89.99')
  assert.equal(grid.cells[1].east, 180)
  assert.equal(grid.cells[1].north, 90)
  assert.equal(dashboardCoordinateGrid(points, .01, 1).cells.length, 1)
})

test('tracks sort by time and avoid connecting across the antimeridian or simultaneous samples', () => {
  const rows = [
    { lon: -179, lat: 0, at: '2026-11-08T04:00:00Z' }, { lon: 178, lat: 1, at: '2026-11-08T01:00:00Z' },
    { lon: -179.5, lat: 0, at: '2026-11-08T02:00:00Z' }, { lon: -178, lat: 2, at: '2026-11-08T02:00:00Z' },
    { lon: -177, lat: 2, at: '2026-11-08T05:00:00Z' }, { lon: -175, lat: 2, at: 'broken' },
  ]
  const { points } = dashboardCoordinates(rows, 'lat', 'lon')
  const [track] = dashboardCoordinateTracks(points, 'at', '', 20).tracks
  assert.deepEqual(track.points.map(point => point?.index ?? null), [1, null, 2, null, 3, null, 0, 4])
  assert.equal(track.shownPoints, 5)
  assert.equal(track.points[0].row, rows[1])
})

test('bounding boxes treat the antimeridian endpoints alike and hotspots merge rounded samples', () => {
  const rows = [{ lon: 179, lat: 2 }, { lon: -179, lat: 2 }, { lon: 180, lat: 2 }, { lon: -180, lat: 2 },
    { lon: 3, lat: 2 }, { lon: 178, lat: 40 }, { lon: 3.0001, lat: 2.0001 }]
  const { points } = dashboardCoordinates(rows, 'lat', 'lon')
  assert.deepEqual(dashboardCoordinateBounds(points, 175, -175, 0, 10).inside, rows.slice(0, 4))
  assert.deepEqual(dashboardCoordinateBounds(points, 170, 180, 0, 10).inside, [rows[0], rows[2], rows[3]])
  const hotspots = dashboardCoordinateHotspots(points, 3, 10)
  assert.deepEqual(hotspots.groups.filter(group => group.rows.length === 2).map(group => group.rows), [[rows[2], rows[3]], [rows[4], rows[6]]])
})

test('distance bands use the short arc across the date line and include zero distances', () => {
  const rows = [{ lon: 180, lat: 0 }, { lon: -179, lat: 0 }, { lon: 180, lat: 1 }, { lon: 0, lat: 0 }]
  const { points } = dashboardCoordinates(rows, 'lat', 'lon')
  const boundaries = dashboardDistanceBoundaries('1,111,112,20000')
  assert.deepEqual(boundaries, [1, 111, 112, 20000])
  const result = dashboardDistanceBands(points, 0, -180, boundaries)
  assert.deepEqual(result.map(band => band.rows.length), [1, 0, 2, 0, 1])
  assert.equal(result[0].rows[0], rows[0])
  assert.equal(dashboardDistanceBoundaries('0, 1'), undefined)
  assert.equal(dashboardDistanceBoundaries('2, 1'), undefined)
})

test('spatial configuration survives draft and published saves in both modes', async () => {
  const languages = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Spatial')
  for (const mode of ['dashboard', 'wall']) {
    design.boards[mode].widgets = dashboardSpatialTools.map(kind => {
      for (const language of languages) assert.ok(language[dashboardWidgetLabelKey(kind)])
      return { ...dashboardWidget(kind, kind, kind, mode), reportCode: 'locations', timeField: 'timestamp',
        spatialOptions: { latitudeField: 'lat', longitudeField: 'lon', gridDegrees: .25, pointLimit: 9999,
          west: 170, east: -170, referenceLon: 0, referenceLat: 0, distanceBands: '1, 2, 5' } }
    })
  }
  const saved = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const version of [saved.draft, saved.published]) for (const mode of ['dashboard', 'wall']) {
    const widgets = version.boards[mode].widgets
    assert.deepEqual(widgets.map(widget => widget.kind), dashboardSpatialTools)
    assert.equal(widgets[1].spatialOptions.gridDegrees, .25)
    assert.equal(widgets[2].timeField, 'timestamp')
    assert.equal(widgets[3].spatialOptions.east, -170)
    assert.equal(widgets[5].spatialOptions.referenceLat, 0)
    assert.equal(widgets[5].spatialOptions.pointLimit, 2000)
    assert.equal(widgets[5].action, 'filter')
  }
})
