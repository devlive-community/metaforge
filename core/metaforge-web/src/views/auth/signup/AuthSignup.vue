<template>
  <AuthShell tab="signup">
    <template #default="{ options }">
      <MfResult v-if="!options.registration" status="info" :title="$t('auth.signup.closed')" :sub-title="$t('auth.signup.closedHint')">
        <template #extra>
          <MfButton variant="primary" @click="$router.replace({ path: '/auth/signin', query: redirectQuery })">{{ $t('auth.signup.toSignin') }}</MfButton>
        </template>
      </MfResult>
      <MfForm v-else ref="formRef" :model="form" layout="vertical" @finish="onSubmit(options)">
        <MfFormItem name="username" :rules="[{ required: true, message: $t('auth.field.usernameRequired') }, { pattern: USERNAME_PATTERN, message: $t('auth.field.usernameRule') }]">
          <MfInput v-model="form.username" size="lg" :maxlength="32" :placeholder="$t('auth.field.username')">
            <template #prefix><User :size="17"/></template>
          </MfInput>
          <p class="m-0 mt-1.5 text-xs text-content-tertiary">{{ $t('auth.field.usernameRule') }}</p>
        </MfFormItem>
        <template v-if="options.emailVerification">
          <MfFormItem name="email" :rules="[{ required: true, message: $t('auth.field.emailRequired') }, { pattern: EMAIL_PATTERN, message: $t('auth.field.emailInvalid') }]">
            <MfInput v-model="form.email" size="lg" :placeholder="$t('auth.field.email')">
              <template #prefix><Mail :size="17"/></template>
            </MfInput>
          </MfFormItem>
          <MfFormItem name="code" :rules="[{ required: true, message: $t('auth.field.codeRequired') }, { pattern: /^\d{6}$/, message: $t('auth.field.codeRule') }]">
            <AuthCodeField v-model="form.code" :label="code.label.value" :disabled="code.disabled.value || !EMAIL_PATTERN.test(form.email.trim())" @send="code.send(form.email)"/>
          </MfFormItem>
        </template>
        <MfFormItem v-if="options.captcha" name="captcha" :rules="[{ required: true, message: $t('auth.field.captchaRequired') }]">
          <AuthCaptcha v-model="form.captcha" :image="captcha.image.value" :loading="captcha.loading.value" @refresh="captcha.refresh"/>
        </MfFormItem>
        <MfFormItem name="password" :rules="[{ required: true, message: $t('auth.field.passwordRequired') }, { min: 6, max: 20, message: $t('auth.field.passwordRule') }]">
          <MfInput v-model="form.password" size="lg" show-password :placeholder="$t('auth.field.newPassword')">
            <template #prefix><Lock :size="17"/></template>
          </MfInput>
        </MfFormItem>
        <MfFormItem name="confirm" :rules="[{ required: true, message: $t('auth.field.confirmRequired') }, { validator: value => value === form.password || $t('auth.field.confirmMismatch') }]">
          <MfInput v-model="form.confirm" size="lg" show-password :placeholder="$t('auth.field.confirm')">
            <template #prefix><Lock :size="17"/></template>
          </MfInput>
        </MfFormItem>
        <MfFormItem v-if="agreements(options).length" name="agree" :rules="[{ validator: value => value === true || $t('auth.signup.agreeRequired') }]">
          <MfCheckbox v-model="form.agree">
            <span class="text-content">
              {{ $t('auth.signup.agree') }}
              <template v-for="(item, index) in agreements(options)" :key="item.key">
                <template v-if="index > 0">{{ $t('auth.signup.and') }}</template>
                <a :href="item.url" target="_blank" rel="noopener noreferrer" class="font-medium text-primary no-underline hover:underline" @click.stop>{{ $t(`auth.signup.${item.key}`) }}</a>
              </template>
            </span>
          </MfCheckbox>
        </MfFormItem>

        <MfButton variant="primary" type="submit" size="lg" block :loading="submitting">{{ $t('auth.signup.submit') }}</MfButton>
        <p class="m-0 text-center text-sm text-content-secondary">
          {{ $t('auth.signup.hasAccount') }}
          <router-link :to="{ path: '/auth/signin', query: redirectQuery }" replace class="font-medium text-primary no-underline hover:underline">{{ $t('auth.signup.toSignin') }}</router-link>
        </p>
      </MfForm>
    </template>
  </AuthShell>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Lock, Mail, User } from '@lucide/vue'
import UserService from '@/services/user'
import { useRouteRedirect } from '@/composables/useRouteRedirect'
import { useAuthCaptcha } from '@/composables/useAuthCaptcha'
import { useAuthOptions } from '@/composables/useAuthOptions'
import { useEmailCode } from '@/composables/useEmailCode'
import { agreements, EMAIL_PATTERN, USERNAME_PATTERN, type AuthOptions } from '@/model/system-setting'
import AuthShell from '../components/AuthShell.vue'
import AuthCaptcha from '../components/AuthCaptcha.vue'
import AuthCodeField from '../components/AuthCodeField.vue'

defineOptions({ name: 'AuthSignup' })

const router = useRouter()
const { t } = useI18n()
const { query: redirectQuery } = useRouteRedirect()
const { options } = useAuthOptions()
const captcha = useAuthCaptcha()
const code = useEmailCode('SIGNUP')
const formRef = ref()
const form = ref({ username: '', email: '', code: '', captcha: '', password: '', confirm: '', agree: false })
const submitting = ref(false)

watch(() => options.value?.registration && options.value?.captcha, enabled => { if (enabled) captcha.refresh() }, { immediate: true })

async function onSubmit(current: AuthOptions) {
  if (submitting.value) return
  submitting.value = true
  const response = await UserService.signup({
    username: form.value.username.trim(), password: form.value.password,
    email: current.emailVerification ? form.value.email.trim() : undefined, code: current.emailVerification ? form.value.code.trim() : undefined,
    captcha: current.captcha ? Number(form.value.captcha) : undefined, timestamp: current.captcha ? captcha.timestamp.value : undefined,
  })
  submitting.value = false
  if (!response.status) {
    MfMessage.error(response.message)
    if (current.captcha) { form.value.captcha = ''; captcha.refresh() }
    return
  }
  MfMessage.success(t('auth.signup.done'))
  router.replace({ path: '/auth/signin', query: redirectQuery.value })
}
</script>
