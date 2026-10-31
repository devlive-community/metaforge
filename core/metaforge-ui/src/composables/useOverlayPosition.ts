import { nextTick, onBeforeUnmount, ref, watch, type Ref } from 'vue'

export type OverlayPlacement = 'bottomLeft' | 'bottom' | 'bottomRight' | 'topLeft' | 'top' | 'topRight'

interface OverlayPositionOptions {
    /** Trigger element the overlay is anchored to. */
    root: Ref<HTMLElement | undefined>
    open: Readonly<Ref<boolean>>
    placement: () => OverlayPlacement
    /** Distance between trigger and overlay in pixels. */
    gap?: number
    /** Opt in to measured viewport clamping and vertical flipping. */
    overlay?: Ref<HTMLElement | undefined>
    maxWidth?: number
    /** Keep a Select popup as wide as its trigger when the layout changes. */
    matchTriggerWidth?: boolean
}

/**
 * Pins a teleported overlay to the trigger's viewport rect with
 * `position: fixed`, so popups escape `overflow: hidden/auto` ancestors
 * (tables, cards) instead of being clipped or forcing scrollbars.
 */
export function useOverlayPosition(options: OverlayPositionOptions): Ref<Record<string, string>> {
    const style = ref<Record<string, string>>({})
    let observer: ResizeObserver | undefined
    let revision = 0

    function update(): void {
        const trigger = options.root.value
        if (!trigger) return
        const rect = trigger.getBoundingClientRect()
        const gap = options.gap ?? 6
        const center = rect.left + rect.width / 2
        if (options.overlay) {
            const margin = 8
            const viewportWidth = document.documentElement?.clientWidth || window.innerWidth
            const viewportHeight = document.documentElement?.clientHeight || window.innerHeight
            const limits = {
                ...(options.matchTriggerWidth ? { width: `${Math.max(0, Math.min(rect.width, viewportWidth - margin * 2))}px` } : {}),
                maxWidth: `${Math.max(0, Math.min(options.maxWidth ?? viewportWidth, viewportWidth - margin * 2))}px`,
                maxHeight: `${Math.max(0, viewportHeight - margin * 2)}px`,
            }
            const overlay = options.overlay.value
            if (!overlay) {
                style.value = { ...limits, top: '0px', left: '0px', visibility: 'hidden' }
                return
            }
            const bounds = overlay.getBoundingClientRect()
            const placement = options.placement()
            const above = rect.top - gap - bounds.height
            const below = rect.bottom + gap
            let top = placement.startsWith('top') ? above : below
            if (placement.startsWith('top') && above < margin && below + bounds.height <= viewportHeight - margin) top = below
            else if (!placement.startsWith('top') && below + bounds.height > viewportHeight - margin && above >= margin) top = above
            const left = placement.endsWith('Left') ? rect.left
                : placement.endsWith('Right') ? rect.right - bounds.width : center - bounds.width / 2
            style.value = {
                ...limits,
                left: `${Math.max(margin, Math.min(left, viewportWidth - bounds.width - margin))}px`,
                top: `${Math.max(margin, Math.min(top, viewportHeight - bounds.height - margin))}px`,
            }
            return
        }
        const topEdge = window.innerHeight - rect.top + gap
        style.value = {
            bottomLeft: { top: `${rect.bottom + gap}px`, left: `${rect.left}px` },
            bottom: { top: `${rect.bottom + gap}px`, left: `${center}px`, transform: 'translateX(-50%)' },
            bottomRight: { top: `${rect.bottom + gap}px`, left: `${rect.right}px`, transform: 'translateX(-100%)' },
            topLeft: { bottom: `${topEdge}px`, left: `${rect.left}px` },
            top: { bottom: `${topEdge}px`, left: `${center}px`, transform: 'translateX(-50%)' },
            topRight: { bottom: `${topEdge}px`, left: `${rect.right}px`, transform: 'translateX(-100%)' },
        }[options.placement()]
    }

    function start(): void {
        const current = ++revision
        update()
        window.addEventListener('resize', update)
        // Capture phase catches scrolls from any ancestor container while open.
        document.addEventListener('scroll', update, true)
        if (options.overlay) {
            void nextTick(() => {
                if (current !== revision || !options.open.value) return
                update()
                if (typeof ResizeObserver !== 'undefined' && options.overlay?.value) {
                    observer = new ResizeObserver(update)
                    observer.observe(options.overlay.value)
                    if (options.root.value) observer.observe(options.root.value)
                }
            })
        }
    }

    function stop(): void {
        revision++
        observer?.disconnect()
        observer = undefined
        window.removeEventListener('resize', update)
        document.removeEventListener('scroll', update, true)
    }

    watch(options.open, value => (value ? start() : stop()))
    onBeforeUnmount(stop)

    return style
}
