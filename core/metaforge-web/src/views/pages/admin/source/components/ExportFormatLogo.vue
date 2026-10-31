<template>
  <span class="inline-flex shrink-0 items-center justify-center rounded-mf-md" :class="[small ? 'size-5' : 'size-10', !showImage && tone]">
    <img v-if="showImage" :src="logo" :alt="format.label" class="size-full object-contain" @error="failed = true">
    <component :is="icon" v-else :size="small ? 14 : 20"/>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Braces, CodeXml, File, FileText } from '@lucide/vue'
import type { ExportFormat } from '@/model/source-export'

/** A format's logo from the store catalogue, or an icon while it is missing or fails to load. */
const props = defineProps<{ format: ExportFormat; logo?: string; active?: boolean; small?: boolean }>()
const failed = ref(false)
watch(() => props.logo, () => { failed.value = false })
const showImage = computed(() => !!props.logo && !failed.value)
const icon = computed(() => ({ json: Braces, csv: FileText, txt: FileText, xml: CodeXml })[props.format.known || ''] || File)
const tone = computed(() => props.small ? 'text-content-secondary' : props.active ? 'bg-primary text-on-primary' : 'bg-surface-hover text-content-secondary')
</script>
