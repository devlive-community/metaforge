import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'

const dataModule = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const compile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } }).outputText
const utilities = dataModule(compile(await readFile(new URL('../src/utils/role-code.ts', import.meta.url), 'utf8')))
const { normalizeRoleCode, validRoleCode } = await import(utilities)
const httpUrl = dataModule('export const calls = []; export class HttpUtils { get(url, params) { calls.push({method:"get",url,params}); return Promise.resolve({status:true,data:true}) } post(url, body) { calls.push({method:"post",url,body}); return Promise.resolve({status:true}) } put(url, body) { calls.push({method:"put",url,body}); return Promise.resolve({status:true}) } }')
const baseUrl = dataModule('export class BaseService { constructor() {} }')
const imports = { '@/services/base': baseUrl, '@/utils/http': httpUrl, '@/utils/role-code': utilities }
let serviceCode = compile(await readFile(new URL('../src/services/role.ts', import.meta.url), 'utf8'))
serviceCode = serviceCode.replace(/from (['"])([^'"]+)\1/g, (_match, _quote, name) => `from ${JSON.stringify(imports[name])}`)
const { default: service } = await import(dataModule(serviceCode))
const { calls } = await import(httpUrl)

test('role code validation permits blank auto-generation and valid identifiers only', () => {
    assert.equal(normalizeRoleCode('  READ_ONLY  '), 'READ_ONLY')
    for (const value of [undefined, '', ' ', 'READ_ONLY', 'reader-2', 'A'.repeat(64)]) assert.equal(validRoleCode(value), true)
    for (const value of ['bad code', 'bad/code', '_BAD', '中文', 'A'.repeat(65)]) assert.equal(validRoleCode(value), false)
})

test('a new role with a custom code is posted instead of mistaken for an update', async () => {
    calls.length = 0
    await service.saveOrUpdate({ name: 'Read role', code: 'CUSTOM_READ' })
    assert.equal(calls[0].method, 'post')
    assert.equal(calls[0].url, '/api/v1/role')
    assert.equal(calls[0].body.code, 'CUSTOM_READ')
})

test('editing addresses the original code and keeps legacy id-based updates compatible', async () => {
    calls.length = 0
    await service.saveOrUpdate({ id: 1, code: 'MODIFIED' }, 'ORIGINAL')
    assert.equal(calls[0].method, 'put')
    assert.equal(calls[0].url, '/api/v1/role/ORIGINAL')
    await service.saveOrUpdate({ id: 1, code: 'ORIGINAL' })
    assert.equal(calls[1].method, 'put')
    assert.equal(calls[1].url, '/api/v1/role')
})

test('availability checks send the code as a query parameter', async () => {
    calls.length = 0
    await service.checkCode('CUSTOM_READ')
    assert.deepEqual(calls[0], { method: 'get', url: '/api/v1/role/check-code', params: { code: 'CUSTOM_READ' } })
})
