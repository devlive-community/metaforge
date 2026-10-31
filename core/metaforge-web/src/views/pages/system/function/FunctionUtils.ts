import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export function useHeaders()
{
    const { t } = useI18n()

    // MfTable 列格式（bodyCell 契约）；plugin / type / action 列无 dataIndex，通过 #bodyCell 按 column.key 渲染
    const headers = computed(() => [
        { title: t('common.id'), dataIndex: 'id', key: 'id' },
        { title: t('common.name'), dataIndex: 'name', key: 'name', width: 150, ellipsis: true },
        { title: t('function.import.dataType'), key: 'type', width: 90 },
        { title: t('function.field.category'), dataIndex: 'category', key: 'category', width: 110 },
        { title: t('common.description'), dataIndex: 'description', key: 'description', width: 220, ellipsis: true },
        { title: t('function.field.syntax'), dataIndex: 'syntax', key: 'syntax', width: 220, ellipsis: true },
        { title: t('function.field.return_type'), dataIndex: 'returnType', key: 'returnType', width: 120 },
        { title: t('function.field.version'), dataIndex: 'version', key: 'version', width: 110 },
        { title: t('common.plugin'), key: 'plugin' },
        { title: t('common.createTime'), dataIndex: 'createTime', key: 'createTime' },
        { title: t('common.updateTime'), dataIndex: 'updateTime', key: 'updateTime' },
        { title: t('common.action'), key: 'action' }
    ])

    // 数据类型选项（创建/编辑表单用）
    const typeHeaders = computed(() => [
        { label: t('function.common.function'), value: 'FUNCTION' },
        { label: t('function.common.keyword'), value: 'KEYWORD' },
        { label: t('function.common.operator'), value: 'OPERATOR' }
    ])

    return {
        headers,
        typeHeaders
    }
}
