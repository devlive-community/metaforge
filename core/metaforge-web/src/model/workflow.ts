import type { BaseModel } from '@/model/base'
import type { NodeDefinition, NodeInstance, WorkflowConnection } from '@/views/components/editor/workflow/types'

export interface WorkflowGraph {
  nodes: NodeInstance[]
  connections: WorkflowConnection[]
  description?: string
  timeout?: number
  publishedAt?: number
  revision?: number
}
export interface WorkflowModel extends BaseModel {
  state?: string
  message?: string
  work?: string
  elapsed?: number
  executor?: string
  configure?: WorkflowGraph
}
export interface WorkflowExecutor {
  name: string
  nodes: NodeDefinition[]
  categories: string[]
  maxTransforms: number
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
