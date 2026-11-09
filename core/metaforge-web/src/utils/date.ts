/**
 * Every date shown in the app is written `YYYY-MM-DD HH:mm:ss` with a 24-hour clock, regardless of
 * the browser locale: never format dates with toLocaleString / Intl date styles, which produce
 * `2026/11/5` in Chinese locales. Dates sent by the server arrive already formatted this way.
 */
export const DATE_FORMAT = 'YYYY-MM-DD'
export const TIME_FORMAT = 'HH:mm:ss'
export const DATE_TIME_FORMAT = `${DATE_FORMAT} ${TIME_FORMAT}`

type DateInput = Date | number | string | null | undefined

const pad = (value: number) => String(value).padStart(2, '0')

function toDate(value: DateInput): Date | undefined {
  if (value === null || value === undefined || value === '') return undefined
  const date = value instanceof Date ? value : new Date(typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : value)
  return Number.isNaN(date.getTime()) ? undefined : date
}

/** Calendar fields in the browser's time zone, or in `timeZone` (an IANA name) when given. */
function fields(date: Date, timeZone?: string) {
  if (!timeZone) {
    return { YYYY: String(date.getFullYear()), MM: pad(date.getMonth() + 1), DD: pad(date.getDate()), HH: pad(date.getHours()), mm: pad(date.getMinutes()), ss: pad(date.getSeconds()) }
  }
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-US', {
    timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23',
  }).formatToParts(date).map(part => [part.type, part.value]))
  return { YYYY: parts.year, MM: parts.month, DD: parts.day, HH: parts.hour === '24' ? '00' : parts.hour, mm: parts.minute, ss: parts.second }
}

/** Formats with YYYY / MM / DD / HH / mm / ss tokens; empty or invalid input gives `fallback`. */
export function formatDate(value: DateInput, format = DATE_TIME_FORMAT, options: { timeZone?: string; fallback?: string } = {}): string {
  const date = toDate(value)
  if (!date) return options.fallback ?? '-'
  const values = fields(date, options.timeZone)
  return format.replace(/YYYY|MM|DD|HH|mm|ss/g, token => values[token as keyof typeof values])
}

export const formatDateTime = (value: DateInput, options?: { timeZone?: string; fallback?: string }) => formatDate(value, DATE_TIME_FORMAT, options)
export const formatDay = (value: DateInput, options?: { timeZone?: string; fallback?: string }) => formatDate(value, DATE_FORMAT, options)
export const formatClock = (value: DateInput, options?: { timeZone?: string; fallback?: string }) => formatDate(value, TIME_FORMAT, options)

export class DateUtils
{
    static formatTime(date: Date, format: string): string
    {
        return formatDate(date, format)
    }
}
