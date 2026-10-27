import assert from 'node:assert/strict'
import test from 'node:test'
import { h, nextTick, ref } from 'vue'
import { component, sourceModule } from './component.mjs'
import { click, InteractionEvent, keydown, mountHost, pointerDown, TestDocument } from './interaction-host.mjs'

const Dropdown = await component('MfDropdown')
const Popover = await component('MfPopover')
const Select = await component('MfSelect')
const Menu = await component('MfMenu')
const MenuItem = await component('MfMenuItem')
const SubMenu = await component('MfSubMenu')
const { useDismissableLayer } = await sourceModule('../src/composables/useDismissableLayer.ts')

test('click dropdown closes outside and unregisters on unmount', async () => {
    const host = mountHost(h(Dropdown, { trigger: 'click' }, {
        default: () => h('button', 'Open'),
        overlay: () => h('div', { 'data-panel': 'dropdown' }, 'Choices'),
    }))
    try {
        await click(host.document, host.find(node => node.tag === 'button'))
        assert.ok(host.find(node => node.props['data-panel'] === 'dropdown'))
        pointerDown(host.document, host.container)
        await nextTick()
        assert.ok(!host.find(node => node.props['data-panel'] === 'dropdown'))
        assert.equal(host.document.count('pointerdown'), 0)
        await click(host.document, host.find(node => node.tag === 'button'))
        assert.equal(host.document.count('keydown'), 1)
    }
    finally { host.unmount() }
    assert.equal(host.document.count('keydown'), 0)
})

test('an outside control that stops click propagation still dismisses the dropdown and acts once', async () => {
    let actions = 0
    const host = mountHost(h('div', [
        h(Dropdown, { trigger: 'click' }, {
            default: () => h('button', { 'data-trigger': true }, 'Open'),
            overlay: () => h('div', { 'data-panel': true }, 'Choices'),
        }),
        h('button', { 'data-outside': true, onClick: event => { event.stopPropagation(); actions++ } }, 'Outside'),
    ]))
    try {
        await click(host.document, host.find(node => node.props['data-trigger']))
        assert.ok(host.find(node => node.props['data-panel']))
        await click(host.document, host.find(node => node.props['data-outside']))
        assert.ok(!host.find(node => node.props['data-panel']))
        assert.equal(actions, 1)
    }
    finally { host.unmount() }
})

test('a conditionally rendered context menu can dismiss and reopen without leaking listeners', async () => {
    const document = new TestDocument()
    const previousDocument = globalThis.document
    globalThis.document = document
    const visible = ref(false)
    const ContextMenu = {
        setup() {
            const root = ref()
            useDismissableLayer(root, visible, () => { visible.value = false })
            return () => visible.value ? h('div', { ref: root, 'data-panel': true }, 'Menu') : null
        },
    }
    const host = mountHost(h(ContextMenu), document)
    try {
        visible.value = true
        await nextTick()
        pointerDown(document, host.find(node => node.props['data-panel']))
        assert.equal(visible.value, true)
        await keydown(document, host.container, 'Escape')
        assert.equal(visible.value, false)
        visible.value = true
        await nextTick()
        assert.equal(document.count('pointerdown'), 1)
        pointerDown(document, host.container)
        await nextTick()
        assert.equal(visible.value, false)
        assert.equal(document.count('pointerdown'), 0)
    }
    finally { host.unmount(); globalThis.document = previousDocument }
})

test('click popover keeps inside interactions open but closes outside or on Escape', async () => {
    const host = mountHost(h(Popover, { trigger: 'click' }, {
        default: () => h('button', { 'data-trigger': true }, 'Open'),
        content: () => h('button', { 'data-inside': true }, 'Inside'),
    }))
    try {
        await click(host.document, host.find(node => node.props['data-trigger']))
        await click(host.document, host.find(node => node.props['data-inside']))
        assert.ok(host.find(node => node.props.role === 'tooltip'))
        await keydown(host.document, host.find(node => node.props['data-inside']), 'Escape')
        assert.ok(!host.find(node => node.props.role === 'tooltip'))
        await click(host.document, host.find(node => node.props['data-trigger']))
        pointerDown(host.document, host.container)
        await nextTick()
        assert.ok(!host.find(node => node.props.role === 'tooltip'))
    }
    finally { host.unmount() }
})

test('Select closes outside without changing the selection', async () => {
    const model = ref('one')
    const host = mountHost(() => h(Select, {
        modelValue: model.value,
        options: [{ label: 'One', value: 'one' }, { label: 'Two', value: 'two' }],
        'onUpdate:modelValue': value => { model.value = value },
    }))
    try {
        await click(host.document, host.find(node => node.props.class?.includes('mf-invalidable')))
        assert.ok(host.find(node => node.props.role === 'listbox'))
        pointerDown(host.document, host.container)
        await nextTick()
        assert.ok(!host.find(node => node.props.role === 'listbox'))
        assert.equal(model.value, 'one')
    }
    finally { host.unmount() }
})

test('Escape closes a nested Select before reaching its drawer-like parent', async () => {
    let parentEscapes = 0
    const host = mountHost(h('div', { onKeydown: event => { if (event.key === 'Escape') parentEscapes++ } }, [
        h(Select, { options: [{ label: 'One', value: 'one' }] }),
    ]))
    try {
        const trigger = host.find(node => node.props.class?.includes('mf-invalidable'))
        await click(host.document, trigger)
        const first = await keydown(host.document, trigger, 'Escape')
        assert.ok(!host.find(node => node.props.role === 'listbox'))
        assert.equal(first.defaultPrevented, true)
        assert.equal(parentEscapes, 0)
        await keydown(host.document, trigger, 'Escape')
        assert.equal(parentEscapes, 1)
    }
    finally { host.unmount() }
})

test('Select search is reset when dismissed and can be reopened', async () => {
    const originalFrame = globalThis.requestAnimationFrame
    globalThis.requestAnimationFrame = callback => { queueMicrotask(() => callback(0)); return 1 }
    const host = mountHost(h(Select, { searchable: true, options: [{ label: 'One', value: 'one' }, { label: 'Two', value: 'two' }] }))
    try {
        await click(host.document, host.find(node => node.props.class?.includes('mf-invalidable')))
        const input = host.find(node => node.tag === 'input')
        input.value = 'tw'
        input.dispatchEvent(new InteractionEvent('input', [input]))
        await nextTick()
        assert.equal(host.all(node => node.props.role === 'option').length, 1)
        await keydown(host.document, input, 'Escape')
        await click(host.document, host.find(node => node.props.class?.includes('mf-invalidable')))
        assert.equal(host.find(node => node.tag === 'input').value, '')
        assert.equal(host.all(node => node.props.role === 'option').length, 2)
    }
    finally { host.unmount(); globalThis.requestAnimationFrame = originalFrame }
})

test('horizontal submenu closes outside while inline expansion stays open', async () => {
    const mode = ref('horizontal')
    const host = mountHost(() => h(Menu, { mode: mode.value }, {
        default: () => h(SubMenu, { key_: 'tools', title: 'Tools' }, () => h(MenuItem, { key_: 'leaf' }, () => 'Leaf')),
    }))
    try {
        const title = () => host.find(node => node.tag === 'div' && node.props.onClick)
        await click(host.document, title())
        assert.equal(host.find(node => node.props.class?.includes('absolute')).style.display, '')
        pointerDown(host.document, host.container)
        await nextTick()
        assert.equal(host.find(node => node.props.class?.includes('absolute')).style.display, 'none')
        mode.value = 'inline'
        await nextTick()
        await click(host.document, title())
        const children = host.find(node => node.props.class?.includes('border-l'))
        assert.equal(children.style.display, '')
        pointerDown(host.document, host.container)
        await nextTick()
        assert.equal(children.style.display, '')
        assert.equal(host.document.count('pointerdown'), 0)
    }
    finally { host.unmount() }
})

test('menu activation reaches the link and emits only once', async () => {
    let linkClicks = 0
    let activations = 0
    const host = mountHost(h(Menu, { onClick: () => activations++ }, {
        default: () => h(MenuItem, { key_: 'entry' }, () => h('a', { href: '/entry', onClick: () => linkClicks++ }, 'Entry')),
    }))
    try {
        await click(host.document, host.find(node => node.tag === 'a'))
        assert.equal(linkClicks, 1)
        assert.equal(activations, 1)
    }
    finally { host.unmount() }
})

test('disabled menu items block links, parent actions and auxiliary clicks', async () => {
    let actions = 0
    const host = mountHost(h(Menu, { onClick: () => actions++ }, {
        default: () => h(MenuItem, { key_: 'entry', disabled: true, onClick: () => actions++ }, () => h('a', { href: '/entry', onClick: () => actions++ }, 'Entry')),
    }))
    try {
        const link = host.find(node => node.tag === 'a')
        assert.equal((await click(host.document, link)).defaultPrevented, true)
        assert.equal((await click(host.document, link, 'auxclick')).defaultPrevented, true)
        assert.equal((await click(host.document, host.find(node => node.tag === 'li'))).defaultPrevented, true)
        assert.equal(actions, 0)
    }
    finally { host.unmount() }
})
