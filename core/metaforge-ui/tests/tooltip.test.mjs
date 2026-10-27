import assert from 'node:assert/strict'
import test from 'node:test'
import { h, nextTick, ref } from 'vue'
import { component } from './component.mjs'
import { InteractionEvent, keydown, mountHost } from './interaction-host.mjs'

const Tooltip = await component('MfTooltip')
const settle = async () => { await nextTick(); await nextTick() }
const trigger = host => host.find(node => node.props.onMouseenter)
const panel = host => host.find(node => node.props.role === 'tooltip')

test('tooltip text is mounted outside the table scroll container only while shown', async () => {
    const host = mountHost(h('div', { 'data-scroll': true }, [h(Tooltip, { content: 'Assign menus to the system administrator' }, () => h('button', 'Assign'))]))
    try {
        const scroll = host.find(node => node.props['data-scroll'])
        assert.ok(!panel(host))
        trigger(host).props.onMouseenter()
        await settle()
        assert.ok(host.document.body.contains(panel(host)))
        assert.equal(scroll.contains(panel(host)), false)
        assert.ok(panel(host).textContent.includes('Assign menus'))
        trigger(host).props.onMouseleave()
        await settle()
        assert.ok(!panel(host))
        assert.equal(host.document.count('scroll'), 0)
        assert.equal(host.document.count('keydown'), 0)
    }
    finally { host.unmount() }
})

test('tooltip flips vertically and clamps horizontally at viewport edges', async () => {
    for (const placement of ['top', 'bottom']) {
        const host = mountHost(h(Tooltip, { content: 'Long tooltip', placement }, () => h('button', 'Action')))
        try {
            host.document.defaultView.innerWidth = 337
            host.document.defaultView.innerHeight = 257
            host.document.documentElement = { clientWidth: 320, clientHeight: 240 }
            const top = placement === 'top' ? 2 : 200
            trigger(host).getBoundingClientRect = () => ({ left: 280, right: 312, top, bottom: top + 32, width: 32, height: 32 })
            trigger(host).props.onMouseenter()
            await settle()
            const overlay = panel(host)
            const left = Number.parseFloat(overlay.style.left)
            const y = Number.parseFloat(overlay.style.top)
            assert.ok(left >= 8)
            assert.ok(left + overlay.getBoundingClientRect().width <= 312)
            assert.ok(y >= 8 && y + overlay.getBoundingClientRect().height <= 232)
            assert.equal(y, placement === 'top' ? 40 : 158)
        }
        finally { host.unmount() }
    }
})

test('keyboard focus keeps the tooltip open until Escape or focus leaves the trigger', async () => {
    const host = mountHost(h(Tooltip, { content: 'Help' }, () => h('button', 'Action')))
    try {
        const root = trigger(host)
        root.props.onFocusin()
        root.props.onMouseenter()
        await settle()
        root.props.onMouseleave()
        await settle()
        assert.ok(panel(host))
        await keydown(host.document, host.find(node => node.tag === 'button'), 'Escape')
        assert.ok(!panel(host))
        root.props.onFocusout(new InteractionEvent('focusout', [root], { relatedTarget: host.container }))
        root.props.onFocusin()
        await settle()
        assert.ok(panel(host))
        root.props.onFocusout(new InteractionEvent('focusout', [root], { relatedTarget: host.container }))
        await settle()
        assert.ok(!panel(host))
    }
    finally { host.unmount() }
})

test('empty content creates no invisible overflowing panel', async () => {
    const content = ref('')
    const host = mountHost(() => h(Tooltip, { content: content.value }, () => h('button', 'Action')))
    try {
        trigger(host).props.onMouseenter()
        await settle()
        assert.ok(!panel(host))
        assert.equal(host.document.count('scroll'), 0)
        content.value = 'Now available'
        await settle()
        assert.ok(panel(host))
        content.value = ''
        await settle()
        assert.ok(!panel(host))
    }
    finally { host.unmount() }
})

test('size changes and scrolling update position, and unmount removes observers and listeners', async () => {
    const previousObserver = globalThis.ResizeObserver
    const observers = []
    globalThis.ResizeObserver = class {
        constructor(callback) { this.callback = callback; this.targets = []; this.disconnected = false; observers.push(this) }
        observe(target) { this.targets.push(target) }
        disconnect() { this.disconnected = true }
    }
    const host = mountHost(h(Tooltip, { content: 'Resize me' }, () => h('button', 'Action')))
    try {
        trigger(host).props.onMouseenter()
        await settle()
        assert.equal(observers.length, 1)
        assert.equal(observers[0].targets.length, 2)
        host.document.defaultView.innerWidth = 320
        panel(host).getBoundingClientRect = () => ({ left: 0, right: 304, top: 0, bottom: 80, width: 304, height: 80 })
        observers[0].callback()
        await settle()
        assert.equal(panel(host).style.left, '8px')
        trigger(host).getBoundingClientRect = () => ({ left: 20, right: 52, top: 150, bottom: 182, width: 32, height: 32 })
        host.document.dispatchEvent(new Event('scroll'))
        await settle()
        assert.equal(panel(host).style.top, '64px')
    }
    finally { host.unmount(); globalThis.ResizeObserver = previousObserver }
    assert.equal(observers[0].disconnected, true)
    assert.equal(host.document.count('scroll'), 0)
    assert.equal(host.document.count('pointerdown'), 0)
})
