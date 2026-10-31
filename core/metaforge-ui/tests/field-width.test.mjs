import assert from 'node:assert/strict'
import test from 'node:test'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { component } from './component.mjs'

const Select = await component('MfSelect')
const Button = await component('MfButton')
const Input = await component('MfInput')
const InputNumber = await component('MfInputNumber')
const render = (node) => renderToString(createSSRApp({ render: () => node }))
const rootClass = html => html.match(/^<div[^>]*class="([^"]*)"/)?.[1] || ''

test('a select fills its container unless the caller gives it a width', async () => {
    assert.match(rootClass(await render(h(Select, { options: [] }))), /(^|\s)w-full(\s|$)/)
    const sized = rootClass(await render(h(Select, { options: [], class: 'w-48 shrink-0' })))
    assert.match(sized, /(^|\s)w-48(\s|$)/)
    assert.doesNotMatch(sized, /(^|\s)w-full(\s|$)/)
    assert.doesNotMatch(rootClass(await render(h(Select, { options: [], class: 'md:w-72' }))), /(^|\s)w-full(\s|$)/)
    assert.match(rootClass(await render(h(Select, { options: [], class: 'min-w-0 flex-1' }))), /(^|\s)w-full(\s|$)/)
})

test('inputs follow a width class too', async () => {
    for (const field of [Input, InputNumber]) {
        assert.match(rootClass(await render(h(field))), /(^|\s)w-full(\s|$)/)
        const sized = rootClass(await render(h(field, { class: 'w-20' })))
        assert.match(sized, /(^|\s)w-20(\s|$)/)
        assert.doesNotMatch(sized, /(^|\s)w-full(\s|$)/)
    }
})

test('button labels stay on one line', async () => {
    assert.match(await render(h(Button, null, () => '导出对比结果')), /class="[^"]*whitespace-nowrap/)
})
