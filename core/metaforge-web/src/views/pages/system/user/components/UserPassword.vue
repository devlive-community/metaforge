<template>
  <MfModal v-model:open="visible" :title="title">
    <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit" @finish-failed="onError">
      <MfFormItem name="password"
                  :label="$t('user.common.newPassword')"
                  :rules="[
                    { required: true, message: $t('user.auth.passwordTip') },
                    { min: 6, message: $t('user.auth.passwordSizeTip') },
                    { max: 20, message: $t('user.auth.passwordSizeTip') }
                ]">
        <MfInput v-model="formState.password"
                 type="password"
                 show-password
                 :placeholder="$t('user.placeholder.newPassword')"/>
      </MfFormItem>

      <MfFormItem name="confirmPassword"
                  :label="$t('user.common.confirmPassword')"
                  :rules="[
                    { required: true, message: $t('user.auth.confirmPasswordTip') },
                    { validator: validatePassword }
                ]">
        <MfInput v-model="formState.confirmPassword"
                 type="password"
                 show-password
                 :placeholder="$t('user.placeholder.confirmNewPassword')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>
          <MfButton variant="primary" type="submit" :loading="loading" :disabled="loading">
            {{ $t('user.common.resetPassword') }}
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
import { UserModel } from '@/model/user'
import UserService from '@/services/user'

defineOptions({ name: 'UserPassword' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: UserModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const title = computed(() => t('user.common.resetPasswordInfo').replace('$VALUE', String(props.info?.username ?? '')))
const formState = ref<{ password: string; confirmPassword: string }>({ password: '', confirmPassword: '' })

const validatePassword = (value: string) => {
  if (value !== formState.value.password) {
    return t('user.auth.passwordNotMatchTip')
  }
  return true
}

const onError = (error: any) => {
  const names = (error?.errorFields || []).map((field: any) => (Array.isArray(field.name) ? field.name.join('.') : field.name))
  MfMessage.error(`Validation error field: [ ${ names.join(', ') } ]`)
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    loading.value = true
    UserService.resetPassword(props.info.code!, formState.value.password)
               .then((response) => {
                 if (response.status) {
                   MfMessage.success(t('user.tip.resetPasswordSuccess').replace('$VALUE', String(props.info?.username ?? '')))
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
