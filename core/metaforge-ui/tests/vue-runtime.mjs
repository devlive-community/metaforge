export * from 'vue'

// Interaction tests exercise real Vue state/lifecycle without a browser or
// animation clock. Only the visual transition wrapper is replaced.
export const Transition = {
    inheritAttrs: false,
    setup(_props, { slots }) {
        return () => slots.default?.()
    },
}
