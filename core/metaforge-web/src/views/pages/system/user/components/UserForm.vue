<template>
  <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit" @finish-failed="onError">
    <MfFormItem name="username"
                :label="$t('user.common.username')"
                :rules="[
                  { required: true, message: $t('user.auth.usernameTip') },
                  { min: 3, message: $t('user.auth.usernameSizeTip') },
                  { max: 20, message: $t('user.auth.usernameSizeTip') }
              ]">
      <MfInput v-model="formState.username"/>
    </MfFormItem>

    <MfFormItem name="password"
                :label="$t('user.common.password')"
                :rules="[
                  { required: true, message: $t('user.auth.passwordTip') },
                  { min: 6, message: $t('user.auth.passwordSizeTip') },
                  { max: 20, message: $t('user.auth.passwordSizeTip') }
              ]">
      <MfInput v-model="formState.password"
               type="password"
               show-password
               :placeholder="$t('user.auth.passwordTip')"/>
    </MfFormItem>

    <MfFormItem name="confirmPassword"
                :label="$t('user.common.confirmPassword')"
                :rules="[
                  { required: true, message: $t('user.auth.passwordTip') },
                  { validator: validatePassword }
              ]">
      <MfInput v-model="formState.confirmPassword"
               type="password"
               show-password
               :placeholder="$t('user.auth.confirmPasswordTip')"/>
    </MfFormItem>

    <div class="flex justify-end">
      <MfButton variant="primary" type="submit" :loading="loading" :disabled="loading">
        {{ $t('common.save') }}
      </MfButton>
    </div>
  </MfForm>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { UserModel } from '@/model/user'
import UserService from '@/services/user'

defineOptions({ name: 'UserForm' })

const props = defineProps<{ info?: UserModel }>()
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const loading = ref(false)
const formState = ref<UserModel>({
  code: undefined,
  username: undefined,
  password: undefined,
  confirmPassword: undefined
} as UserModel)

if (props.info) {
  const { code, username } = props.info as UserModel
  formState.value = { code, username } as UserModel
}

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

const onSubmit = () => {
  loading.value = true
  UserService.saveOrUpdate(formState.value)
             .then((response) => {
               if (response.status) {
                 emit('close', true)
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (loading.value = false))
}
</script>
