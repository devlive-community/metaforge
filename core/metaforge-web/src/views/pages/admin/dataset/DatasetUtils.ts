import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * Hook for dataset table headers
 */
export function useDatasetHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；source/syncMode/state/action 走 #bodyCell
    // 除名称外全部定宽，剩余空间由名称列吸收，避免自动布局把空白摊得到处都是
    const headers = computed(() => [
        { title: t('common.name'), dataIndex: 'name', key: 'name', width: 200, ellipsis: true },
        { title: t('common.source'), key: 'source', width: '320px' },
        { title: t('common.state'), key: 'state', width: '110px' },
        { title: t('dataset.common.syncMode'), key: 'syncMode', width: '96px' },
        { title: t('dataset.prototype.fieldCount'), dataIndex: 'columnCount', key: 'columnCount', width: '80px' },
        { title: t('dataset.common.totalRows'), dataIndex: 'totalRows', key: 'totalRows', width: '90px' },
        { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime', width: '180px' },
        { title: t('common.action'), key: 'action' }
    ])

    // MfTable 列格式（bodyCell 契约）；progress/state/action 走 #bodyCell
    const historyHeaders = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.elapsed'), dataIndex: 'elapsed', key: 'elapsed' },
        { title: t('dataset.history.totalCount'), dataIndex: 'totalCount', key: 'totalCount' },
        { title: t('dataset.history.processedCount'), dataIndex: 'processedCount', key: 'processedCount' },
        { title: t('dataset.history.progress'), key: 'progress' },
        { title: t('common.count'), dataIndex: 'count', key: 'count' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.state'), key: 'state' },
        { title: t('common.action'), key: 'action' }
    ])

    return {
        headers,
        historyHeaders
    }
}
