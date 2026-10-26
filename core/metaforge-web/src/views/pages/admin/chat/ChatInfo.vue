<template>
  <MfModal v-model:open="visible" :title="$t('common.chat')">
    <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit">
      <MfFormItem name="name"
                  :label="$t('common.name')"
                  :rules="[ { required: true, message: $t('common.name') } ]">
        <MfInput v-model="formState.name"/>
      </MfFormItem>

      <MfFormItem name="avatar" :label="$t('common.avatar')">
        <MfInput v-model="formState.avatar"/>
      </MfFormItem>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>

          <MfButton variant="primary" type="submit" :disabled="loading" :loading="loading">
            {{ $t('common.save') }}
          </MfButton>
        </MfSpace>
      </div>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import ChatService from '@/services/chat.ts'
import { ChatModel, ChatRequest } from '@/model/chat.ts'

defineOptions({ name: 'ChatInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const formState = ref<ChatModel>(ChatRequest.of())

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (formState.value) {
    loading.value = true
    ChatService.saveOrUpdate(formState.value)
               .then((response) => {
                 if (response.status) {
                   MfMessage.success(t('common.success'))
                   onCancel()
                 }
                 else {
                   MfMessage.error(response.message)
                 }
               })
               .finally(() => (loading.value = false))
  }
}
</script>
