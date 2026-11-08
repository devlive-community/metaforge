import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const load = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardTools, dashboardChartType, dashboardContainerTool, dashboardEmbeddedChartOptions } = await load('dashboard-layout')
const { screenComponentExample } = await load('screen-component-example')
const { prepareQueryChart } = await load('query-chart-series')
const { aggregateQueryChart } = await load('query-chart')
const { isInformationBlock } = await load('information-block')

test('catalog examples have renderable chart data and supply children or entries where required', () => {
  for (const kind of dashboardTools) {
    const { widget, snapshot, report, children } = screenComponentExample(kind, kind, key => key, ['/first.svg', '/second.svg'], Date.UTC(2026, 10, 8))
    if (dashboardContainerTool(kind)) assert.ok(children.length > 0 && children.every(child => child.parentId === widget.id), kind)
    if (isInformationBlock(kind)) assert.ok(widget.informationItems?.length, kind)
    if (!dashboardChartType(kind)) continue
    assert.ok(snapshot?.columns.length && report?.code === widget.reportCode && report.realtime === false, kind)
    const options = dashboardEmbeddedChartOptions(widget, snapshot, false)
    const data = aggregateQueryChart(snapshot.columns, options)
    const chart = prepareQueryChart(options, data)
    assert.equal(chart?.issue, undefined, kind)
    assert.ok(chart?.option || data.points.some(point => point.value !== null), kind)
  }
})
