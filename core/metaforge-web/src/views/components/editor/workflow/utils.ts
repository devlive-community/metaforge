import { ConfigureItem, NodeDefinition, NodeInstance, ValidationEntry, WorkflowConnection } from './types'

/**
 * Merges node definitions into node instances. Instances persisted in a saved
 * workflow only carry { id, tid, position, data } — definition fields
 * (label, ports, configure) are restored from the matching definition.
 */
export const mergeDefinitions = (definitions: NodeDefinition[], instances: NodeInstance[]): NodeInstance[] => {
  const definitionMap = new Map<string, NodeDefinition>()
  definitions.forEach((definition) => definitionMap.set(definition.id, definition))

  return instances.map((instance) => {
    const definition = definitionMap.get(instance.tid)
    if (!definition) {
      return instance
    }

    const configure: ConfigureItem[] = (definition.configure || []).map((item) => ({
      ...item,
      value: instance.data && instance.data[item.field] !== undefined ? instance.data[item.field] : item.value
    }))

    return {
      ...instance,
      label: definition.label,
      key: definition.key,
      description: definition.description,
      category: instance.category || definition.category,
      ports: definition.ports || [],
      configure
    }
  })
}

/**
 * Flattens node configure values into the persisted data shape
 * { nodes: [{ id, tid, category, position, key, data }], connections }.
 */
export const flattenNodes = (nodes: NodeInstance[]): Array<{ id: string; tid: string; category?: string; position: { x: number; y: number }; key?: string; data: Record<string, any> }> => {
  return nodes.map((node) => {
    const data: Record<string, any> = {}
    if (node.data) {
      Object.assign(data, node.data)
    }
    (node.configure || []).forEach((item) => {
      data[item.field] = item.value
    })
    return {
      id: node.id,
      tid: node.tid,
      category: node.category,
      position: node.position,
      key: node.key,
      data
    }
  })
}

/**
 * Validates required configure fields and required ports against the
 * current connections, producing the workflow validation list.
 */
export const validateWorkflow = (nodes: NodeInstance[], connections: WorkflowConnection[]): ValidationEntry[] => {
  const validation: ValidationEntry[] = []

  nodes.forEach((node) => {
    (node.configure || []).forEach((item) => {
      if (!item.required) {
        return
      }
      const empty = item.value === undefined || item.value === null || item.value === ''
          || (Array.isArray(item.value) && item.value.length === 0)
      if (empty) {
        validation.push({
          type: 'configure',
          scope: item.field,
          message: item.message || `${ item.label } is required`
        })
      }
    });

    (node.ports || []).forEach((port) => {
      if (!port.required) {
        return
      }
      const portId = `${ node.id }-${ port.id }`
      const connected = connections.some((connection) => connection.source === portId || connection.target === portId)
      if (!connected) {
        validation.push({
          type: 'port',
          scope: port.id,
          message: `${ port.label || port.id } is required`
        })
      }
    })
  })

  return validation
}
