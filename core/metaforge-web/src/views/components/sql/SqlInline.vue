<script setup lang="ts">
import { computed } from 'vue'
import { tokenizeSql } from '@/model/executed-sql'
const props = defineProps<{ code: string }>()
const tokens = computed(() => tokenizeSql(props.code))
</script>
<template><MfTooltip :content="code" class="w-full min-w-0"><code class="block truncate font-mono text-xs"><span v-for="(token,index) in tokens" :key="index" :class="token.kind === 'keyword' ? 'font-semibold text-primary' : 'text-content'">{{ token.text }}</span></code></MfTooltip></template>
