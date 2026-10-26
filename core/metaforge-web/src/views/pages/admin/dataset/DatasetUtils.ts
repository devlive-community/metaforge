import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * Hook for dataset table headers
 */
export function useDatasetHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；source/syncMode/state/action 走 #bodyCell
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.name'), dataIndex: 'name', key: 'name' },
        { title: t('common.description'), dataIndex: 'description', key: 'description' },
        { title: t('common.source'), key: 'source' },
        { title: t('dataset.common.syncMode'), key: 'syncMode' },
        { title: t('common.scheduler'), dataIndex: 'scheduler', key: 'scheduler' },
        { title: t('common.executor'), dataIndex: 'executor', key: 'executor' },
        { title: t('common.state'), key: 'state' },
        { title: t('dataset.common.totalRows'), dataIndex: 'totalRows', key: 'totalRows' },
        { title: t('dataset.common.totalSize'), dataIndex: 'totalSize', key: 'totalSize' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
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
