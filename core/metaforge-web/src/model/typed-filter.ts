export type FilterFieldKind = 'text' | 'number' | 'date' | 'boolean'
export type FilterOperator = 'eq' | 'ne' | 'contains' | 'in' | 'null' | 'notNull' | 'gt' | 'gte' | 'lt' | 'lte' | 'between'
export type FilterJoin = 'and' | 'or'
export interface FilterField { key: string; label: string; kind: FilterFieldKind }
export interface FilterRule { field: string; kind?: FilterFieldKind; operator: FilterOperator; value: string; end: string }
export interface FilterGroup { join: FilterJoin; rules: FilterRule[] }

export const operatorsByKind: Record<FilterFieldKind, readonly FilterOperator[]> = {
  text: ['eq', 'ne', 'contains', 'in', 'null', 'notNull'],
  number: ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between'],
  date: ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between', 'null', 'notNull'],
  boolean: ['eq', 'ne', 'null', 'notNull']
}

export function newFilterRule(field: FilterField): FilterRule {
  return { field: field.key, kind: field.kind, operator: 'eq', value: '', end: '' }
}

export function filterNeedsValue(operator: FilterOperator) {
  return operator !== 'null' && operator !== 'notNull'
}

export function isCompleteFilter(rule: FilterRule) {
  if (!rule.field || (rule.kind && !operatorsByKind[rule.kind].includes(rule.operator))) return false
  if (!filterNeedsValue(rule.operator)) return true
  if (!rule.value.trim() || (rule.operator === 'between' && !rule.end.trim())) return false
  if (rule.kind === 'number') {
    return Number.isFinite(Number(rule.value))
        && (rule.operator !== 'between' || (Number.isFinite(Number(rule.end)) && Number(rule.value) <= Number(rule.end)))
  }
  if (rule.kind === 'date') {
    const day = (value: string) => /^\d{4}-\d{2}-\d{2}$/.test(value) && Number.isFinite(Date.parse(value))
    return day(rule.value) && (rule.operator !== 'between' || (day(rule.end) && rule.value <= rule.end))
  }
  if (rule.kind === 'boolean') return rule.value === 'true' || rule.value === 'false'
  return true
}
