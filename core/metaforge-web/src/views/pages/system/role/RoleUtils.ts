import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；active / action 列无 dataIndex，通过 #bodyCell 按 column.key 渲染
    const headers = computed(() => [
            { title: t('common.id'), dataIndex: 'id', key: 'id' },
            { title: t('common.name'), dataIndex: 'name', key: 'name', width: 160, ellipsis: true },
            { title: t('common.code'), dataIndex: 'code', key: 'code' },
            { title: t('common.description'), dataIndex: 'description', key: 'description', width: 240, ellipsis: true },
            { title: t('role.common.userCount'), dataIndex: 'userCount', key: 'userCount', width: 90 },
            { title: t('role.common.menuCount'), dataIndex: 'menuCount', key: 'menuCount', width: 90 },
            { title: t('common.active'), key: 'active', width: 90 },
            { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
            { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
            { title: t('common.action'), key: 'action' }
        ]
    )

    return {
        headers
    }
}
