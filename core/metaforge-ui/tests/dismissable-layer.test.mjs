import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from './component.mjs'
import { InteractionEvent, TestDocument } from './interaction-host.mjs'

const { registerDismissableLayer } = await sourceModule('../src/composables/useDismissableLayer.ts')

function layer(document, root, close) {
    return registerDismissableLayer(document, { contains: event => event.composedPath().includes(root), dismiss: close })
}

test('outside pointerdown dismisses a layer without preventing the clicked control', () => {
    const doc = new TestDocument()
    let dismissed = 0
    let otherHandler = 0
    layer(doc, {}, () => dismissed++)
    doc.addEventListener('pointerdown', () => otherHandler++)
    const event = new InteractionEvent('pointerdown', [{}, doc], { pointerType: 'touch' })
    doc.dispatchEvent(event)
    assert.equal(dismissed, 1)
    assert.equal(otherHandler, 1)
    assert.equal(event.defaultPrevented, false)
    assert.equal(event.propagationStopped, false)
    assert.equal(doc.count('keydown'), 0)
})

test('inside pointerdown includes composed paths and leaves the layer open', () => {
    const doc = new TestDocument()
    const root = {}
    let dismissed = 0
    const unregister = layer(doc, root, () => dismissed++)
    doc.dispatchEvent(new InteractionEvent('pointerdown', [{}, root, doc]))
    assert.equal(dismissed, 0)
    unregister()
})

test('Escape closes the topmost layer first and blocks parent dismissal', () => {
    const doc = new TestDocument()
    const order = []
    layer(doc, {}, () => order.push('parent'))
    layer(doc, {}, () => order.push('child'))
    const event = new InteractionEvent('keydown', [doc], { key: 'Escape' })
    doc.dispatchEvent(event)
    assert.deepEqual(order, ['child'])
    assert.equal(event.defaultPrevented, true)
    assert.equal(event.propagationStopped, true)
    assert.equal(doc.count('keydown'), 1)
    doc.dispatchEvent(new InteractionEvent('keydown', [doc], { key: 'Escape' }))
    assert.deepEqual(order, ['child', 'parent'])
    assert.equal(doc.count('keydown'), 0)
})

test('clicking a parent popup outside its child closes only the child', () => {
    const doc = new TestDocument()
    const parent = {}
    const closed = []
    const unregister = layer(doc, parent, () => closed.push('parent'))
    layer(doc, {}, () => closed.push('child'))
    doc.dispatchEvent(new InteractionEvent('pointerdown', [parent, doc]))
    assert.deepEqual(closed, ['child'])
    unregister()
})

test('clicking outside nested layers closes all of them from the inside out', () => {
    const doc = new TestDocument()
    const order = []
    layer(doc, {}, () => order.push('parent'))
    layer(doc, {}, () => order.push('child'))
    doc.dispatchEvent(new InteractionEvent('pointerdown', [{}, doc]))
    assert.deepEqual(order, ['child', 'parent'])
    assert.equal(doc.count('pointerdown'), 0)
})

test('unregister removes listeners and stale cleanup cannot remove a reopened layer', () => {
    const doc = new TestDocument()
    let closed = 0
    const old = layer(doc, {}, () => closed++)
    old()
    assert.equal(doc.count('pointerdown'), 0)
    const current = layer(doc, {}, () => closed++)
    old()
    assert.equal(doc.count('pointerdown'), 1)
    doc.dispatchEvent(new InteractionEvent('pointerdown', [{}, doc]))
    assert.equal(closed, 1)
    current()
})

test('separate documents have separate layer stacks', () => {
    const first = new TestDocument()
    const second = new TestDocument()
    const closed = []
    layer(first, {}, () => closed.push('first'))
    const unregister = layer(second, {}, () => closed.push('second'))
    first.dispatchEvent(new InteractionEvent('pointerdown', [{}, first]))
    assert.deepEqual(closed, ['first'])
    unregister()
})

test('unrelated and already-handled keys do not dismiss popups', () => {
    const doc = new TestDocument()
    let closed = 0
    const unregister = layer(doc, {}, () => closed++)
    doc.dispatchEvent(new InteractionEvent('keydown', [doc], { key: 'Enter' }))
    const event = new InteractionEvent('keydown', [doc], { key: 'Escape' })
    event.preventDefault()
    doc.dispatchEvent(event)
    assert.equal(closed, 0)
    unregister()
})
