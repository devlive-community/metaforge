<template>
  <BaseLayout>
    <div class="h-screen flex items-center justify-center">
      <div class="w-full max-w-md px-4 mx-auto">
        <MfCard class="w-full">
          <template #header>
            <div class='flex items-center justify-center'>
              <img src="/static/images/logo.png" alt="MetaForge" width="56" height="56" class="dc-brand-logo"/>
            </div>
          </template>

          <div class="text-center text-content-secondary mb-2">
            {{ $t('user.auth.signupTip') }}
          </div>

          <div class="px-6 py-8 relative">
            <div v-if="loading" class="flex items-center justify-center py-8">
              <MfSpinner size="lg" label="Loading..."/>
            </div>
            <MfForm v-else
                    ref="formRef"
                    :model="formState"
                    layout="vertical"
                    @finish="onSubmit"
                    @finish-failed="onError">
              <MfFormItem name="username"
                          :label="$t('user.common.username')"
                          :rules="[
                            { required: true, message: $t('user.auth.usernameTip') },
                            { min: 3, message: $t('user.auth.usernameSizeTip') },
                            { max: 20, message: $t('user.auth.usernameSizeTip') }
                        ]">
                <MfInput v-model="formState.username"
                         :placeholder="$t('user.auth.usernameTip')"/>
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

              <MfFormItem v-if="showCaptcha"
                          name="captcha"
                          :label="$t('user.common.captcha')"
                          :rules="[
                            { required: true, message: $t('user.auth.captchaTip') },
                            { min: 1, message: $t('user.auth.captchaSizeTip') },
                            { max: 6, message: $t('user.auth.captchaSizeTip') }
                        ]">
                <div class="flex items-center gap-2">
                  <MfInput v-model="formState.captcha"
                           :placeholder="$t('user.auth.captchaTip')"/>
                  <MfButton variant="ghost"
                            class="px-0!"
                            :loading="captchaLoading"
                            :disabled="captchaLoading"
                            @click="initCaptcha">
                    <img v-if="!captchaLoading" style="min-width: 120px; height: 100%;" :src="'data:image/png;base64,' + captchaImage"/>
                  </MfButton>
                </div>
              </MfFormItem>

              <MfSpace direction="vertical" fill>
                <MfButton variant="primary"
                          type="submit"
                          block
                          :disabled="submitting"
                          :loading="submitting">
                  {{ $t('user.common.signup') }}
                </MfButton>

                <MfDivider class="text-sm py-2">
                  {{ $t('user.auth.hasUserTip') }}
                </MfDivider>

                <MfButton block @click="$router.push('/auth/signin')">
                  {{ $t('user.common.signin') }}
                </MfButton>
              </MfSpace>
            </MfForm>
          </div>
        </MfCard>
      </div>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import CaptchaService from '@/services/captcha'
import UserService from '@/services/user'
import router from '@/router'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'

interface Props
{
  username: string
  password: string
  confirmPassword: string
  timestamp: number
  captcha: number
}

defineOptions({ name: 'AuthSignup' })

const { t } = useI18n()

const formRef = ref()
const formState = ref<Props>({} as Props)
const submitting = ref(false)
const loading = ref(false)
const showCaptcha = ref(false)
const captchaImage = ref<string | null>(null)
const captchaLoading = ref(false)

const initCaptcha = () => {
  captchaLoading.value = true
  formState.value.timestamp = Date.parse(new Date().toString())
  CaptchaService.getCaptcha(formState.value.timestamp)
                .then(response => {
                  if (response.data !== false) {
                    showCaptcha.value = true
                    captchaImage.value = response.data.image
                  }
                })
                .finally(() => {
                  captchaLoading.value = false
                  loading.value = false
                })
}

const validatePassword = (value: unknown) => {
  if (value !== formState.value.password) {
    return t('user.auth.passwordNotMatchTip')
  }
  return true
}

const onError = (error: any) => {
  const names = (error?.errorFields || []).map((field: any) => (Array.isArray(field.name) ? field.name.join('.') : field.name))
  MfMessage.error(`Validation error field: [ ${ names.join(', ') } ]`)
}

const onSubmit = async () => {
  submitting.value = true
  UserService.signup(formState.value as any)
             .then(response => {
               if (response.status) {
                 router.push('/auth/signin')
               }
               else {
                 MfMessage.error(response.message)
                 initCaptcha()
               }
             })
             .finally(() => (submitting.value = false))
}

// created 等价：setup 阶段立即初始化验证码
loading.value = true
initCaptcha()
</script>
