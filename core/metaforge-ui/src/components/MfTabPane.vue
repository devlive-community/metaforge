<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, useSlots, watch, type Ref } from 'vue'

interface TabsContext {
    fill?: Ref<boolean>
    register: (entry: { key: string | number, label: string | number, disabled: boolean, renderTab: (() => unknown[]) | undefined }) => void
    unregister: (key: string | number) => void
    isActive: (key: string | number) => boolean
}

const props = withDefaults(defineProps<{
    /** Stable identity; also usable as `:key` on the pane. */
    itemKey?: string | number
    /** Nav label (plain text). Use the `#tab` slot for rich labels. */
    tab?: string | number
    disabled?: boolean
}>(), {
    itemKey: '',
    tab: '',
    disabled: false,
})

const slots = useSlots()
const key = computed(() => props.itemKey !== '' ? props.itemKey : props.tab)
const tabs = inject<TabsContext | null>('mfTabs', null)
const active = computed(() => tabs?.isActive(key.value) ?? false)
const fill = computed(() => tabs?.fill?.value ?? false)

function register(): void {
    tabs?.register({
        key: key.value,
        label: props.tab,
        disabled: props.disabled,
        renderTab: slots.tab ? () => slots.tab!() : undefined,
    })
}
onMounted(register)
watch(() => [key.value, props.tab, props.disabled], (value, previous) => {
    if (value[0] !== previous[0]) tabs?.unregister(previous[0] as string | number)
    register()
})
onBeforeUnmount(() => tabs?.unregister(key.value))
</script>

<template>
    <div v-show="active" role="tabpanel" class="outline-none" :class="fill && 'h-full min-h-0 overflow-auto'">
        <slot />
    </div>
</template>
