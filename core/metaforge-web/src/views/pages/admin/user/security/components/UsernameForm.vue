<template>
  <div class="dc-rename">
    <!-- 页头：标题 + 描述 -->
    <div class="dc-rename__pagehead">
      <div>
        <div class="dc-rename__title">{{ $t('user.common.modifyUsername') }}</div>
        <div class="dc-rename__desc">{{ $t('user.tip.modifyUsernamePage') }}</div>
      </div>
    </div>

    <div class="grid min-w-0 grid-cols-1 xl:grid-cols-3 gap-4">
      <!-- 表单区 -->
      <div class="min-w-0 xl:col-span-2">
        <div class="dc-rename__card">
          <div v-if="defaultLoading" class="flex items-center justify-center py-10">
            <MfSpinner/>
          </div>
          <MfForm v-else-if="!defaultLoading && formState" ref="form" :model="formState" layout="vertical" class="dc-rename__form">
              <MfFormGrid>
              <MfFormItem name="oldUsername" :label="$t('user.common.currentUsername')">
                <MfInput :model-value="formState.oldUsername" disabled/>
              </MfFormItem>

              <MfFormItem name="newUsername" :error="checkState === 'error' ? checkMessage : undefined" :rules="usernameRules" :label="$t('user.common.newUsername')" :tip="$t('user.tip.newUsernameFormat')">
                <MfInput v-model="formState.newUsername" name="new-username" autocomplete="off" :maxlength="20" :disabled="submitting" :invalid="checkState === 'error'">
                  <template #suffix>
                    <CircleCheck :size="14" v-if="checkState === 'success'" class="dc-rename__check--success"/>
                    <CircleX :size="14" v-else-if="checkState === 'error'" class="dc-rename__check--error"/>
                    <MfLoadingIcon v-else-if="checkState === 'loading'" :size="14" class="dc-rename__check--loading"/>
                  </template>
                </MfInput>
              </MfFormItem>

              <MfFormItem name="password" class="col-span-full" :rules="[{ required: true, message: $t('user.validation.currentPassword') }]" :label="$t('user.common.currentPassword')" :tip="$t('user.tip.passwordConfirm')">
                <MfInput v-model="formState.password" name="current-password" autocomplete="current-password" :disabled="submitting" type="password" show-password/>
              </MfFormItem>
              </MfFormGrid>
            </MfForm>

            <div class="dc-rename__actions">
              <MfButton variant="primary"
                        :loading="submitting"
                        :disabled="checkState !== 'success' || !formState?.password"
                        @click="onSubmit">
                {{ $t('user.common.saveChanges') }}
              </MfButton>
              <MfButton :disabled="submitting" @click="onReset">
                {{ $t('common.cancel') }}
              </MfButton>
            </div>
          
        </div>
      </div>

      <!-- 说明卡 -->
      <div>
        <div class="dc-rename__card dc-rename__info">
          <div class="dc-rename__info-head">
            <div class="dc-rename__info-icon">
              <ShieldCheck :size="16" />
            </div>
            <div class="dc-rename__info-title">{{ $t('user.common.modifyUsernameTip') }}</div>
          </div>

          <div class="dc-rename__info-list">
            <div v-for="(tip, index) in infoTips"
                 :key="index"
                 class="dc-rename__info-item">
              <span class="dc-rename__info-index">{{ index + 1 }}</span>
              <span>{{ $t(tip) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { CircleCheck, CircleX, ShieldCheck } from '@lucide/vue'
import UserService from '@/services/user'
import { UsernameModel } from '@/model/user'
import Common from '@/utils/common'

defineOptions({ name: 'UsernameForm' })

const { t } = useI18n()
const router = useRouter()

const form = ref<any>()
const defaultLoading = ref(false)
const submitting = ref(false)
const formState = ref<UsernameModel | null>(null)

const checkState = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
const checkMessage = ref('')
let checkGeneration = 0
let checkTimer: ReturnType<typeof setTimeout> | null = null

const infoTips = [
  'user.tip.renameInfo1',
  'user.tip.renameInfo2',
  'user.tip.renameInfo3',
  'user.tip.renameInfo4'
]

const handlerInitialize = () => {
  defaultLoading.value = true
  UserService.getInfo()
             .then(response => {
               if (response.status && response.data) {
                 formState.value = { oldUsername: response.data.username, newUsername: undefined, password: undefined } as unknown as UsernameModel
               }
             })
             .finally(() => (defaultLoading.value = false))
}

const onUsernameInput = () => {
  ++checkGeneration
  checkState.value = 'idle'
  checkMessage.value = ''
  const username = formState.value?.newUsername?.trim()

  if (checkTimer) {
    clearTimeout(checkTimer)
  }

  if (!username) {
    return
  }

  if (!/^[A-Za-z_][A-Za-z0-9_]{3,19}$/.test(username)) { checkState.value = 'error'; checkMessage.value = t('user.tip.newUsernameFormat'); return }
  checkState.value = 'loading'
  checkTimer = setTimeout(() => validateUsername(username), 500)
}

const validateUsername = (username: string) => {
  const generation = checkGeneration
  checkState.value = 'loading'
  UserService.checkUsername(username)
             .then(response => {
               // 输入可能已变化，丢弃过期结果
               if (generation !== checkGeneration || formState.value?.newUsername?.trim() !== username) {
                 return
               }
               if (response.status && response.data === true) {
                 checkState.value = 'success'
                 checkMessage.value = ''
               }
               else {
                 checkState.value = 'error'
                 checkMessage.value = response.code === 4027 ? t('user.validation.usernameTaken') : response.code === 4026 ? t('user.tip.newUsernameFormat') : response.message || t('user.tip.renameFailed')
               }
             })
             .catch(() => {
               if (generation !== checkGeneration || formState.value?.newUsername?.trim() !== username) return
               checkState.value = 'error'
               checkMessage.value = t('common.networkError')
             })
}

const onSubmit = async () => {
  if (submitting.value) return
  try { await form.value?.validate() } catch { return }
  submitting.value = true
  UserService.changeUsername(formState.value as UsernameModel)
             .then((response) => {
               if (response.status) {
                 MfMessage.success(t('user.tip.changeUsernameSuccessfully') as string)
                 localStorage.removeItem(Common.token)
                 localStorage.removeItem(Common.menu)
                 router.push('/auth/signin')
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (submitting.value = false))
}

const onReset = () => {
  ++checkGeneration
  if (checkTimer) clearTimeout(checkTimer)
  form.value?.clearValidate()
  if (formState.value) {
    formState.value.newUsername = undefined
    formState.value.password = undefined
  }
  checkState.value = 'idle'
  checkMessage.value = ''
}

const usernameRules = computed(() => [
  { required: true, pattern: /^[A-Za-z_][A-Za-z0-9_]{3,19}$/, message: t('user.tip.newUsernameFormat') },
  { validator: () => checkState.value === 'success' || checkMessage.value || t('user.validation.usernameChecking') },
])
watch(() => formState.value?.newUsername, onUsernameInput)
onBeforeUnmount(() => { ++checkGeneration; if (checkTimer) clearTimeout(checkTimer) })
handlerInitialize()
</script>

<style scoped>
.dc-rename__pagehead {
    padding: 18px 20px;
    border-radius: var(--dc-radius-lg);
    background: var(--dc-gradient-hero);
    border: 1px solid var(--dc-border-light);
    margin-bottom: 16px;
}

.dc-rename__title {
    font-size: 22px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-rename__desc {
    font-size: 13px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-rename__card {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
}

.dc-rename__check--success {
    color: var(--dc-success);
}

.dc-rename__check--error {
    color: var(--dc-danger);
}

.dc-rename__check--loading {
    color: var(--dc-text-secondary);
}

.dc-rename__field-hint {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 6px;
}

.dc-rename__field-error {
    font-size: 12px;
    color: var(--dc-danger);
    margin-top: 6px;
}

.dc-rename__actions {
    display: flex;
    gap: 12px;
    margin-top: 8px;
}

.dc-rename__info {
    background: var(--dc-primary-surface);
    border: 1px solid var(--dc-border-light);
    box-shadow: none;
}

.dc-rename__info-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
}

.dc-rename__info-icon {
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

.dc-rename__info-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-rename__info-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.dc-rename__info-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    font-size: 13px;
    color: var(--dc-text-primary);
    line-height: 1.6;
}

.dc-rename__info-index {
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
</style>

<style scoped>
.dc-rename__form { min-width: 0; max-width: 42rem; }
.dc-rename__card { min-width: 0; }
.dc-rename__actions { flex-wrap: wrap; }
</style>
