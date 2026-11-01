<template>
  <div class="flex flex-col gap-4">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div class="min-w-0">
        <h1 class="m-0 text-xl font-semibold text-content">{{ $t('navigation.systemSettings') }}</h1>
        <p class="m-0 mt-1 text-sm text-content-tertiary">{{ $t('settings.description') }}</p>
      </div>
      <MfButton variant="primary" :loading="saving" :disabled="loading" @click="save"><Save :size="15"/>{{ $t('settings.save') }}</MfButton>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-24"><MfSpinner/></div>
    <div v-else class="grid grid-cols-1 items-start gap-4 xl:grid-cols-2">
      <section class="flex min-w-0 flex-col gap-4 rounded-mf-lg border border-line bg-surface p-5">
        <div>
          <h2 class="m-0 text-base font-semibold text-content">{{ $t('settings.auth.title') }}</h2>
          <p class="m-0 mt-1 text-xs text-content-tertiary">{{ $t('settings.auth.description') }}</p>
        </div>
        <MfAlert v-if="waiting.length" variant="warning" :title="$t('settings.auth.needsMail', { features: waiting.map(key => $t(`settings.auth.${key}`)).join('、') })"/>
        <MfForm ref="authRef" :model="auth" layout="vertical">
          <ul class="m-0 flex list-none flex-col divide-y divide-line p-0">
            <li v-for="key in SWITCHES" :key="key" class="flex items-center justify-between gap-4 py-3 first:pt-0">
              <span class="flex min-w-0 flex-col">
                <span class="flex items-center gap-2 text-sm font-semibold text-content">{{ $t(`settings.auth.${key}`) }}
                  <MfTag v-if="MAIL_FEATURES.includes(key)" size="sm" :variant="ready ? 'success' : 'default'">{{ $t(ready ? 'settings.auth.mailReady' : 'settings.auth.mailMissing') }}</MfTag></span>
                <span class="mt-0.5 text-xs text-content-tertiary">{{ $t(`settings.auth.${key}Hint`) }}</span>
              </span>
              <div class="flex shrink-0 items-center gap-3">
                <MfInputNumber v-if="key === 'rememberMe' && auth.rememberMe" v-model="auth.rememberDays" class="w-28" :min="1" :max="365" :precision="0"/>
                <span v-if="key === 'rememberMe' && auth.rememberMe" class="text-sm text-content-secondary">{{ $t('settings.auth.days') }}</span>
                <MfSwitch v-model="auth[key]" :aria-label="$t(`settings.auth.${key}`)"/>
              </div>
            </li>
          </ul>
          <div class="grid grid-cols-1 gap-4 border-t border-line pt-4 md:grid-cols-2">
            <MfFormItem name="termsUrl" :label="$t('settings.auth.termsUrl')" :rules="[{ validator: optionalUrl }]">
              <MfInput v-model="auth.termsUrl" placeholder="https://"/>
            </MfFormItem>
            <MfFormItem name="privacyUrl" :label="$t('settings.auth.privacyUrl')" :rules="[{ validator: optionalUrl }]">
              <MfInput v-model="auth.privacyUrl" placeholder="https://"/>
            </MfFormItem>
          </div>
          <p class="m-0 text-xs text-content-tertiary">{{ $t('settings.auth.agreementHint') }}</p>
        </MfForm>
      </section>

      <section class="flex min-w-0 flex-col gap-4 rounded-mf-lg border border-line bg-surface p-5">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="m-0 text-base font-semibold text-content">{{ $t('settings.mail.title') }}</h2>
            <p class="m-0 mt-1 text-xs text-content-tertiary">{{ $t('settings.mail.description') }}</p>
          </div>
          <MfTag size="sm" :variant="ready ? 'success' : 'default'">{{ $t(ready ? 'settings.mail.ready' : 'settings.mail.notReady') }}</MfTag>
        </div>
        <MfForm ref="mailRef" :model="mail" layout="vertical">
          <div class="grid grid-cols-1 gap-4 md:grid-cols-[minmax(0,1fr)_9rem]">
            <MfFormItem name="host" :label="$t('settings.mail.host')" :rules="[{ validator: value => !needsMail || !!String(value || '').trim() || $t('settings.mail.hostRequired') }]">
              <MfInput v-model="mail.host" placeholder="smtp.example.com"/>
            </MfFormItem>
            <MfFormItem name="port" :label="$t('settings.mail.port')" :rules="[{ required: true, message: $t('settings.mail.portRequired') }]">
              <MfInputNumber v-model="mail.port" class="w-full" :min="1" :max="65535" :precision="0"/>
            </MfFormItem>
          </div>
          <div class="flex items-center justify-between gap-4">
            <span class="flex min-w-0 flex-col"><span class="text-sm font-semibold text-content">{{ $t('settings.mail.ssl') }}</span>
              <span class="mt-0.5 text-xs text-content-tertiary">{{ $t('settings.mail.sslHint') }}</span></span>
            <MfSwitch v-model="mail.ssl" :aria-label="$t('settings.mail.ssl')"/>
          </div>
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <MfFormItem name="username" :label="$t('settings.mail.username')">
              <MfInput v-model="mail.username"/>
            </MfFormItem>
            <MfFormItem name="password" :label="$t('settings.mail.password')">
              <MfInput v-model="mail.password" show-password :placeholder="mail.passwordSet ? $t('settings.mail.passwordKept') : ''"/>
            </MfFormItem>
          </div>
          <MfFormItem name="from" :label="$t('settings.mail.from')"
                      :rules="[{ validator: value => (!needsMail && !String(value || '').trim()) || EMAIL_PATTERN.test(String(value || '').trim()) || $t('settings.mail.fromInvalid') }]">
            <MfInput v-model="mail.from" placeholder="noreply@example.com"/>
          </MfFormItem>
        </MfForm>
        <div class="flex flex-col gap-2 border-t border-line pt-4">
          <span class="text-sm font-semibold text-content">{{ $t('settings.mail.test') }}</span>
          <div class="flex items-center gap-2">
            <MfInput v-model="testTo" class="min-w-0 flex-1" :placeholder="$t('settings.mail.testTo')"/>
            <MfButton class="shrink-0" :loading="testing" :disabled="!ready || !EMAIL_PATTERN.test(testTo.trim())" @click="test"><Send :size="15"/>{{ $t('settings.mail.testSend') }}</MfButton>
          </div>
          <span class="text-xs text-content-tertiary">{{ $t('settings.mail.testHint') }}</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Save, Send } from '@lucide/vue'
import SystemSettingService from '@/services/system-setting'
import { defaultAuthSettings, defaultMailSettings, EMAIL_PATTERN, mailReady, URL_PATTERN, waitingForMail } from '@/model/system-setting'

defineOptions({ name: 'SettingsHome' })

const SWITCHES = ['registration', 'captcha', 'rememberMe', 'emailSignin', 'emailVerification', 'passwordReset'] as const
const MAIL_FEATURES: readonly string[] = ['emailVerification', 'passwordReset']

const { t } = useI18n()
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const testTo = ref('')
const auth = ref(defaultAuthSettings())
const mail = ref(defaultMailSettings())
const authRef = ref()
const mailRef = ref()

const ready = computed(() => mailReady(mail.value))
const waiting = computed(() => waitingForMail(auth.value, mail.value))
// The mail server is required once a feature that sends mail is on.
const needsMail = computed(() => auth.value.emailVerification || auth.value.passwordReset)
const optionalUrl = (value: unknown) => !String(value || '').trim() || URL_PATTERN.test(String(value).trim()) || t('settings.auth.urlInvalid')

function apply(data: any) {
  auth.value = { ...defaultAuthSettings(), ...(data?.auth || {}) }
  mail.value = { ...defaultMailSettings(), ...(data?.mail || {}), password: '' }
}

async function load() {
  loading.value = true
  const response = await SystemSettingService.get()
  loading.value = false
  if (response.status) apply(response.data)
  else MfMessage.error(response.message)
}

async function save() {
  try {
    await Promise.all([authRef.value?.validate(), mailRef.value?.validate()])
  }
  catch {
    return
  }
  saving.value = true
  const response = await SystemSettingService.save(auth.value, mail.value)
  saving.value = false
  if (!response.status) { MfMessage.error(response.message); return }
  apply(response.data)
  MfMessage.success(t('settings.saved'))
}

async function test() {
  testing.value = true
  const response = await SystemSettingService.testMail(mail.value, testTo.value.trim())
  testing.value = false
  if (response.status) MfMessage.success(t('settings.mail.testSent', { to: testTo.value.trim() }))
  else MfMessage.error(response.message)
}

onMounted(load)
</script>
