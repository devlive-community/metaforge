import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { compile, defineComponent, h, nextTick, reactive } from 'vue'
import { parse } from 'vue/compiler-sfc'
import ts from 'typescript'
import { component } from '../../metaforge-ui/tests/component.mjs'
import { click, mountHost } from '../../metaforge-ui/tests/interaction-host.mjs'

const filename = new URL('../src/views/pages/admin/source/components/TableCreate.vue', import.meta.url)
const { descriptor } = parse(await readFile(filename, 'utf8'))
const render = compile(descriptor.template.content)
const script = ts.createSourceFile('TableCreate.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = script.statements.flatMap(statement => statement.declarationList?.declarations ?? [])
    .find(node => node.name.getText(script) === 'indexColumns')
const { outputText } = ts.transpileModule(`return ${declaration.initializer.getText(script)}`, {
    compilerOptions: { target: ts.ScriptTarget.ES2020 },
})
const indexColumns = new Function('t', outputText)(key => key)
const names = ['MfDrawer', 'MfForm', 'MfFormItem', 'MfTable', 'MfInput', 'MfSelect', 'MfTag', 'MfButton', 'MfTooltip', 'MfAlert']
const components = Object.fromEntries(await Promise.all(names.map(async name => [name, await component(name)])))

function mountIndexStep(rows, manualRows = rows, callbacks = {}) {
    const state = reactive({
        visible: true, loading: false, saving: false, steps: [], currentStep: 3,
        formModel: {}, indexColumns, visibleIndexRows: rows, indexRows: manualRows,
        columnOptions: [{ label: 'id', value: 'id' }, { label: 'email', value: 'email' }],
        methodOptions: [{ label: 'BTREE', value: 'BTREE' }, { label: 'HASH', value: 'HASH' }],
        indexTabs: [], indexTab: 'primary', indexCounts: {}, canAddIndex: false,
        onAddIndex() {}, onPrev() {}, onCancel() {}, onNext() {},
        onDeleteIndexRow() {}, ...callbacks,
    })
    return mountHost(h(defineComponent({ setup: () => state, render })), undefined, app => {
        app.config.globalProperties.$t = key => key
        for (const [name, implementation] of Object.entries(components)) app.component(name, implementation)
        for (const name of ['Check', 'ArrowRight', 'Plus', 'ArrowUp', 'ArrowDown', 'Trash2', 'Copy', 'Scan', 'Download', 'SqlCodeBlock', 'MfCheckbox', 'MfTextarea', 'MfSpinner']) {
            app.component(name, { render: () => h('span') })
        }
    })
}

const row = (kind, extra = {}) => ({
    __key: 'idx_1', kind, name: kind === 'primary' ? 'PRIMARY' : 'idx_name',
    columns: ['id'], method: 'BTREE', comment: '', ...extra,
})
const action = (host, key) => host.find(node => node.tag === 'button' && node.textContent.trim() === key)
const rowActions = host => host.all(node => {
    if (node.tag !== 'button') return false
    for (let parent = node.parent; parent; parent = parent.parent) {
        if (parent.tag === 'tbody') return ['common.edit', 'common.confirm', 'common.cancel', 'common.delete'].includes(node.textContent.trim())
    }
    return false
}).map(node => node.textContent.trim())

test('primary and derived unique indexes render records and only offer delete', async () => {
    for (const record of [row('primary'), row('unique', { __key: '__auto_email', name: 'uk_email', columns: ['email'] })]) {
        const removed = []
        const host = mountIndexStep([record], [], { onDeleteIndexRow: value => removed.push(value) })
        try {
            const cells = host.all(node => node.tag === 'td')
            assert.equal(cells[0].textContent, '1')
            assert.equal(cells[1].textContent, record.name)
            assert.equal(cells[3].textContent, record.columns.join(', '))
            assert.deepEqual(rowActions(host), ['common.delete'])
            await click(host.document, action(host, 'common.delete'))
            assert.deepEqual(removed, [record])
        }
        finally { host.unmount() }
    }
})

test('manual indexes show live editors and a field selector with only a delete action', async () => {
    for (const kind of ['normal', 'unique']) {
        const record = reactive(row(kind))
        const removed = []
        const host = mountIndexStep([record], [record], { onDeleteIndexRow: value => removed.push(value) })
        try {
            assert.deepEqual(rowActions(host), ['common.delete'])
            const nameInput = host.find(node => node.tag === 'input' && node.value === 'idx_name')
            assert.ok(nameInput)
            const cells = host.all(node => node.tag === 'td')
            assert.ok(cells[3].textContent.includes('id'))
            assert.ok(!cells[3].children.some(node => node.tag === 'input'))
            nameInput.props.onInput({ target: { value: 'idx_live' } })
            await nextTick()
            assert.equal(record.name, 'idx_live')
            await click(host.document, action(host, 'common.delete'))
            assert.deepEqual(removed, [record])
        }
        finally { host.unmount() }
    }
})
