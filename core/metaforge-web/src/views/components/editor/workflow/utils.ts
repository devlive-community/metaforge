import type { NodeDefinition, NodeInstance, ValidationEntry, WorkflowConnection } from './types'

export const nodeForPort = (port: string, nodes: NodeInstance[]) => nodes.find(node =>
  (node.ports || []).some(item => port === `${node.id}-${item.id}`))
export const mergeDefinitions = (definitions: NodeDefinition[], instances: NodeInstance[]): NodeInstance[] => instances.map((original, index) => {
  const instance = { ...original, position: { x: Number.isFinite(original.position?.x) ? original.position.x : 60 + index * 280, y: Number.isFinite(original.position?.y) ? original.position.y : 180 } }
  const definition = definitions.find(node => node.id === instance.tid)
  if (!definition) return { ...instance, configure: [], ports: [] }
  const data = { ...Object.fromEntries((definition.configure || []).filter(field => field.value !== undefined).map(field => [field.field, field.value])), ...instance.data }
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
export function validateWorkflow(nodes: NodeInstance[], connections: WorkflowConnection[], maxTransforms = 1): ValidationEntry[] {
  const issues: ValidationEntry[] = []
  if (nodes.filter(node => node.category === 'source').length !== 1 || nodes.filter(node => node.category === 'sink').length !== 1)
    issues.push({ type: 'port', scope: '', message: 'workflow.studio.needEndpoints' })
  if (nodes.filter(node => node.category === 'transform').length > maxTransforms)
    issues.push({ type: 'port', scope: '', message: 'workflow.studio.transformLimit' })
  nodes.forEach(node => {
    if (!node.ports?.length) issues.push({ type: 'port', scope: node.id, message: 'workflow.studio.unsupportedNode' })
    ;(node.configure || []).forEach(field => {
      const value = node.data?.[field.field]
      if (field.type === 'map' && value !== undefined && (typeof value !== 'object' || value === null || Array.isArray(value)))
        issues.push({ type: 'configure', scope: node.id, message: 'workflow.studio.invalidMap' })
      if (field.required && (value === undefined || value === null || String(value).trim() === '' ||
        Array.isArray(value) && !value.length || typeof value === 'object' && !Object.keys(value).length))
        issues.push({ type: 'configure', scope: node.id, message: `${node.label}: ${field.label}` })
    })
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
