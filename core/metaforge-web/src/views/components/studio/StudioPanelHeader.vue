<template>
  <div class="flex shrink-0 items-center gap-2.5 bg-linear-to-l from-primary/10 to-surface px-3 py-2.5">
    <span class="grid size-9 shrink-0 place-items-center rounded-mf-md bg-primary/12 text-primary"><Layers :size="20"/></span>
    <div class="min-w-0 flex-1">
      <h2 class="truncate font-semibold">{{ title }}</h2>
    </div>
    <div class="flex shrink-0 gap-1">
      <slot name="actions"/>
      <MfButton v-if="pinnable" size="sm" variant="ghost" class="size-7! p-0!" :aria-pressed="pinned"
                :aria-label="$t(pinned ? 'dashboard.studio.unpinPanel' : 'dashboard.studio.pinPanel')"
                :title="$t(pinned ? 'dashboard.studio.unpinPanel' : 'dashboard.studio.pinPanel')" @click="emit('pin')">
        <Pin v-if="pinned" :size="14" class="text-primary"/><PinOff v-else :size="14"/>
      </MfButton>
      <MfButton size="sm" variant="ghost" class="size-7! p-0!" :aria-label="$t('dashboard.studio.closePanel')" @click="emit('close')">
        <X :size="16"/>
      </MfButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Layers, Pin, PinOff, X } from '@lucide/vue'

defineProps<{ title: string; pinned: boolean; pinnable: boolean }>()
const emit = defineEmits<{ pin: []; close: [] }>()
</script>
