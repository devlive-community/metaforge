<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
    applyMfTheme,
    setMfThemeMode,
    type MfThemeMode,
    type MfThemeTokens,
} from '../theme/theme'

const props = withDefaults(defineProps<{
    /** Token overrides written as `--mf-*` variables on this subtree. */
    tokens?: MfThemeTokens
    mode?: MfThemeMode
    /** Apply to the document root instead of this subtree. */
    global?: boolean
}>(), {
    tokens: () => ({}),
    mode: 'light',
    global: false,
})

const root = ref<HTMLElement>()

function target(): HTMLElement {
    return props.global ? document.documentElement : root.value!
}

function sync(): void {
    if (!props.global && !root.value) return
    setMfThemeMode(props.mode, target())
    applyMfTheme(props.tokens, { target: target() })
}

watch(() => [props.tokens, props.mode] as const, sync, { deep: true })

onMounted(sync)

onBeforeUnmount(() => {
    if (!props.global && root.value) {
        for (const name of Object.keys(props.tokens)) {
            root.value!.style.removeProperty(`--mf-${name}`)
        }
        delete root.value!.dataset.theme
    }
})
</script>

<template>
    <div ref="root" class="contents">
        <slot />
    </div>
</template>
