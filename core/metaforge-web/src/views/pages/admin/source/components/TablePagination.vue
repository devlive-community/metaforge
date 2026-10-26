<template>
  <MfModal v-model:open="visible" :title="$t('source.common.jumpPage')">
    <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit">
      <MfFormItem name="showPageSize" :label="$t('source.common.showPageSize')">
        <MfInputNumber v-model="formState.size" :min="1" :max="10000" :style="{ width: '100%' }"/>
      </MfFormItem>

      <MfButton variant="primary" type="submit">
        {{ $t('common.apply') }}
      </MfButton>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

defineOptions({ name: 'TablePagination' })

const props = withDefaults(defineProps<{ isVisible?: boolean; pagination?: any }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void; (e: 'change', value: any): void }>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const formState = ref<any>(props.pagination ? props.pagination : { size: 500 })

const onSubmit = () => {
  visible.value = false
  emit('change', formState.value)
}
</script>
