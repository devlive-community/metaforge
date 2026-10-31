import { computed, inject, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { nativeDetail, nativePage, parentObjectPrefix, safeDownloadUrl, type NativeDetail, type NativeEntry } from '@/model/source-native'
import { useSourceManageRoute } from './useSourceManageRoute'
import type { useSourceCapabilities } from './useSourceCapabilities'

export function useNativeBrowser() {
  const route = useSourceManageRoute()
  const router = useRouter()
  const { t, locale } = useI18n()
  const { profile, can } = inject<ReturnType<typeof useSourceCapabilities>>('metadataCapabilityState')!
  const refresh = inject<Ref<number>>('metadataRefreshKey', ref(0))
  const panelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const panelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const panelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(360))
  const storage = computed(() => profile.value?.kind === 'storage')
  const namespace = computed(() => String(route.params.database || ''))
  const prefix = computed(() => typeof route.query.prefix === 'string' ? route.query.prefix : storage.value ? '' : '*')
  const selectedKey = computed(() => typeof route.query.key === 'string' ? route.query.key : null)
  const keyword = ref(prefix.value)
  const limit = ref(50)
  const entries = ref<NativeEntry[]>([])
  const loading = ref(false)
  const error = ref('')
  const cursor = ref('')
  const previousCursors = ref<string[]>([])
  const nextCursor = ref<string | null>(null)
  const hasMore = ref(false)
  const page = computed(() => previousCursors.value.length + 1)
  const detail = ref<NativeDetail | null>(null)
  const detailLoading = ref(false)
  const detailError = ref('')
  const detailCursor = ref('')
  const previousDetailCursors = ref<string[]>([])
  const downloading = ref(false)
  let listRevision = 0, detailRevision = 0, downloadRevision = 0
  let alive = true
  const identity = () => [route.params.source, namespace.value, prefix.value].join('\u0000')
  const listCapability = () => storage.value ? 'object:list' : 'key:list'
  const detailCapability = () => storage.value ? 'object:read' : 'key:read'
  const issue = (failure: unknown) => failure instanceof Error ? failure.message : String(failure)
  function updateQuery(changes: Record<string, string | undefined>, replace = false) {
    const query = { ...route.query, ...changes }
    return replace ? router.replace({ path: '/manage', query }) : router.push({ path: '/manage', query })
  }
  function search() { updateQuery({ prefix: keyword.value, key: undefined }) }
  function open(entry: NativeEntry) {
    if (entry.type === 'folder') updateQuery({ prefix: entry.id, key: undefined })
    else if (can(detailCapability())) updateQuery({ key: entry.id })
  }
  function up() { updateQuery({ prefix: parentObjectPrefix(prefix.value), key: undefined }) }
  function closeDetail() { updateQuery({ key: undefined }, true) }
  async function loadList() {
    const request = ++listRevision, key = identity()
    if (!namespace.value || !can(listCapability())) { entries.value = []; loading.value = false; return }
    loading.value = true; error.value = ''
    try {
      const result = nativePage(await MetadataService.nativeBrowse(String(route.params.source), { namespace: namespace.value, prefix: prefix.value, cursor: cursor.value, limit: limit.value }))
      if (!alive || request !== listRevision || key !== identity()) return
      entries.value = result.entries; nextCursor.value = result.nextCursor || null; hasMore.value = result.hasMore
    } catch (failure) { if (alive && request === listRevision) { entries.value = []; hasMore.value = false; error.value = issue(failure) } }
    finally { if (alive && request === listRevision) loading.value = false }
  }
  function resetList() { cursor.value = ''; previousCursors.value = []; hasMore.value = false; entries.value = []; loadList() }
  function next() { if (!loading.value && hasMore.value && nextCursor.value) { previousCursors.value.push(cursor.value); cursor.value = nextCursor.value; loadList() } }
  function previous() { if (!loading.value && previousCursors.value.length) { cursor.value = previousCursors.value.pop()!; loadList() } }
  function setLimit(value: unknown) { const size = Number(value); if ([50, 100, 200].includes(size)) { limit.value = size; resetList() } }
  async function loadDetail(preview = false) {
    const request = ++detailRevision, key = identity(), id = selectedKey.value
    if (id === null || !namespace.value || !can(detailCapability())) { detail.value = null; detailLoading.value = false; panelOpen.value = false; return }
    panelOpen.value = true; detailLoading.value = true; detailError.value = ''; detail.value = null
    try {
      const result = nativeDetail(await MetadataService.nativeDetail(String(route.params.source), { namespace: namespace.value, key: id, cursor: detailCursor.value, limit: 50, preview }))
      if (!alive || request !== detailRevision || key !== identity() || id !== selectedKey.value) return
      detail.value = result
    } catch (failure) { if (alive && request === detailRevision) detailError.value = issue(failure) }
    finally { if (alive && request === detailRevision) detailLoading.value = false }
  }
  function nextDetail() {
    if (!detailLoading.value && detail.value?.hasMore && detail.value.nextCursor) {
      previousDetailCursors.value.push(detailCursor.value); detailCursor.value = detail.value.nextCursor; loadDetail()
    }
  }
  function previousDetail() { if (!detailLoading.value && previousDetailCursors.value.length) { detailCursor.value = previousDetailCursors.value.pop()!; loadDetail() } }
  async function download() {
    if (downloading.value || selectedKey.value === null || !can('object:download')) return
    const request = ++downloadRevision, key = identity(), id = selectedKey.value
    downloading.value = true
    try {
      const result = await MetadataService.nativeDownload(String(route.params.source), { namespace: namespace.value, key: id })
      if (!alive || request !== downloadRevision || key !== identity() || id !== selectedKey.value) return
      if (!result.status) throw new Error(String(result.message))
      const href = safeDownloadUrl(result.data?.url)
      const anchor = document.createElement('a')
      anchor.href = href; anchor.download = String(result.data?.fileName || 'download'); anchor.rel = 'noopener noreferrer'
      document.body.append(anchor); anchor.click(); anchor.remove()
    } catch (failure) { if (alive && request === downloadRevision) MfMessage.error(issue(failure)) }
    finally { if (alive && request === downloadRevision) downloading.value = false }
  }
  const formatSize = (value: unknown) => {
    const size = Number(value)
    if (size < 0 || !Number.isFinite(size)) return '-'
    if (size < 1024) return size + ' B'
    if (size < 1048576) return (size / 1024).toFixed(1) + ' KiB'
    return (size / 1048576).toFixed(1) + ' MiB'
  }
  const formatTime = (value: unknown) => value == null ? '-' : new Intl.DateTimeFormat(locale.value, { dateStyle: 'short', timeStyle: 'medium' }).format(Number(value))
  const formatTtl = (value: unknown) => value === -1 ? t('source.native.persistent') : value === -2 ? t('source.native.expired') : value == null ? '-' : Math.max(0, Math.ceil(Number(value) / 1000)) + ' s'
  const headers = computed(() => storage.value ? [
    { title: t('common.name'), dataIndex: 'name', width: 300, ellipsis: true },
    { title: t('source.native.size'), dataIndex: 'size', width: 110 },
    { title: t('common.updateTime'), dataIndex: 'modifiedAt', width: 170 },
  ] : [
    { title: t('source.native.key'), dataIndex: 'name', width: 300, ellipsis: true },
    { title: t('source.structure.type'), dataIndex: 'type', width: 100 },
    { title: 'TTL', dataIndex: 'ttl', width: 130 },
  ])
  const detailAttributes = computed(() => Object.entries(detail.value?.entry.attributes || {})
    .filter(([name]) => name !== 'previewSupported')
    .map(([name, value]) => ({ name, value, label: name === 'contentType' ? t('source.native.contentType') : name === 'etag' ? 'ETag' : name === 'storageClass' ? t('source.native.storageClass') : name })))
  const detailRows = computed(() => (detail.value?.rows || []).map((row, index) => ({ ...row, rowId: String(index) })))
  const detailHeaders = computed(() => [
    { title: detail.value?.entry.type === 'hash' ? t('source.native.hashField') : detail.value?.entry.type === 'zset' ? t('source.native.score') : detail.value?.entry.type === 'stream' ? t('source.native.streamId') : '#', dataIndex: 'position', width: 100, ellipsis: true },
    { title: t('source.native.value'), dataIndex: 'value', width: 260, ellipsis: true },
  ])
  watch(() => [String(route.params.source), namespace.value, prefix.value], () => { keyword.value = prefix.value; resetList() }, { immediate: true })
  watch(() => [String(route.params.source), namespace.value, selectedKey.value], () => {
    detailCursor.value = ''; previousDetailCursors.value = []; downloadRevision++; downloading.value = false; loadDetail()
  }, { immediate: true })
  watch(refresh, () => { resetList(); loadDetail(detail.value?.content != null) })
  onScopeDispose(() => { alive = false; listRevision++; detailRevision++; downloadRevision++; panelOpen.value = false })
  return { can, storage, namespace, prefix, keyword, limit, entries, loading, error, hasMore, page, previousCursors,
    detail, detailLoading, detailError, detailRows, detailHeaders, detailAttributes, detailCursor, previousDetailCursors, downloading,
    panelOpen, panelHost, panelWidth, headers, formatSize, formatTime, formatTtl, search, open, up, closeDetail,
    loadDetail, download, next, previous, nextDetail, previousDetail, setLimit, resetList }
}
