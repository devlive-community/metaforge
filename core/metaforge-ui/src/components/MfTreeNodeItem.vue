<script setup lang="ts">
import { computed, inject, type Ref } from 'vue'
import clsx from 'clsx'
import type { MfTreeNode } from './MfTree.vue'

interface TreeContext {
    selectedKeys: Ref<(string | number)[]>
    expandedKeys: Ref<(string | number)[]>
    loadingKeys: Ref<Set<string | number>>
    toggle: (node: MfTreeNode) => Promise<void>
    select: (node: MfTreeNode) => void
}

const props = defineProps<{
    node: MfTreeNode
}>()

const tree = inject<TreeContext | null>('mfTree', null)

const selected = computed(() => tree?.selectedKeys.value.includes(props.node.key) ?? false)
const expanded = computed(() => tree?.expandedKeys.value.includes(props.node.key) ?? false)
const loading = computed(() => tree?.loadingKeys.value.has(props.node.key) ?? false)

defineSlots<{
    title(props: { node: MfTreeNode }): unknown
}>()

const rowClass = computed(() => clsx(
    'flex w-full cursor-pointer items-center gap-1.5 rounded-mf-md py-1.5 pr-2 text-sm select-none',
    'transition-[color,background-color] duration-(--mf-duration-fast) ease-(--mf-easing)',
    props.node.disabled
        ? 'cursor-not-allowed opacity-40'
        : 'text-content-secondary hover:bg-surface-hover hover:text-content',
    selected.value && 'bg-surface-active font-medium text-primary',
))
</script>

<template>
    <li role="treeitem" :aria-expanded="node.isLeaf ? undefined : expanded" :aria-selected="selected">
        <div :class="rowClass" @click="tree?.select(node)">
            <span class="inline-flex size-4 shrink-0 items-center justify-center" @click.stop="tree?.toggle(node)">
                <svg v-if="loading" class="size-3.5 animate-spin text-primary" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
                </svg>
                <svg
                    v-else-if="!node.isLeaf"
                    class="size-3.5 text-content-tertiary transition-transform duration-(--mf-duration-base) ease-(--mf-easing)"
                    :class="expanded && 'rotate-90'"
                    viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"
                >
                    <path d="M9 6l6 6-6 6" />
                </svg>
            </span>
            <slot name="title" :node="node">
                <span class="truncate">{{ node.title }}</span>
            </slot>
        </div>
        <div
            v-if="node.children?.length && expanded"
            class="ml-3.5 flex flex-col gap-0.5 border-l border-line pl-1.5"
        >
            <MfTreeNodeItem v-for="child in node.children" :key="child.key" :node="child">
                <template #title="{ node: childNode }: { node: MfTreeNode }">
                    <slot name="title" :node="childNode" />
                </template>
            </MfTreeNodeItem>
        </div>
    </li>
</template>
