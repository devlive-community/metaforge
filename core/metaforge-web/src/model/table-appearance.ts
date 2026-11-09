export interface TableAppearance {
  density?: 'compact' | 'default'
  striped?: boolean
  bordered?: boolean
}

export function readTableAppearance(value: unknown): TableAppearance | undefined {
  if (!value || typeof value !== 'object') return
  const style = value as Record<string, unknown>
  const density = style.density === 'compact' || style.density === 'default' ? style.density : undefined
  const striped = typeof style.striped === 'boolean' ? style.striped : undefined
  const bordered = typeof style.bordered === 'boolean' ? style.bordered : undefined
  if (density !== undefined || striped !== undefined || bordered !== undefined) return { density, striped, bordered }
}
