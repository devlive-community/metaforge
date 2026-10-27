import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export type TableResource = 'source' | 'dataset' | 'user' | 'role' | 'menu' | 'function' | 'workflow' | 'history' | 'log' | 'snippet' | 'report' | 'schedule'

export interface TableFilterField
{
    key: string
    label: string
    type: 'text' | 'select' | 'date'
    advanced?: boolean
    placeholder?: string
    options?: { value: string; label: string }[]
}

export function createTableFilterFields(resource: TableResource, t: (key: string) => string): TableFilterField[]
{
    const text = (key: string, label: string, advanced = false): TableFilterField => ({ key, label: t(label), type: 'text', advanced })
    const select = (key: string, label: string, values: [string, string][], advanced = false): TableFilterField => ({
        key, label: t(label), type: 'select', advanced,
        options: [{ value: '', label: t('dataTable.all') }, ...values.map(([value, title]) => ({ value, label: t(title) }))]
    })
    const boolean = (key: string, label: string, advanced = false) => select(key, label, [['true', 'dataTable.yes'], ['false', 'dataTable.no']], advanced)
    const active = (advanced = false) => select('active', 'common.active', [['true', 'dataTable.enabled'], ['false', 'dataTable.disabled']], advanced)
    const dates = (login = false): TableFilterField[] => [
        { key: 'createdFrom', label: t(login ? 'dataTable.loginFrom' : 'dataTable.createdFrom'), type: 'date', advanced: true },
        { key: 'createdTo', label: t(login ? 'dataTable.loginTo' : 'dataTable.createdTo'), type: 'date', advanced: true }
    ]
    const keyword = text('keyword', resource === 'user' ? 'common.username' : 'common.name')
    const result = [['SUCCESS', 'dataTable.success'], ['FAILURE', 'dataTable.failure']] as [string, string][]
    switch (resource) {
        case 'source': return [keyword, select('available', 'common.available', [['true', 'dataTable.available'], ['false', 'dataTable.unavailable']]), text('type', 'source.common.type', true), text('host', 'common.host', true), text('protocol', 'common.protocol', true), boolean('publish', 'common.public', true), ...dates()]
        case 'dataset': return [keyword, select('syncMode', 'dataset.common.syncMode', [['MANUAL', 'dataset.common.syncModeManual'], ['TIMING', 'dataset.common.syncModeTiming'], ['OUT_SYNC', 'dataset.common.syncModeOutSync']]), text('executor', 'common.executor', true), text('scheduler', 'common.scheduler', true), ...dates()]
        case 'user': return [keyword, boolean('system', 'dataTable.systemAccount'), active(true), ...dates()]
        case 'role': return [keyword, text('code', 'common.code'), text('description', 'common.description', true), ...dates()]
        case 'menu': return [keyword, text('group', 'common.group'), active(true), text('url', 'dataTable.menuPath', true), ...dates()]
        case 'function': return [keyword, select('type', 'common.type', [['KEYWORD', 'function.common.keyword'], ['OPERATOR', 'function.common.operator'], ['FUNCTION', 'function.common.function']]), text('plugin', 'common.plugin', true), text('content', 'common.content', true), ...dates()]
        case 'workflow': return [keyword, select('state', 'common.state', ['CREATED', 'QUEUE', 'RUNNING', 'SUCCESS', 'FAILURE', 'TIMEOUT', 'STOPPING', 'STOPPED', 'INTERRUPTED'].map(value => [value, `dataTable.state${value}`])), text('executor', 'common.executor', true), ...dates()]
        case 'history': return [text('content', 'dataTable.sqlContent'), select('state', 'common.state', result), select('mode', 'common.from', ['ADHOC', 'HISTORY', 'REPORT', 'SNIPPET', 'SYNC', 'DATASET'].map(value => [value, `dataTable.mode${value}`]), true), ...dates()]
        case 'log': return [text('ip', 'common.ip'), select('state', 'dataTable.loginResult', result), text('device', 'common.device', true), text('client', 'common.client', true), ...dates(true)]
        case 'snippet': return [keyword, text('context', 'dataTable.sqlContent'), text('description', 'common.description', true), ...dates()]
        case 'report': return [keyword, select('type', 'common.type', [['QUERY', 'dataTable.queryReport'], ['DATASET', 'dataTable.datasetReport']]), boolean('realtime', 'common.realtime', true), ...dates()]
        case 'schedule': return [keyword, active(), boolean('isSystem', 'dataTable.systemTask', true), text('expression', 'common.expression', true), ...dates()]
    }
}

export function useTableFilters(resource: TableResource)
{
    const { t } = useI18n()
    return computed(() => createTableFilterFields(resource, t))
}
