import assert from 'node:assert/strict'
import test from 'node:test'
import { h, nextTick, ref } from 'vue'
import { component } from './component.mjs'
import { click, InteractionEvent, mountHost } from './interaction-host.mjs'

const Toolbar = await component('MfTableToolbar')

test('more filters toggle in place and retain entered values without submitting', async () => {
    const value = ref('saved code')
    let queries = 0
    const host = mountHost(h(Toolbar, { onSearch: () => queries++ }, {
        filter: () => h('input', { 'data-basic': true }),
        'advanced-filter': () => h('input', { value: value.value, 'data-extra': true }),
    }))
    try {
        const toggle = () => host.find(node => node.tag === 'button' && node.props['aria-expanded'] !== undefined)
        const field = host.find(node => node.props['data-extra'])
        assert.equal(toggle().props['aria-expanded'], false)
        assert.equal(toggle().props.type, 'button')
        assert.equal(field.parent.style.display, 'none')
        await click(host.document, toggle())
        assert.equal(toggle().props['aria-expanded'], true)
        assert.equal(field.parent.style.display, '')
        value.value = 'new code'
        await nextTick()
        await click(host.document, toggle())
        await click(host.document, toggle())
        assert.equal(host.find(node => node.props['data-extra']), field)
        assert.equal(field.value, 'new code')
        assert.equal(queries, 0)
    }
    finally { host.unmount() }
})

test('form submission and reset emit their own action exactly once', async () => {
    let searches = 0
    let resets = 0
    const host = mountHost(h(Toolbar, { onSearch: () => searches++, onReset: () => resets++ }, {
        filter: () => h('input'),
    }))
    try {
        const form = host.find(node => node.tag === 'form')
        const event = new InteractionEvent('submit', [form])
        form.props.onSubmit(event)
        assert.equal(event.defaultPrevented, true)
        assert.equal(searches, 1)
        const reset = host.find(node => node.tag === 'button' && node.textContent.trim() === 'Reset')
        assert.equal(reset.props.type, 'button')
        await click(host.document, reset)
        assert.equal(searches, 1)
        assert.equal(resets, 1)
        assert.ok(!host.find(node => node.props['aria-expanded'] !== undefined))
    }
    finally { host.unmount() }
})

test('loading prevents repeated search and reset while tools and business actions stay available', () => {
    const host = mountHost(h(Toolbar, { loading: true }, {
        filter: () => h('input'),
        actions: () => h('button', { 'data-create': true }, 'Create'),
        tools: () => h('button', { 'data-columns': true }, 'Columns'),
    }))
    try {
        assert.equal(host.find(node => node.tag === 'button' && node.props.type === 'submit').disabled, true)
        assert.equal(host.find(node => node.tag === 'button' && node.textContent.trim() === 'Reset').disabled, true)
        assert.ok(host.find(node => node.props['data-create']))
        assert.ok(host.find(node => node.props['data-columns']))
    }
    finally { host.unmount() }
})
