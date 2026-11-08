export function reportDateKey(value: unknown): string {
  const key = value instanceof Date && Number.isFinite(value.getTime())
    ? value.toISOString().slice(0, 10)
    : typeof value === 'string' ? /^(\d{4}-\d{2}-\d{2})(?:[T ]|$)/.exec(value)?.[1] : undefined
  if (!key) return ''
  const day = new Date(key + 'T00:00:00Z')
  return Number.isFinite(day.getTime()) && day.toISOString().slice(0, 10) === key ? key : ''
}

/** Parse a date-only cell in the viewer's local time, or honor the offset of a timestamp. */
export function reportTimestamp(value: unknown) {
  const day = reportDateKey(value)
  if (!day) return
  const time = value instanceof Date ? value.getTime() : Date.parse(value === day ? day + 'T00:00:00' : String(value).replace(' ', 'T'))
  return Number.isFinite(time) ? time : undefined
}

export function reportNumber(value: unknown): number | undefined {
  if (typeof value !== 'number' && (typeof value !== 'string' || !/^[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?$/.test(value.trim()))) return
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}
