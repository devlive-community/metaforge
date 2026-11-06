import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync, readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { WORKFLOW_METRICS, WORKFLOW_METRIC_CHARTS, metricGroups, metricText } = await sourceModule('../../metaforge-web/src/model/workflow-metrics.ts')
const root = join(dirname(fileURLToPath(import.meta.url)), '../../..')
const executors = readdirSync(join(root, 'executor')).filter(name => name.startsWith('metaforge-executor-') && name !== 'metaforge-executor-spi')

/** The `metrics:` list of an executor's workflow.yaml (a block list or `[]`). */
function declared(module) {
  const yaml = readFileSync(join(root, 'executor', module, 'src/main/resources/org/devlive/metaforge/executor/workflow/workflow.yaml'), 'utf8')
  const block = yaml.match(/^metrics:[ \t]*(\[\])?\n((?:[ \t]+-[^\n]*\n|[ \t]*#[^\n]*\n)*)/m)
  assert.ok(block, `${module} declares the metrics it reports`)
  return block[1] ? [] : [...block[2].matchAll(/^[ \t]+-[ \t]*(\S+)/gm)].map(match => match[1])
}

test('every metric an executor declares has a place on the run pages', () => {
  const metricsModel = readFileSync(join(root, 'executor/metaforge-executor-spi/src/main/kotlin/org/devlive/metaforge/executor/configure/ExecutorMetrics.kt'), 'utf8')
  const locales = ['zh-cn', 'en'].map(name => JSON.parse(readFileSync(join(root, 'core/metaforge-web/src/i18n/locales', name + '.json'), 'utf8')))
  assert.ok(executors.length >= 2)
  for (const module of executors) {
    for (const key of declared(module)) {
      assert.ok(key in WORKFLOW_METRICS || WORKFLOW_METRIC_CHARTS.includes(key), `${module}: ${key} is registered in model/workflow-metrics.ts`)
      const field = key === 'queue' ? 'queueDepth' : key
      assert.ok(new RegExp(`var ${field}\\b`).test(metricsModel), `${module}: ${key} is an ExecutorMetrics field`)
    }
  }
  for (const key of Object.keys(WORKFLOW_METRICS)) {
    locales.forEach(locale => assert.ok(locale['workflow.studio.metric.' + key], `label for ${key}`))
  }
})

test('metrics render only what is known and declared', () => {
  const snapshot = { rowsRead: 12345, totalRows: -1, progress: -1, etaSeconds: 90, bytesWritten: 3 * 1024 * 1024, writeRowsPerSecond: 812.34,
    avgBatchMs: 12.5, queueDepth: 2, queueCapacity: 4, bottleneck: 'TARGET' }
  assert.equal(metricText('totalRows', snapshot), undefined, 'unknown totals stay empty')
  assert.equal(metricText('progress', snapshot), undefined)
  assert.equal(metricText('etaSeconds', snapshot), '1m 30s')
  assert.equal(metricText('bytesWritten', snapshot), '3.0 MB')
  assert.match(metricText('writeRowsPerSecond', snapshot), /^812\.3 rows\/s$/)
  assert.equal(metricText('avgBatchMs', snapshot), '13ms')
  assert.equal(metricText('queue', snapshot), '2 / 4')
  assert.equal(metricText('bottleneck', snapshot), 'TARGET')
  assert.equal(metricText('rowsRead', undefined), undefined)
  assert.deepEqual(metricGroups(['rowsRead', 'series', 'queue', 'unknown', 'writeRowsPerSecond']).map(group => [group.group, group.keys]),
    [['volume', ['rowsRead']], ['throughput', ['writeRowsPerSecond']], ['pipeline', ['queue']]])
  assert.deepEqual(metricGroups([]), [])
})
