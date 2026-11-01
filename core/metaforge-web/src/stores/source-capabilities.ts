import { ref } from 'vue'
import { defineStore } from 'pinia'
import MetadataService from '@/services/metadata'
export interface SourceCapabilities { kind: string; capabilities: string[] }
type Entry = { profile: SourceCapabilities | null; loading: boolean; error: string; contractError?: boolean }
const KINDS = ['relational', 'key-value', 'document', 'storage', 'graph']
const valid = (data: any) => !!data && typeof data.kind === 'string' && Array.isArray(data.capabilities)
const profileOf = (data: any): SourceCapabilities => ({
  kind: KINDS.includes(data.kind) ? data.kind : 'unknown',
  capabilities: [...new Set<string>(data.capabilities.filter((value: unknown) => typeof value === 'string' && /^[a-z-]+:[a-z-]+$/.test(value)))]
})
export const useSourceCapabilitiesStore = defineStore('source-capabilities', () => {
  const entries = ref<Record<string, Entry>>({})
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
      if (!valid(response.data)) {
        entries.value[code].contractError = true
        throw new Error('Invalid source management capabilities response')
      }
      entries.value[code].profile = profileOf(response.data)
    }).catch(error => { entries.value[code].error = error instanceof Error ? error.message : String(error) })
      .finally(() => { entries.value[code].loading = false; pending.delete(code) })
    pending.set(code, request)
    return request
  }
  /** Loads the sources not known yet in one request, as a source list needs. */
  async function loadMany(codes: string[]): Promise<void> {
    const missing = [...new Set(codes.filter(code => code && !entries.value[code]?.profile && !pending.has(code)))]
    if (!missing.length) return
    missing.forEach(code => { entries.value[code] = { profile: null, loading: true, error: '' } })
    const response = await MetadataService.getCapabilitiesOf(missing)
    missing.forEach(code => {
      const data = response.status ? response.data?.[code] : null
      entries.value[code] = valid(data)
          ? { profile: profileOf(data), loading: false, error: '' }
          : { profile: null, loading: false, error: String(response.message || 'Unable to load source capabilities'), contractError: response.code === 0 }
    })
  }
  /** The declared capabilities of a loaded source; empty while unknown. */
  const capabilitiesOf = (code: string) => entries.value[code]?.profile?.capabilities || []
  return { entries, load, loadMany, capabilitiesOf }
})
