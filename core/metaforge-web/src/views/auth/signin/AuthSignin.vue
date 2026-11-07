<template>
  <AuthShell tab="signin">
    <template #default="{ options }">
      <MfForm ref="formRef" :model="form" layout="vertical" @finish="onSubmit(options)">
        <MfFormItem name="username" :rules="[{ required: true, message: $t(options.emailSignin ? 'auth.field.accountRequired' : 'auth.field.usernameRequired') }, { max: 255 }]">
          <MfInput v-model="form.username" size="lg" name="username" autocomplete="username" :placeholder="$t(options.emailSignin ? 'auth.field.account' : 'auth.field.username')">
            <template #prefix><User :size="17"/></template>
          </MfInput>
        </MfFormItem>
        <MfFormItem name="password" :rules="[{ required: true, message: $t('auth.field.passwordRequired') }]">
          <MfInput v-model="form.password" size="lg" show-password name="password" autocomplete="current-password" :placeholder="$t('auth.field.password')">
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

      <!-- Only the providers an administrator turned on and configured; none, and the row is not there. -->
      <div v-if="options.providers.length" class="mt-7 flex flex-col gap-5">
        <MfDivider :label="$t('auth.oauth.others')"/>
        <div class="flex items-center justify-center gap-4">
          <MfTooltip v-for="provider in options.providers" :key="provider.id" :content="$t('auth.oauth.with', { name: provider.name })">
            <button type="button" class="inline-flex size-12 items-center justify-center rounded-mf-lg border border-line bg-surface text-content outline-none transition-colors hover:border-primary/60 hover:bg-primary/5 focus-visible:ring-2 focus-visible:ring-primary/40 disabled:opacity-60"
                    :aria-label="$t('auth.oauth.with', { name: provider.name })" :disabled="!!leaving" @click="signinWith(provider.id)">
              <MfSpinner v-if="leaving === provider.id"/>
              <AuthProviderIcon v-else :provider="provider.id" :size="22"/>
            </button>
          </MfTooltip>
        </div>
      </div>
    </template>
  </AuthShell>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { Lock, User } from '@lucide/vue'
import UserService from '@/services/user'
import AuthService from '@/services/auth'
import { useRouteRedirect } from '@/composables/useRouteRedirect'
import { useSigninSession } from '@/composables/useSigninSession'
import { useAuthCaptcha } from '@/composables/useAuthCaptcha'
import { useAuthOptions } from '@/composables/useAuthOptions'
import type { AuthOptions, OAuthProviderId } from '@/model/system-setting'
import AuthShell from '../components/AuthShell.vue'
import AuthCaptcha from '../components/AuthCaptcha.vue'
import AuthProviderIcon from '../components/AuthProviderIcon.vue'

defineOptions({ name: 'AuthSignin' })

// 登录成功回到被拦截前的原地址（未登录访问受保护页、登录过期都会带上 redirect）。
const { query: redirectQuery, original } = useRouteRedirect()
const { complete } = useSigninSession()
const { options } = useAuthOptions()
const captcha = useAuthCaptcha()
const formRef = ref()
const form = ref({ username: '', password: '', captcha: '', remember: false })
const submitting = ref(false)
// The provider the browser is leaving for, so the buttons do not start a second sign-in.
const leaving = ref<OAuthProviderId | null>(null)
// Coming back with the browser's back button restores the page as it was left.
const resetLeaving = (event: PageTransitionEvent) => { if (event.persisted) leaving.value = null }
onMounted(() => window.addEventListener('pageshow', resetLeaving))
onBeforeUnmount(() => window.removeEventListener('pageshow', resetLeaving))

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
    await complete(response.data)
  }
  finally {
    submitting.value = false
  }
}

/** Leaves for the provider's sign-in page; the callback page finishes the sign-in. */
function signinWith(provider: OAuthProviderId) {
  leaving.value = provider
  window.location.href = AuthService.authorizeUrl(provider, original.value || undefined)
}
</script>
