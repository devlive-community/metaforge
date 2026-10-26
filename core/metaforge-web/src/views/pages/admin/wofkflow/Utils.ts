import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；executor/state/action 走 #bodyCell
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.name'), dataIndex: 'name', key: 'name' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.endTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.elapsed'), dataIndex: 'elapsed', key: 'elapsed' },
        { title: t('common.executor'), key: 'executor' },
        { title: t('common.state'), key: 'state' },
        { title: t('common.action'), key: 'action' }
    ])

    return {
        headers
    }
}
