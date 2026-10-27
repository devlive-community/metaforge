import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue'

interface DismissableLayer {
    contains: (event: Event) => boolean
    dismiss: () => void
}

type LayerDocument = Pick<Document, 'addEventListener' | 'removeEventListener'>

interface LayerRegistry {
    layers: DismissableLayer[]
    pointerdown: (event: Event) => void
    keydown: (event: Event) => void
}

const registries = new WeakMap<LayerDocument, LayerRegistry>()

function removeLayer(document: LayerDocument, registry: LayerRegistry, layer: DismissableLayer): void {
    const index = registry.layers.indexOf(layer)
    if (index < 0) return
    registry.layers.splice(index, 1)
    if (registry.layers.length === 0) {
        document.removeEventListener('pointerdown', registry.pointerdown, true)
        document.removeEventListener('keydown', registry.keydown, true)
        registries.delete(document)
    }
}

function dismissLayer(document: LayerDocument, registry: LayerRegistry, layer: DismissableLayer): void {
    removeLayer(document, registry, layer)
    layer.dismiss()
}

/** One listener pair per document; Escape dismisses only the topmost popup. */
export function registerDismissableLayer(document: LayerDocument, layer: DismissableLayer): () => void {
    let registry = registries.get(document)
    if (!registry) {
        const created: LayerRegistry = {
            layers: [],
            pointerdown(event) {
                for (const active of [...created.layers].reverse()) {
                    if (created.layers.includes(active) && !active.contains(event)) {
                        dismissLayer(document, created, active)
                    }
                }
            },
            keydown(event) {
                if ((event as KeyboardEvent).key !== 'Escape' || event.defaultPrevented) return
                const active = created.layers[created.layers.length - 1]
                if (!active) return
                event.preventDefault()
                // A Select inside a drawer closes before the drawer itself.
                event.stopPropagation()
                dismissLayer(document, created, active)
            },
        }
        registry = created
        registries.set(document, registry)
        document.addEventListener('pointerdown', registry.pointerdown, true)
        document.addEventListener('keydown', registry.keydown, true)
    }
    const active = { ...layer }
    const current = registry
    current.layers.push(active)
    return () => removeLayer(document, current, active)
}

export function useDismissableLayer(
    root: Ref<HTMLElement | undefined>,
    active: Readonly<Ref<boolean>>,
    dismiss: () => void,
    /** Extra elements (e.g. a teleported overlay) that belong to this layer. */
    extra: ReadonlyArray<Ref<HTMLElement | undefined>> = [],
): void {
    let unregister: (() => void) | undefined
    let stop: (() => void) | undefined
    onMounted(() => {
        stop = watch(active, value => {
            unregister?.()
            unregister = undefined
            if (value) {
                const document = root.value?.ownerDocument ?? globalThis.document
                unregister = registerDismissableLayer(document, {
                    contains(event) {
                        const elements = [root.value, ...extra.map(item => item.value)]
                            .filter((element): element is HTMLElement => !!element)
                        return elements.some(element => event.composedPath().includes(element) || element.contains(event.target as Node | null))
                    },
                    dismiss,
                })
            }
        }, { immediate: true, flush: 'sync' })
    })
    onBeforeUnmount(() => {
        stop?.()
        unregister?.()
    })
}
