import { computed, onScopeDispose, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { ObjectUtils } from '@/utils/object'
import { sourceManageLocation } from '@/model/source-manage'
import { groupDependencies, mapDropTableInfo, type DependencyKind, type DropTableInfo, type TableDependency } from '@/model/source-table-drop'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'

/**
 * Drop table dialog: the table's facts, the objects depending on it and the statement (previewed
 * by the server) load together before the content shows. The drop runs once the user ticks the
 * acknowledgement and types the table name, then the workspace moves to the database.
 */
export function useTableDrop(onDone: () => void) {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()

  const loading = ref(true)
  const submitting = ref(false)
  const info = ref<DropTableInfo | null>(null)
  const dependencies = ref<Record<DependencyKind, TableDependency[]> | null>(null)
  const dependencyError = ref('')
  const statement = ref('')
  const loadError = ref('')
  const expanded = ref<DependencyKind | null>(null)
  const acknowledged = ref(false)
  const typedName = ref('')
  let alive = true

  const location = () => [route.params.source, route.params.database, route.params.table].map(value => String(value || '')) as [string, string, string]
  const table = computed(() => location()[2])
  const nameMatches = computed(() => !!table.value && typedName.value === table.value)
  const canSubmit = computed(() => !loading.value && !submitting.value && !!statement.value && acknowledged.value && nameMatches.value)
  const count = (kind: DependencyKind) => dependencies.value ? dependencies.value[kind].length : null
  const failure = (response: any) => !response?.status || response.data?.isSuccessful === false
  const message = (response: any) => response?.data?.message || response?.message || t('common.loadingFailed')

  async function load() {
    const [source, database, name] = location()
    if (!source || !database || !name) { loading.value = false; return }
    loading.value = true
    const [detail, related, preview] = await Promise.all([
      MetadataService.getTable(source, database, name),
      MetadataService.getTableDependencies(source, database, name),
      MetadataService.dropTable(source, database, name, { preview: true }),
    ])
    if (!alive) return
    info.value = mapDropTableInfo(failure(detail) ? null : detail.data?.columns?.[0], name, database)
    if (failure(related)) dependencyError.value = message(related)
    else {
      dependencies.value = groupDependencies(related.data?.columns || [])
      // Some kinds could not be checked (no privilege on the schema tables, for example).
      dependencyError.value = related.data?.message || ''
    }
    if (failure(preview)) loadError.value = message(preview)
    else statement.value = String(preview.data?.content || '')
    loading.value = false
  }

  const toggle = (kind: DependencyKind) => { expanded.value = expanded.value === kind ? null : kind }
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
      const response = await MetadataService.dropTable(source, database, name, { preview: false })
      if (!alive) return
      if (failure(response)) { MfMessage.error(message(response)); return }
      MfMessage.success(t('source.tip.dropTableSuccess').replace('$VALUE', name))
      onDone()
      // The table is gone, so the workspace shows its database instead.
      router.replace(sourceManageLocation({ source, database }))
    } finally {
      if (alive) submitting.value = false
    }
  }

  load()
  onScopeDispose(() => { alive = false })

  return {
    loading, submitting, info, dependencies, dependencyError, statement, loadError, expanded, acknowledged, typedName, table, nameMatches, canSubmit,
    count, toggle, copy, submit,
  }
}
