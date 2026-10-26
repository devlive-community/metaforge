<script setup lang="ts">
</script>

<template>
    <nav class="mf-breadcrumb flex flex-wrap items-center gap-1.5 text-sm text-content-tertiary" aria-label="breadcrumb">
        <slot />
    </nav>
</template>

<style>
/* antd behavior: the trailing separator of the last crumb is hidden */
.mf-breadcrumb .mf-breadcrumb-item:last-child > .mf-breadcrumb-sep {
    display: none;
}
</style>
