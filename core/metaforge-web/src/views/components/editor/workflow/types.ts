/**
 * Shared types for the local workflow editor, mirroring the executor
 * node YAML schema and the persisted workflow configure shape.
 */

export interface ConfigureItem
{
  field: string
  label: string
  type: 'input' | 'number' | 'switch' | 'textarea' | 'password' | 'select' | 'array' | 'map' | 'source'
  value?: any
  required?: boolean
  placeholder?: string
  description?: string
  message?: string
  options?: Array<{ label: string; value: any }>
  disabled?: boolean
  min?: number
  max?: number
  step?: number
  /** Editor tab: basic, sql or advanced. Derived from the field when the declaration omits it. */
  group?: 'basic' | 'sql' | 'advanced'
  validated?: { valid: boolean; message?: string }
}

export interface PortItem
{
  id: string
  label: string
  type: 'input' | 'output'
  required?: boolean
}

export interface NodeDefinition
{
  id: string
  label: string
  key?: string
  tid?: string
  description?: string
  category?: string
  configure?: ConfigureItem[]
  ports?: PortItem[]
}

export interface NodeInstance
{
  id: string
  tid: string
  key?: string
  category?: string
  position: { x: number; y: number }
  data?: Record<string, any>
  configure?: ConfigureItem[]
  ports?: PortItem[]
  label?: string
  description?: string
}

export interface WorkflowConnection
{
  id: string
  source: string
  target: string
}

export interface ValidationEntry
{
  type: 'configure' | 'port'
  scope: string
  message: string
}

export interface WorkflowEditorModel
{
  nodes: NodeInstance[]
  connections: WorkflowConnection[]
  data?: {
    nodes: Array<{ id: string; tid: string; category?: string; position: { x: number; y: number }; key?: string; data: Record<string, any> }>
    connections: WorkflowConnection[]
  }
  validation?: ValidationEntry[]
}
