import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import PluginService from '@/services/plugin'
import { emptyExportForm, exportBody, exportFormats } from '@/model/source-export'
import { useSourceCascade } from './useSourceCascade'

/**
 * Data export tool page: pick a source, database and table, an installed format and optionally a
 * row count, and download the file from the same export the table dialog uses. A source and
 * database passed in the address (from the database overview) are selected on arrival.
 */
export function useExportTool() {
  const { t } = useI18n()
  const route = useRoute()
  const { sourceCode, databaseName, tableName, sourceOptions, databaseOptions, tableOptions } = useSourceCascade()
  const format = ref('')
  const formatOptions = ref<{ label: string; value: string }[]>([])
  // Empty exports every row; a number exports that many from the first row.
  const count = ref('')
  const exporting = ref(false)
  const canExport = computed(() => !!(sourceCode.value && databaseName.value && tableName.value && format.value) && !exporting.value)

  async function onExport() {
    if (!canExport.value) return
    const limit = Number(count.value)
    const body = exportBody({ ...emptyExportForm(), format: format.value, includeStructure: false, scope: limit >= 1 ? 'range' : 'all', count: limit >= 1 ? limit : 1 })
    exporting.value = true
    try {
      const response = await MetadataService.exportFile(sourceCode.value, databaseName.value, tableName.value, body)
      if (!response.status || !response.blob) { MfMessage.error(response.message || t('common.loadingFailed')); return }
      const url = URL.createObjectURL(response.blob)
      const link = document.createElement('a')
      link.href = url
      link.download = response.fileName || tableName.value
      link.click()
      URL.revokeObjectURL(url)
      MfMessage.success(t('source.export.downloaded', { file: link.download }))
    } finally {
      exporting.value = false
    }
  }

  onMounted(async () => {
    const response = await PluginService.getPlugins()
    if (response.status) {
      formatOptions.value = exportFormats(response.data || []).map(item => ({ label: item.label, value: item.name }))
      format.value = formatOptions.value[0]?.value || ''
    }
    const source = typeof route.query.source === 'string' ? route.query.source : ''
    const database = typeof route.query.database === 'string' ? route.query.database : ''
    if (source) {
      sourceCode.value = source
      // Changing the source clears the database first; pick it once that has run.
      await nextTick()
      if (database) databaseName.value = database
    }
  })

  return { sourceCode, databaseName, tableName, sourceOptions, databaseOptions, tableOptions, format, formatOptions, count, exporting, canExport, onExport }
}
