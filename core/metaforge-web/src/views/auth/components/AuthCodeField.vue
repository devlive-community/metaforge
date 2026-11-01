<template>
  <MfInput :model-value="modelValue" size="lg" :maxlength="6" :placeholder="$t('auth.field.code')" @update:model-value="emit('update:modelValue', $event)">
    <template #prefix><ShieldCheck :size="17"/></template>
    <template #suffix>
      <button type="button" class="-mr-3 h-11 shrink-0 whitespace-nowrap border-l border-line px-4 text-sm font-semibold text-primary outline-none hover:bg-primary/5 focus-visible:bg-primary/5 disabled:cursor-not-allowed disabled:text-content-tertiary disabled:hover:bg-transparent"
              :disabled="disabled" @click="emit('send')">
        {{ label }}
      </button>
    </template>
  </MfInput>
</template>

<script setup lang="ts">
import { ShieldCheck } from '@lucide/vue'

/** The mailed code with its send button, which counts down before it can be pressed again. */
defineProps<{ modelValue?: string; label: string; disabled: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: string]; send: [] }>()
</script>
