<template>
  <div class="flex overflow-hidden rounded-mf-md bg-code-bg font-mono text-xs leading-5" :class="invalid && 'ring-1 ring-danger'">
    <ol ref="gutter" class="m-0 shrink-0 list-none select-none overflow-hidden border-r border-code-header py-2.5 pl-3 pr-2 text-right text-code-muted" aria-hidden="true"
        :style="{ height }">
      <li v-for="line in lineCount" :key="line">{{ line }}</li>
    </ol>
    <textarea :id="id" ref="area" :value="modelValue" :disabled="disabled" :aria-label="label" spellcheck="false" wrap="off"
              class="min-w-0 flex-1 resize-none bg-transparent py-2.5 pl-3 pr-3 text-code-fg caret-code-fg outline-none placeholder:text-code-muted disabled:opacity-60"
              :style="{ height }" @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)" @scroll="syncScroll" @keydown.tab.prevent="indent"/>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

defineOptions({ name: 'SqlCodeEditor' })
const props = withDefaults(defineProps<{ modelValue: string; id?: string; label?: string; disabled?: boolean; invalid?: boolean; height?: string }>(), {
  id: undefined, label: undefined, disabled: false, invalid: false, height: '11rem',
})
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const area = ref<HTMLTextAreaElement>()
const gutter = ref<HTMLElement>()
const lineCount = computed(() => props.modelValue.split('\n').length)
// The line numbers follow the text as it scrolls.
const syncScroll = () => { if (gutter.value && area.value) gutter.value.scrollTop = area.value.scrollTop }
/** Tab inserts two spaces instead of leaving the field. */
function indent(event: KeyboardEvent) {
  const target = event.target as HTMLTextAreaElement
  const { selectionStart, selectionEnd, value } = target
  emit('update:modelValue', `${value.slice(0, selectionStart)}  ${value.slice(selectionEnd)}`)
  requestAnimationFrame(() => { target.selectionStart = target.selectionEnd = selectionStart + 2 })
}
</script>
