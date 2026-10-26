<template>
  <div class="dc-password">
    <!-- 页头：标题 + 描述 -->
    <div class="dc-password__pagehead">
      <div>
        <div class="dc-password__title">{{ $t('user.common.modifyPassword') }}</div>
        <div class="dc-password__desc">{{ $t('user.tip.passwordPage') }}</div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <!-- 表单区 -->
      <div class="lg:col-span-2">
        <div class="dc-password__card">
          <MfForm layout="vertical" class="dc-password__form">
            <MfFormItem name="oldPassword" required-mark :label="$t('user.common.currentPassword')">
              <MfInput v-model="formState.oldPassword"
                       type="password"
                       show-password
                       :placeholder="$t('user.placeholder.currentPassword')">
                <template #prefix>
                  <LockOutlined :style="{ fontSize: '14px', color: 'var(--dc-text-secondary)' }"/>
                </template>
              </MfInput>
              <div class="dc-password__hint">{{ $t('user.tip.currentPasswordLive') }}</div>
            </MfFormItem>

            <MfFormItem name="newPassword" required-mark :label="$t('user.common.newPassword')">
              <MfInput v-model="formState.newPassword"
                       type="password"
                       show-password
                       :placeholder="$t('user.placeholder.newPassword')">
                <template #prefix>
                  <LockOutlined :style="{ fontSize: '14px', color: 'var(--dc-text-secondary)' }"/>
                </template>
              </MfInput>
              <div class="dc-password__hint">{{ $t('user.tip.newPasswordFormat') }}</div>

              <!-- 强度条 -->
              <div class="dc-password__meter">
                <div class="dc-password__meter-segments">
                  <span v-for="i in 4"
                        :key="i"
                        class="dc-password__meter-segment"
                        :style="{ background: i <= score ? meterColor : 'var(--dc-border-light)' }"></span>
                </div>
                <div class="dc-password__meter-label">
                  {{ $t('user.common.passwordStrength') }}:
                  <b :style="{ color: meterColor }">{{ meterLabel }}</b>
                </div>
              </div>

              <!-- 达标清单 -->
              <div class="dc-password__rules">
                <div v-for="rule in rules"
                     :key="rule.key"
                     class="dc-password__rule">
                  <span class="dc-password__rule-dot" :class="{ 'dc-password__rule-dot--on': rule.checked }"></span>
                  <span>{{ $t(rule.nameKey) }}</span>
                </div>
              </div>
            </MfFormItem>

            <MfFormItem name="confirmPassword" required-mark :label="$t('user.common.confirmPassword')">
              <MfInput v-model="formState.confirmPassword"
                       type="password"
                       show-password
                       :placeholder="$t('user.placeholder.confirmNewPassword')">
                <template #prefix>
                  <LockOutlined :style="{ fontSize: '14px', color: 'var(--dc-text-secondary)' }"/>
                </template>
              </MfInput>
              <div class="dc-password__hint">{{ $t('user.tip.confirmNewPassword') }}</div>
            </MfFormItem>
          </MfForm>

          <div class="dc-password__actions">
            <MfButton variant="primary"
                      :loading="loading"
                      :disabled="!canSubmit"
                      @click="onSubmit">
              {{ $t('user.common.saveChanges') }}
            </MfButton>
            <MfButton :disabled="loading" @click="onReset">
              {{ $t('common.cancel') }}
            </MfButton>
          </div>
        </div>
      </div>

      <!-- 安全提示 -->
      <div>
        <div class="dc-password__card dc-password__tips">
          <div class="dc-password__section-head">
            <div class="dc-password__section-icon">
              <SafetyCertificateOutlined :style="{ fontSize: '16px' }"/>
            </div>
            <div class="dc-password__section-title">{{ $t('user.common.securityTips') }}</div>
          </div>

          <div class="dc-password__tips-list">
            <div v-for="(tip, index) in tipList"
                 :key="index"
                 class="dc-password__tips-item">
              <span class="dc-password__tips-index">{{ index + 1 }}</span>
              <span>{{ $t(tip) }}</span>
            </div>
          </div>

          <div class="dc-password__tips-promo">
            <div class="dc-password__tips-promo-icon">
              <LockOutlined :style="{ fontSize: '18px', color: '#fff' }"/>
            </div>
            <div class="min-w-0">
              <div class="dc-password__tips-promo-title">{{ $t('user.common.protectAccount') }}</div>
              <div class="dc-password__tips-promo-desc">{{ $t('user.tip.protectAccount') }}</div>
            </div>
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
import {
  LockOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons-vue'
import UserService from '@/services/user'
import { UserPasswordModel } from '@/model/user'
import router from '@/router'
import Common from '@/utils/common'

defineOptions({ name: 'PasswordForm' })

const { t } = useI18n()

const loading = ref(false)
const formState = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = computed(() => ([
  { key: 'length', checked: (formState.newPassword || '').length >= 8, nameKey: 'user.password.ruleLength' },
  { key: 'uppercase', checked: /[A-Z]/.test(formState.newPassword || ''), nameKey: 'user.password.ruleUppercase' },
  { key: 'digit', checked: /\d/.test(formState.newPassword || ''), nameKey: 'user.password.ruleDigit' },
  { key: 'special', checked: /[^A-Za-z0-9]/.test(formState.newPassword || ''), nameKey: 'user.password.ruleSpecial' }
]))

const score = computed(() => rules.value.filter(rule => rule.checked).length)

const meterColor = computed(() => {
  if (score.value <= 1) {
    return 'var(--dc-danger)'
  }
  if (score.value <= 3) {
    return 'var(--dc-warning)'
  }
  return 'var(--dc-success)'
})

const meterLabel = computed(() => {
  if (score.value <= 1) {
    return t('user.password.weak')
  }
  if (score.value <= 3) {
    return t('user.password.medium')
  }
  return t('user.password.strong')
})

const tipList = [
  'user.tip.securityInfo1',
  'user.tip.securityInfo2',
  'user.tip.securityInfo3',
  'user.tip.securityInfo4',
  'user.tip.securityInfo5'
]

const canSubmit = computed(() => {
  const { oldPassword, newPassword, confirmPassword } = formState
  return !!oldPassword
      && !!newPassword
      && !!confirmPassword
      && newPassword === confirmPassword
      && newPassword.length >= 8
      && newPassword.length <= 32
})

const onSubmit = () => {
  if (formState.newPassword !== formState.confirmPassword) {
    MfMessage.error(t('user.auth.passwordNotMatchTip') as string)
    return
  }
  loading.value = true
  UserService.changePassword(formState as unknown as UserPasswordModel)
             .then((response) => {
               if (response.status) {
                 MfMessage.success(t('user.tip.changePasswordSuccessfully') as string)
                 localStorage.removeItem(Common.token)
                 localStorage.removeItem(Common.menu)
                 router.push('/auth/signin')
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (loading.value = false))
}

const onReset = () => {
  formState.oldPassword = ''
  formState.newPassword = ''
  formState.confirmPassword = ''
}
</script>

<style scoped>
.dc-password__pagehead {
    padding: 18px 20px;
    border-radius: var(--dc-radius-lg);
    background: var(--dc-gradient-hero);
    border: 1px solid var(--dc-border-light);
    margin-bottom: 16px;
}

.dc-password__title {
    font-size: 22px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-password__desc {
    font-size: 13px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-password__card {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
    margin-bottom: 16px;
}

.dc-password__hint {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 6px;
}

.dc-password__meter {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 10px;
}

.dc-password__meter-segments {
    display: flex;
    gap: 6px;
    flex: 1;
}

.dc-password__meter-segment {
    height: 4px;
    border-radius: 2px;
    flex: 1;
}

.dc-password__meter-label {
    font-size: 12px;
    color: var(--dc-text-secondary);
    white-space: nowrap;
}

.dc-password__rules {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 6px 16px;
    margin-top: 12px;
}

.dc-password__rule {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-password__rule-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    border: 1px solid var(--dc-border);
    background: var(--dc-surface);
    flex-shrink: 0;
}

.dc-password__rule-dot--on {
    background: var(--dc-primary);
    border-color: var(--dc-primary);
}

.dc-password__actions {
    display: flex;
    gap: 12px;
    margin-top: 8px;
}

.dc-password__section-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
}

.dc-password__section-icon {
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

.dc-password__section-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-password__tips-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.dc-password__tips-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    font-size: 13px;
    color: var(--dc-text-primary);
    line-height: 1.6;
}

.dc-password__tips-index {
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

.dc-password__tips-promo {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 16px;
    padding: 14px;
    border-radius: var(--dc-radius-lg);
    background: var(--dc-gradient-hero);
    border: 1px solid var(--dc-border-light);
}

.dc-password__tips-promo-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-gradient-promo);
    flex-shrink: 0;
}

.dc-password__tips-promo-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-password__tips-promo-desc {
    font-size: 11px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}
</style>
