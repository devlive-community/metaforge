<template>
  <BaseLayout>
    <div class="h-screen flex items-center justify-center">
      <div class="w-full max-w-md px-4 mx-auto">
        <MfCard class="w-full">
          <template #header>
            <div class='flex items-center justify-center'>
              <MfAvatar src="/static/images/logo.png" alt="MetaForge"/>
            </div>
          </template>

          <div class="text-center text-content-secondary mb-2">
            {{ $t('user.auth.signinTip') }}
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
                  {{ $t('user.common.signin') }}
                </MfButton>

                <MfDivider class="text-sm py-2">
                  {{ $t('user.auth.notUserTip') }}
                </MfDivider>

                <MfButton block @click="$router.push('/auth/signup')">
                  {{ $t('user.common.signup') }}
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
import { MfMessage } from 'metaforge-ui'
import { useUserStore } from '@/stores/user'
import UserService from '@/services/user'
import CaptchaService from '@/services/captcha'
import CommonUtils from '@/utils/common'
import router from '@/router'
import { signInTarget } from '@/router/navigation'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'

interface Props
{
  username: string
  password: string
  timestamp: number
  captcha: number
}

defineOptions({ name: 'AuthSignin' })

const userStore = useUserStore()

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

const onError = (error: any) => {
  const names = (error?.errorFields || []).map((field: any) => (Array.isArray(field.name) ? field.name.join('.') : field.name))
  MfMessage.error(`Validation error field: [ ${ names.join(', ') } ]`)
}

const onSubmit = async () => {
  try {
    submitting.value = true
    const loginResponse = await UserService.signin(formState.value as any)

    if (loginResponse.status) {
      localStorage.setItem(CommonUtils.token, JSON.stringify(loginResponse.data))
      userStore.userInfo = loginResponse.data

      // 获取用户信息和菜单
      const menuResponse = await UserService.getMenus()
      if (menuResponse.status) {
        userStore.updateMenu(menuResponse.data)
        // 更新路由并跳转
        await router.replace(signInTarget(router.currentRoute.value.query.redirect))
      }
      else {
        if (!menuResponse.status) {
          MfMessage.error(menuResponse.message)
        }
        userStore.logout()
      }
    }
    else {
      MfMessage.error(loginResponse.message)
      initCaptcha()
    }
  }
  catch (error) {
    console.error('Login error:', error)
    MfMessage.error('Login failed')
    initCaptcha()
  }
  finally {
    submitting.value = false
  }
}

// created 等价：setup 阶段立即初始化验证码
loading.value = true
initCaptcha()
</script>
