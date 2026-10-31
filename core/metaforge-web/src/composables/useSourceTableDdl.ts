import { computed, inject, nextTick, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import FormatService from '@/services/format'
import { ObjectUtils } from '@/utils/object'
import { DDL_CARDS, DEFAULT_DDL_CARDS, ddlFileName, joinDdl, mapDdlHistory,
  type DdlAction, type DdlHistoryEntry, type DdlKind } from '@/model/source-table-ddl'
import { emptyTableFilters } from '@/model/filter'
import { compareLocation } from '@/model/source-manage'
import { useWorkspaceLoading } from './useWorkspaceLoading'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'
import { usePagedList } from './usePagedList'
import { usePanelState } from './usePanelState'

type CardAction = Exclude<DdlAction, 'compare'>
export interface DdlBlock { kind: DdlKind; sql: string; loading: boolean; error: string }
export interface DdlCard { action: CardAction; blocks: DdlBlock[]; formatting: boolean }

/**
 * DDL tab: statement cards built by the server, and a quick menu docked in the workspace's
 * right panel (like the trigger and table properties editors) that generates more of them.
 * Statements generated from the menu are recorded in the table's DDL history.
 */
export function useSourceTableDdl() {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const refreshKey = inject<Ref<number> | null>('metadataRefreshKey', null)
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))

  const cards = ref<DdlCard[]>([])
  const active = ref<CardAction>('create')
  const recent = ref<DdlHistoryEntry[]>([])
  const recentLoading = ref(false)
  const historyPanel = usePanelState()
  const historyDetail = ref<DdlHistoryEntry | null>(null)
  const cardElements = new Map<CardAction, HTMLElement>()
  let revision = 0
  let alive = true

  const location = () => [route.params.source, route.params.database, route.params.table].map(value => String(value || '')) as [string, string, string]
  const identity = () => location().join('/')
  const table = computed(() => location()[2])
  const canSample = computed(() => can('table:read'))
  const actionEnabled = (action: DdlAction) => action === 'compare' ? can('column:read') : action !== 'sample-data' || canSample.value
  const history = usePagedList<DdlHistoryEntry>(filter => {
    const [source, database, name] = location()
    return MetadataService.getTableDdlHistory(source, database, name, filter.page || 1, filter.size || 10)
  }, { onData: rows => { history.data.value = rows.map(mapDdlHistory) } })

  function errorMessage(response: any) {
    return response?.data?.message || response?.message || t('common.loadingFailed')
  }
  const succeeded = (response: any) => response?.status && response.data?.isSuccessful !== false

  /** Fetches every block of a card; menu requests are recorded, the opening cards are not. */
  async function loadCard(card: DdlCard, record: boolean) {
    const scope = identity()
    const [source, database, name] = location()
    card.formatting = false
    await Promise.all(card.blocks.map(async block => {
      block.loading = true
      block.error = ''
      try {
        const response = record
            ? await MetadataService.generateTableDdl(source, database, name, block.kind)
            : await MetadataService.getTableDdl(source, database, name, block.kind)
        if (!alive || identity() !== scope) return
        if (succeeded(response)) block.sql = response.data?.content || ''
        else { block.sql = ''; block.error = errorMessage(response) }
      } finally {
        if (alive && identity() === scope) block.loading = false
      }
    }))
    if (record && alive && identity() === scope) loadRecent()
  }

  const newCard = (action: CardAction): DdlCard => ({ action, formatting: false, blocks: DDL_CARDS[action].kinds.map(kind => ({ kind, sql: '', loading: true, error: '' })) })

  async function loadRecent() {
    const request = ++revision
    const [source, database, name] = location()
    recentLoading.value = true
    try {
      const response = await MetadataService.getTableDdlHistory(source, database, name, 1, 4)
      if (request !== revision || !alive) return
      recent.value = response.status ? (response.data?.content || []).map(mapDdlHistory) : []
    } finally {
      if (request === revision && alive) recentLoading.value = false
    }
  }
  function initialize() {
    cards.value = DEFAULT_DDL_CARDS.map(newCard)
    active.value = DEFAULT_DDL_CARDS[0]
    cards.value.forEach(card => loadCard(card, false))
    loadRecent()
  }

  /** Quick menu: compare goes to the compare page; any other action regenerates its card and scrolls to it. */
  async function runAction(action: DdlAction) {
    if (!actionEnabled(action)) return
    if (action === 'compare') {
      const [source, database, name] = location()
      router.push(compareLocation({ source, database, table: name, mode: 'structure' }))
      return
    }
    active.value = action
    let card = cards.value.find(item => item.action === action)
    if (!card) {
      cards.value.push(newCard(action))
      card = cards.value[cards.value.length - 1]
    }
    await nextTick()
    cardElements.get(action)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    await loadCard(card, true)
  }
  const bindCard = (action: CardAction) => (element: unknown) => {
    if (element instanceof HTMLElement) cardElements.set(action, element)
    else cardElements.delete(action)
  }

  const cardSql = (card: DdlCard) => joinDdl(card.blocks.map(block => block.sql))
  function copy(card: DdlCard) {
    const sql = cardSql(card)
    if (!sql) return
    ObjectUtils.copy(sql)
    MfMessage.success(t('query.tip.copySuccess'))
  }
  function download(card: DdlCard) {
    const sql = cardSql(card)
    if (!sql) return
    const url = URL.createObjectURL(new Blob([sql + '\n'], { type: 'application/sql;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = ddlFileName(table.value, card.action)
    link.click()
    URL.revokeObjectURL(url)
  }
  /** Formatting is done by the server's SQL formatter and only changes what is shown. */
  async function format(card: DdlCard) {
    const blocks = card.blocks.filter(block => block.sql)
    if (!blocks.length || card.formatting) return
    const scope = identity()
    card.formatting = true
    try {
      for (const block of blocks) {
        const response = await FormatService.formatSql({ sql: block.sql })
        if (!alive || identity() !== scope) return
        if (response.status && typeof response.data === 'string') block.sql = response.data
        else { MfMessage.error(response.message || t('common.loadingFailed')); return }
      }
    } finally {
      card.formatting = false
    }
  }

  function openHistory() {
    historyDetail.value = null
    history.onSearch(emptyTableFilters())
    historyPanel.open()
  }
  const toggleHistoryDetail = (entry: DdlHistoryEntry) => { historyDetail.value = historyDetail.value?.id === entry.id ? null : entry }

  // 快捷操作面板默认收起，由页面的「DDL 操作」按钮开合；开合状态接入工作台右侧布局
  const panelOpen = ref(false)
  function togglePanel() {
    panelOpen.value = !panelOpen.value
    fieldPanelOpen.value = panelOpen.value
  }
  function closePanel() {
    panelOpen.value = false
    fieldPanelOpen.value = false
  }
  watch(panelOpen, value => { fieldPanelOpen.value = value })

  watch(identity, initialize, { immediate: true })
  if (refreshKey) watch(refreshKey, initialize)
  useWorkspaceLoading(computed(() => recentLoading.value || cards.value.some(card => card.blocks.some(block => block.loading))))
  onScopeDispose(() => {
    alive = false
    revision++
    panelOpen.value = false
    fieldPanelOpen.value = false
  })

  return {
    can, cards, active, recent, recentLoading, table, fieldPanelHost, fieldPanelWidth, actionEnabled, runAction, bindCard,
    copy, download, format, cardSql, history, historyVisible: historyPanel.visible, historyDetail, openHistory, toggleHistoryDetail,
    panelOpen, togglePanel, closePanel,
  }
}
