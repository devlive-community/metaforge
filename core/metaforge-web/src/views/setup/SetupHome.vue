<template>
  <BaseLayout>
    <div class="min-h-dvh flex items-center justify-center py-8">
      <div class="w-full max-w-xl px-4 mx-auto">
        <MfCard class="w-full" padding="none">
          <template #header>
            <div class="flex items-center justify-center gap-3">
              <MfAvatar src="/static/images/logo.png" alt="MetaForge"/>
              <div>
                <div class="text-base font-semibold">{{ $t('setup.title') }}</div>
                <div class="text-xs text-content-tertiary">{{ $t('setup.subtitle') }}</div>
              </div>
            </div>
          </template>

          <!-- 自绘步骤条 -->
          <div class="grid grid-cols-4 px-6 pt-6 pb-6">
            <div v-for="(label, index) in stepLabels" :key="label" class="relative flex min-w-0 flex-col items-center gap-2 text-center">
              <span v-if="index < stepLabels.length - 1" class="absolute top-3.5 left-[calc(50%+1rem)] right-[calc(-50%+1rem)] h-px" :class="step > index ? 'bg-primary' : 'bg-line'" aria-hidden="true"/>
              <span
                class="relative flex size-7 shrink-0 items-center justify-center rounded-mf-full text-xs font-semibold transition-colors duration-(--mf-duration-base)"
                :class="step > index ? 'bg-primary text-on-primary' : step === index ? 'border-2 border-primary text-primary' : 'border border-line text-content-tertiary'">
                <svg v-if="step > index" class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true">
                  <path d="M5 13l4 4L19 7" />
                </svg>
                <template v-else>{{ index + 1 }}</template>
              </span>
              <span class="text-xs" :class="step >= index ? 'text-content' : 'text-content-tertiary'">{{ label }}</span>
            </div>
          </div>

          <div class="px-6 pb-6">
            <!-- Step 1: environment -->
            <div v-if="step === 0">
              <div v-if="envLoading" class="flex items-center justify-center py-8">
                <MfSpinner size="lg" label="Loading..."/>
              </div>
              <template v-else>
                <MfAlert v-if="envError" variant="danger" :title="$t('common.loadingFailed')">
                  {{ envError }}
                  <MfButton class="mt-3" @click="loadEnvironment">{{ $t('setup.install.retry') }}</MfButton>
                </MfAlert>
                <MfAlert v-else-if="!environment.currentAvailable" variant="info" :title="$t('setup.env.notConfigured')">
                  {{ $t('setup.env.chooseDatabase') }}
                </MfAlert>
                <div v-else class="grid gap-3">
                  <div class="flex items-center justify-between rounded-mf-md border border-line px-4 py-3 text-sm">
                    <span class="text-content-tertiary">{{ $t('setup.env.database') }}</span>
                    <span class="font-medium text-content">{{ environment.database || '--' }}</span>
                  </div>
                  <div class="flex items-center justify-between rounded-mf-md border border-line px-4 py-3 text-sm">
                    <span class="text-content-tertiary">{{ $t('setup.env.product') }}</span>
                    <span class="font-medium text-content">
                      {{ environment.product || '--' }}
                      <MfTag v-if="environment.embedded" variant="warning" size="sm">{{ $t('setup.env.embeddedMode') }}</MfTag>
                    </span>
                  </div>
                  <div class="flex items-center justify-between rounded-mf-md border border-line px-4 py-3 text-sm">
                    <span class="text-content-tertiary">{{ $t('setup.env.version') }}</span>
                    <span class="max-w-[60%] truncate font-medium text-content">{{ environment.version || '--' }}</span>
                  </div>
                </div>

                <div v-if="environment.currentAvailable" class="mt-4 flex items-center gap-3">
                  <MfButton variant="outline" :loading="testing" @click="onTestConnection()">
                    {{ testing ? $t('setup.env.testing') : $t('setup.env.test') }}
                  </MfButton>
                  <MfTag v-if="testResult === 'ok'" variant="success">{{ $t('setup.env.connected') }} · {{ testElapsed }}ms</MfTag>
                  <MfTag v-else-if="testResult === 'failed'" variant="danger">{{ $t('setup.env.testFailed') }}</MfTag>
                  <MfTag v-else-if="environment.connected === false" variant="danger">{{ $t('setup.env.disconnected') }}</MfTag>
                  <MfTag v-else variant="info">{{ $t('setup.env.connected') }}</MfTag>
                </div>

                <div class="mt-6 flex justify-end">
                  <MfButton variant="primary" :disabled="Boolean(envError)" @click="step = 1">
                    {{ $t('setup.next') }}
                  </MfButton>
                </div>
              </template>
            </div>

            <!-- Step 2: database target -->
            <div v-else-if="step === 1">
              <MfAlert v-if="environment.degraded" variant="warning" :title="$t('setup.database.degradedTitle')" class="mb-4">
                {{ $t('setup.database.degradedTip') }} <span class="font-mono">{{ environment.degradedFrom }}</span>
              </MfAlert>
              <MfRadioGroup v-model="databaseType" class="flex-col! items-stretch gap-3">
                <label v-if="environment.currentAvailable && environment.connected" class="flex cursor-pointer items-start gap-3 rounded-mf-md border p-4 transition-colors duration-(--mf-duration-fast)"
                       :class="databaseType === 'current' ? 'border-primary bg-surface-active' : 'border-line hover:border-line-strong'">
                  <MfRadio value="current"/>
                  <div>
                    <div class="text-sm font-medium text-content">{{ $t('setup.database.current') }}</div>
                    <div class="mt-1 text-xs text-content-tertiary">{{ $t('setup.database.currentTip') }}</div>
                  </div>
                </label>
                <label class="flex cursor-pointer items-start gap-3 rounded-mf-md border p-4 transition-colors duration-(--mf-duration-fast)"
                       :class="databaseType === 'embedded' ? 'border-primary bg-surface-active' : 'border-line hover:border-line-strong'">
                  <MfRadio value="embedded"/>
                  <div>
                    <div class="text-sm font-medium text-content">{{ $t('setup.database.embedded') }}</div>
                    <div class="mt-1 text-xs text-content-tertiary">{{ $t('setup.database.embeddedTip') }}</div>
                  </div>
                </label>
                <label class="flex cursor-pointer items-start gap-3 rounded-mf-md border p-4 transition-colors duration-(--mf-duration-fast)"
                       :class="databaseType === 'mysql' ? 'border-primary bg-surface-active' : 'border-line hover:border-line-strong'">
                  <MfRadio value="mysql"/>
                  <div class="flex-1">
                    <div class="text-sm font-medium text-content">{{ $t('setup.database.mysql') }}</div>
                    <div class="mt-1 text-xs text-content-tertiary">{{ $t('setup.database.mysqlTip') }}</div>
                  </div>
                </label>
              </MfRadioGroup>
              <p v-if="!databaseType" class="mt-3 text-xs text-content-tertiary">{{ $t('setup.database.choose') }}</p>

              <div v-if="databaseType === 'mysql'" class="mt-5 space-y-4 rounded-mf-lg border border-line bg-page/60 p-4">
                <div>
                  <h2 class="text-sm font-medium text-content">{{ $t('setup.database.connectionSettings') }}</h2>
                  <p class="mt-1 text-xs leading-5 text-content-tertiary">{{ $t('setup.database.connectionSettingsTip') }}</p>
                </div>
                <div class="grid gap-4 sm:grid-cols-2">
                  <MfFormItem name="databaseHost" :label="$t('setup.database.host')">
                    <MfInput v-model="databaseConfigure.host" placeholder="127.0.0.1"/>
                  </MfFormItem>
                  <MfFormItem name="databasePort" :label="$t('setup.database.port')">
                    <MfInputNumber v-model="databaseConfigure.port" :controls="false" :min="1" :max="65535" placeholder="3306"/>
                  </MfFormItem>
                  <MfFormItem name="databaseName" :label="$t('setup.database.name')">
                    <MfInput v-model="databaseConfigure.database" placeholder="metaforge"/>
                  </MfFormItem>
                  <MfFormItem name="databaseUsername" :label="$t('setup.database.username')">
                    <MfInput v-model="databaseConfigure.username" placeholder="root"/>
                  </MfFormItem>
                  <MfFormItem name="databasePassword" :label="$t('setup.database.password')" class="sm:col-span-2">
                    <MfInput v-model="databaseConfigure.password" type="password" show-password :placeholder="$t('setup.database.passwordPlaceholder')"/>
                  </MfFormItem>
                </div>
                <div class="flex flex-wrap items-center gap-3 border-t border-line pt-4">
                  <MfButton :loading="dbTesting" @click="onTestDatabase">
                    {{ dbTesting ? $t('setup.env.testing') : $t('setup.env.test') }}
                  </MfButton>
                  <span v-if="dbTestResult === 'ok'" class="inline-flex items-center gap-1.5 text-xs text-success" role="status">
                    <svg class="size-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                      <circle cx="12" cy="12" r="9"/>
                      <path d="m8 12 3 3 5-6" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    {{ $t('setup.env.connected') }} · {{ dbTestElapsed }}ms
                  </span>
                </div>
                <MfAlert v-if="dbTestResult === 'failed'" variant="danger" :title="$t('setup.env.testFailed')" class="border border-danger/15">
                  <p class="mt-1 text-xs leading-5">{{ $t('setup.database.connectionFailedHint') }}</p>
                  <details v-if="dbTestFailedMessage" class="mt-2">
                    <summary class="w-fit cursor-pointer rounded-mf-sm text-xs font-medium outline-none hover:text-content focus-visible:ring-2 focus-visible:ring-primary/40">
                      {{ $t('setup.database.errorDetails') }}
                    </summary>
                    <pre class="mt-2 whitespace-pre-wrap rounded-mf-md bg-surface/80 p-3 font-mono text-xs leading-5 text-content-secondary [overflow-wrap:anywhere]">{{ dbTestFailedMessage }}</pre>
                  </details>
                </MfAlert>
              </div>

              <div class="mt-6 flex justify-between">
                <MfButton @click="step = 0">{{ $t('setup.back') }}</MfButton>
                <MfButton variant="primary" :disabled="!canContinueDatabase" @click="step = 2">
                  {{ $t('setup.next') }}
                </MfButton>
              </div>
            </div>

            <!-- Step 3: administrator account -->
            <div v-else-if="step === 2">
              <MfForm ref="formRef" :model="formState" layout="vertical" @finish="onInstall" @finish-failed="onInvalid">
                <MfFormItem name="username" :label="$t('setup.account.username')"
                            :rules="[
                              { required: true, message: $t('setup.account.usernameTip') },
                              { min: 3, message: $t('setup.account.usernameSizeTip') },
                              { max: 20, message: $t('setup.account.usernameSizeTip') }
                            ]">
                  <MfInput v-model="formState.username" :placeholder="$t('setup.account.usernameTip')"/>
                </MfFormItem>

                <MfFormItem name="password" :label="$t('setup.account.password')"
                            :rules="[
                              { required: true, message: $t('setup.account.passwordTip') },
                              { min: 6, message: $t('setup.account.passwordSizeTip') },
                              { max: 20, message: $t('setup.account.passwordSizeTip') }
                            ]">
                  <MfInput v-model="formState.password" type="password" show-password :placeholder="$t('setup.account.passwordTip')"/>
                </MfFormItem>

                <MfFormItem name="confirmPassword" :label="$t('setup.account.confirmPassword')"
                            :rules="[
                              { required: true, message: $t('setup.account.passwordTip') },
                              { validator: validatePassword }
                            ]">
                  <MfInput v-model="formState.confirmPassword" type="password" show-password :placeholder="$t('setup.account.passwordTip')"/>
                </MfFormItem>

                <MfFormItem name="name" :label="$t('setup.account.name')">
                  <MfInput v-model="formState.name" :placeholder="$t('setup.account.name')"/>
                </MfFormItem>

                <div class="mt-2 flex justify-between">
                  <MfButton @click="step = 1">{{ $t('setup.back') }}</MfButton>
                  <MfButton variant="primary" type="submit">{{ $t('setup.install.submit') }}</MfButton>
                </div>
              </MfForm>
            </div>

            <!-- Step 4: install result -->
            <div v-else class="flex flex-col items-center gap-4 py-6 text-center">
              <template v-if="installState === 'running'">
                <MfSpinner size="lg"/>
                <p class="text-sm text-content-secondary">{{ $t('setup.install.running') }}</p>
              </template>
              <template v-else-if="installState === 'success'">
                <MfAlert variant="success" :title="$t('setup.install.success')" class="text-left">
                  <div v-if="restartRequired">{{ $t('setup.install.restartTip') }}</div>
                  <div v-else>{{ $t('setup.install.successTip') }}</div>
                </MfAlert>
                <p v-if="restartRequired && configPath" class="font-mono text-xs text-content-tertiary">{{ configPath }}</p>
                <MfButton v-if="restartRequired" variant="outline" @click="onFinished">
                  {{ $t('setup.install.acknowledge') }}
                </MfButton>
                <MfButton v-else variant="primary" @click="$router.push('/auth/signin')">
                  {{ $t('setup.install.goToSignin') }}
                </MfButton>
              </template>
              <template v-else>
                <MfAlert variant="danger" :title="$t('setup.install.failed')" class="text-left">
                  {{ installMessage }}
                </MfAlert>
                <MfButton variant="outline" @click="step = 2">{{ $t('setup.install.retry') }}</MfButton>
              </template>
            </div>
          </div>
        </MfCard>
      </div>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import SetupService from '@/services/setup'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
import { invalidateSetupCheck } from '@/router'

defineOptions({ name: 'SetupHome' })

const { t } = useI18n()

const step = ref(0)
const stepLabels = computed(() => [
  t('setup.steps.environment'),
  t('setup.steps.database'),
  t('setup.steps.account'),
  t('setup.steps.finish'),
])

const envLoading = ref(true)
const environment = ref<Record<string, any>>({})
const envError = ref('')
const testing = ref(false)
const testResult = ref<'idle' | 'ok' | 'failed'>('idle')
const testElapsed = ref(0)

const databaseType = ref<'current' | 'embedded' | 'mysql' | ''>('')
const databaseConfigure = ref({
  host: '127.0.0.1',
  port: 3306,
  database: 'metaforge',
  username: 'root',
  password: '',
})
const dbTesting = ref(false)
const dbTestResult = ref<'idle' | 'ok' | 'failed'>('idle')
const dbTestElapsed = ref(0)
const dbTestFailedMessage = ref('')
const canContinueDatabase = computed(() => {
  if (databaseType.value === 'current') {
    return environment.value.currentAvailable && environment.value.connected
  }
  return databaseType.value === 'embedded' || (databaseType.value === 'mysql' && dbTestResult.value === 'ok')
})

watch(databaseConfigure, () => {
  dbTestResult.value = 'idle'
  dbTestFailedMessage.value = ''
}, { deep: true })

const formRef = ref()
const formState = ref({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
})

const installState = ref<'idle' | 'running' | 'success' | 'failed'>('idle')
const installMessage = ref('')
const restartRequired = ref(false)
const configPath = ref('')

const validatePassword = (value: unknown) => {
  if (value !== formState.value.password) {
    return t('setup.account.passwordNotMatchTip')
  }
  return true
}

const onInvalid = () => MfMessage.error(t('setup.account.passwordNotMatchTip'))

const loadEnvironment = () => {
  envLoading.value = true
  envError.value = ''
  SetupService.getEnvironment()
              .then(response => {
                if (response.status) {
                  environment.value = response.data || {}
                }
                else {
                  envError.value = response.message || t('common.loadingFailed')
                }
              })
              .catch(error => (envError.value = String(error)))
              .finally(() => (envLoading.value = false))
}

const onTestConnection = () => {
  testing.value = true
  testResult.value = 'idle'
  SetupService.testConnection()
              .then(response => {
                testResult.value = response.status && response.data?.connected ? 'ok' : 'failed'
                testElapsed.value = response.data?.elapsed ?? 0
              })
              .catch(() => (testResult.value = 'failed'))
              .finally(() => (testing.value = false))
}

const onTestDatabase = () => {
  dbTesting.value = true
  dbTestResult.value = 'idle'
  dbTestFailedMessage.value = ''
  const configure = { ...databaseConfigure.value }
  SetupService.testConnection({ type: 'mysql', ...configure })
              .then(response => {
                if (JSON.stringify(configure) !== JSON.stringify(databaseConfigure.value)) return
                if (response.status && response.data?.connected) {
                  dbTestResult.value = 'ok'
                  dbTestElapsed.value = response.data.elapsed ?? 0
                }
                else {
                  dbTestResult.value = 'failed'
                  dbTestFailedMessage.value = response.message || ''
                }
              })
              .catch((error) => {
                dbTestResult.value = 'failed'
                dbTestFailedMessage.value = String(error)
              })
              .finally(() => (dbTesting.value = false))
}

const onInstall = () => {
  if (!canContinueDatabase.value || installState.value === 'running') return
  step.value = 3
  installState.value = 'running'
  const payload: Record<string, unknown> = {
    username: formState.value.username,
    password: formState.value.password,
    name: formState.value.name,
    database: databaseType.value === 'mysql'
      ? { type: 'mysql', ...databaseConfigure.value }
      : { type: databaseType.value },
  }
  SetupService.install(payload)
              .then(response => {
                if (response.status) {
                  installState.value = 'success'
                  restartRequired.value = Boolean(response.data?.restartRequired)
                  configPath.value = response.data?.configPath || ''
                  invalidateSetupCheck()
                }
                else {
                  installState.value = 'failed'
                  installMessage.value = response.message || ''
                }
              })
              .catch((error) => {
                installState.value = 'failed'
                installMessage.value = String(error)
              })
}

const onFinished = () => {
  // 重启前停留在完成页提示；重启后用户刷新即可进入登录
  window.location.reload()
}

onMounted(loadEnvironment)
</script>
