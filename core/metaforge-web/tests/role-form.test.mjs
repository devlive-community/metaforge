import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { h, nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import { compileScript, parse } from 'vue/compiler-sfc'
import ts from 'typescript'
import { component } from '../../metaforge-ui/tests/component.mjs'
import { InteractionEvent, mountHost } from '../../metaforge-ui/tests/interaction-host.mjs'

const dataModule = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const compile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } }).outputText
const modelUrl = dataModule(compile(await readFile(new URL('../src/model/role.ts', import.meta.url), 'utf8')))
const utilityUrl = dataModule(compile(await readFile(new URL('../src/utils/role-code.ts', import.meta.url), 'utf8')))
const stringUrl = dataModule(compile(await readFile(new URL('../src/utils/string.ts', import.meta.url), 'utf8')))
const serviceUrl = dataModule('export const state={checks:[],saves:[], check:async()=>({status:true,data:true}),save:async()=>({status:true})};export default {checkCode(code){state.checks.push(code);return state.check(code)},saveOrUpdate(body,original){state.saves.push({body,original});return state.save(body,original)}}')
const messageUrl = dataModule('export const MfMessage={success(){},error(){}}')
const lodashUrl = dataModule(`import lodash from ${JSON.stringify(import.meta.resolve('lodash'))}; export const cloneDeep = lodash.cloneDeep`)
const filename = new URL('../src/views/pages/system/role/RoleInfo.vue', import.meta.url)
const { descriptor } = parse(await readFile(filename, 'utf8'), { filename: filename.pathname })
let code = compile(compileScript(descriptor, { id: 'role-form-test', inlineTemplate: true }).content)
const imports = { '@/model/role': modelUrl, '@/utils/string': stringUrl, '@/utils/role-code': utilityUrl, '@/services/role': serviceUrl, 'metaforge-ui': messageUrl, lodash: lodashUrl }
code = code.replace(/from (['"])([^'"]+)\1/g, (_match, _quote, name) => `from ${JSON.stringify(imports[name] || import.meta.resolve(name))}`)
const { default: Form } = await import(dataModule(code))
const { state } = await import(serviceUrl)
const components = Object.fromEntries(await Promise.all(['MfForm', 'MfFormItem', 'MfInput', 'MfTextarea', 'MfButton', 'MfModal'].map(async name => [name, await component(name)])))
const settle = () => new Promise(resolve => setImmediate(resolve))

function mount(info = null) {
    state.checks = []
    state.saves = []
    state.check = async () => ({ status: true, data: true })
    const host = mountHost(h(Form, { isVisible: true, info }), undefined, app => {
        app.use(createI18n({ legacy: false, locale: 'en', missingWarn: false, fallbackWarn: false, messages: { en: {} } }))
        for (const [name, value] of Object.entries(components)) app.component(name, value)
    })
    return host
}

async function input(host, field, value) {
    field.value = value
    field.props.onInput(new InteractionEvent('input', [field]))
    await nextTick()
}

async function fill(host, code) {
    const fields = host.all(node => node.tag === 'input')
    await input(host, fields[0], code)
    await input(host, fields[1], 'Read role')
    await input(host, host.find(node => node.tag === 'textarea'), 'Read-only role')
    return fields[0]
}

async function submit(host) {
    const form = host.find(node => node.tag === 'form')
    await form.props.onSubmit(new InteractionEvent('submit', [form]))
    await settle()
}

test('edit mode disables the code input and sends the original identity', async () => {
    const host = mount({ id: 1, code: 'ORIGINAL', name: 'Read role', description: 'Read-only role' })
    try {
        const field = host.all(node => node.tag === 'input')[0]
        assert.equal(field.disabled, true)
        assert.equal(field.value, 'ORIGINAL')
        await submit(host)
        assert.equal(state.checks.length, 0)
        assert.equal(state.saves[0].original, 'ORIGINAL')
        assert.equal(state.saves[0].body.code, 'ORIGINAL')
    }
    finally { host.unmount() }
})

test('create checks a specified code once and saves it only after the check succeeds', async () => {
    const host = mount()
    let resolve
    state.check = () => new Promise(done => { resolve = done })
    try {
        const field = await fill(host, ' CUSTOM_READ ')
        field.props.onBlur(new InteractionEvent('blur', [field]))
        await submit(host)
        assert.deepEqual(state.checks, ['CUSTOM_READ'])
        assert.equal(state.saves.length, 0)
        resolve({ status: true, data: true })
        await settle()
        assert.equal(state.saves[0].body.code, 'CUSTOM_READ')
        assert.equal(state.saves[0].original, undefined)
    }
    finally { host.unmount() }
})

test('duplicate codes block submission and stale results cannot overwrite newer checks', async () => {
    const host = mount()
    try {
        const field = await fill(host, 'TAKEN')
        state.check = async () => ({ status: true, data: false })
        await submit(host)
        assert.equal(state.saves.length, 0)
        assert.ok(host.container.textContent.includes('role.validate.codeExists') || host.document.body.textContent.includes('role.validate.codeExists'))
        let resolve
        state.check = code => code === 'OLDER' ? new Promise(done => { resolve = done }) : Promise.resolve({ status: true, data: true })
        await input(host, field, 'OLDER')
        field.props.onBlur(new InteractionEvent('blur', [field]))
        await input(host, field, 'NEWER')
        field.props.onBlur(new InteractionEvent('blur', [field]))
        await settle()
        resolve({ status: true, data: false })
        await settle()
        assert.ok(host.document.body.textContent.includes('role.tip.codeAvailable'))
        assert.ok(!host.document.body.textContent.includes('role.validate.codeExists'))
    }
    finally { host.unmount() }
})
