import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；active / action 列无 dataIndex，通过 #bodyCell 按 column.key 渲染
    // 名称/描述列限定宽度并配合 EllipsisCell 省略 + tooltip；其余列按内容自适应宽度
    const headers = computed(() => [
            { title: t('common.id'), dataIndex: 'id', key: 'id' },
            { title: t('common.name'), dataIndex: 'name', key: 'name', width: 180 },
            { title: t('common.description'), dataIndex: 'description', key: 'description', width: 240 },
            { title: t('menu.common.url'), dataIndex: 'url', key: 'url', width: 200 },
            { title: t('common.group'), dataIndex: 'group', key: 'group' },
            { title: t('common.sorted'), dataIndex: 'sorted', key: 'sorted', width: 80 },
            { title: t('common.type'), dataIndex: 'type', key: 'type', width: 90 },
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
