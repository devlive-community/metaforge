import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；state 列通过 #bodyCell 渲染
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.device'), dataIndex: 'device', key: 'device' },
        { title: t('common.client'), dataIndex: 'client', key: 'client' },
        { title: t('common.ip'), dataIndex: 'ip', key: 'ip' },
        { title: t('common.state'), key: 'state' },
        { title: t('common.ua'), dataIndex: 'ua', key: 'ua', width: 350 },
        { title: t('common.loginTime'), dataIndex: 'createTime', key: 'createTime' }
    ])

    return {
        headers
    }
}
