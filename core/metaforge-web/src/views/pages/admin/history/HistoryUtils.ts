import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；source/type/mode/state/format/user/content/action 走 #bodyCell
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.plugin'), key: 'source', width: 200, ellipsis: true },
        { title: t('common.type'), key: 'type', width: 70 },
        { title: t('history.common.content'), key: 'content', width: 260, ellipsis: true },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.endTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.elapsed'), dataIndex: 'elapsed', key: 'elapsed', width: 90 },
        { title: t('common.from'), key: 'mode', width: 100 },
        { title: t('common.count'), dataIndex: 'count', key: 'count', width: 80 },
        { title: t('common.format'), key: 'format', width: 70 },
        { title: t('common.state'), key: 'state', width: 90 },
        { title: t('history.common.user'), key: 'user', width: 120 },
        { title: t('common.action'), key: 'action' }
    ])

    return {
        headers
    }
}
