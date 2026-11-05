import { ref } from 'vue'
import { defineStore } from 'pinia'
import MetadataService from '@/services/metadata'

export interface SourceDatabaseOption { label: string; value: string }
type Entry = { loading: boolean; loaded: boolean; options: SourceDatabaseOption[]; error: string }

/**
 * The databases of a source, shared by the workbench header (its selector) and the workspace
 * (its loading and "choose a database" states, and the first database it opens by default).
 */
export const useSourceDatabasesStore = defineStore('source-databases', () => {
  const entries = ref<Record<string, Entry>>({})
  const pending = new Map<string, Promise<void>>()

  function load(code: string, force = false): Promise<void> {
    if (!code) return Promise.resolve()
    if (pending.has(code)) return pending.get(code)!
    if (!force && entries.value[code]?.loaded) return Promise.resolve()
    entries.value[code] = { loading: true, loaded: false, options: entries.value[code]?.options ?? [], error: '' }
    const request = MetadataService.getDatabaseBySource(code).then(response => {
      if (!response.status) throw new Error(String(response.message || 'Unable to load databases'))
      entries.value[code].options = (response.data?.columns || [])
          .map((item: any) => ({ label: item.object_name || item.schema_name, value: item.object_name || item.schema_name }))
          .filter((item: SourceDatabaseOption) => !!item.value)
    }).catch(error => {
      entries.value[code].options = []
      entries.value[code].error = error instanceof Error ? error.message : String(error)
    }).finally(() => {
      entries.value[code].loading = false
      entries.value[code].loaded = true
      pending.delete(code)
    })
    pending.set(code, request)
    return request
  }

  const entryOf = (code: string): Entry => entries.value[code] ?? { loading: false, loaded: false, options: [], error: '' }
  return { entries, load, entryOf }
})
