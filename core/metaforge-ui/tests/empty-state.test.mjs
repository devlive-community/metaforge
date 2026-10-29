import assert from 'node:assert/strict'
import test from 'node:test'
import { h, nextTick, ref } from 'vue'
import { component } from './component.mjs'
import { click, InteractionEvent, mountHost } from './interaction-host.mjs'

const Table = await component('MfTable')
const Select = await component('MfSelect')

test('table distinguishes pending, empty and populated results without displaying empty during loading', async () => {
    const loading = ref(false)
    const rows = ref([])
    const host = mountHost(() => h(Table, { loading: loading.value, dataSource: rows.value, emptyText: '暂无数据' }))
    try {
        assert.ok(host.container.textContent.includes('暂无数据'))
        assert.equal(host.find(node => node.tag === 'td').props.colspan, 1)
        loading.value = true
        await nextTick()
        assert.ok(!host.container.textContent.includes('暂无数据'))
        assert.ok(host.find(node => node.props['aria-busy'] === true))
        rows.value = [{ id: 1 }]
        loading.value = false
        await nextTick()
        assert.ok(!host.container.textContent.includes('暂无数据'))
        assert.ok(host.find(node => node.tag === 'tr' && node.props.onClick))
    }
    finally { host.unmount() }
})

test('table preserves a custom empty slot and its action', async () => {
    let calls = 0
    const host = mountHost(h(Table, {}, { emptyText: () => h('button', { onClick: () => calls++ }, 'Create source') }))
    try {
        assert.ok(!host.container.textContent.includes('No data'))
        await click(host.document, host.find(node => node.tag === 'button'))
        assert.equal(calls, 1)
    }
    finally { host.unmount() }
})

test('Select suppresses empty while pending and distinguishes missing options from unmatched search', async () => {
    const originalFrame = globalThis.requestAnimationFrame
    globalThis.requestAnimationFrame = callback => { queueMicrotask(() => callback(0)); return 1 }
    const loading = ref(true)
    const options = ref([])
    const host = mountHost(() => h(Select, {
        searchable: true, loading: loading.value, options: options.value,
        emptyText: '暂无选项', noMatchText: '没有匹配选项', loadingText: '正在获取选项',
    }))
    try {
        await click(host.document, host.find(node => node.props.class?.includes('mf-invalidable')))
        // The teleported popup mounts to body, outside the app container.
        const popupText = () => host.document.body.textContent
        assert.ok(popupText().includes('正在获取选项'))
        assert.ok(!popupText().includes('暂无选项'))
        loading.value = false
        await nextTick()
        assert.ok(popupText().includes('暂无选项'))
        options.value = [{ label: 'MySQL', value: 'mysql' }]
        await nextTick()
        const input = host.find(node => node.tag === 'input')
        input.value = 'Oracle'
        input.dispatchEvent(new InteractionEvent('input', [input]))
        await nextTick()
        assert.ok(popupText().includes('没有匹配选项'))
        assert.ok(!popupText().includes('暂无选项'))
    }
    finally { host.unmount(); globalThis.requestAnimationFrame = originalFrame }
})
