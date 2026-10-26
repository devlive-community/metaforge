import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；source/type/mode/format/state/action 列通过 #bodyCell 按 column.key 渲染
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.plugin'), key: 'source' },
        { title: t('common.type'), key: 'type' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.endTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.elapsed'), dataIndex: 'elapsed', key: 'elapsed' },
        { title: t('common.from'), key: 'mode' },
        { title: t('common.count'), dataIndex: 'count', key: 'count' },
        { title: t('common.format'), key: 'format' },
        { title: t('common.state'), key: 'state' },
        { title: t('common.action'), key: 'action' }
    ])

    return {
        headers
    }
}
