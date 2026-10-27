import assert from 'node:assert/strict'
import test from 'node:test'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { component } from './component.mjs'

const Menu = await component('MfMenu')
const MenuItem = await component('MfMenuItem')
const items = ['/home', '/admin/dashboard', '/admin/dataset', '/store', '/admin/query']

async function renderMenu(selectedKeys = [], mode = 'horizontal') {
    const app = createSSRApp({
        render: () => h(Menu, { selectedKeys, mode }, {
            default: () => items.map(key => h(MenuItem, { key, key_: key }, () => key)),
        }),
    })
    const html = await renderToString(app)
    const entries = [...html.matchAll(/<li\b[^>]*>/g)].map(([tag]) => ({
        classes: (tag.match(/class="([^"]*)"/)?.[1] || '').split(/\s+/),
        selected: tag.includes('aria-selected="true"'),
    }))
    const classes = (html.match(/<ul\b[^>]*class="([^"]*)"/)?.[1] || '').split(/\s+/)
    return { classes, entries }
}

test('only the selected item receives the active background and text', async () => {
    const { entries } = await renderMenu(['/home'])
    assert.equal(entries.length, items.length)
    for (const [index, entry] of entries.entries()) {
        assert.equal(entry.selected, index === 0)
        assert.equal(entry.classes.includes('bg-surface-active'), entry.selected)
        assert.equal(entry.classes.includes('text-primary'), entry.selected)
        assert.equal(entry.classes.includes('hover:bg-surface-hover'), !entry.selected)
    }
})

test('menus with no selected keys keep every item neutral', async () => {
    const { entries } = await renderMenu()
    assert.ok(entries.every(entry => !entry.selected && !entry.classes.includes('bg-surface-active')))
})

test('changing selection removes the previous highlight', async () => {
    for (const key of items) {
        const { entries } = await renderMenu([key])
        assert.deepEqual(entries.map(entry => entry.classes.includes('bg-surface-active')), items.map(item => item === key))
    }
})

test('root menu direction follows horizontal, vertical and inline modes', async () => {
    for (const mode of ['horizontal', 'vertical', 'inline']) {
        const { classes } = await renderMenu([], mode)
        assert.equal(classes.includes('flex-row'), mode === 'horizontal')
        assert.equal(classes.includes('flex-col'), mode !== 'horizontal')
    }
})

test('vertical and inline menu items span the menu width', async () => {
    for (const mode of ['horizontal', 'vertical', 'inline']) {
        const { entries } = await renderMenu([], mode)
        assert.ok(entries.every(entry => entry.classes.includes('w-full') === (mode !== 'horizontal')))
    }
})

test('an item without a menu context is not highlighted', async () => {
    const html = await renderToString(createSSRApp({ render: () => h(MenuItem, {}, () => 'Action') }))
    assert.ok(html.includes('aria-selected="false"'))
    assert.ok(!html.includes('bg-surface-active'))
})

test('navigation items preserve native link destinations and new-tab attributes', async () => {
    const html = await renderToString(createSSRApp({
        render: () => h(MenuItem, {}, () => h('a', { href: '/destination', target: '_blank', rel: 'noopener' }, 'Destination')),
    }))
    assert.ok(html.includes('href="/destination"'))
    assert.ok(html.includes('target="_blank"'))
    assert.ok(html.includes('rel="noopener"'))
})

test('disabled menu items expose their disabled state to assistive technology', async () => {
    const html = await renderToString(createSSRApp({
        render: () => h(MenuItem, { disabled: true }, () => h('a', { href: '/disabled' }, 'Disabled')),
    }))
    assert.ok(html.includes('aria-disabled="true"'))
})
