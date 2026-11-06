import type { ConfigureItem, NodeDefinition, NodeInstance, ValidationEntry, WorkflowConnection } from './types'

type Limits = Partial<Record<'sources' | 'transforms' | 'sinks', number>>

export const nodeForPort = (port: string, nodes: NodeInstance[]) => nodes.find(node =>
  (node.ports || []).some(item => port === `${node.id}-${item.id}`))
export const mergeDefinitions = (definitions: NodeDefinition[], instances: NodeInstance[]): NodeInstance[] => instances.map((original, index) => {
  const instance = { ...original, position: { x: Number.isFinite(original.position?.x) ? original.position.x : 60 + index * 280, y: Number.isFinite(original.position?.y) ? original.position.y : 180 } }
  const definition = definitions.find(node => node.id === instance.tid)
  if (!definition) return { ...instance, configure: [], ports: [] }
  const data = { ...defaultValues(definition.configure), ...instance.data }
  return { ...instance, key: definition.key, category: definition.category, label: instance.label || definition.label,
    description: instance.description ?? definition.description, ports: definition.ports || [], configure: definition.configure || [], data }
})
export const flattenNodes = (nodes: NodeInstance[]) => nodes.map(({ id, tid, category, position, key, data, label, description }) =>
  ({ id, tid, category, position, key, data: { ...data }, label, description }))

export function canConnect(nodes: NodeInstance[], connections: WorkflowConnection[], source: string, target: string): boolean {
  const from = nodeForPort(source, nodes), to = nodeForPort(target, nodes)
  if (!from || !to || from.id === to.id || !from.ports?.some(port => port.type === 'output' && source === `${from.id}-${port.id}`)
    || !to.ports?.some(port => port.type === 'input' && target === `${to.id}-${port.id}`)) return false
  if (connections.some(edge => edge.source === source || edge.target === target)) return false
  const visited = new Set<string>()
  const visit = (id: string): boolean => {
    if (id === from.id) return true
    if (visited.has(id)) return false
    visited.add(id)
    return connections.filter(edge => nodeForPort(edge.source, nodes)?.id === id)
      .some(edge => { const next = nodeForPort(edge.target, nodes); return !!next && visit(next.id) })
  }
  return !visit(to.id)
}
/** Declared default values of a field list, used for new nodes and empty settings. */
export const defaultValues = (fields?: ConfigureItem[]): Record<string, any> =>
  Object.fromEntries((fields || []).filter(field => field.value !== undefined).map(field => [field.field, field.value]))
/** Editor tab of a field: the declared group, else SQL text areas, required fields, then the rest. */
export const fieldGroup = (field: ConfigureItem): 'basic' | 'sql' | 'advanced' =>
  field.group || (field.type === 'sql' || field.type === 'textarea' && /query|sql$/i.test(field.field) ? 'sql' : field.required ? 'basic' : 'advanced')
const isEmpty = (value: unknown) => value === undefined || value === null || String(value).trim() === '' ||
  Array.isArray(value) && !value.length || typeof value === 'object' && !Array.isArray(value) && !Object.keys(value as object).length
/** Problems of values against declared fields, mirroring the server: required, maps, numeric bounds. */
export function fieldIssues(fields: ConfigureItem[] | undefined, values: Record<string, any> | undefined, owner: string,
  label: (field: ConfigureItem) => string = field => field.label): string[] {
  const issues: string[] = []
  ;(fields || []).forEach(field => {
    const value = values?.[field.field]
    if (isEmpty(value)) { if (field.required) issues.push(`${owner}: ${label(field)}`); return }
    if ((field.type === 'map' || field.type === 'mapping') && (typeof value !== 'object' || Array.isArray(value))) issues.push('workflow.studio.invalidMap')
    if (field.type === 'number' && (typeof value !== 'number' || field.min !== undefined && value < field.min || field.max !== undefined && value > field.max))
      issues.push(`${owner}: ${label(field)}`)
  })
  return issues
}
/** Fields to clear after `changed` changes: everything that reads its choices from it, transitively. */
export function dependentFields(fields: ConfigureItem[] | undefined, changed: string): string[] {
  const cleared = new Set<string>(), queue = [changed]
  while (queue.length) {
    const current = queue.shift()!
    ;(fields || []).filter(field => Object.values(field.from || {}).includes(current) && !cleared.has(field.field))
      .forEach(field => { cleared.add(field.field); queue.push(field.field) })
  }
  return [...cleared]
}
/** The node connected to the input of `nodeId`, whose output feeds it. */
export const upstreamOf = (nodes: NodeInstance[], connections: WorkflowConnection[], nodeId?: string): NodeInstance | undefined => {
  const edge = connections.find(item => nodeForPort(item.target, nodes)?.id === nodeId)
  return edge ? nodeForPort(edge.source, nodes) : undefined
}
/** Values of the named fields, or undefined while any of them is still empty. */
export function resolveFrom(from: Record<string, string> | undefined, values: Record<string, any> | undefined): Record<string, string> | undefined {
  if (!from) return undefined
  const resolved: Record<string, string> = {}
  for (const [key, field] of Object.entries(from)) {
    const value = values?.[field]
    if (value === undefined || value === null || String(value).trim() === '') return undefined
    resolved[key] = String(value)
  }
  return resolved
}
/**
 * Maps incoming columns onto target columns with the same name (case-insensitive), keeping mappings
 * the user already chose. The result is `incoming=target`, the shape the executors read.
 */
export function matchColumns(targets: string[], incoming: string[], current: Record<string, string> = {}): Record<string, string> {
  const result: Record<string, string> = {}
  const mapped = new Set(Object.values(current))
  Object.entries(current).forEach(([source, target]) => { if (targets.includes(target)) result[source] = target })
  targets.filter(target => !mapped.has(target)).forEach(target => {
    const source = incoming.find(name => name.toLowerCase() === target.toLowerCase() && !(name in result))
    if (source) result[source] = target
  })
  return result
}
const limitOf = (limits: Limits, category: string) => {
  const value = limits[`${category}s` as keyof Limits]
  return typeof value === 'number' ? value : category === 'transform' ? 0 : 1
}
export function validateWorkflow(nodes: NodeInstance[], connections: WorkflowConnection[], limits: Limits = {},
  label?: (field: ConfigureItem) => string): ValidationEntry[] {
  const issues: ValidationEntry[] = []
  const count = (category: string) => nodes.filter(node => node.category === category).length
  if (['source', 'sink'].some(category => count(category) < 1 || count(category) > limitOf(limits, category)))
    issues.push({ type: 'port', scope: '', message: 'workflow.studio.needEndpoints' })
  if (count('transform') > limitOf(limits, 'transform'))
    issues.push({ type: 'port', scope: '', message: 'workflow.studio.transformLimit' })
  nodes.forEach(node => {
    if (!node.ports?.length) issues.push({ type: 'port', scope: node.id, message: 'workflow.studio.unsupportedNode' })
    fieldIssues(node.configure, node.data, node.label || node.tid, label).forEach(message => issues.push({ type: 'configure', scope: node.id, message }))
    ;(node.ports || []).filter(port => port.required).forEach(port => {
      if (!connections.some(edge => (port.type === 'output' ? edge.source : edge.target) === `${node.id}-${port.id}`))
        issues.push({ type: 'port', scope: node.id, message: 'workflow.studio.disconnected' })
    })
  })
  const accepted: WorkflowConnection[] = []
  connections.forEach(edge => {
    if (!canConnect(nodes, accepted, edge.source, edge.target)) issues.push({ type: 'port', scope: edge.id, message: 'workflow.studio.invalidConnection' })
    else accepted.push(edge)
  })
  return issues
}
export function arrangeNodes(nodes: NodeInstance[], connections: WorkflowConnection[]): NodeInstance[] {
  const ordered: NodeInstance[] = [], visited = new Set<string>()
  const visit = (node: NodeInstance) => {
    if (visited.has(node.id)) return
    visited.add(node.id); ordered.push(node)
    connections.filter(edge => nodeForPort(edge.source, nodes)?.id === node.id).forEach(edge => {
      const next = nodeForPort(edge.target, nodes); if (next) visit(next)
    })
  }
  nodes.filter(node => node.category === 'source').forEach(visit); nodes.forEach(visit)
  return ordered.map((node, index) => ({ ...node, position: { x: 60 + index * 280, y: 180 } }))
}
