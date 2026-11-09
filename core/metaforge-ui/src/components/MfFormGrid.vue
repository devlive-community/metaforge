<script setup lang="ts">
defineOptions({ name: 'MfFormGrid' })
</script>

<template>
    <div class="mf-form-grid min-w-0 w-full">
        <div class="mf-form-grid__fields grid min-w-0 grid-cols-1 items-start gap-4">
            <slot />
        </div>
    </div>
</template>

<style scoped>
.mf-form-grid { container: mf-form-layout / inline-size; }
@media (min-width: 640px) {
    @container mf-form-layout (min-width: 22rem) {
        .mf-form-grid__fields { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }
}
</style>
