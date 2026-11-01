<template>
  <AuthShell tab="signin">
    <template #default="{ options }">
      <MfForm ref="formRef" :model="form" layout="vertical" @finish="onSubmit(options)">
        <MfFormItem name="username" :rules="[{ required: true, message: $t(options.emailSignin ? 'auth.field.accountRequired' : 'auth.field.usernameRequired') }, { max: 255 }]">
          <MfInput v-model="form.username" size="lg" :placeholder="$t(options.emailSignin ? 'auth.field.account' : 'auth.field.username')">
            <template #prefix><User :size="17"/></template>
          </MfInput>
        </MfFormItem>
        <MfFormItem name="password" :rules="[{ required: true, message: $t('auth.field.passwordRequired') }]">
          <MfInput v-model="form.password" size="lg" show-password :placeholder="$t('auth.field.password')">
            <template #prefix><Lock :size="17"/></template>
          </MfInput>
        </MfFormItem>
        <MfFormItem v-if="options.captcha" name="captcha" :rules="[{ required: true, message: $t('auth.field.captchaRequired') }]">
          <AuthCaptcha v-model="form.captcha" :image="captcha.image.value" :loading="captcha.loading.value" @refresh="captcha.refresh"/>
        </MfFormItem>

        <div v-if="options.rememberMe || options.passwordReset" class="flex items-center justify-between gap-3">
          <MfCheckbox v-if="options.rememberMe" v-model="form.remember">{{ $t('auth.signin.remember', { days: options.rememberDays }) }}</MfCheckbox>
          <span v-else/>
          <router-link v-if="options.passwordReset" :to="{ path: '/auth/reset', query: redirectQuery }" class="shrink-0 text-sm font-medium text-primary no-underline hover:underline">
            {{ $t('auth.signin.forgot') }}
          </router-link>
        </div>

        <MfButton variant="primary" type="submit" size="lg" block :loading="submitting">{{ $t('auth.signin.submit') }}</MfButton>
      </MfForm>
    </template>
  </AuthShell>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { Lock, User } from '@lucide/vue'
import { useUserStore } from '@/stores/user'
import UserService from '@/services/user'
import CommonUtils from '@/utils/common'
import { useRouteRedirect } from '@/composables/useRouteRedirect'
import { useAuthCaptcha } from '@/composables/useAuthCaptcha'
import { useAuthOptions } from '@/composables/useAuthOptions'
import type { AuthOptions } from '@/model/system-setting'
import AuthShell from '../components/AuthShell.vue'
import AuthCaptcha from '../components/AuthCaptcha.vue'

defineOptions({ name: 'AuthSignin' })

const userStore = useUserStore()
// 登录成功回到被拦截前的原地址（未登录访问受保护页、登录过期都会带上 redirect）。
const { query: redirectQuery, restore } = useRouteRedirect()
const { options } = useAuthOptions()
const captcha = useAuthCaptcha()
const formRef = ref()
const form = ref({ username: '', password: '', captcha: '', remember: false })
const submitting = ref(false)

// A captcha image is fetched only when the captcha is on.
watch(() => options.value?.captcha, enabled => { if (enabled) captcha.refresh() }, { immediate: true })

async function onSubmit(current: AuthOptions) {
  if (submitting.value) return
  submitting.value = true
  try {
    const response = await UserService.signin({
      username: form.value.username.trim(), password: form.value.password, captcha: current.captcha ? Number(form.value.captcha) : undefined,
      timestamp: current.captcha ? captcha.timestamp.value : undefined, remember: current.rememberMe && form.value.remember,
    })
    if (!response.status) {
      MfMessage.error(response.message)
      if (current.captcha) { form.value.captcha = ''; captcha.refresh() }
      return
    }
    localStorage.setItem(CommonUtils.token, JSON.stringify(response.data))
    userStore.userInfo = response.data
    const menus = await UserService.getMenus()
    if (!menus.status) {
      MfMessage.error(menus.message)
      userStore.logout()
      return
    }
    userStore.updateMenu(menus.data)
    await restore()
  }
  finally {
    submitting.value = false
  }
}
</script>
