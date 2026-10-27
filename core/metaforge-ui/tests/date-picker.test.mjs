import assert from 'node:assert/strict'
import test from 'node:test'
import { h, nextTick, ref } from 'vue'
import { component, sourceModule } from './component.mjs'
import { click, keydown, mountHost, pointerDown } from './interaction-host.mjs'

const Picker = await component('MfDatePicker')
const { dateKey, monthDays, moveMonth, parseDate } = await sourceModule('../src/components/calendar.ts')

test('calendar uses valid local dates, Monday weeks and leap-year month boundaries', () => {
    assert.equal(dateKey(parseDate('2024-02-29')), '2024-02-29')
    for (const invalid of ['2023-02-29', '2026-13-01', '2026-04-31', 'bad']) assert.equal(parseDate(invalid), undefined)
    const days = monthDays(parseDate('2024-02-15'))
    assert.equal(days.length, 42)
    assert.equal(days[0].getDay(), 1)
    assert.equal(dateKey(moveMonth(parseDate('2024-01-31'), 1)), '2024-02-29')
    assert.equal(dateKey(moveMonth(parseDate('2026-01-31'), -1)), '2025-12-31')
})

test('custom calendar selects, clears, and restores trigger focus without a native date input', async () => {
    const value = ref('2026-02-10')
    const host = mountHost(() => h(Picker, { modelValue: value.value, 'onUpdate:modelValue': date => { value.value = date } }))
    try {
        const trigger = () => host.find(node => node.tag === 'button' && node.props['aria-haspopup'] === 'dialog')
        await click(host.document, trigger())
        assert.ok(host.find(node => node.props.role === 'grid'))
        assert.ok(!host.find(node => node.tag === 'input'))
        await click(host.document, host.find(node => node.props['data-date'] === '2026-02-15'))
        assert.equal(value.value, '2026-02-15')
        assert.ok(!host.find(node => node.props.role === 'dialog'))
        assert.equal(host.document.activeElement, trigger())
        await click(host.document, trigger())
        await click(host.document, host.find(node => node.tag === 'button' && node.textContent.trim() === 'Clear'))
        assert.equal(value.value, '')
    }
    finally { host.unmount() }
})

test('calendar respects date limits and supports arrows, Escape and outside dismissal', async () => {
    const host = mountHost(h(Picker, { modelValue: '2026-02-10', min: '2026-02-10', max: '2026-02-20' }))
    try {
        const trigger = () => host.find(node => node.props['aria-haspopup'] === 'dialog')
        await click(host.document, trigger())
        assert.equal(host.find(node => node.props['data-date'] === '2026-02-09').disabled, true)
        await keydown(host.document, host.find(node => node.props['data-date'] === '2026-02-10'), 'ArrowRight')
        await nextTick()
        assert.equal(host.document.activeElement.props['data-date'], '2026-02-11')
        await keydown(host.document, host.document.activeElement, 'Escape')
        assert.ok(!host.find(node => node.props.role === 'dialog'))
        assert.equal(host.document.activeElement, trigger())
        await click(host.document, trigger())
        pointerDown(host.document, host.container)
        await nextTick()
        assert.ok(!host.find(node => node.props.role === 'dialog'))
    }
    finally { host.unmount() }
    assert.equal(host.document.count('pointerdown'), 0)
    assert.equal(host.document.count('scroll'), 0)
})
