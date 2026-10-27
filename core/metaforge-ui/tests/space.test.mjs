import assert from 'node:assert/strict'
import test from 'node:test'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { component } from './component.mjs'

const Space = await component('MfSpace')

function renderSpace(props = {}) {
    return renderToString(createSSRApp({ render: () => h(Space, props, () => [h('span', 'First'), h('span', 'Second')]) }))
}

test('numeric size creates the requested pixel gap', async () => {
    assert.match(await renderSpace({ size: 16 }), /style="[^"]*gap:16px/)
})

test('zero is accepted as an explicit gap', async () => {
    assert.match(await renderSpace({ size: 0 }), /style="[^"]*gap:0px/)
})

test('wrap false keeps horizontal content on one line', async () => {
    const html = await renderSpace({ wrap: false })
    assert.ok(html.includes('flex-nowrap'))
    assert.ok(!html.includes('flex-wrap'))
})

test('default horizontal content can still wrap', async () => {
    assert.ok((await renderSpace()).includes('flex-wrap'))
})

test('numeric spacing also applies to vertical content', async () => {
    const html = await renderSpace({ direction: 'vertical', size: 16, fill: true })
    assert.ok(html.includes('flex-col'))
    assert.ok(html.includes('w-full'))
    assert.match(html, /gap:16px/)
})
