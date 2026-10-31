import assert from 'node:assert/strict'
import test from 'node:test'
import { h, nextTick, ref } from 'vue'
import { component } from './component.mjs'
import { mountHost } from './interaction-host.mjs'
const Tabs = await component('MfTabs')
const Pane = await component('MfTabPane')

test('top tabs stay horizontal above their content and update async counts', async () => {
  const count = ref(0)
  const host = mountHost(h({ setup: () => () => h(Tabs, { activeKey: 'indexes' }, () => h(Pane, { itemKey: 'indexes', tab: `Indexes (${count.value})` }, () => 'Contents')) }))
  try {
    await nextTick()
    const list = host.find(node => node.props.role === 'tablist')
    assert.match(list.props.class, /flex-row/)
    assert.match(list.props.class, /order-1/)
    assert.ok(host.all(node => String(node.props.class).includes('min-w-0 flex-1')).some(node => node.props.class.includes('order-2')))
    count.value = 4
    await nextTick()
    assert.ok(host.all(node => node.text.includes('Indexes (4)')).length)
  } finally { host.unmount() }
})
