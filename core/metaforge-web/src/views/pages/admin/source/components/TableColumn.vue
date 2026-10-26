<template>
  <MfDrawer v-model:open="visible" width="20%" :title="$t('source.common.visibleColumn')">
    <MfCheckboxGroup v-model="value"
                     :options="columns.map((item: any) => ({ label: item.field, value: item.field }))"/>

    <template #footer>
      <MfButton variant="primary" @click="onCancel">
        {{ $t('common.apply') }}
      </MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

defineOptions({ name: 'TableColumn' })

const props = withDefaults(defineProps<{ isVisible?: boolean; columns?: any[] }>(), {
  isVisible: false,
  columns: () => []
})
const emit = defineEmits<{
  (e: 'close', value: boolean): void
  (e: 'change', value: string[]): void
}>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const value = ref<string[]>(props.columns.map((item) => item.field))

const onCancel = () => {
  emit('change', value.value)
  visible.value = false
}
</script>
