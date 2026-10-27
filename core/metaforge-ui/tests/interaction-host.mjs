import { createRenderer, nextTick } from 'vue'

export class TestDocument extends EventTarget {
    listeners = new Map()
    constructor() {
        super()
        this.defaultView = Object.assign(new EventTarget(), { innerHeight: 900, innerWidth: 1280 })
        this.body = new HostNode('body', this)
    }
    addEventListener(type, listener, options) {
        if (!this.listeners.has(type)) this.listeners.set(type, new Set())
        this.listeners.get(type).add(listener)
        super.addEventListener(type, listener, options)
    }
    removeEventListener(type, listener, options) {
        this.listeners.get(type)?.delete(listener)
        super.removeEventListener(type, listener, options)
    }
    count(type) { return this.listeners.get(type)?.size || 0 }
}

export class InteractionEvent extends Event {
    propagationStopped = false
    immediateStopped = false
    constructor(type, path, options = {}) {
        super(type, { bubbles: true, cancelable: true })
        this.path = path
        Object.assign(this, options)
    }
    get target() { return this.path[0] }
    composedPath() { return this.path }
    stopPropagation() { super.stopPropagation(); this.propagationStopped = true }
    stopImmediatePropagation() { super.stopImmediatePropagation(); this.immediateStopped = true; this.propagationStopped = true }
}

class HostNode extends EventTarget {
    constructor(tag, document, text = '') {
        super()
        this.tag = tag
        this.tagName = tag.toUpperCase()
        this.ownerDocument = document
        this.children = []
        this.props = {}
        this.style = { display: '' }
        this.text = text
        this.parent = null
    }
    contains(node) {
        for (let current = node; current; current = current.parent) if (current === this) return true
        return false
    }
    get textContent() { return this.tag === '#comment' ? '' : this.text + this.children.map(child => child.textContent).join('') }
    getBoundingClientRect() { return { top: 0, left: 0, bottom: 36, right: 200, width: 200, height: 36 } }
    focus() { this.ownerDocument.activeElement = this }
}

function remove(node) {
    if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
    node.parent = null
}

export function mountHost(vnode, document = new TestDocument(), configure = () => {}) {
    const previousDocument = globalThis.document
    const previousWindow = globalThis.window
    globalThis.document = document
    globalThis.window = document.defaultView
    const renderer = createRenderer({
        querySelector: selector => selector === 'body' ? document.body : null,
        createElement: tag => new HostNode(tag, document),
        createText: text => new HostNode('#text', document, text),
        createComment: text => new HostNode('#comment', document, text),
        insert(node, parent, anchor = null) {
            remove(node)
            node.parent = parent
            const index = anchor ? parent.children.indexOf(anchor) : -1
            parent.children.splice(index < 0 ? parent.children.length : index, 0, node)
        },
        remove,
        setText: (node, text) => { node.text = text },
        setElementText(node, text) { node.text = text; node.children.forEach(child => { child.parent = null }); node.children = [] },
        parentNode: node => node.parent,
        nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1] || null,
        patchProp(node, key, _previous, value) {
            node.props[key] = value
            if (['value', 'type', 'disabled'].includes(key)) node[key] = value
            if (key === 'style' && value && typeof value === 'object') Object.assign(node.style, value)
        },
    })
    const container = new HostNode('test-root', document)
    const app = renderer.createApp({ render: () => typeof vnode === 'function' ? vnode() : vnode })
    configure(app)
    app.mount(container)
    function nodes() {
        const result = []
        const visit = node => { result.push(node); node.children.forEach(visit) }
        visit(container)
        visit(document.body)
        return result
    }
    return {
        document,
        container,
        find: predicate => nodes().find(predicate),
        all: predicate => nodes().filter(predicate),
        unmount() {
            try { app.unmount() }
            finally {
                globalThis.document = previousDocument
                globalThis.window = previousWindow
            }
        },
    }
}

function pathTo(node, document) {
    const path = []
    for (let current = node; current; current = current.parent) path.push(current)
    return [...path, document]
}

function invoke(handler, event) {
    for (const callback of Array.isArray(handler) ? handler : [handler]) {
        if (event.immediateStopped) break
        callback?.(event)
    }
}

export function pointerDown(document, node) {
    const event = new InteractionEvent('pointerdown', pathTo(node, document))
    document.dispatchEvent(event)
    return event
}

export async function click(document, node, type = 'click') {
    pointerDown(document, node)
    const event = new InteractionEvent(type, pathTo(node, document))
    const property = type === 'auxclick' ? 'onAuxclick' : 'onClick'
    for (const current of [...event.path].reverse()) {
        invoke(current.props?.[`${property}Capture`], event)
        if (event.propagationStopped) break
    }
    if (!event.propagationStopped) {
        for (const current of event.path) {
            invoke(current.props?.[property], event)
            if (event.propagationStopped) break
        }
    }
    await nextTick()
    return event
}

export async function keydown(document, node, key) {
    const event = new InteractionEvent('keydown', pathTo(node, document), { key })
    document.dispatchEvent(event)
    if (!event.propagationStopped) {
        for (const current of event.path) {
            invoke(current.props?.onKeydown, event)
            if (event.propagationStopped) break
        }
    }
    await nextTick()
    return event
}
