<script setup lang="ts">
import { computed, defineComponent, provide, ref, watch } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    activeKey?: string | number
    tabPosition?: 'top' | 'left' | 'right' | 'bottom'
}>(), {
    activeKey: '',
    tabPosition: 'top',
})

const emit = defineEmits<{
    'update:activeKey': [key: string | number]
    change: [key: string | number]
}>()

interface PaneEntry {
    key: string | number
    label: string | number
    disabled: boolean
    renderTab: (() => unknown[]) | undefined
}

const innerKey = ref<string | number>(props.activeKey)
watch(() => props.activeKey, value => { innerKey.value = value })

const panes = ref<PaneEntry[]>([])

provide('mfTabs', {
    register(entry: PaneEntry): void {
        if (!panes.value.some(p => p.key === entry.key)) {
            panes.value = [...panes.value, entry]
            if (innerKey.value === '' && panes.value.length === 1) {
                select(entry)
            }
        }
    },
    unregister(key: string | number): void {
        panes.value = panes.value.filter(p => p.key !== key)
    },
    isActive(key: string | number): boolean {
        return innerKey.value === key
    },
})

function select(entry: PaneEntry): void {
    if (entry.disabled) return
    if (innerKey.value !== entry.key) {
        innerKey.value = entry.key
        emit('update:activeKey', entry.key)
        emit('change', entry.key)
    }
}

// renders a pane's custom #tab slot or its plain label
const TabContent = defineComponent({
    props: { entry: { type: Object, required: true } },
    setup: (p: { entry: PaneEntry }) => () => p.entry.renderTab ? p.entry.renderTab() : String(p.entry.label),
})

const vertical = computed(() => props.tabPosition === 'left' || props.tabPosition === 'right')

const navClass = computed(() => clsx(
    'flex gap-1',
    vertical
        ? clsx('flex-col py-1', props.tabPosition === 'right' ? 'border-l border-line order-2' : 'border-r border-line order-1')
        : clsx('flex-row items-center border-b border-line px-1', props.tabPosition === 'bottom' ? 'order-2' : 'order-1'),
))

function tabClass(entry: PaneEntry): string {
    const active = innerKey.value === entry.key
    return clsx(
        'cursor-pointer whitespace-nowrap px-3.5 py-2 text-sm font-medium select-none',
        'transition-[color,border-color,background-color] duration-(--mf-duration-base) ease-(--mf-easing)',
        entry.disabled && 'cursor-not-allowed opacity-40',
        active
            ? vertical ? 'text-primary bg-surface-active rounded-mf-md' : 'border-b-2 border-b-primary text-primary -mb-px'
            : clsx('text-content-secondary hover:text-content', vertical ? 'rounded-mf-md hover:bg-surface-hover' : 'border-b-2 border-b-transparent', !entry.disabled && ''),
    )
}
</script>

<template>
    <div class="flex w-full" :class="vertical ? 'flex-row' : 'flex-col'">
        <div role="tablist" :class="navClass">
            <div
                v-for="pane in panes"
                :key="pane.key"
                role="tab"
                :aria-selected="innerKey === pane.key"
                :aria-disabled="pane.disabled"
                :class="tabClass(pane)"
                @click="select(pane)"
            >
                <TabContent :entry="pane" />
            </div>
        </div>
        <div class="min-w-0 flex-1" :class="vertical ? 'px-3' : (tabPosition === 'bottom' ? '' : 'pt-3')">
            <slot />
        </div>
    </div>
</template>
