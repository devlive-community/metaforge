<template>
  <div class="flex flex-col gap-4 lg:flex-row lg:items-start">
    <!-- Left: one tab per group, each with how much of it is on. -->
    <aside class="flex w-full shrink-0 flex-col gap-4 rounded-mf-lg border border-line bg-surface p-4 lg:sticky lg:top-4 lg:w-64">
      <div class="flex items-center gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-primary"><SlidersHorizontal :size="18"/></span>
        <div class="min-w-0">
          <h1 class="m-0 text-base font-semibold text-content">{{ $t('navigation.systemSettings') }}</h1>
          <p class="m-0 mt-0.5 text-xs text-content-tertiary">{{ $t('settings.subtitle') }}</p>
        </div>
      </div>
      <nav class="flex flex-col gap-1" :aria-label="$t('navigation.systemSettings')">
        <button v-for="item in tabs" :key="item.key" type="button" class="flex items-center gap-3 rounded-mf-md px-3 py-2.5 text-left outline-none transition-colors focus-visible:ring-2 focus-visible:ring-primary/40"
                :class="item.key === tab ? 'bg-primary/10 text-primary' : 'text-content-secondary hover:bg-surface-hover hover:text-content'"
                :aria-current="item.key === tab ? 'page' : undefined" @click="select(item.key)">
          <component :is="item.icon" :size="17" class="shrink-0"/>
          <span class="flex min-w-0 flex-1 flex-col">
            <span class="truncate text-sm font-semibold">{{ $t(`settings.${item.key}.title`) }}</span>
            <span class="truncate text-xs text-content-tertiary">{{ item.status }}</span>
          </span>
        </button>
      </nav>
    </aside>

    <!-- Right: the chosen group. Every group stays mounted so one save checks them all. -->
    <section class="flex min-w-0 flex-1 flex-col rounded-mf-lg border border-line bg-surface">
      <header class="flex flex-wrap items-center justify-between gap-4 border-b border-line px-6 py-4">
        <div class="min-w-0">
          <h2 class="m-0 flex items-center gap-2 text-lg font-semibold text-content">{{ $t(`settings.${tab}.title`) }}
            <MfTag v-if="tab === 'mail'" size="sm" :variant="ready ? 'success' : 'default'">{{ $t(ready ? 'settings.mail.ready' : 'settings.mail.notReady') }}</MfTag></h2>
          <p class="m-0 mt-1 text-sm text-content-tertiary">{{ $t(`settings.${tab}.description`) }}</p>
        </div>
        <MfButton variant="primary" :loading="saving" :disabled="loading" @click="save"><Save :size="15"/>{{ $t('settings.save') }}</MfButton>
      </header>

      <div v-if="loading" class="flex items-center justify-center py-24"><MfSpinner/></div>
      <div v-else class="px-6 py-5">
        <div v-show="tab === 'auth'" class="flex flex-col gap-4">
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
        </div>
        <div v-show="tab === 'mail'" class="flex flex-col gap-4">
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
        </div>
        <div v-show="tab === 'oauth'" class="flex flex-col gap-4">
          <MfForm ref="siteRef" :model="auth" layout="vertical">
            <MfFormItem name="siteUrl" :label="$t('settings.oauth.siteUrl')" :rules="[{ validator: optionalUrl }]">
              <MfInput v-model="auth.siteUrl" :placeholder="origin"/>
            </MfFormItem>
            <p class="m-0 text-xs text-content-tertiary">{{ $t('settings.oauth.siteUrlHint') }}</p>
          </MfForm>
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2 2xl:grid-cols-3">
            <div v-for="id in OAUTH_PROVIDERS" :key="id" class="flex min-w-0 flex-col gap-3 rounded-mf-md border border-line p-4">
              <div class="flex items-center justify-between gap-3">
                <span class="flex min-w-0 items-center gap-2">
                  <AuthProviderIcon :provider="id" :size="20" class="shrink-0"/>
                  <span class="truncate text-sm font-semibold text-content">{{ $t(`settings.oauth.${id}`) }}</span>
                  <MfTag size="sm" class="shrink-0" :variant="status(id).variant">{{ $t(`settings.oauth.status.${status(id).key}`) }}</MfTag>
                </span>
                <MfSwitch v-model="oauth[id].enabled" class="shrink-0" :aria-label="$t(`settings.oauth.${id}`)"/>
              </div>
              <MfAlert v-if="status(id).key === 'incomplete'" variant="warning" :title="$t(id === 'oidc' ? 'settings.oauth.incompleteOidc' : 'settings.oauth.incomplete')"/>
              <div v-for="field in fields(id)" :key="field" class="flex min-w-0 flex-col gap-1.5">
                <span class="text-sm font-medium text-content">{{ $t(`settings.oauth.field.${field}`) }}</span>
                <MfInput v-model="oauth[id][field]" :show-password="field === 'clientSecret'" :placeholder="placeholder(id, field)"
                         :aria-label="`${ $t(`settings.oauth.${id}`) } ${ $t(`settings.oauth.field.${field}`) }`"/>
              </div>
              <div class="flex min-w-0 flex-col gap-1.5">
                <span class="text-sm font-medium text-content">{{ $t('settings.oauth.callback') }}</span>
                <div class="flex items-center gap-2">
                  <MfInput :model-value="callback(id)" readonly class="min-w-0 flex-1" :aria-label="$t('settings.oauth.callback')"/>
                  <MfTooltip :content="$t('settings.oauth.copy')">
                    <MfButton class="shrink-0" :aria-label="$t('settings.oauth.copy')" @click="copy(callback(id))"><Copy :size="15"/></MfButton>
                  </MfTooltip>
                </div>
                <span class="text-xs text-content-tertiary">{{ $t('settings.oauth.callbackHint') }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Copy, KeyRound, Mail, Save, Send, ShieldCheck, SlidersHorizontal } from '@lucide/vue'
import SystemSettingService from '@/services/system-setting'
import { apiBaseUrl } from '@/utils/http'
import {
  defaultAuthSettings, defaultMailSettings, defaultOAuthGroup, defaultOAuthSettings, EMAIL_PATTERN, incompleteProviders, mailReady, OAUTH_PROVIDERS,
  oauthCallbackUrl, oauthConfigured, URL_PATTERN, waitingForMail,
} from '@/model/system-setting'
import type { OAuthProviderId, OAuthSettings } from '@/model/system-setting'
import AuthProviderIcon from '@/views/auth/components/AuthProviderIcon.vue'

defineOptions({ name: 'SettingsHome' })

const SWITCHES = ['registration', 'captcha', 'rememberMe', 'emailSignin', 'emailVerification', 'passwordReset'] as const
const MAIL_FEATURES: readonly string[] = ['emailVerification', 'passwordReset']

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
type Tab = 'auth' | 'mail' | 'oauth'
const TABS: Tab[] = ['auth', 'mail', 'oauth']
// The tab is kept in ?tab= so a refresh stays on it.
const tab = computed<Tab>(() => TABS.includes(route.query.tab as Tab) ? route.query.tab as Tab : 'auth')
const select = (key: Tab) => { if (key !== tab.value) router.replace({ query: { ...route.query, tab: key } }) }
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const testTo = ref('')
const auth = ref(defaultAuthSettings())
const mail = ref(defaultMailSettings())
const oauth = ref(defaultOAuthGroup())
const authRef = ref()
const mailRef = ref()
const siteRef = ref()
// Where the browser reaches the server; the callback address uses it while no site address is set.
const origin = apiBaseUrl()

type OAuthField = 'clientId' | 'clientSecret' | 'name' | 'issuer' | 'authorizationUrl' | 'tokenUrl' | 'userinfoUrl' | 'scopes'
const CLIENT_FIELDS: OAuthField[] = ['clientId', 'clientSecret']
const OIDC_FIELDS: OAuthField[] = [...CLIENT_FIELDS, 'name', 'issuer', 'authorizationUrl', 'tokenUrl', 'userinfoUrl', 'scopes']
const fields = (id: OAuthProviderId) => id === 'oidc' ? OIDC_FIELDS : CLIENT_FIELDS
const callback = (id: OAuthProviderId) => oauthCallbackUrl(auth.value.siteUrl, origin, id)

function placeholder(id: OAuthProviderId, field: OAuthField) {
  if (field === 'clientSecret') return oauth.value[id].secretSet ? t('settings.oauth.secretKept') : ''
  if (field === 'name') return 'SSO'
  if (field.endsWith('Url') || field === 'issuer') return 'https://'
  return ''
}

function status(id: OAuthProviderId) {
  const settings = oauth.value[id]
  if (!settings.enabled) return { key: 'off', variant: 'default' } as const
  return oauthConfigured(id, settings) ? { key: 'on', variant: 'success' } as const : { key: 'incomplete', variant: 'warning' } as const
}

async function copy(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    MfMessage.success(t('settings.oauth.copied'))
  }
  catch {
    MfMessage.error(t('settings.oauth.copyFailed'))
  }
}

const ready = computed(() => mailReady(mail.value))
const tabs = computed(() => [
  { key: 'auth' as const, icon: ShieldCheck, status: t('settings.tabStatus.auth', { count: SWITCHES.filter(key => auth.value[key]).length }) },
  { key: 'mail' as const, icon: Mail, status: t(ready.value ? 'settings.mail.ready' : 'settings.mail.notReady') },
  { key: 'oauth' as const, icon: KeyRound, status: t('settings.tabStatus.oauth', { count: OAUTH_PROVIDERS.filter(id => oauth.value[id].enabled).length }) },
])
const waiting = computed(() => waitingForMail(auth.value, mail.value))
// The mail server is required once a feature that sends mail is on.
const needsMail = computed(() => auth.value.emailVerification || auth.value.passwordReset)
const optionalUrl = (value: unknown) => !String(value || '').trim() || URL_PATTERN.test(String(value).trim()) || t('settings.auth.urlInvalid')

function apply(data: any) {
  auth.value = { ...defaultAuthSettings(), ...(data?.auth || {}) }
  mail.value = { ...defaultMailSettings(), ...(data?.mail || {}), password: '' }
  const group = defaultOAuthGroup()
  for (const id of OAUTH_PROVIDERS) group[id] = { ...defaultOAuthSettings(), ...(data?.oauth?.[id] || {}) as Partial<OAuthSettings>, clientSecret: '' }
  oauth.value = group
}

async function load() {
  loading.value = true
  const response = await SystemSettingService.get()
  loading.value = false
  if (response.status) apply(response.data)
  else MfMessage.error(response.message)
}

async function save() {
  // Checked in tab order; a failing group is brought forward so its errors can be seen.
  for (const [key, form] of [['auth', authRef], ['mail', mailRef], ['oauth', siteRef]] as const) {
    try {
      await form.value?.validate()
    }
    catch {
      select(key)
      return
    }
  }
  const incomplete = incompleteProviders(oauth.value)
  if (incomplete.length) {
    select('oauth')
    MfMessage.error(t('settings.oauth.incompleteSave', { providers: incomplete.map(id => t(`settings.oauth.${id}`)).join('、') }))
    return
  }
  saving.value = true
  const response = await SystemSettingService.save(auth.value, mail.value, oauth.value)
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
