<template>
  <MfTooltip v-if="text" :content="text" class="max-w-full">
    <span class="block truncate">{{ text }}</span>
  </MfTooltip>
  <span v-else class="text-content-tertiary">-</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'EllipsisCell' })

const props = defineProps<{ text?: string | number | null }>()

const text = computed(() => (props.text === undefined || props.text === null || props.text === '' ? '' : String(props.text)))
</script>
