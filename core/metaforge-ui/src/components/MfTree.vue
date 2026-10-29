<script setup lang="ts">
import { computed, provide, ref, watch } from 'vue'
import MfTreeNodeItem from './MfTreeNodeItem.vue'

export interface MfTreeNode {
    key: string | number
    title: string
    children?: MfTreeNode[]
    isLeaf?: boolean
    disabled?: boolean
}

const props = withDefaults(defineProps<{
    data?: MfTreeNode[]
    selectedKeys?: (string | number)[]
    expandedKeys?: (string | number)[]
    /** Async loading: called when a non-leaf node without children expands. */
    loadData?: (node: MfTreeNode) => Promise<MfTreeNode[] | void>
    defaultExpandAll?: boolean
}>(), {
    data: () => [],
    selectedKeys: () => [],
    expandedKeys: undefined,
    defaultExpandAll: false,
})

const emit = defineEmits<{
    'update:selectedKeys': [keys: (string | number)[]]
    'update:expandedKeys': [keys: (string | number)[]]
    select: [selectedKeys: (string | number)[], info: { node: MfTreeNode, selected: boolean }]
    expand: [expandedKeys: (string | number)[], info: { node: MfTreeNode, expanded: boolean }]
}>()

function collectKeys(nodes: MfTreeNode[]): (string | number)[] {
    return nodes.flatMap(node => [node.key, ...(node.children ? collectKeys(node.children) : [])])
}

const innerExpanded = ref<(string | number)[]>(props.expandedKeys ?? (props.defaultExpandAll ? collectKeys(props.data) : []))

watch(() => props.expandedKeys, value => {
    if (value !== undefined) innerExpanded.value = value
})

const loadingKeys = ref(new Set<string | number>())

provide('mfTree', {
    selectedKeys: computed(() => props.selectedKeys),
    expandedKeys: computed(() => innerExpanded.value),
    loadingKeys: computed(() => loadingKeys.value),
    async toggle(node: MfTreeNode): Promise<void> {
        if (node.disabled || node.isLeaf) return
        const expanded = innerExpanded.value.includes(node.key)
        if (!expanded && !node.children?.length && props.loadData) {
            loadingKeys.value.add(node.key)
            try {
                const children = await props.loadData(node)
                if (children && children.length > 0) {
                    node.children = children
                }
            }
            finally {
                loadingKeys.value.delete(node.key)
            }
        }
        const next = expanded
            ? innerExpanded.value.filter(k => k !== node.key)
            : [...innerExpanded.value, node.key]
        innerExpanded.value = next
        emit('update:expandedKeys', next)
        emit('expand', next, { node, expanded: !expanded })
    },
    select(node: MfTreeNode): void {
        if (node.disabled) return
        const selected = props.selectedKeys.includes(node.key)
        const next = selected ? [] : [node.key]
        emit('update:selectedKeys', next)
        emit('select', next, { node, selected: !selected })
    },
})
</script>

<template>
    <ul role="tree" class="flex flex-col gap-1">
        <MfTreeNodeItem v-for="node in data" :key="node.key" :node="node">
            <template #title="{ node: item }">
                <slot name="title" :node="item" />
            </template>
        </MfTreeNodeItem>
        <li v-if="data.length === 0" class="px-2 py-4 text-center text-sm text-content-tertiary">No data</li>
    </ul>
</template>
