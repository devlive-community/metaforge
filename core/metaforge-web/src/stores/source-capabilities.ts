import { ref } from 'vue'
import { defineStore } from 'pinia'
import MetadataService from '@/services/metadata'
export interface SourceCapabilities { kind: string; capabilities: string[] }
export const useSourceCapabilitiesStore = defineStore('source-capabilities', () => {
  const entries = ref<Record<string, { profile: SourceCapabilities | null; loading: boolean; error: string; contractError?: boolean }>>({})
  const pending = new Map<string, Promise<void>>()
  function load(code: string, force = false): Promise<void> {
    if (!code) return Promise.resolve()
    if (pending.has(code)) return pending.get(code)!
    if (!force && entries.value[code]?.profile) return Promise.resolve()
    entries.value[code] = { profile: null, loading: true, error: '' }
    const request = MetadataService.getCapabilities(code).then(response => {
      if (!response.status) {
        entries.value[code].contractError = response.code === 0
        throw new Error(String(response.message || 'Unable to load source capabilities'))
      }
      if (!response.data || typeof response.data.kind !== 'string' || !Array.isArray(response.data.capabilities)) {
        entries.value[code].contractError = true
        throw new Error('Invalid source management capabilities response')
      }
      entries.value[code].profile = { kind: ['relational', 'key-value', 'document', 'storage', 'graph'].includes(response.data.kind) ? response.data.kind : 'unknown', capabilities: [...new Set<string>(response.data.capabilities.filter((value: unknown) => typeof value === 'string' && /^[a-z-]+:[a-z-]+$/.test(value)))] }
    }).catch(error => { entries.value[code].error = error instanceof Error ? error.message : String(error) })
      .finally(() => { entries.value[code].loading = false; pending.delete(code) })
    pending.set(code, request)
    return request
  }
  return { entries, load }
})
