import { computed, inject, onScopeDispose, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { ObjectUtils } from '@/utils/object'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'

export interface TruncateTableInfo { name: string; database: string; engine: string; rows: number | null }

/**
 * Truncate table dialog: the table's facts and the statement (previewed by the server) load
 * together before the content shows; the run is allowed once the user ticks the acknowledgement
 * and types the table name.
 */
export function useTableTruncate(onDone: () => void) {
  const { t } = useI18n()
  const route = useRoute()
  const refreshKey = inject<Ref<number> | null>('metadataRefreshKey', null)

  const loading = ref(true)
  const submitting = ref(false)
  const info = ref<TruncateTableInfo | null>(null)
  const statement = ref('')
  const loadError = ref('')
  const acknowledged = ref(false)
  const typedName = ref('')
  let alive = true

  const location = () => [route.params.source, route.params.database, route.params.table].map(value => String(value || '')) as [string, string, string]
  const table = computed(() => location()[2])
  const nameMatches = computed(() => !!table.value && typedName.value === table.value)
  const canSubmit = computed(() => !loading.value && !submitting.value && !!statement.value && acknowledged.value && nameMatches.value)
  const failure = (response: any) => !response?.status || response.data?.isSuccessful === false
  const message = (response: any) => response?.data?.message || response?.message || t('common.loadingFailed')

  async function load() {
    const [source, database, name] = location()
    if (!source || !database || !name) { loading.value = false; return }
    loading.value = true
    const [detail, preview] = await Promise.all([
      MetadataService.getTable(source, database, name),
      MetadataService.truncateTable(source, database, name, { preview: true }),
    ])
    if (!alive) return
    const row = failure(detail) ? null : detail.data?.columns?.[0]
    const rows = Number(row?.object_rows)
    info.value = { name, database, engine: String(row?.object_engine || '-'), rows: row && Number.isFinite(rows) ? rows : null }
    if (failure(preview)) loadError.value = message(preview)
    else statement.value = String(preview.data?.content || '')
    loading.value = false
  }

  function copy() {
    if (!statement.value) return
    ObjectUtils.copy(statement.value)
    MfMessage.success(t('query.tip.copySuccess'))
  }

  async function submit() {
    if (!canSubmit.value) return
    const [source, database, name] = location()
    submitting.value = true
    try {
      const response = await MetadataService.truncateTable(source, database, name, { preview: false })
      if (!alive) return
      if (failure(response)) { MfMessage.error(message(response)); return }
      MfMessage.success(t('source.tip.truncateTableSuccess').replace('$VALUE', name))
      // Row counts and sizes shown by the workspace change with the data.
      if (refreshKey) refreshKey.value++
      onDone()
    } finally {
      if (alive) submitting.value = false
    }
  }

  load()
  onScopeDispose(() => { alive = false })

  return { loading, submitting, info, statement, loadError, acknowledged, typedName, table, nameMatches, canSubmit, copy, submit }
}
