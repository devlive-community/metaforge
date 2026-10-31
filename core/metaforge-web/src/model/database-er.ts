/** One table of the database ER diagram, with its columns in order. */
export interface ErTableColumn { name: string; type: string; nullable: boolean; primary: boolean; comment: string }
export interface ErTable { name: string; comment: string; columns: ErTableColumn[] }
/** A foreign key column pointing at a column of another (or the same) table. */
export interface ErRelation { name: string; table: string; column: string; referenceTable: string; referenceColumn: string }
export interface ErBox { table: ErTable; x: number; y: number; width: number; height: number }

/** Card geometry, shared by the layout and the template so edges meet the rows they connect. */
export const ER_CARD = { width: 260, header: 40, row: 28, gapX: 120, gapY: 64 }

/** Rows of GET .../{database}/er (object_type table, column, foreign_key) into tables and relations. */
export function mapDatabaseEr(rows: any[]): { tables: ErTable[]; relations: ErRelation[] } {
  const tables = new Map<string, ErTable>()
  const table = (name: string) => {
    if (!tables.has(name)) tables.set(name, { name, comment: '', columns: [] })
    return tables.get(name)!
  }
  const columns: (ErTableColumn & { table: string; position: number })[] = []
  const relations: ErRelation[] = []
  for (const row of rows || []) {
    const kind = String(row?.object_type || '')
    const name = String(row?.table_name || '')
    if (!name) continue
    if (kind === 'table') table(name).comment = String(row.object_comment || '')
    else if (kind === 'column') {
      columns.push({ table: name, name: String(row.object_name), type: String(row.data_type || '').toUpperCase(), nullable: String(row.nullable).toUpperCase() !== 'NO',
        primary: Number(row.is_primary) === 1, comment: String(row.object_comment || ''), position: Number(row.position) || 0 })
    }
    else if (kind === 'foreign_key') {
      relations.push({ name: String(row.object_name), table: name, column: String(row.data_type), referenceTable: String(row.reference_table), referenceColumn: String(row.reference_column) })
    }
  }
  columns.sort((left, right) => left.position - right.position).forEach(({ table: owner, position: _position, ...column }) => table(owner).columns.push(column))
  const known = [...tables.values()].sort((left, right) => left.name.localeCompare(right.name))
  return { tables: known, relations: relations.filter(relation => tables.has(relation.table) && tables.has(relation.referenceTable)) }
}

/** Tables on a grid of about √n columns; each grid row is as tall as its tallest card. */
export function layoutTables(tables: ErTable[]): ErBox[] {
  const perRow = Math.max(1, Math.ceil(Math.sqrt(tables.length)))
  const boxes: ErBox[] = []
  let y = 0
  for (let start = 0; start < tables.length; start += perRow) {
    const row = tables.slice(start, start + perRow)
    let tallest = 0
    row.forEach((table, index) => {
      const height = ER_CARD.header + Math.max(1, table.columns.length) * ER_CARD.row
      tallest = Math.max(tallest, height)
      boxes.push({ table, x: index * (ER_CARD.width + ER_CARD.gapX), y, width: ER_CARD.width, height })
    })
    y += tallest + ER_CARD.gapY
  }
  return boxes
}

/** A curve from the foreign key row to the referenced row, leaving and entering on the facing sides. */
export function relationPath(boxes: Map<string, ErBox>, relation: ErRelation): string | null {
  const from = boxes.get(relation.table)
  const to = boxes.get(relation.referenceTable)
  if (!from || !to) return null
  const rowY = (box: ErBox, column: string) => {
    const index = Math.max(0, box.table.columns.findIndex(item => item.name === column))
    return box.y + ER_CARD.header + index * ER_CARD.row + ER_CARD.row / 2
  }
  const y1 = rowY(from, relation.column)
  const y2 = rowY(to, relation.referenceColumn)
  if (from === to || Math.abs(from.x - to.x) < 1) {
    // Same column of cards (or a self reference): loop out on the right.
    const x = from.x + from.width
    const bend = 48
    return `M ${x} ${y1} C ${x + bend} ${y1}, ${x + bend} ${y2}, ${x} ${y2}`
  }
  const rightward = from.x < to.x
  const x1 = rightward ? from.x + from.width : from.x
  const x2 = rightward ? to.x : to.x + to.width
  const bend = Math.max(40, Math.abs(x2 - x1) / 2)
  return `M ${x1} ${y1} C ${x1 + (rightward ? bend : -bend)} ${y1}, ${x2 + (rightward ? -bend : bend)} ${y2}, ${x2} ${y2}`
}

/** Size of the drawing, for fitting it to the canvas. */
export function drawingSize(boxes: ErBox[]) {
  return boxes.reduce((size, box) => ({ width: Math.max(size.width, box.x + box.width), height: Math.max(size.height, box.y + box.height) }), { width: 0, height: 0 })
}
