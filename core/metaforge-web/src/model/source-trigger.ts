export type TriggerTiming = 'BEFORE' | 'AFTER'
export type TriggerEvent = 'INSERT' | 'UPDATE' | 'DELETE'

export const TRIGGER_TIMINGS: TriggerTiming[] = ['BEFORE', 'AFTER']
export const TRIGGER_EVENTS: TriggerEvent[] = ['INSERT', 'UPDATE', 'DELETE']

export interface SourceTrigger {
  name: string
  timing: TriggerTiming
  event: TriggerEvent
  body: string
  definer: string
  /** YYYY-MM-DD HH:mm:ss, or empty when the database does not report it. */
  created: string
  sqlMode: string
  charset: string
}

export interface TriggerForm {
  name: string
  timing: TriggerTiming
  event: TriggerEvent
  definer: string
  body: string
}

export type TriggerIssue = 'nameRequired' | 'nameInvalid' | 'nameTaken' | 'bodyRequired'

const pick = <T extends string>(value: unknown, options: T[], fallback: T) => {
  const text = String(value ?? '').toUpperCase()
  return (options.includes(text as T) ? text : fallback) as T
}

export function mapTriggers(rows: Record<string, unknown>[]): SourceTrigger[] {
  return rows.filter(row => row.trigger_name).map(row => ({
    name: String(row.trigger_name), timing: pick(row.timing, TRIGGER_TIMINGS, 'BEFORE'), event: pick(row.event, TRIGGER_EVENTS, 'INSERT'),
    body: String(row.body ?? ''), definer: String(row.definer ?? ''), created: String(row.created ?? '').replace('T', ' ').slice(0, 19),
    sqlMode: String(row.sql_mode ?? ''), charset: String(row.charset ?? ''),
  }))
}

export function emptyTriggerForm(table: string): TriggerForm {
  return { name: `trg_${table}_bi`, timing: 'BEFORE', event: 'INSERT', definer: '', body: 'BEGIN\n  \nEND' }
}

export const triggerToForm = (trigger: SourceTrigger): TriggerForm =>
  ({ name: trigger.name, timing: trigger.timing, event: trigger.event, definer: trigger.definer, body: trigger.body })

/** A copy starts from the trigger's definition under a name that does not exist yet. */
export function copyName(name: string, existing: string[]): string {
  const taken = new Set(existing.map(item => item.toLowerCase()))
  let candidate = `${name}_copy`
  for (let index = 2; taken.has(candidate.toLowerCase()); index++) candidate = `${name}_copy${index}`
  return candidate
}

/** The definition the server builds statements from; `originalName` turns it into an edit. */
export function formToTrigger(form: TriggerForm, originalName?: string) {
  return {
    name: form.name.trim(), timing: form.timing, event: form.event, definer: form.definer.trim() || undefined, body: form.body.trim(),
    ...(originalName ? { originalName } : {}),
  }
}

export function validateTriggerForm(form: TriggerForm, existing: string[], originalName?: string): Partial<Record<'name' | 'body', TriggerIssue>> {
  const issues: Partial<Record<'name' | 'body', TriggerIssue>> = {}
  const name = form.name.trim()
  if (!name) issues.name = 'nameRequired'
  else if (name.length > 64 || /[`\r\n\u0000]/.test(name)) issues.name = 'nameInvalid'
  else if (name.toLowerCase() !== (originalName || '').toLowerCase() && existing.some(item => item.toLowerCase() === name.toLowerCase())) issues.name = 'nameTaken'
  const body = form.body.trim()
  if (!body || /^BEGIN\s*END;?$/i.test(body)) issues.body = 'bodyRequired'
  return issues
}
