import assert from 'node:assert/strict'
import test from 'node:test'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { component } from './component.mjs'

const Badge = await component('MfBadge')
const render = (node) => renderToString(createSSRApp({ render: () => node }))
const badgeOf = html => html.match(/<span class="([^"]*rounded-mf-full[^"]*)"( style="([^"]*)")?/)

test('a badge wrapping content sits on its top-right corner instead of beside it', async () => {
    const html = await render(h(Badge, { count: 3, offset: [8, 8] }, () => h('button', 'bell')))
    const [, classes, , style] = badgeOf(html)
    assert.match(classes, /(^|\s)absolute(\s|$)/)
    assert.match(classes, /translate-x-1\/2/)
    assert.match(classes, /-translate-y-1\/2/)
    assert.match(style, /right:\s*8px/)
    assert.match(style, /top:\s*8px/)
    assert.match(html, />3</)
})

test('a standalone badge stays inline, and counts cap at max', async () => {
    const [, classes, , style] = badgeOf(await render(h(Badge, { count: 'New', variant: 'success' })))
    assert.doesNotMatch(classes, /(^|\s)absolute(\s|$)/)
    assert.ok(!style, "no corner position")
    assert.match(await render(h(Badge, { count: 120, max: 99 }, () => h('i'))), />99\+</)
    assert.doesNotMatch(await render(h(Badge, { count: 0 }, () => h('i'))), /rounded-mf-full/)
})
