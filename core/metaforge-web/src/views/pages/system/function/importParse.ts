import { FunctionSourceDocument } from '@/model/function'

/**
 * Parse a pasted/uploaded function document into columns + rows.
 * Supported: JSON array of objects, CSV, TSV, Markdown table, plain lines
 * (one function name per line -> single implicit column).
 */
export interface ParseOptions
{
  /** Treat the first delimited row as headers (default true). */
  firstRowHeader?: boolean
  /** Drop blank lines before parsing (default true). */
  skipEmptyRows?: boolean
}

export function parseFunctionDocument(text: string, options: ParseOptions = {}): FunctionSourceDocument
{
  const { firstRowHeader = true, skipEmptyRows = true } = options
  const trimmed = (text || '').trim()
  if (!trimmed) {
    return { columns: [], rows: [] }
  }

  if (trimmed.startsWith('[') || trimmed.startsWith('{')) {
    const parsed = JSON.parse(trimmed)
    const items = Array.isArray(parsed) ? parsed : [parsed]
    const columns = collectObjectKeys(items)
    const rows = items.map(item => columns.map(column => stringify(item?.[column])))
    return { columns, rows }
  }

  const lines = trimmed.split(/\r?\n/).filter(line => skipEmptyRows || line.trim().length > 0)
  if (lines.length === 0) {
    return { columns: [], rows: [] }
  }

  // Markdown table: | a | b | rows with a --- separator
  if (lines[0].includes('|') && lines.filter(line => line.includes('|')).length >= 2) {
    const tableLines = lines.filter(line => line.includes('|') && !/^\s*\|?[\s:|-]+\|?\s*$/.test(line))
    const splitRow = (line: string) => line.replace(/^\s*\|/, '').replace(/\|\s*$/, '').split('|').map(cell => cell.trim())
    const columns = splitRow(tableLines[0])
    const rows = tableLines.slice(1).map(splitRow)
    if (columns.length > 1) {
      return { columns, rows }
    }
  }

  const delimiter = detectDelimiter(lines[0])
  if (delimiter) {
    const firstCells = splitDelimited(lines[0], delimiter).map(cell => cell.trim())
    if (firstRowHeader && lines.length > 1) {
      return { columns: firstCells, rows: lines.slice(1).map(line => splitDelimited(line, delimiter)) }
    }
    if (!firstRowHeader) {
      const width = Math.max(...lines.map(line => splitDelimited(line, delimiter).length))
      const columns = Array.from({ length: width }, (_, index) => `column_${ index + 1 }`)
      return { columns, rows: lines.map(line => splitDelimited(line, delimiter)) }
    }
  }

  if (!firstRowHeader) {
    return { columns: ['name'], rows: lines.map(line => [line.trim()]), implicitColumn: true }
  }
  return { columns: ['name'], rows: lines.map(line => [line.trim()]), implicitColumn: true }
}

/** Build entries from a document plus the configured source-column mapping. */
export function mapDocumentEntries(
    document: FunctionSourceDocument,
    mapping: Record<string, string | undefined>,
): Record<string, string>[]
{
  return document.rows.map(row => {
    const entry: Record<string, string> = {}
    Object.entries(mapping).forEach(([key, column]) => {
      if (!column) return
      const index = document.columns.indexOf(column)
      if (index >= 0 && row[index] !== undefined && String(row[index]).trim() !== '') {
        entry[key] = String(row[index]).trim()
      }
    })
    return entry
  })
}

/** Guess a source column for a system field by name similarity. */
export function guessColumn(key: string, columns: string[]): string | undefined
{
  if (columns.length === 0) return undefined
  const candidates: Record<string, string[]> = {
    name: ['name', '函数名', '函数名称', 'function', 'function_name', 'keyword', '标题', 'title'],
    type: ['type', '类型', 'data_type'],
    category: ['category', '分类', '类别', '场景', '适用场景', 'scenario'],
    description: ['description', '描述', '说明', '功能描述', 'usage'],
    syntax: ['syntax', '语法', '使用方式', 'usage', 'signature', '格式'],
    examples: ['examples', 'example', '示例', '使用示例', 'sample'],
    parameters: ['parameters', 'parameter', '参数', '参数说明', 'params'],
    return_type: ['return_type', 'returntype', 'return_value', '返回值', '返回类型', 'returns'],
    notes: ['notes', 'note', '备注', '注意事项', '使用限制', 'remark', '说明备注'],
    version: ['version', '版本', '适用版本', '适用数据库版本', 'since']
  }
  const wanted = candidates[key] || [key]
  for (const candidate of wanted) {
    const hit = columns.find(column => column.trim().toLowerCase() === candidate.toLowerCase())
    if (hit) return hit
  }
  for (const candidate of wanted) {
    const hit = columns.find(column => column.toLowerCase().includes(candidate.toLowerCase()))
    if (hit) return hit
  }
  return undefined
}

function collectObjectKeys(items: Record<string, unknown>[]): string[]
{
  const columns: string[] = []
  items.forEach(item => {
    Object.keys(item || {}).forEach(key => {
      if (!columns.includes(key)) columns.push(key)
    })
  })
  return columns
}

function detectDelimiter(line: string): string | undefined
{
  if (line.includes('\t')) return '\t'
  if ((line.match(/,/g) || []).length >= 1) return ','
  return undefined
}

function splitDelimited(line: string, delimiter: string): string[]
{
  const cells: string[] = []
  let current = ''
  let quoted = false
  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    if (char === '"') {
      if (quoted && line[i + 1] === '"') {
        current += '"'
        i++
      }
      else {
        quoted = !quoted
      }
    }
    else if (char === delimiter && !quoted) {
      cells.push(current)
      current = ''
    }
    else {
      current += char
    }
  }
  cells.push(current)
  return cells.map(cell => cell.trim())
}

function stringify(value: unknown): string
{
  if (value === undefined || value === null) return ''
  if (typeof value === 'string') return value
  return JSON.stringify(value)
}
