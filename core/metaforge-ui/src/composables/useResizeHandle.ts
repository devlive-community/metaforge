import { onScopeDispose, ref } from 'vue'

interface ResizeOptions {
    modelValue: number
    min: number
    max: number
    reverse: boolean
}

/** Pointer and keyboard resizing share bounds and always release document locks. */
export function useResizeHandle(options: ResizeOptions, update: (value: number) => void, commit: (value: number) => void) {
    const dragging = ref(false)
    let release: (() => void) | undefined
    const bounded = (value: number) => Math.round(Math.min(options.max, Math.max(options.min, value)))

    function start(event: PointerEvent) {
        if (event.button !== 0 || options.max <= options.min) return
        release?.()
        event.preventDefault()
        const target = event.currentTarget as HTMLElement
        const document = target.ownerDocument
        const window = document.defaultView
        if (!window) return
        const startX = event.clientX
        const startValue = options.modelValue
        let value = startValue
        const oldCursor = document.documentElement.style.cursor
        const oldSelection = document.documentElement.style.userSelect
        dragging.value = true
        document.documentElement.style.cursor = 'col-resize'
        document.documentElement.style.userSelect = 'none'
        const move = (next: PointerEvent) => {
            if (next.pointerId !== event.pointerId) return
            value = bounded(startValue + (next.clientX - startX) * (options.reverse ? -1 : 1))
            update(value)
        }
        const finish = (next?: PointerEvent) => {
            if (next && next.pointerId !== event.pointerId) return
            release?.()
            commit(value)
        }
        const blur = () => finish()
        const keydown = (next: KeyboardEvent) => {
            if (next.key !== 'Escape') return
            next.preventDefault()
            update(bounded(startValue))
            release?.()
        }
        release = () => {
            window.removeEventListener('pointermove', move)
            window.removeEventListener('pointerup', finish)
            window.removeEventListener('pointercancel', finish)
            window.removeEventListener('blur', blur)
            window.removeEventListener('keydown', keydown)
            document.documentElement.style.cursor = oldCursor
            document.documentElement.style.userSelect = oldSelection
            dragging.value = false
            release = undefined
            try { target.releasePointerCapture?.(event.pointerId) } catch { /* The pointer may already be released. */ }
        }
        window.addEventListener('pointermove', move)
        window.addEventListener('pointerup', finish)
        window.addEventListener('pointercancel', finish)
        window.addEventListener('blur', blur)
        window.addEventListener('keydown', keydown)
        try { target.setPointerCapture?.(event.pointerId) } catch { /* Window listeners also cover unsupported capture. */ }
    }

    function onKeydown(event: KeyboardEvent) {
        if (dragging.value) return
        let value: number
        if (event.key === 'Home') value = options.min
        else if (event.key === 'End') value = options.max
        else if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
            value = options.modelValue + (event.key === 'ArrowRight' ? 1 : -1) * (options.reverse ? -1 : 1) * (event.shiftKey ? 32 : 8)
        } else return
        event.preventDefault()
        value = bounded(value)
        update(value)
        commit(value)
    }

    onScopeDispose(() => release?.())
    return { dragging, start, onKeydown }
}
