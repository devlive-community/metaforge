import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；active/system/action 走 #bodyCell
    // 名称/描述限定宽度并配合 EllipsisCell 省略 + tooltip；表达式/时间列不设宽度，按内容自适应
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.name'), dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
        { title: t('common.description'), dataIndex: 'description', key: 'description', width: 240, ellipsis: true },
        { title: t('common.expression'), dataIndex: 'expression', key: 'expression' },
        { title: t('common.active'), key: 'active', width: 90 },
        { title: t('common.system'), key: 'system', width: 90 },
        { title: t('common.type'), dataIndex: 'type', key: 'type' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.action'), key: 'action' }
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
