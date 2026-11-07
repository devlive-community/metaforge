/**
 * Runtime settings a plugin declares (RuntimeConfigureField), shown by the settings page and the
 * sync dialog. Fields carry an optional group; declared groups come first in a fixed order.
 */
export interface RuntimeField {
  name: string
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'PASSWORD' | 'SELECT'
  defaultValue: string
  description: string
  options?: string[]
  group?: string | null
  tunable?: boolean
}

const GROUP_ORDER = ['parallel', 'throughput', 'fault', 'general']

/** Fields by group in display order, keeping each field's declared order; ungrouped fields come last. */
export function groupRuntimeFields(fields: RuntimeField[] | undefined | null): { group: string | null; fields: RuntimeField[] }[] {
  const groups = new Map<string | null, RuntimeField[]>()
  for (const field of fields || []) {
    const key = field.group || null
    groups.set(key, [...(groups.get(key) || []), field])
  }
  const rank = (group: string | null) => {
    if (group === null) return GROUP_ORDER.length + 1
    const index = GROUP_ORDER.indexOf(group)
    return index < 0 ? GROUP_ORDER.length : index
  }
  return [...groups.entries()].sort(([left], [right]) => rank(left) - rank(right)).map(([group, items]) => ({ group, fields: items }))
}
