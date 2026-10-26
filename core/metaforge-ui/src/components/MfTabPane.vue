<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, useSlots } from 'vue'

interface TabsContext {
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

onMounted(() => {
    tabs?.register({
        key: key.value,
        label: props.tab,
        disabled: props.disabled,
        renderTab: slots.tab ? () => slots.tab!() : undefined,
    })
})
onBeforeUnmount(() => tabs?.unregister(key.value))
</script>

<template>
    <div v-show="active" role="tabpanel" class="outline-none">
        <slot />
    </div>
</template>
