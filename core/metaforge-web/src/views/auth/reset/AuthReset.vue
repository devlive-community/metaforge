<template>
  <AuthShell tab="reset">
    <template #default="{ options }">
      <MfResult v-if="!options.passwordReset" status="info" :title="$t('auth.reset.closed')" :sub-title="$t('auth.reset.closedHint')">
        <template #extra>
          <MfButton variant="primary" @click="$router.replace({ path: '/auth/signin', query: redirectQuery })">{{ $t('auth.reset.back') }}</MfButton>
        </template>
      </MfResult>
      <MfForm v-else ref="formRef" :model="form" layout="vertical" @finish="onSubmit">
        <p class="m-0 text-sm text-content-secondary">{{ $t('auth.reset.hint') }}</p>
        <MfFormItem name="email" :rules="[{ required: true, message: $t('auth.field.emailRequired') }, { pattern: EMAIL_PATTERN, message: $t('auth.field.emailInvalid') }]">
          <MfInput v-model="form.email" size="lg" name="email" autocomplete="email" :placeholder="$t('auth.field.email')">
            <template #prefix><Mail :size="17"/></template>
          </MfInput>
        </MfFormItem>
        <MfFormItem name="code" :rules="[{ required: true, message: $t('auth.field.codeRequired') }, { pattern: /^\d{6}$/, message: $t('auth.field.codeRule') }]">
          <AuthCodeField v-model="form.code" :label="code.label.value" :disabled="code.disabled.value || !EMAIL_PATTERN.test(form.email.trim())" @send="code.send(form.email)"/>
        </MfFormItem>
        <MfFormItem name="password" :rules="[{ required: true, message: $t('auth.field.passwordRequired') }, { min: 6, max: 20, message: $t('auth.field.passwordRule') }]">
          <MfInput v-model="form.password" size="lg" show-password name="new-password" autocomplete="new-password" :placeholder="$t('auth.field.newPassword')">
            <template #prefix><Lock :size="17"/></template>
          </MfInput>
        </MfFormItem>
        <MfFormItem name="confirm" :rules="[{ required: true, message: $t('auth.field.confirmRequired') }, { validator: value => value === form.password || $t('auth.field.confirmMismatch') }]">
          <MfInput v-model="form.confirm" size="lg" show-password name="confirm-password" autocomplete="new-password" :placeholder="$t('auth.field.confirm')">
            <template #prefix><Lock :size="17"/></template>
          </MfInput>
        </MfFormItem>

        <MfButton variant="primary" type="submit" size="lg" block :loading="submitting">{{ $t('auth.reset.submit') }}</MfButton>
        <p class="m-0 text-center text-sm">
          <router-link :to="{ path: '/auth/signin', query: redirectQuery }" replace class="font-medium text-primary no-underline hover:underline">{{ $t('auth.reset.back') }}</router-link>
        </p>
      </MfForm>
    </template>
  </AuthShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Lock, Mail } from '@lucide/vue'
import AuthService from '@/services/auth'
import { useRouteRedirect } from '@/composables/useRouteRedirect'
import { useEmailCode } from '@/composables/useEmailCode'
import { EMAIL_PATTERN } from '@/model/system-setting'
import AuthShell from '../components/AuthShell.vue'
import AuthCodeField from '../components/AuthCodeField.vue'

defineOptions({ name: 'AuthReset' })

const router = useRouter()
const { t } = useI18n()
const { query: redirectQuery } = useRouteRedirect()
const code = useEmailCode('RESET')
const formRef = ref()
const form = ref({ email: '', code: '', password: '', confirm: '' })
const submitting = ref(false)

async function onSubmit() {
  if (submitting.value) return
  submitting.value = true
  const response = await AuthService.reset({ email: form.value.email.trim(), code: form.value.code.trim(), password: form.value.password })
  submitting.value = false
  if (!response.status) { MfMessage.error(response.message); return }
  MfMessage.success(t('auth.reset.done'))
  router.replace({ path: '/auth/signin', query: redirectQuery.value })
}
</script>
