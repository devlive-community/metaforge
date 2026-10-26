<script setup lang="ts">
withDefaults(defineProps<{
    href?: string
    separator?: string
}>(), {
    href: '',
})

const emit = defineEmits<{ click: [event: MouseEvent] }>()
</script>

<template>
    <span class="mf-breadcrumb-item inline-flex items-center gap-1.5">
        <a v-if="href" :href="href" class="transition-colors duration-(--mf-duration-fast) hover:text-primary" @click="emit('click', $event)">
            <slot />
        </a>
        <span v-else class="transition-colors duration-(--mf-duration-fast) hover:text-content" @click="emit('click', $event)">
            <slot />
        </span>
        <span class="mf-breadcrumb-sep text-content-tertiary/70 select-none" aria-hidden="true">{{ separator ?? '/' }}</span>
    </span>
</template>
