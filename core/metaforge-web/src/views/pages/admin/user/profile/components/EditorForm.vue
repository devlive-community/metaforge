<template>
  <div class="relative">
    <MfSkeleton :loading="defaultLoading">
      <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit">
        <MfFormItem name="fontSize"
                     class="w-[40%]"
                     :label="$t('user.common.fontSize')"
                     :rules="[{ pattern: /^[0-9]*$/, message: 'Please enter number!' }]">
          <MfInput v-model="formState.fontSize"/>
          <div class="text-xs text-content-tertiary">{{ $t('user.tip.fontSize') }}</div>
        </MfFormItem>

        <MfButton variant="primary" type="submit" :loading="submitting" :disabled="submitting">
          {{ $t('common.save') }}
        </MfButton>
      </MfForm>
    </MfSkeleton>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import UserService from '@/services/user'
import { UserEditor } from '@/model/user'
import Common from '@/utils/common'

defineOptions({ name: 'EditorForm' })

const { t } = useI18n()

const defaultLoading = ref(false)
const submitting = ref(false)
const formState = ref<UserEditor | null>(null)

const handlerInitialize = () => {
  defaultLoading.value = true
  UserService.getInfo()
             .then(response => {
               if (response.status) {
                 const configure = response.data.editorConfigure
                 if (response.data && configure) {
                   formState.value = configure as UserEditor
                 }
               }
             })
             .finally(() => (defaultLoading.value = false))
}

const onSubmit = () => {
  submitting.value = true
  UserService.changeEditor(formState.value as UserEditor)
             .then((response) => {
               if (response.status) {
                 MfMessage.success(t('common.successfully') as string)
                 localStorage.setItem(Common.userEditorConfigure, JSON.stringify(formState.value))
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (submitting.value = false))
}

handlerInitialize()
</script>
