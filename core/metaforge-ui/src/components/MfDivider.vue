<script setup lang="ts">
withDefaults(defineProps<{
    label?: string
    dashed?: boolean
}>(), {
    dashed: false,
})
</script>

<template>
    <div class="flex w-full items-center gap-3" role="separator">
        <span class="h-px flex-1 bg-line" :class="dashed && 'border-t border-dashed border-line bg-transparent!'" />
        <span v-if="label || $slots.default" class="text-xs text-content-tertiary">
            <slot>{{ label }}</slot>
        </span>
        <span class="h-px flex-1 bg-line" :class="dashed && 'border-t border-dashed border-line bg-transparent!'" />
    </div>
</template>
