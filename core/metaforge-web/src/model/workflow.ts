import type { BaseModel } from '@/model/base'
import type { ConfigureItem, NodeDefinition, NodeInstance, WorkflowConnection } from '@/views/components/editor/workflow/types'

export interface WorkflowGraph {
  nodes: NodeInstance[]
  connections: WorkflowConnection[]
  description?: string
  timeout?: number
  publishedAt?: number
  revision?: number
  /** Job settings declared by the executor (batch size, parallelism...). */
  settings?: Record<string, unknown>
}
export interface WorkflowModel extends BaseModel {
  state?: string
  message?: string
  work?: string
  elapsed?: number
  executor?: string
  configure?: WorkflowGraph
}
export type WorkflowCategory = 'source' | 'transform' | 'sink'
/** How a running workflow is stopped: by the executor itself, by interrupting the worker, or not at all. */
export type WorkflowStopMode = 'cooperative' | 'interrupt' | 'none'
export interface WorkflowCapabilities {
  stop: WorkflowStopMode
  rowCount: boolean
  metrics: boolean
  preview: boolean
}
/** An executor as declared by its plugin: the editor follows this declaration, and so does the server. */
export interface WorkflowExecutor {
  name: string
  description?: string
  topology: 'pipeline'
  capabilities: WorkflowCapabilities
  /** Maximum nodes per category, keyed by `sources`, `transforms` and `sinks`. */
  limits: Partial<Record<'sources' | 'transforms' | 'sinks', number>>
  settings: ConfigureItem[]
  nodes: NodeDefinition[]
  categories: WorkflowCategory[]
}
/** Maximum number of nodes of one category; inputs and outputs default to one, transforms to none. */
export const categoryLimit = (executor: Pick<WorkflowExecutor, 'limits'> | undefined, category?: string): number => {
  const value = executor?.limits?.[`${category}s` as keyof WorkflowExecutor['limits']]
  return typeof value === 'number' ? Math.max(0, value) : category === 'transform' ? 0 : 1
}
/** Display the supported palette types in pipeline order, independent of declaration order. */
export function workflowCategories(executor: Pick<WorkflowExecutor, 'categories' | 'nodes' | 'limits'>): WorkflowCategory[] {
  const order: WorkflowCategory[] = ['source', 'transform', 'sink']
  return order.filter(category => executor.categories.includes(category)
    && categoryLimit(executor, category) > 0
    && executor.nodes.some(node => node.category === category))
}
export interface WorkflowRun {
  id: string
  code: string
  name: string
  executor: string
  state: string
  startedAt: number
  finishedAt?: number
  elapsed: number
  message?: string
  count?: number
  preview?: Record<string, unknown>[]
  nodes: { id: string; label: string; category: string }[]
  metrics?: {
    rowsRead: number; rowsWritten: number; rowsFailed: number; rowsSkipped: number
    progress: number; elapsedMs: number; avgWriteRowsPerSecond: number
    stages: { name: string; state: string; elapsedMs: number }[]
  }
}
export const workflowStateVariant = (state?: string): 'success' | 'danger' | 'primary' | 'default' =>
  state === 'SUCCESS' ? 'success' : ['FAILURE', 'TIMEOUT'].includes(state || '') ? 'danger' : state === 'RUNNING' ? 'primary' : 'default'
