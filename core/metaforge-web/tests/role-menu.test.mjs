import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { h, nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import { compileScript, parse } from 'vue/compiler-sfc'
import ts from 'typescript'
import { component } from '../../metaforge-ui/tests/component.mjs'
import { click, InteractionEvent, mountHost } from '../../metaforge-ui/tests/interaction-host.mjs'

const dataModule = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const compile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } }).outputText
const apiUrl = dataModule('export const state={saves:[],errors:[],updates:[],refreshes:0,get:async()=>({status:true,data:[]}),save:async()=>({status:true}),refresh:async()=>({status:true,data:[]})};export default {getAllMenuById(id){return state.get(id)},saveMenu(id,menus){state.saves.push({id,menus});return state.save(id,menus)}}')
const userUrl = dataModule(`import {state} from ${JSON.stringify(apiUrl)};export default {getMenus(){state.refreshes++;return state.refresh()}}`)
const storeUrl = dataModule(`import {state} from ${JSON.stringify(apiUrl)};export const useUserStore=()=>({updateMenu(value){state.updates.push(value)}})`)
const messageUrl = dataModule(`import {state} from ${JSON.stringify(apiUrl)};export const MfMessage={error(message){state.errors.push(message)}}`)
const iconUrl = dataModule(`import {h} from ${JSON.stringify(import.meta.resolve('vue'))};export const RightOutlined={render(){return h('svg')}}`)
const utilityUrl = dataModule(compile(await readFile(new URL('../src/utils/role-code.ts', import.meta.url), 'utf8')))
const filename = new URL('../src/views/pages/system/role/RoleMenu.vue', import.meta.url)
const { descriptor } = parse(await readFile(filename, 'utf8'), { filename: filename.pathname })
let source = compile(compileScript(descriptor, { id: 'role-menu-test', inlineTemplate: true }).content)
const imports = { '@/services/role': apiUrl, '@/services/user': userUrl, '@/stores/user': storeUrl, '@/utils/role-code': utilityUrl, 'metaforge-ui': messageUrl, '@ant-design/icons-vue': iconUrl }
source = source.replace(/from (['"])([^'"]+)\1/g, (_match, _quote, name) => `from ${JSON.stringify(imports[name] || import.meta.resolve(name))}`)
const { default: Menu } = await import(dataModule(source))
const { state } = await import(apiUrl)
const components = Object.fromEntries(await Promise.all(['MfCheckbox', 'MfModal', 'MfSpinner', 'MfAlert', 'MfButton'].map(async name => [name, await component(name)])))
const tree = [{ id: 1, title: 'Admin', selected: true, children: [{ id: 2, title: 'Sources', selected: true }, { id: 3, title: 'Users', selected: false }] }]
const settle = () => new Promise(resolve => setImmediate(resolve))

function mount(load = async () => ({ status: true, data: tree })) {
    Object.assign(state, { saves: [], errors: [], updates: [], refreshes: 0, get: load, save: async () => ({ status: true }), refresh: async () => ({ status: true, data: tree }) })
    const closes = []
    const host = mountHost(h(Menu, { isVisible: true, info: { id: 7, code: 'OPS', name: 'Operators' }, onClose: value => closes.push(value) }), undefined, app => {
        app.use(createI18n({ legacy: false, locale: 'en', missingWarn: false, fallbackWarn: false, messages: { en: {
            common: { refresh: 'Refresh' }, role: { common: { assignMenu: 'Assign $NAME' }, tip: { expandMenu: 'Expand $NAME', collapseMenu: 'Collapse $NAME', menuRefreshFailed: 'Saved; navigation refresh failed' }, validate: { menuLoadFailed: 'Load failed', invalidMenus: 'Invalid menu selection', menuSaveFailed: 'Save failed' } }
        } } }))
        for (const [name, value] of Object.entries(components)) app.component(name, value)
    })
    return { ...host, closes }
}
const checkboxes = host => host.all(node => node.tag === 'input' && node.props.type === 'checkbox')
const button = (host, text) => host.find(node => node.tag === 'button' && node.textContent.trim() === text)
async function check(host, input, checked) {
    input.checked = checked
    input.props.onChange(new InteractionEvent('change', [input]))
    await nextTick()
}

test('partial stored assignments remain partial and selecting all children clears the mixed state', async () => {
    const host = mount()
    try {
        await settle()
        assert.equal(checkboxes(host)[0].props.checked, false)
        assert.equal(checkboxes(host)[0].props.indeterminate, true)
        assert.equal(checkboxes(host)[0].props['aria-checked'], 'mixed')
        await click(host.document, host.find(node => node.props['aria-label'] === 'Expand Admin'))
        assert.equal(checkboxes(host)[1].props.checked, true)
        assert.equal(checkboxes(host)[2].props.checked, false)
        await check(host, checkboxes(host)[2], true)
        assert.equal(checkboxes(host)[0].props.checked, true)
        assert.equal(checkboxes(host)[0].props.indeterminate, false)
        assert.equal(checkboxes(host)[0].props['aria-checked'], true)
        await click(host.document, button(host, 'Assign Operators'))
        await settle()
        assert.deepEqual(new Set(state.saves[0].menus), new Set([1, 2, 3]))
        assert.equal(state.saves[0].id, 7)
        assert.equal(state.updates.length, 1)
        assert.deepEqual(host.closes, [false])
    }
    finally { host.unmount() }
})

test('unchecking all menus sends an empty assignment and refreshes the navigation store', async () => {
    const host = mount()
    try {
        await settle()
        await check(host, checkboxes(host)[0], true)
        await check(host, checkboxes(host)[0], false)
        await click(host.document, button(host, 'Assign Operators'))
        await settle()
        assert.deepEqual(state.saves, [{ id: 7, menus: [] }])
        assert.equal(state.refreshes, 1)
        assert.equal(state.updates.length, 1)
    }
    finally { host.unmount() }
})

test('a failed load cannot accidentally submit an empty assignment and can be retried', async () => {
    const host = mount(async () => ({ status: false, code: 4035 }))
    try {
        await settle()
        assert.ok(host.find(node => node.props.role === 'alert'))
        assert.ok(!button(host, 'Assign Operators'))
        assert.equal(state.saves.length, 0)
        state.get = async () => ({ status: true, data: tree })
        await click(host.document, button(host, 'Refresh'))
        await settle()
        assert.ok(button(host, 'Assign Operators'))
    }
    finally { host.unmount() }
})

test('a rejected save keeps the dialog open and does not refresh navigation', async () => {
    const host = mount()
    try {
        await settle()
        state.save = async () => ({ status: false, code: 4033 })
        await click(host.document, button(host, 'Assign Operators'))
        await settle()
        assert.deepEqual(state.errors, ['Invalid menu selection'])
        assert.equal(state.refreshes, 0)
        assert.equal(state.updates.length, 0)
        assert.deepEqual(host.closes, [])
    }
    finally { host.unmount() }
})

test('navigation refresh failures are reported as saved changes and close the dialog', async () => {
    const host = mount()
    try {
        await settle()
        state.refresh = async () => ({ status: false })
        await click(host.document, button(host, 'Assign Operators'))
        await settle()
        assert.deepEqual(state.errors, ['Saved; navigation refresh failed'])
        assert.deepEqual(host.closes, [false])
        assert.equal(state.updates.length, 0)
    }
    finally { host.unmount() }
})
