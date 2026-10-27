import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；active/system/action 走 #bodyCell
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.name'), dataIndex: 'name', key: 'name' },
        { title: t('common.description'), dataIndex: 'description', key: 'description' },
        { title: t('common.expression'), dataIndex: 'expression', key: 'expression' },
        { title: t('common.active'), key: 'active' },
        { title: t('common.system'), key: 'system' },
        { title: t('common.type'), dataIndex: 'type', key: 'type' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.action'), key: 'action', width: '150px' }
    ])

    // MfTable 列格式（bodyCell 契约）
    const historyHeaders = computed(() => [
            { title: t('common.id'), dataIndex: 'id', key: 'id' },
            { title: t('common.name'), dataIndex: 'name', key: 'name' },
            { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
            { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
            { title: t('common.elapsed'), dataIndex: 'elapsed', key: 'elapsed' },
            { title: t('common.state'), dataIndex: 'state', key: 'state' },
            { title: t('common.result'), dataIndex: 'result', key: 'result' }
        ]
    )

    return {
        headers,
        historyHeaders
    }
}
