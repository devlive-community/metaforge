<script setup lang="ts">
import { useResizeHandle } from '../composables/useResizeHandle'
const props = withDefaults(defineProps<{
    modelValue: number
    min: number
    max: number
    label: string
    reverse?: boolean
}>(), { reverse: false })
const emit = defineEmits<{ 'update:modelValue': [value: number]; change: [value: number] }>()
const { dragging, start, onKeydown } = useResizeHandle(props, value => emit('update:modelValue', value), value => emit('change', value))
</script>

<template>
    <div role="separator" tabindex="0" aria-orientation="vertical" :aria-label="label"
         :aria-valuemin="Math.round(min)" :aria-valuemax="Math.round(max)" :aria-valuenow="Math.round(modelValue)"
         class="mx-1 w-2 shrink-0 touch-none cursor-col-resize self-stretch rounded-mf-full outline-none transition-[background-color] duration-(--mf-duration-fast) hover:bg-primary/15 focus-visible:bg-primary/15 focus-visible:ring-1 focus-visible:ring-inset focus-visible:ring-primary"
         :class="dragging && 'bg-primary/15'" @pointerdown="start" @keydown="onKeydown"/>
</template>
