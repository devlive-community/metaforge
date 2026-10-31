import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；type/public/version/available/action 走 #bodyCell
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.name'), dataIndex: 'name', key: 'name', width: 200, ellipsis: true },
        { title: t('common.type'), key: 'type' },
        { title: t('common.protocol'), dataIndex: 'protocol', key: 'protocol' },
        { title: t('common.host'), dataIndex: 'host', key: 'host' },
        { title: t('common.port'), dataIndex: 'port', key: 'port' },
        { title: t('common.public'), key: 'public' },
        { title: t('common.version'), key: 'version' },
        { title: t('common.available'), key: 'available' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.action'), key: 'action' }
    ])

    // MfTable 列格式（bodyCell 契约）；elapsed/state/result 走 #bodyCell
    const historyHeaders = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id', width: 80 },
        { title: t('common.name'), dataIndex: 'name', key: 'name' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.elapsed'), key: 'elapsed' },
        { title: t('common.state'), key: 'state' },
        { title: t('common.result'), key: 'result' }
    ])

    return {
        headers,
        historyHeaders
    }
}
