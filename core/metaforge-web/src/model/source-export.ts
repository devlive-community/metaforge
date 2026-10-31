/** An installed convert plugin offered as an export format. */
export interface ExportFormat { name: string; key: string; label: string; known: KnownFormat | null }
export type KnownFormat = 'json' | 'csv' | 'txt' | 'xml'

export interface ExportForm {
  format: string
  scope: 'all' | 'range'
  /** 1-based first row of a range. */
  start: number
  count: number
  includeStructure: boolean
  compress: boolean
  excludeNull: boolean
}

export const emptyExportForm = (): ExportForm => ({ format: '', scope: 'all', start: 1, count: 5000, includeStructure: true, compress: false, excludeNull: false })

/** Formats shown as cards; any further installed format goes in the "more formats" menu. */
export const VISIBLE_FORMATS = 4
const ORDER: KnownFormat[] = ['json', 'csv', 'txt', 'xml']

/** Installed convert plugins as export formats, known ones first; raw rows (NoneConvert) are not a format. */
export function exportFormats(plugins: any[]): ExportFormat[] {
  return (plugins || [])
      .filter(plugin => plugin?.type === 'CONVERT' && plugin?.name && plugin.name !== 'NoneConvert')
      .map(plugin => {
        const label = String(plugin.name).replace(/Convert$/, '')
        const lower = label.toLowerCase()
        const known = (ORDER as string[]).includes(lower) ? lower as KnownFormat : null
        return { name: String(plugin.name), key: String(plugin.key || ''), label: label.toUpperCase(), known }
      })
      .sort((left, right) => rank(left) - rank(right) || left.label.localeCompare(right.label))
}
const rank = (format: ExportFormat) => format.known ? ORDER.indexOf(format.known) : ORDER.length

/** The request for export-preview and export-file. */
export function exportBody(form: ExportForm) {
  return {
    format: form.format,
    scope: form.scope,
    offset: Math.max(0, Math.floor(form.start) - 1),
    limit: Math.max(1, Math.floor(form.count)),
    includeStructure: form.includeStructure,
    compress: form.compress,
    excludeNull: form.excludeNull,
  }
}

/** Indents the compact output of the JSON and XML writers for reading; other formats are shown as written. */
export function readablePreview(format: KnownFormat | null, content: string): string {
  if (format === 'json') {
    try { return JSON.stringify(JSON.parse(content), null, 2) } catch { return content }
  }
  if (format === 'xml') return indentXml(content)
  return content
}

export function indentXml(content: string): string {
  const parts = content.replace(/>\s*</g, '>\n<').split('\n')
  let depth = 0
  return parts.map(part => {
    const line = part.trim()
    if (/^<\//.test(line)) depth = Math.max(0, depth - 1)
    const out = '  '.repeat(depth) + line
    if (/^<[^!?/][^>]*[^/]>$/.test(line) && !/<\/[^>]+>$/.test(line.slice(1))) depth++
    return out
  }).join('\n')
}

export type PreviewTokenKind = 'key' | 'string' | 'number' | 'literal' | 'tag' | 'plain'
export interface PreviewToken { kind: PreviewTokenKind; text: string }

/** Splits preview text into lines of coloured tokens: JSON keys/strings/numbers/literals and XML tags. */
export function previewTokens(format: KnownFormat | null, content: string): PreviewToken[][] {
  return content.split('\n').map(line => {
    if (format === 'json') return jsonLine(line)
    if (format === 'xml') return xmlLine(line)
    return [{ kind: 'plain', text: line }]
  })
}

function jsonLine(line: string): PreviewToken[] {
  const tokens: PreviewToken[] = []
  const pattern = /("(?:[^"\\]|\\.)*")(\s*:)?|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|\b(true|false|null)\b/g
  let last = 0
  for (const match of line.matchAll(pattern)) {
    if (match.index! > last) tokens.push({ kind: 'plain', text: line.slice(last, match.index) })
    if (match[1]) {
      tokens.push({ kind: match[2] ? 'key' : 'string', text: match[1] })
      if (match[2]) tokens.push({ kind: 'plain', text: match[2] })
    }
    else tokens.push({ kind: match[3] ? 'number' : 'literal', text: match[0] })
    last = match.index! + match[0].length
  }
  if (last < line.length) tokens.push({ kind: 'plain', text: line.slice(last) })
  return tokens
}

function xmlLine(line: string): PreviewToken[] {
  const tokens: PreviewToken[] = []
  const pattern = /(<\/?[\w:-]+)|(\s[\w:-]+=)("[^"]*")|(\/?>)/g
  let last = 0
  for (const match of line.matchAll(pattern)) {
    if (match.index! > last) tokens.push({ kind: 'plain', text: line.slice(last, match.index) })
    if (match[1] || match[4]) tokens.push({ kind: 'tag', text: match[0] })
    else {
      tokens.push({ kind: 'key', text: match[2] })
      tokens.push({ kind: 'string', text: match[3] })
    }
    last = match.index! + match[0].length
  }
  if (last < line.length) tokens.push({ kind: 'plain', text: line.slice(last) })
  return tokens
}
