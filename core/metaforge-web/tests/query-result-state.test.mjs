import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'

const source = await readFile(new URL('../src/model/query-result-state.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { queryResultState } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('pending queries never present a previous response as empty or failed', () => {
    for (const response of [null, { status: false }, { status: true, data: { columns: [] } }]) {
        assert.equal(queryResultState(response, true), 'loading')
    }
})

test('a result without a response is waiting for the first query', () => {
    assert.equal(queryResultState(null, false), 'initial')
    assert.equal(queryResultState(undefined, false), 'initial')
})

test('server failure wins over missing or stale result data', () => {
    assert.equal(queryResultState({ status: false, message: 'Access denied' }, false), 'error')
    assert.equal(queryResultState({ status: false, data: { columns: [{ id: 1 }] } }, false), 'error')
})

test('successful zero-row queries and statements without result sets are empty', () => {
    assert.equal(queryResultState({ status: true, data: { columns: [] } }, false), 'empty')
    assert.equal(queryResultState({ status: true }, false), 'empty')
})

test('a real result row remains data even when all its values are empty or zero', () => {
    assert.equal(queryResultState({ status: true, data: { columns: [{ value: 0, name: '', optional: null }] } }, false), 'ready')
})
