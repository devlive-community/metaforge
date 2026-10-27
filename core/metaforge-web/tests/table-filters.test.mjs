import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'

const source = await readFile(new URL('../src/views/components/table/tableFilters.ts', import.meta.url), 'utf8')
let { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } })
outputText = outputText.replace(/from (['"])(vue|vue-i18n)\1/g, (_match, _quote, name) => `from ${JSON.stringify(import.meta.resolve(name))}`)
const { createTableFilterFields } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)
const fields = resource => createTableFilterFields(resource, key => key)

test('page schemas expose relevant business fields rather than one universal filter set', () => {
    const expected = { source: 'available', dataset: 'syncMode', user: 'system', role: 'code', menu: 'group', function: 'type', workflow: 'state', history: 'content', log: 'ip', snippet: 'context', report: 'realtime', schedule: 'isSystem' }
    for (const [resource, key] of Object.entries(expected)) assert.ok(fields(resource).some(field => field.key === key), resource)
    assert.ok(!fields('source').some(field => field.key === 'active'))
    assert.ok(!fields('log').some(field => ['active', 'code', 'keyword'].includes(field.key)))
    assert.equal(new Set(Object.keys(expected).map(resource => fields(resource).map(field => field.key).join(','))).size, 12)
})

test('enum options match the API values and date filters use the custom calendar control', () => {
    const sync = fields('dataset').find(field => field.key === 'syncMode')
    assert.deepEqual(sync.options.map(option => option.value), ['', 'MANUAL', 'TIMING', 'OUT_SYNC'])
    const result = fields('log').find(field => field.key === 'state')
    assert.deepEqual(result.options.map(option => option.value), ['', 'SUCCESS', 'FAILURE'])
    for (const resource of ['source', 'dataset', 'user', 'log']) {
        const dates = fields(resource).filter(field => field.type === 'date')
        assert.deepEqual(dates.map(field => field.key), ['createdFrom', 'createdTo'])
        assert.ok(dates.every(field => field.advanced))
    }
})
