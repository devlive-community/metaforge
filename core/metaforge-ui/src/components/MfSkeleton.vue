<script setup lang="ts">
import clsx from 'clsx'

withDefaults(defineProps<{
    loading?: boolean
    rows?: number
    avatar?: boolean
    paragraph?: boolean
    round?: boolean
    active?: boolean
}>(), {
    loading: true,
    rows: 3,
    avatar: false,
    paragraph: true,
    round: false,
    active: true,
})

const block = 'bg-surface-active'
const anim = 'animate-pulse'
</script>

<template>
    <div v-if="loading" :class="clsx('flex w-full gap-4', active && anim)" aria-busy="true" aria-live="polite">
        <span v-if="avatar" :class="clsx('size-10 shrink-0', block, round ? 'rounded-mf-full' : 'rounded-mf-md')" />
        <div class="flex w-full flex-col gap-2.5">
            <span :class="clsx('h-4 w-2/5', block, round && 'rounded-mf-full')" />
            <template v-if="paragraph">
                <span
                    v-for="row in rows"
                    :key="row"
                    :class="clsx('h-3', row === rows ? 'w-3/5' : 'w-full', block, round && 'rounded-mf-full')"
                />
            </template>
        </div>
    </div>
    <slot v-else />
</template>
