import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；active / action 列无 dataIndex，通过 #bodyCell 按 column.key 渲染
    const headers = computed(() => [
            { title: t('common.id'), dataIndex: 'id', key: 'id' },
            { title: t('common.name'), dataIndex: 'name', key: 'name' },
            { title: t('common.description'), dataIndex: 'description', key: 'description' },
            { title: t('common.group'), dataIndex: 'group', key: 'group' },
            { title: t('common.sorted'), dataIndex: 'sorted', key: 'sorted' },
            { title: t('common.type'), dataIndex: 'type', key: 'type' },
            { title: t('common.active'), key: 'active' },
            { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
            { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
            { title: t('common.action'), key: 'action' }
        ]
    )

    return {
        headers
    }
}
