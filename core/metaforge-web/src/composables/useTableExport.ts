import { computed, onScopeDispose, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import PluginService from '@/services/plugin'
import { VISIBLE_FORMATS, emptyExportForm, exportBody, exportFormats, previewTokens, readablePreview, type ExportFormat, type ExportForm } from '@/model/source-export'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'
import { usePluginLogos } from './usePluginLogos'

/** A directory chosen with the File System Access API (Chromium browsers). */
interface DirectoryHandle {
  name: string
  getFileHandle(name: string, options: { create: boolean }): Promise<{ createWritable(): Promise<{ write(data: Blob): Promise<void>; close(): Promise<void> }> }>
  requestPermission?(descriptor: { mode: 'readwrite' }): Promise<string>
}

/**
 * Export dialog: installed convert plugins as formats (logos from the store catalogue), a preview
 * of the first rows written by the chosen plugin, and the file saved to a chosen directory where
 * the browser allows it, or downloaded otherwise.
 */
export function useTableExport(onDone: () => void) {
  const { t } = useI18n()
  const route = useRoute()
  const { logos } = usePluginLogos()

  const loading = ref(true)
  const formats = ref<ExportFormat[]>([])
  const form = ref<ExportForm>(emptyExportForm())
  const total = ref<number | null>(null)
  const preview = ref('')
  const previewError = ref('')
  const previewLoading = ref(false)
  const exporting = ref(false)
  const directory = ref<DirectoryHandle | null>(null)
  const canPickDirectory = typeof window !== 'undefined' && 'showDirectoryPicker' in window
  let previewRevision = 0
  let previewTimer: ReturnType<typeof setTimeout> | undefined
  let alive = true

  const location = () => [route.params.source, route.params.database, route.params.table].map(value => String(value || '')) as [string, string, string]
  const table = computed(() => location()[2])
  const visibleFormats = computed(() => formats.value.slice(0, VISIBLE_FORMATS))
  const moreFormats = computed(() => formats.value.slice(VISIBLE_FORMATS))
  const selected = computed(() => formats.value.find(format => format.name === form.value.format) || null)
  const previewLines = computed(() => previewTokens(selected.value?.known || null, preview.value))
  const canExport = computed(() => !!selected.value && !exporting.value && (form.value.scope === 'all' || (form.value.start >= 1 && form.value.count >= 1)))

  async function load() {
    const response = await PluginService.getPlugins()
    if (!alive) return
    formats.value = response.status ? exportFormats(response.data || []) : []
    form.value.format = formats.value[0]?.name || ''
    loading.value = false
  }

  // The preview follows every choice that changes the file, a little after the last change.
  watch(form, () => {
    if (!form.value.format) return
    clearTimeout(previewTimer)
    previewTimer = setTimeout(loadPreview, 250)
  }, { deep: true })
  async function loadPreview() {
    const request = ++previewRevision
    const [source, database, name] = location()
    previewLoading.value = true
    const response = await MetadataService.previewExport(source, database, name, exportBody({ ...form.value, compress: false }))
    if (request !== previewRevision || !alive) return
    previewLoading.value = false
    if (!response.status) {
      preview.value = ''
      previewError.value = response.message || t('common.loadingFailed')
      return
    }
    previewError.value = ''
    preview.value = readablePreview(selected.value?.known || null, String(response.data?.content || ''))
    const count = Number(response.data?.total)
    total.value = Number.isFinite(count) && count >= 0 ? count : null
  }

  const step = (delta: number) => { form.value.count = Math.max(1, Math.floor(form.value.count) + delta) }
  async function pickDirectory() {
    try {
      directory.value = await (window as any).showDirectoryPicker({ mode: 'readwrite' })
    } catch { /* Cancelled. */ }
  }

  /** Writes into the chosen directory; returns false when the browser refuses, so the caller downloads instead. */
  async function saveToDirectory(blob: Blob, fileName: string) {
    const target = directory.value
    if (!target) return false
    try {
      if (target.requestPermission && await target.requestPermission({ mode: 'readwrite' }) !== 'granted') return false
      const writable = await (await target.getFileHandle(fileName, { create: true })).createWritable()
      await writable.write(blob)
      await writable.close()
      return true
    } catch {
      return false
    }
  }
  function download(blob: Blob, fileName: string) {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.click()
    URL.revokeObjectURL(url)
  }

  async function submit() {
    if (!canExport.value) return
    const [source, database, name] = location()
    exporting.value = true
    try {
      const response = await MetadataService.exportFile(source, database, name, exportBody(form.value))
      if (!alive) return
      if (!response.status || !response.blob) { MfMessage.error(response.message || t('common.loadingFailed')); return }
      const fileName = response.fileName || name
      const saved = await saveToDirectory(response.blob, fileName)
      if (!saved) download(response.blob, fileName)
      MfMessage.success(saved ? t('source.export.savedTo', { file: fileName, directory: directory.value?.name }) : t('source.export.downloaded', { file: fileName }))
      onDone()
    } finally {
      if (alive) exporting.value = false
    }
  }

  load()
  onScopeDispose(() => { alive = false; previewRevision++; clearTimeout(previewTimer) })

  return {
    loading, formats, visibleFormats, moreFormats, selected, logos, form, total, preview, previewLines, previewError, previewLoading, exporting,
    directory, canPickDirectory, canExport, table, step, pickDirectory, submit,
  }
}
