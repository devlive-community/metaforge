<template>
  <div class="flex w-full items-center gap-2">
    <MfInput :model-value="modelValue" size="lg" class="min-w-0 flex-1" name="captcha" :placeholder="$t('auth.field.captcha')" @update:model-value="emit('update:modelValue', $event)">
      <template #prefix><Calculator :size="17"/></template>
    </MfInput>
    <button type="button" class="inline-flex h-11 w-32 shrink-0 items-center justify-center overflow-hidden rounded-mf-md border border-line bg-surface outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
            :aria-label="$t('auth.field.captchaRefresh')" :title="$t('auth.field.captchaRefresh')" @click="emit('refresh')">
      <MfSpinner v-if="loading"/>
      <img v-else-if="image" :src="`data:image/png;base64,${image}`" alt="" class="h-full w-full object-contain"/>
      <RefreshCw v-else :size="16" class="text-content-tertiary"/>
    </button>
  </div>
</template>

<script setup lang="ts">
import { Calculator, RefreshCw } from '@lucide/vue'

/** The arithmetic captcha: the answer field and its image, which a click replaces. */
defineProps<{ modelValue?: string; image: string; loading: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: string]; refresh: [] }>()
</script>
