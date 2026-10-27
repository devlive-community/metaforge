<script setup lang="ts">
import { computed, provide, ref, watch } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    selectedKeys?: (string | number)[]
    openKeys?: (string | number)[]
    mode?: 'vertical' | 'horizontal' | 'inline'
    collapsed?: boolean
}>(), {
    selectedKeys: () => [],
    openKeys: undefined,
    mode: 'vertical',
    collapsed: false,
})

const emit = defineEmits<{
    click: [info: { key: string | number, keyPath: (string | number)[] }]
    'update:openKeys': [keys: (string | number)[]]
}>()

const innerOpenKeys = ref<(string | number)[]>(props.openKeys ?? [])
watch(() => props.openKeys, value => {
    if (value !== undefined) innerOpenKeys.value = value
})

provide('mfMenu', {
    selectedKeys: computed(() => props.selectedKeys),
    openKeys: computed(() => innerOpenKeys.value),
    mode: computed(() => props.mode),
    toggleOpen(key: string | number): void {
        const open = innerOpenKeys.value.includes(key)
        innerOpenKeys.value = open
            ? innerOpenKeys.value.filter(k => k !== key)
            : [...innerOpenKeys.value, key]
        emit('update:openKeys', innerOpenKeys.value)
    },
    itemClick(key: string | number, keyPath: (string | number)[]): void {
        emit('click', { key, keyPath })
    },
})

const horizontal = computed(() => props.mode === 'horizontal')

const rootClass = computed(() => clsx(
    'flex text-sm select-none',
    horizontal.value
        ? 'flex-row items-center gap-1.5'
        : clsx('flex-col gap-1', props.collapsed && 'items-center'),
))
</script>

<template>
    <ul :class="rootClass" role="menu">
        <slot />
    </ul>
</template>
