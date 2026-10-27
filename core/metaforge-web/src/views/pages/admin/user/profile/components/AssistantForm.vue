<template>
  <div class="dc-ai">
    <!-- 页头：标题 + 描述 + 标语 -->
    <div class="dc-ai__pagehead">
      <div class="min-w-0">
        <div class="dc-ai__title">{{ $t('user.common.assistant') }}</div>
        <div class="dc-ai__desc">{{ $t('user.tip.assistantPage') }}</div>
      </div>
      <div class="dc-ai__slogan">AI for Better Data</div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <!-- 基础配置 -->
      <div class="lg:col-span-2">
        <div class="dc-ai__card">
          <div class="dc-ai__section-head">
            <div class="dc-ai__section-icon">
              <Settings :size="16" />
            </div>
            <div>
              <div class="dc-ai__section-title">{{ $t('user.common.basicConfig') }}</div>
              <div class="dc-ai__section-sub">{{ $t('user.tip.basicConfig') }}</div>
            </div>
          </div>

          <div class="relative">
            <MfForm v-if="formState" layout="vertical" class="dc-ai__form">
              <MfFormItem name="host" required-mark :label="$t('user.common.host')">
                <MfInput v-model="formState.host"
                         :placeholder="$t('user.placeholder.host')"/>
                <div class="dc-ai__hint">{{ $t('user.tip.host') }}</div>
              </MfFormItem>

              <MfFormItem name="token" required-mark :label="$t('user.common.apiToken')">
                <MfInput v-model="formState.token" type="password" show-password>
                  <template #suffix>
                    <span class="dc-ai__kbd">🔑</span>
                  </template>
                </MfInput>
                <div class="dc-ai__hint">{{ $t('user.tip.token') }}</div>
              </MfFormItem>

              <MfFormItem name="timeout" required-mark :label="$t('user.common.timeoutSeconds')">
                <MfInputNumber v-model="formState.timeout"
                                class="w-full"
                                :min="1"
                                :max="600"/>
                <div class="dc-ai__hint">{{ $t('user.tip.timeout') }}</div>
              </MfFormItem>

              <MfFormItem name="contentCount" required-mark :label="$t('user.common.contextCount')">
                <MfInputNumber v-model="formState.contentCount"
                                class="w-full"
                                :min="1"
                                :max="20"/>
                <div class="dc-ai__hint">{{ $t('user.tip.contentCount') }}</div>
              </MfFormItem>
            </MfForm>

            <div class="dc-ai__actions">
              <MfButton variant="primary"
                        :loading="loading.submitting"
                        :disabled="!canSubmit"
                        @click="onSubmit">
                {{ $t('user.common.saveConfig') }}
              </MfButton>
              <MfButton :loading="testing" @click="onTestConnection">
                {{ $t('user.common.testConnection') }}
              </MfButton>
            </div>

            <div v-if="loading.default" class="absolute inset-0 z-10 flex items-center justify-center">
              <MfSpinner size="lg" label="Loading..."/>
            </div>
          </div>
        </div>
      </div>

      <!-- 右列：配置说明 + 快速配置 -->
      <div>
        <div class="dc-ai__card">
          <div class="dc-ai__section-head">
            <div class="dc-ai__section-icon">
              <BookOpen :size="16" />
            </div>
            <div class="dc-ai__section-title">{{ $t('user.common.configGuide') }}</div>
          </div>

          <div class="dc-ai__guide-list">
            <div v-for="(guide, index) in guides"
                 :key="guide.titleKey"
                 class="dc-ai__guide-item">
              <span class="dc-ai__guide-index">{{ index + 1 }}</span>
              <div class="min-w-0">
                <div class="dc-ai__guide-title">{{ $t(guide.titleKey) }}</div>
                <div class="dc-ai__guide-desc">{{ $t(guide.descKey) }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="dc-ai__card">
          <div class="dc-ai__section-head">
            <div class="dc-ai__section-icon">
              <Zap :size="16" />
            </div>
            <div>
              <div class="dc-ai__section-title">{{ $t('user.common.quickConfig') }}</div>
              <div class="dc-ai__section-sub">{{ $t('user.tip.quickConfig') }}</div>
            </div>
          </div>

          <div class="dc-ai__providers">
            <div v-for="provider in providers"
                 :key="provider.name"
                 class="dc-ai__provider"
                 @click="selectProvider(provider)">
              <span class="dc-ai__provider-icon" :style="{ background: provider.tint }">
                <component :size="15" :is="provider.icon" :style="{ color: provider.color }"/>
              </span>
              <span class="dc-ai__provider-name">{{ provider.name }}</span>
            </div>
          </div>

          <div class="dc-ai__provider-tip">
            <InfoCircleOutlined :style="{ fontSize: '12px' }"/>
            <span>{{ $t('user.tip.quickConfigHint') }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { BookOpen, Compass, Plug, Settings, Zap } from '@lucide/vue'
import UserService from '@/services/user'
import { UserChatModel } from '@/model/user'
import Common from '@/utils/common'
import { isEmpty } from 'lodash'

defineOptions({ name: 'AssistantForm' })

const { t } = useI18n()

const loading = reactive({
  default: false,
  submitting: false
})
const testing = ref(false)
const formState = ref<UserChatModel | null>(null)

const providers = computed(() => ([
  { name: 'OpenAI', host: 'https://api.openai.com/v1', icon: Plug, tint: 'rgba(0,0,0,0.06)', color: 'rgba(0,0,0,0.8)' },
  { name: 'Claude', host: 'https://api.anthropic.com/v1', icon: Zap, tint: 'rgba(217, 119, 87, 0.12)', color: '#d97757' },
  { name: 'DeepSeek', host: 'https://api.deepseek.com/v1', icon: Compass, tint: 'rgba(77, 107, 254, 0.1)', color: '#4d6bfe' },
  { name: t('user.provider.custom'), host: '', icon: Settings, tint: 'var(--dc-primary-light)', color: 'var(--dc-primary)' }
]))

const guides = [
  { titleKey: 'user.common.host', descKey: 'user.tip.guideHost' },
  { titleKey: 'user.common.apiToken', descKey: 'user.tip.guideToken' },
  { titleKey: 'user.common.timeoutSeconds', descKey: 'user.tip.guideTimeout' },
  { titleKey: 'user.common.contextCount', descKey: 'user.tip.guideContext' }
]

const canSubmit = computed(() => {
  return !!(formState.value?.host && formState.value?.token && formState.value?.timeout && formState.value?.contentCount)
})

const handlerInitialize = () => {
  loading.default = true
  UserService.getInfo()
             .then((response) => {
               if (response.status) {
                 const configure = response.data.chatConfigure
                 if (response.data && configure && !isEmpty(configure)) {
                   formState.value = JSON.parse(configure) as UserChatModel
                 }
                 else {
                   formState.value = { host: undefined, token: undefined, timeout: 60, contentCount: 10 }
                 }
               }
             })
             .finally(() => (loading.default = false))
}

const onSubmit = () => {
  loading.submitting = true
  UserService.changeChart(formState.value as UserChatModel)
             .then((response) => {
               if (response.status) {
                 MfMessage.success(t('common.successfully') as string)
                 localStorage.setItem(Common.userEditorConfigure, JSON.stringify(formState.value))
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (loading.submitting = false))
}

const onTestConnection = () => {
  testing.value = true
  UserService.testChat(formState.value as UserChatModel)
             .then((response) => {
               if (response.status && response.data === true) {
                 MfMessage.success(t('user.tip.testSuccess') as string)
               }
               else {
                 MfMessage.error(`${ t('user.tip.testFailed') }: ${ response.message }`)
               }
             })
             .finally(() => (testing.value = false))
}

const selectProvider = (provider: { host: string }) => {
  if (formState.value) {
    formState.value.host = provider.host || undefined
  }
}

handlerInitialize()
</script>

<style scoped>
.dc-ai__pagehead {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24px;
    padding: 18px 20px;
    border-radius: var(--dc-radius-lg);
    background: var(--dc-gradient-hero);
    border: 1px solid var(--dc-border-light);
    margin-bottom: 16px;
}

.dc-ai__title {
    font-size: 22px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-ai__desc {
    font-size: 13px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-ai__slogan {
    font-size: 14px;
    font-style: italic;
    color: var(--dc-primary);
    opacity: 0.7;
    flex-shrink: 0;
    transform: rotate(-2deg);
}

.dc-ai__card {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
    margin-bottom: 16px;
}

.dc-ai__section-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
}

.dc-ai__section-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-primary-light);
    color: var(--dc-primary);
    flex-shrink: 0;
}

.dc-ai__section-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-ai__section-sub {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}

.dc-ai__hint {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 6px;
}

.dc-ai__actions {
    display: flex;
    gap: 12px;
    margin-top: 8px;
}

.dc-ai__guide-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.dc-ai__guide-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
}

.dc-ai__guide-index {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--dc-primary-light);
    color: var(--dc-primary);
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;
    margin-top: 1px;
}

.dc-ai__guide-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-ai__guide-desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
    line-height: 1.6;
}

.dc-ai__providers {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
}

.dc-ai__provider {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-md);
    background: var(--dc-bg);
    cursor: pointer;
    transition: border-color 0.2s, box-shadow 0.2s;
}

.dc-ai__provider:hover {
    border-color: var(--dc-primary);
    box-shadow: var(--dc-shadow-card);
}

.dc-ai__provider-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: var(--dc-radius-sm);
    flex-shrink: 0;
}

.dc-ai__provider-name {
    font-size: 13px;
    color: var(--dc-text-title);
}

.dc-ai__provider-tip {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    font-size: 11px;
    color: var(--dc-text-secondary);
    margin-top: 12px;
    line-height: 1.5;
}
</style>
