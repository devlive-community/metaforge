<template>
  <div class="relative">
    <div v-if="defaultLoading" class="flex items-center justify-center py-10">
      <MfSpinner/>
    </div>
    <MfForm v-else-if="!defaultLoading && formState" :model="formState" layout="vertical" @finish="onSubmit">
        <MfFormGrid>
        <MfFormItem name="fontSize"
                     :label="$t('user.common.fontSize')"
                     :tip="$t('user.tip.fontSize')"
                     :rules="[{ required: true, min: 1, message: $t('user.validation.fontSize') }]">
          <MfInputNumber v-model="formState.fontSize" class="w-full" :min="1" decimal-only commit-on-change/>
        </MfFormItem>
        </MfFormGrid>

        <MfButton class="self-start" variant="primary" type="submit" :loading="submitting" :disabled="submitting">
          {{ $t('common.save') }}
        </MfButton>
      </MfForm>
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
                 const saved = typeof configure === 'string' ? JSON.parse(configure) : configure
                 formState.value = { fontSize: 12, theme: 'chrome', ...saved }
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
