<template>
  <div class="dc-profile">
    <!-- 页头：品牌渐变横幅 + 标题 + 标语 -->
    <div class="dc-profile__pagehead">
      <div class="min-w-0">
        <div class="dc-profile__title">{{ $t('user.common.profile') }}</div>
        <div class="dc-profile__desc">{{ $t('user.tip.profile') }}</div>
      </div>
      <div class="dc-profile__slogan">
        <div>{{ $t('user.tip.sloganData') }}</div>
        <div class="dc-profile__slogan-strong">{{ $t('user.tip.sloganValue') }}</div>
      </div>
    </div>

    <!-- 资料头部卡：头像 + 名称/徽标 + 上传头像 -->
    <div class="dc-profile__card dc-profile__hero">
      <div class="dc-profile__avatar-wrap" @click="triggerUpload">
        <MfAvatar :size="88"
                  :src="formState?.avatarConfigure?.path || formState?.avatar"
                  class="dc-profile__avatar">
          {{ (formState?.username || '?').charAt(0).toUpperCase() }}
        </MfAvatar>
        <span class="dc-profile__avatar-badge">
          <Camera :size="13" />
        </span>
      </div>

      <div class="dc-profile__hero-meta min-w-0">
        <div class="flex items-center flex-wrap gap-2">
          <span class="dc-profile__hero-name">{{ formState?.username }}</span>
          <span v-for="role in formState?.roles || []"
                :key="role.id"
                class="dc-tag dc-tag--info">
            {{ role.name }}
          </span>
          <span class="dc-tag dc-tag--success">
            {{ $t('user.common.stateNormal') }}
          </span>
        </div>
        <div class="dc-profile__hero-sub">
          {{ formState?.roles?.[0]?.description || $t('user.tip.accountRoleDefault') }}
        </div>
      </div>

      <div class="dc-profile__hero-actions">
        <MfButton @click="triggerUpload">
          <Camera :size="14"/>
          {{ $t('user.common.uploadAvatar') }}
        </MfButton>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-[7fr_5fr] gap-4">
      <!-- 基本信息 -->
      <div>
        <div class="dc-profile__card dc-profile__section">
          <div class="dc-profile__section-head">
            <div class="dc-profile__section-icon dc-profile__section-icon--primary">
              <User :size="16" />
            </div>
            <div>
              <div class="dc-profile__section-title">{{ $t('user.common.basicInfo') }}</div>
              <div class="dc-profile__section-sub">{{ $t('user.tip.basicInfo') }}</div>
            </div>
          </div>

          <MfForm v-if="formState" layout="vertical" class="dc-profile__form">
            <MfFormItem name="" :label="$t('user.common.username')" :tip="$t('user.tip.username')">
              <MfInput :model-value="formState.username" disabled/>
            </MfFormItem>

            <MfFormItem name="" :label="$t('common.role')">
              <MfInput :model-value="(formState.roles || []).map(role => role.name).join(', ')" disabled/>
            </MfFormItem>

            <MfFormItem name="" :label="$t('user.common.createTime')" :tip="$t('user.tip.createTime')">
              <MfInput :model-value="formState.createTime" disabled/>
            </MfFormItem>

            <MfFormItem name="" :label="$t('common.updateTime')">
              <MfInput :model-value="formState.updateTime" disabled/>
            </MfFormItem>
          </MfForm>
        </div>
      </div>

      <!-- 偏好 + 概览 -->
      <div>
        <div class="dc-profile__card dc-profile__section">
          <div class="dc-profile__section-head">
            <div class="dc-profile__section-icon dc-profile__section-icon--primary">
              <ShieldCheck :size="16" />
            </div>
            <div>
              <div class="dc-profile__section-title">{{ $t('user.common.preferences') }}</div>
              <div class="dc-profile__section-sub">{{ $t('user.tip.preferences') }}</div>
            </div>
          </div>

          <div class="dc-profile__pref">
            <div class="dc-profile__pref-label">
              <Globe :size="14" class="dc-profile__pref-icon"/>
              <span>{{ $t('user.common.language') }}</span>
            </div>
            <MfSelect :model-value="language"
                      class="dc-profile__pref-control"
                      :options="[
                        { label: $t('region.common.asia.chineseSimple'), value: 'language_zh-cn' },
                        { label: $t('region.common.northAmerica.english'), value: 'language_en' }
                      ]"
                      @change="onChangeLanguage"/>
          </div>
          <div class="dc-profile__pref-tip">{{ $t('user.tip.language') }}</div>
        </div>

        <div class="dc-profile__card dc-profile__section">
          <div class="dc-profile__section-head">
            <div class="dc-profile__section-icon dc-profile__section-icon--primary">
              <ChartColumn :size="16" />
            </div>
            <div>
              <div class="dc-profile__section-title">{{ $t('user.common.overview') }}</div>
              <div class="dc-profile__section-sub">{{ $t('user.tip.overview') }}</div>
            </div>
          </div>

          <div class="dc-profile__tiles">
            <div class="dc-profile__tile">
              <div class="dc-profile__tile-icon dc-profile__tile-icon--info">
                <Crown :size="16"/>
              </div>
              <div class="min-w-0">
                <div class="dc-profile__tile-label">{{ $t('user.common.accountRole') }}</div>
                <div class="dc-profile__tile-value">{{ roleNames || '-' }}</div>
                <div class="dc-profile__tile-tip">{{ roleDescription }}</div>
              </div>
            </div>

            <div class="dc-profile__tile">
              <div class="dc-profile__tile-icon dc-profile__tile-icon--success">
                <ShieldCheck :size="16"/>
              </div>
              <div class="min-w-0">
                <div class="dc-profile__tile-label">{{ $t('user.common.accountState') }}</div>
                <div class="dc-profile__tile-value dc-profile__tile-value--success">{{ $t('user.common.stateNormal') }}</div>
                <div class="dc-profile__tile-tip">
                  {{ formState?.system ? $t('user.tip.systemAccount') : $t('user.tip.accountStateOk') }}
                </div>
              </div>
            </div>

            <div class="dc-profile__tile">
              <div class="dc-profile__tile-icon dc-profile__tile-icon--primary">
                <Calendar :size="16"/>
              </div>
              <div class="min-w-0">
                <div class="dc-profile__tile-label">{{ $t('user.common.registerTime') }}</div>
                <div class="dc-profile__tile-value">{{ formState?.createTime || '-' }}</div>
                <div class="dc-profile__tile-tip" v-if="registeredDays !== null">
                  {{ $t('user.tip.registeredDays', { days: registeredDays }) }}
                </div>
              </div>
            </div>

            <div class="dc-profile__tile">
              <div class="dc-profile__tile-icon dc-profile__tile-icon--neutral">
                <Clock :size="16"/>
              </div>
              <div class="min-w-0">
                <div class="dc-profile__tile-label">{{ $t('user.common.lastLogin') }}</div>
                <div class="dc-profile__tile-value">{{ lastLog?.createTime || '-' }}</div>
                <div class="dc-profile__tile-tip" v-if="lastLog?.ip">
                  {{ $t('user.tip.fromIp', { ip: lastLog.ip }) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 头像裁剪弹窗 -->
    <MfModal v-model:open="cropVisible"
             :title="$t('common.cropper')"
             width="520px">
      <VuePictureCropper style="max-height: 400px"
                         :boxStyle="{ width: '100%', height: '320px', backgroundColor: '#f8f8f8', margin: 'auto' }"
                         :options="{ viewMode: 1, dragMode: 'crop' }"
                         :img="cropImage"
                         @ready="() => ({})"/>
      <div class="flex justify-end gap-2 mt-3">
        <MfButton size="sm" @click="cropVisible = false">{{ $t('common.cancel') }}</MfButton>
        <MfButton size="sm" variant="primary" :loading="uploading" @click="onCropConfirm">
          {{ $t('common.cropper') }}
        </MfButton>
      </div>
    </MfModal>

    <input ref="uploadInput"
           type="file"
           accept="image/jpg, image/jpeg, image/png, image/gif"
           class="hidden"
           @change="onSelectFile"/>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import VuePictureCropper, { cropper } from 'vue-picture-cropper'
import { Calendar, Camera, ChartColumn, Clock, Crown, Globe, ShieldCheck, User } from '@lucide/vue'
import UserService from '@/services/user'
import { UserModel } from '@/model/user'
import { FilterModel } from '@/model/filter.ts'
import { useI18nHandler } from '@/i18n/I18n'
import { HttpUtils } from '@/utils/http.ts'

defineOptions({ name: 'ProfileForm' })

const { t } = useI18n()
// @ts-ignore
const { loadLocale } = useI18nHandler()

const loading = ref(false)
const uploading = ref(false)
const formState = ref<UserModel | null>(null)
const lastLog = ref<any>(null)
const language = ref('language_zh-cn')

const cropVisible = ref(false)
const cropImage = ref('')
const uploadInput = ref<HTMLInputElement | null>(null)

const roleNames = computed(() => (formState.value?.roles || []).map(role => role.name).join(', '))
const roleDescription = computed(() => formState.value?.roles?.[0]?.description || t('user.tip.accountRoleDefault'))

const registeredDays = computed<number | null>(() => {
  const created = formState.value?.createTime
  if (!created) {
    return null
  }
  const createdTime = new Date(String(created).replace(' ', 'T')).getTime()
  if (Number.isNaN(createdTime)) {
    return null
  }
  return Math.max(0, Math.floor((Date.now() - createdTime) / 86400000))
})

const handlerInitialize = () => {
  loading.value = true
  const filter: FilterModel = new FilterModel()
  filter.size = 1
  filter.orders = [{ column: 'createTime', order: 'desc' }]

  const axios = new HttpUtils().getAxios()
  axios.all([UserService.getInfo(), UserService.getLogs(filter)])
       .then(axios.spread((info: any, logs: any) => {
         if (info.status && info.data) {
           formState.value = info.data
         }
         if (logs.status && logs.data?.content?.length) {
           lastLog.value = logs.data.content[0]
         }
       }))
       .finally(() => (loading.value = false))
}

const triggerUpload = () => {
  uploadInput.value?.click()
}

const onSelectFile = (event: Event) => {
  const { files } = event.target as HTMLInputElement
  if (!files || !files.length) {
    return
  }
  const reader = new FileReader()
  reader.readAsDataURL(files[0])
  reader.onload = () => {
    cropImage.value = String(reader.result)
    cropVisible.value = true
    if (uploadInput.value) {
      uploadInput.value.value = ''
    }
  }
}

const onCropConfirm = async () => {
  if (!cropper) {
    return
  }
  const file = await cropper.getFile()
  if (!file) {
    return
  }
  uploading.value = true
  UserService.uploadAvatar({ mode: 'AVATAR', file })
             .then(response => {
               if (response.status) {
                 if (formState.value) {
                   formState.value.avatar = response.data
                 }
                 MfMessage.success(t('common.successfully'))
                 cropVisible.value = false
                 handlerInitialize()
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (uploading.value = false))
}

const onChangeLanguage = async (value: any) => {
  const prefix = 'language_'
  if (typeof value === 'string' && value.startsWith(prefix)) {
    const locale = value.substring(prefix.length)
    await loadLocale(locale)
    language.value = value
  }
}

const initLocale = () => {
  // 历史存储值可能是 zh_cn 下划线格式，归一化为下拉的 language_zh-cn
  const current = localStorage.getItem('locale') || 'zh-cn'
  const normalized = current.replace('_', '-')
  language.value = `language_${ normalized }`
}

initLocale()
handlerInitialize()
</script>

<style scoped>
.dc-profile__pagehead {
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

.dc-profile__title {
    font-size: 22px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-profile__desc {
    font-size: 13px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-profile__slogan {
    text-align: right;
    font-size: 12px;
    color: var(--dc-text-secondary);
    flex-shrink: 0;
}

.dc-profile__slogan-strong {
    font-size: 14px;
    font-weight: 600;
    color: var(--dc-text-title);
    margin-top: 2px;
}

.dc-profile__card {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
    margin-bottom: 16px;
}

.dc-profile__hero {
    display: flex;
    align-items: center;
    gap: 18px;
}

.dc-profile__avatar-wrap {
    position: relative;
    cursor: pointer;
    flex-shrink: 0;
}

.dc-profile__avatar {
    background: var(--dc-gradient-avatar);
    border: 3px solid var(--dc-surface);
    box-shadow: var(--dc-shadow-avatar);
}

.dc-profile__avatar-badge {
    position: absolute;
    right: 2px;
    bottom: 2px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--dc-surface);
    color: var(--dc-primary);
    border: 1px solid var(--dc-border);
    box-shadow: 0 1px 4px rgba(15, 23, 42, 0.12);
}

.dc-profile__hero-meta {
    flex: 1;
}

.dc-profile__hero-name {
    font-size: 20px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-tag {
    margin-inline-end: 0;
}

.dc-profile__hero-sub {
    font-size: 13px;
    color: var(--dc-text-secondary);
    margin-top: 6px;
}

.dc-profile__hero-actions {
    flex-shrink: 0;
}

.dc-profile__section-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
}

.dc-profile__section-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: var(--dc-radius-md);
    flex-shrink: 0;
}

.dc-profile__section-icon--primary {
    background: var(--dc-primary-light);
    color: var(--dc-primary);
}

.dc-profile__section-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-profile__section-sub {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}

.dc-profile__pref {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.dc-profile__pref-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--dc-text-primary);
}

.dc-profile__pref-icon {
    color: var(--dc-text-secondary);
}

.dc-profile__pref-control {
    width: 200px;
}

.dc-profile__pref-tip {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 8px;
}

.dc-profile__tiles {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
}

.dc-profile__tile {
    display: flex;
    gap: 10px;
    padding: 12px;
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-md);
    background: var(--dc-bg);
    min-width: 0;
}

.dc-profile__tile-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: var(--dc-radius-md);
    font-size: 15px;
    flex-shrink: 0;
}

.dc-profile__tile-icon--primary {
    background: var(--dc-primary-light);
    color: var(--dc-primary);
}

.dc-profile__tile-icon--success {
    background: var(--dc-success-bg);
    color: var(--dc-success);
}

.dc-profile__tile-icon--info {
    background: var(--dc-info-bg);
    color: var(--dc-info);
}

.dc-profile__tile-icon--neutral {
    background: var(--dc-border-light);
    color: var(--dc-text-secondary);
}

.dc-profile__tile-label {
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-profile__tile-value {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
    margin-top: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.dc-profile__tile-value--success {
    color: var(--dc-success);
}

.dc-profile__tile-tip {
    font-size: 11px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.hidden {
    display: none;
}
</style>
