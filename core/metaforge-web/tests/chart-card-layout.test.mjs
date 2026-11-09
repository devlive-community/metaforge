import assert from 'node:assert/strict'
import test from 'node:test'
import * as echarts from 'echarts'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { fitChartToCard } = await sourceModule('../../metaforge-web/src/model/chart-card-layout.ts')

function render(option, width, height) {
  const chart = echarts.init(null, undefined, { renderer: 'svg', ssr: true, width, height })
  try {
    chart.setOption(option)
    return chart.renderToSVGString()
  } finally {
    chart.dispose()
  }
}

function textPosition(svg, text) {
  const element = svg.split('\n').find(line => line.includes('>' + text + '</text>'))
  assert.ok(element, text)
  const translate = /translate\(([\d.]+) ([\d.]+)\)/.exec(element)
  assert.ok(translate, element)
  return { x: Number(translate[1]), y: Number(translate[2]) + Number(/\sy="([\d.]+)"/.exec(element)?.[1] ?? 0) }
}

test('axis titles, date ticks and legend use separate lanes in the exported chart', () => {
  const source = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', name: '日期', data: ['2026-10-25', '2026-10-26', '2026-10-27'] },
    yAxis: { type: 'value', name: '名次', inverse: true },
    series: [{ name: 'Region', type: 'line', data: [2, 4, 1] }],
  }
  const fitted = fitChartToCard(source, 320, 188, '#345')
  const svg = render(fitted, 320, 188)
  const tick = textPosition(svg, '2026-10-25')
  const caption = textPosition(svg, '日期')
  const legend = textPosition(svg, 'Region')
  assert.ok(caption.y - tick.y >= 10)
  assert.ok(legend.y - caption.y >= 10)
  assert.match(svg, /matrix\(0,-1,1,0,14\.5,[\d.]+\)" fill="#345">名次<\/text>/)
  assert.equal(source.xAxis.name, '日期')
})

test('a narrow doughnut keeps the legend outside the ring and its total inside', () => {
  const source = {
    legend: { orient: 'vertical', right: 0 },
    title: { text: '150,420', subtext: 'Total', textAlign: 'center' },
    series: [{ type: 'pie', radius: ['45%', '75%'], label: { show: false }, data: [{ name: 'East', value: 400 }, { name: 'West', value: 834 }] }],
  }
  const fitted = fitChartToCard(source, 250, 160, '#345')
  const svg = render(fitted, 250, 160)
  const total = textPosition(svg, '150,420')
  const legend = textPosition(svg, 'East')
  const ring = fitted.series[0]
  assert.equal(fitted.legend.orient, 'horizontal')
  assert.equal(fitted.title.left, ring.center[0])
  assert.ok(ring.center[1] + ring.radius[1] < legend.y)
  assert.ok(Math.abs(total.y - ring.center[1]) < ring.radius[0])
})

test('reflowing a chart keeps the active zoom and hidden legend series', () => {
  const source = {
    animation: false, legend: { orient: 'vertical', right: 0 },
    xAxis: { type: 'category', name: '日期', data: ['A', 'B', 'C', 'D', 'E'] },
    yAxis: { type: 'value', name: '数值' }, dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    series: [{ type: 'line', name: 'East', data: [1, 2, 3, 4, 5] }, { type: 'line', name: 'West', data: [1, 3, 4, 2, 5] }],
  }
  const chart = echarts.init(null, undefined, { renderer: 'svg', ssr: true, width: 560, height: 220 })
  try {
    chart.setOption(fitChartToCard(source, 560, 220, '#345'))
    chart.dispatchAction({ type: 'legendUnSelect', name: 'West' })
    chart.dispatchAction({ type: 'dataZoom', start: 20, end: 80 })
    chart.resize({ width: 280, height: 168 })
    chart.setOption(fitChartToCard(source, 280, 168, '#345'))
    const { legend, dataZoom } = chart.getOption()
    assert.equal(legend[0].orient, 'horizontal')
    assert.equal(legend[0].selected.West, false)
    assert.deepEqual([dataZoom[0].start, dataZoom[0].end], [20, 80])
  } finally {
    chart.dispose()
  }
})
