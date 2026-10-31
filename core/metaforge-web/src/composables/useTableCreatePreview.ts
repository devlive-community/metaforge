import { computed, onScopeDispose, ref, shallowRef, watch, type Ref } from 'vue'
import MetadataService from '@/services/metadata'

export const TABLE_PREVIEW_KINDS = ['full', 'columns', 'indexes', 'options'] as const
export type TablePreviewKind = typeof TABLE_PREVIEW_KINDS[number]
type Previews = Record<TablePreviewKind, string>
interface PreviewDefinition { name?: string; [key: string]: unknown }
interface PreviewContext { definition: string; kind: TablePreviewKind }

function isPreviews(value: unknown): value is Previews {
  return !!value && typeof value === 'object' && !Array.isArray(value)
      && TABLE_PREVIEW_KINDS.every(kind => typeof (value as Record<string, unknown>)[kind] === 'string')
}

/** Fetch every SQL section once per effective table definition; tabs and navigation options stay local. */
export function useTableCreatePreview(options: {
  active: () => boolean
  source: () => string
  database: () => string
  definition: () => PreviewDefinition
  kind: Ref<TablePreviewKind>
  failureMessage: () => string
}) {
  const request = computed(() => ({ source: options.source(), database: options.database(), definition: options.definition() }))
  const signature = computed(() => JSON.stringify(request.value))
  const cache = shallowRef<{ key: string; values: Previews } | null>(null)
  const pendingKey = ref<string | null>(null)
  const sqlError = ref('')
  const sqlLoading = computed(() => options.active() && pendingKey.value === signature.value)
  const sqlContent = computed(() => cache.value?.key === signature.value ? cache.value.values[options.kind.value] : '')
  const previewContext = computed<PreviewContext>(() => ({ definition: signature.value, kind: options.kind.value }))
  let revision = 0
  let disposed = false
  let inFlight: { key: string; promise: Promise<void> } | null = null

  function load(): Promise<void> {
    if (disposed || !options.active() || !request.value.source || !request.value.definition.name?.trim()) return Promise.resolve()
    const key = signature.value
    if (cache.value?.key === key) return Promise.resolve()
    if (inFlight?.key === key) return inFlight.promise
    const current = ++revision
    const payload = JSON.parse(key) as { source: string; database: string; definition: PreviewDefinition }
    pendingKey.value = key
    sqlError.value = ''
    const promise = Promise.resolve()
        .then(() => MetadataService.previewCreateTableBatch(payload.source, { ...payload.definition, database: payload.database }))
        .then(response => {
          if (!response.status) throw new Error(response.message || options.failureMessage())
          const values: unknown = response.data
          if (!isPreviews(values)) throw new Error(options.failureMessage())
          if (!disposed && current === revision && signature.value === key) cache.value = { key, values: { ...values } }
        })
        .catch(error => {
          if (!disposed && current === revision && signature.value === key) {
            sqlError.value = error instanceof Error ? error.message : options.failureMessage()
          }
        })
        .finally(() => {
          if (inFlight?.promise === promise) inFlight = null
          if (!disposed && current === revision) pendingKey.value = null
        })
    inFlight = { key, promise }
    return promise
  }

  watch([options.active, signature], ([active, key], previous) => {
    if (!previous || previous[1] !== key) {
      revision++
      cache.value = null
      inFlight = null
      pendingKey.value = null
      sqlError.value = ''
    }
    if (active) void load()
  }, { immediate: true })

  function updateSqlContent(content: string, context: PreviewContext) {
    if (disposed || cache.value?.key !== context.definition || signature.value !== context.definition) return
    cache.value = { key: context.definition, values: { ...cache.value.values, [context.kind]: content } }
  }

  onScopeDispose(() => {
    disposed = true
    revision++
    inFlight = null
    pendingKey.value = null
  })

  return { sqlContent, sqlLoading, sqlError, previewContext, updateSqlContent, retryPreview: load }
}
