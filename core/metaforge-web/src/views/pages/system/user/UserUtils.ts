import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）：title + dataIndex/key；
    // role / action 列没有 dataIndex，通过 <template #bodyCell> 按 column.key 自定义渲染
    const headers = computed(() => [
            { title: t('common.id'), dataIndex: 'id', key: 'id' },
            { title: t('common.username'), dataIndex: 'username', key: 'username' },
            { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
            { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
            { title: t('common.role'), key: 'role', width: 100 },
            { title: t('common.action'), key: 'action' }
        ]
    )

    return {
        headers
    }
}
