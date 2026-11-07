import { dashboardMetric, dashboardResourceUrl, type DashboardWidget } from './dashboard-layout'
import { dashboardDateKey } from './dashboard-filters'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from './query-chart'

const finite = (value: number | null) => value !== null && Number.isFinite(value) ? value : null
export function groupedMetrics(rows: QueryChartRow[], category: string, field: string, operation: DashboardWidget['operation'], baseline = '', chartOptions?: QueryChartOptions) {
  const groups = new Map<string, QueryChartRow[]>()
  if (!chartOptions) for (const row of rows) {
    const raw = row[category], name = raw == null ? '—' : typeof raw === 'object' ? JSON.stringify(raw) : String(raw)
    if (!groups.has(name)) groups.set(name, [])
    groups.get(name)!.push(row)
  }
  const aggregated = chartOptions ? aggregateQueryChart(rows, { ...chartOptions, category, metric: '', aggregation: 'count', series: '', type: 'table' }) : undefined
  const buckets: [string, QueryChartRow[]][] = aggregated ? aggregated.points.map(point => [point.category, aggregated.details.get(point.pointId) || []]) : [...groups]
  return buckets.map(([name, values]) => {
    const value = finite(dashboardMetric(values, field, operation)), base = baseline ? finite(dashboardMetric(values, baseline, operation)) : null
    const difference = value !== null && base !== null ? finite(value - base) : null
    return { name, value, baseline: base, difference, percent: difference !== null && base ? finite(difference / Math.abs(base) * 100) : null }
  })
}
export function numericStatistics(rows: QueryChartRow[], fields: string[]) {
  return fields.flatMap(field => {
    const values = rows.flatMap(row => {
      const raw = row[field]
      if (typeof raw !== 'number' && typeof raw !== 'string' || typeof raw === 'string' && !raw.trim()) return []
      return Number.isFinite(Number(raw)) ? [Number(raw)] : []
    })
    if (!values.length) return []
    const sum = values.reduce((a,b)=>a+b,0)
    return [{ field, count: values.length, min: values.reduce((a,b)=>Math.min(a,b)), max: values.reduce((a,b)=>Math.max(a,b)),
      sum: finite(sum), avg: Number.isFinite(sum) ? sum / values.length : values.reduce((mean,value,index)=>mean*(index/(index+1))+value/(index+1),0) }]
  })
}
export function calendarMetrics(rows: QueryChartRow[], dateField: string, field: string, operation: DashboardWidget['operation'], selectedYear = 0) {
  const groups = new Map<string, QueryChartRow[]>()
  let latest = 0
  for (const row of rows) {
    const date = dashboardDateKey(row[dateField])
    if (!date) continue
    latest = Math.max(latest, Number(date.slice(0, 4)))
    if (!groups.has(date)) groups.set(date, [])
    groups.get(date)!.push(row)
  }
  const year = selectedYear || latest
  const points = [...groups].filter(([date])=>Number(date.slice(0,4))===year).flatMap(([date, values])=>{
    const value = finite(dashboardMetric(values,field,operation))
    return value === null ? [] : [[date,value] as [string,number]]
  }).sort(([a],[b])=>a.localeCompare(b))
  return { year, points }
}
export interface MarkdownToken { kind: 'text' | 'bold' | 'emphasis' | 'code' | 'link'; text: string; href?: string }
/** A small safe Markdown subset; raw HTML is always plain text, never executable markup. */
export function markdownInline(text: string): MarkdownToken[] {
  const tokens: MarkdownToken[] = [], pattern = /\*\*([^*]+)\*\*|`([^`]+)`|\[([^\]]+)\]\(([^)]+)\)|\*([^*]+)\*/g
  let at=0
  for (const match of text.matchAll(pattern)) {
    if (match.index!>at) tokens.push({kind:'text',text:text.slice(at,match.index)})
    if (match[1]) tokens.push({kind:'bold',text:match[1]})
    else if (match[2]) tokens.push({kind:'code',text:match[2]})
    else if (match[3]) { const href=dashboardResourceUrl(match[4]);tokens.push(href ? {kind:'link',text:match[3],href} : {kind:'text',text:match[3]}) }
    else tokens.push({kind:'emphasis',text:match[5]})
    at=match.index!+match[0].length
  }
  if(at<text.length)tokens.push({kind:'text',text:text.slice(at)})
  return tokens
}
export interface MarkdownBlock { kind:'heading'|'paragraph'|'quote'|'code'|'list'; text:string; level?:number; ordered?:boolean; items?:string[] }
export function markdownBlocks(text: string): MarkdownBlock[] {
  const blocks: MarkdownBlock[]=[];let code:string[]|undefined
  for(const line of text.slice(0,20000).split(/\r?\n/)){
    if(/^\s*```/.test(line)){if(code){blocks.push({kind:'code',text:code.join('\n')});code=undefined}else code=[];continue}
    if(code){code.push(line);continue}
    if(!line.trim())continue
    const heading=/^(#{1,6})\s+(.+)$/.exec(line),list=/^\s*(?:(\d+)\.|[-*])\s+(.+)$/.exec(line)
    if(heading)blocks.push({kind:'heading',text:heading[2],level:heading[1].length})
    else if(list){const previous=blocks[blocks.length-1],ordered=!!list[1];if(previous?.kind==='list'&&previous.ordered===ordered)previous.items!.push(list[2]);else blocks.push({kind:'list',text:'',ordered,items:[list[2]]})}
    else if(/^>\s?/.test(line))blocks.push({kind:'quote',text:line.replace(/^>\s?/,'')})
    else blocks.push({kind:'paragraph',text:line})
  }
  if(code)blocks.push({kind:'code',text:code.join('\n')})
  return blocks
}
