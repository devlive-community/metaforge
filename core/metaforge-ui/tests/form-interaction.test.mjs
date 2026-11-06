import assert from 'node:assert/strict'
import test from 'node:test'
import { defineComponent, h, nextTick, reactive, ref } from 'vue'
import { component } from './component.mjs'
import { mountHost } from './interaction-host.mjs'

const Form = await component('MfForm')
const FormItem = await component('MfFormItem')
const Input = await component('MfInput')

test('async field errors are displayed and cleared independently of other fields', async () => {
    const message = ref('Username is taken')
    const View = defineComponent({ setup: () => () => h(FormItem, { name: 'name', error: message.value }, () => [h(Input), h('p', 'Help below the field')]) })
    const host = mountHost(h(View))
    try {
        assert.equal(host.find(node => node.props.role === 'alert').textContent, 'Username is taken')
        message.value = undefined
        await nextTick()
        assert.equal(host.find(node => node.props.role === 'alert'), undefined)
        assert.ok(host.find(node => node.textContent === 'Help below the field'))
    }
    finally { host.unmount() }
})

test('validation still reads the model and blocks submission after changing field layout', async () => {
    const model = reactive({ name: '' })
    let submitted = 0
    const View = defineComponent({ setup: () => () => h(Form, { model, onFinish: () => submitted++ }, () =>
        h(FormItem, { name: 'name', rules: [{ required: true, message: 'Name required' }] }, () => [h(Input, { modelValue: model.name }), h('p', 'Help')])) })
    const host = mountHost(h(View))
    try {
        await host.find(node => node.tag === 'form').props.onSubmit({ preventDefault() {} })
        await nextTick()
        assert.equal(submitted, 0)
        assert.equal(host.find(node => node.props.role === 'alert').textContent, 'Name required')
        model.name = 'Ready'
        await nextTick()
        await host.find(node => node.tag === 'form').props.onSubmit({ preventDefault() {} })
        assert.equal(submitted, 1)
    }
    finally { host.unmount() }
})
