import { computed, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSourceManageRoute } from './useSourceManageRoute'
import MetadataService from '@/services/metadata'
import { nativePage, type NativeEntry } from '@/model/source-native'
import { sourceManageLocation } from '@/model/source-manage'
import type { SourceCapabilities } from '@/stores/source-capabilities'

export function useNativeNamespaces(source: Ref<string>, profile: Ref<SourceCapabilities | null>, can: (name: string) => boolean, refresh: Ref<number>) {
  const route = useSourceManageRoute()
  const router = useRouter()
  const entries = ref<NativeEntry[]>([])
  const filter = ref('')
  const loading = ref(false)
  const error = ref('')
  const nextCursor = ref<string | null>(null)
  const hasMore = ref(false)
  const selected = computed(() => String(route.params.database || ''))
  let revision = 0
  let alive = true
  function select(value: string, replace = false) {
    const tab = can('tab:' + String(route.meta.type)) ? String(route.meta.type) : profile.value?.kind === 'storage' ? 'objects' : 'keys'
    const location = sourceManageLocation({ source: source.value, database: value, tab }, replace ? route.query : {})
    return replace ? router.replace(location) : router.push(location)
  }
  async function load(append = false) {
    const request = ++revision
    if (!source.value || !can('namespace:list')) { entries.value = []; loading.value = false; return }
    const code = source.value
    const cursor = append ? nextCursor.value || '' : ''
    if (!append) entries.value = []
    loading.value = true; error.value = ''
    try {
      const page = nativePage(await MetadataService.nativeNamespaces(code, { prefix: filter.value, cursor, limit: 100 }))
      if (!alive || request !== revision || code !== source.value) return
      entries.value = append ? [...new Map([...entries.value, ...page.entries].map(entry => [entry.id, entry])).values()] : page.entries
      hasMore.value = page.hasMore; nextCursor.value = page.nextCursor || null
      if (!selected.value && entries.value[0]) await select(entries.value[0].id, true)
    } catch (failure) { if (alive && request === revision) { error.value = failure instanceof Error ? failure.message : String(failure); hasMore.value = false } }
    finally { if (alive && request === revision) loading.value = false }
  }
  watch(() => [source.value, can('namespace:list')], () => { filter.value = ''; load() }, { immediate: true })
  watch(refresh, () => load())
  onScopeDispose(() => { alive = false; revision++ })
  return { entries, filter, loading, error, hasMore, selected, select, load }
}
