<template>
  <MfModal v-model:open="visible" title="Show Content" width="40%">
    <AceEditor v-if="content" :value="content" :read-only="true"/>

    <template #footer>
      <MfButton variant="danger" @click="onCancel">
        {{ $t('common.cancel') }}
      </MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AceEditor from '@/views/components/editor/AceEditor.vue'

defineOptions({ name: 'SqlInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; content?: string | null }>(), {
  isVisible: false,
  content: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const onCancel = () => {
  visible.value = false
}
</script>
