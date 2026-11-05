<template>
  <div class="dc-notify">
    <div class="dc-notify__pagehead">
      <div class="min-w-0">
        <div class="dc-notify__title">{{ $t('user.common.notify') }}</div>
        <div class="dc-notify__desc">{{ $t('notify.tip.page') }}</div>
      </div>
      <div class="dc-notify__actions">
        <MfButton :disabled="!dirty || saving" @click="reset">{{ $t('common.reset') }}</MfButton>
        <MfButton variant="primary" :loading="saving" :disabled="!dirty || loading" @click="save">{{ $t('common.save') }}</MfButton>
      </div>
    </div>

    <div v-if="loading" class="dc-notify__card dc-notify__loading">
      <MfSpinner size="lg" :label="$t('common.loading')"/>
    </div>

    <MfResult v-else-if="failed"
              status="error"
              class="dc-notify__card"
              :title="$t('notify.tip.loadFailed')"
              :sub-title="failed">
      <template #extra>
        <MfButton @click="load">{{ $t('common.refresh') }}</MfButton>
      </template>
    </MfResult>

    <template v-else>
      <!-- Channels: a switch each; settings only for enabled ones -->
      <div class="dc-notify__card">
        <div class="dc-notify__section-head">
          <div class="dc-notify__section-icon"><Send :size="16"/></div>
          <div class="min-w-0">
            <div class="dc-notify__section-title">{{ $t('notify.common.channels') }}</div>
            <div class="dc-notify__section-sub">{{ $t('notify.tip.channels') }}</div>
          </div>
        </div>

        <div class="dc-notify__channels">
          <div v-for="channel in channels"
               :key="channel.type"
               class="dc-notify__channel"
               :class="{ 'dc-notify__channel--on': settingOf(channel.type).enabled }">
            <div class="dc-notify__channel-head">
              <div class="dc-notify__channel-icon">
                <component :is="channelIcon(channel.type)" :size="18"/>
              </div>
              <div class="min-w-0 flex-1">
                <div class="dc-notify__channel-name">
                  {{ channelName(channel.type) }}
                  <MfTag v-if="channel.builtin" size="sm">{{ $t('notify.text.builtin') }}</MfTag>
                </div>
                <div class="dc-notify__channel-desc">{{ channelDesc(channel.type) }}</div>
              </div>
              <MfButton v-if="settingOf(channel.type).enabled"
                        size="sm"
                        :loading="testing === channel.type"
                        :disabled="saving || (testing !== '' && testing !== channel.type)"
                        @click="test(channel)">
                <Zap :size="14"/>
                {{ $t('notify.common.test') }}
              </MfButton>
              <MfSwitch v-model="settingOf(channel.type).enabled" :disabled="saving"/>
            </div>

            <template v-if="channel.fields.length > 0">
              <MfForm v-if="settingOf(channel.type).enabled"
                      :ref="(form: any) => registerForm(channel.type, form)"
                      layout="vertical"
                      class="dc-notify__fields"
                      :model="settingOf(channel.type).configure">
                <MfFormItem v-for="field in channel.fields"
                            :key="field.name"
                            :name="field.name"
                            :label="fieldLabel(channel.type, field)"
                            :tip="fieldTip(channel.type, field)"
                            :required-mark="field.required"
                            :rules="fieldRules(channel.type, field)">
                  <MfInput v-model="settingOf(channel.type).configure[field.name]"
                           :type="field.type === 'SECRET' ? 'password' : 'text'"
                           :show-password="field.type === 'SECRET'"
                           :maxlength="MAX_VALUE_LENGTH"
                           :placeholder="fieldPlaceholder(channel.type, field)"/>
                </MfFormItem>
              </MfForm>
              <div v-else class="dc-notify__channel-off">{{ $t('notify.tip.enableToConfigure') }}</div>
            </template>
          </div>
        </div>
      </div>

      <!-- Scenes by category; one column per enabled channel -->
      <div class="dc-notify__card">
        <div class="dc-notify__section-head">
          <div class="dc-notify__section-icon"><BellRing :size="16"/></div>
          <div class="min-w-0">
            <div class="dc-notify__section-title">{{ $t('notify.common.scenarios') }}</div>
            <div class="dc-notify__section-sub">{{ $t('notify.tip.scenarios') }}</div>
          </div>
        </div>

        <MfEmpty v-if="enabledChannels.length === 0" :description="$t('notify.tip.noEnabledChannel')"/>

        <div v-else class="dc-notify__matrix">
          <div class="dc-notify__matrix-row dc-notify__matrix-head" :style="gridStyle">
            <div/>
            <div v-for="channel in enabledChannels" :key="channel.type" class="dc-notify__matrix-cell">
              {{ channelName(channel.type) }}
            </div>
          </div>

          <div v-for="category in categories" :key="category.key" class="dc-notify__category">
            <div class="dc-notify__matrix-row dc-notify__category-row" :style="gridStyle">
              <div class="min-w-0">
                <div class="dc-notify__category-name">{{ categoryName(category.key) }}</div>
                <div class="dc-notify__category-desc">{{ categoryDesc(category.key) }}</div>
              </div>
              <div v-for="channel in enabledChannels" :key="channel.type" class="dc-notify__matrix-cell">
                <MfCheckbox :model-value="coverage(settingOf(channel.type), category.scenes) === 'all'"
                            :indeterminate="coverage(settingOf(channel.type), category.scenes) === 'some'"
                            :disabled="saving"
                            @update:model-value="(checked: any) => setScenes(settingOf(channel.type), category.scenes, !!checked)"/>
              </div>
            </div>
            <div v-for="scene in category.scenes" :key="scene" class="dc-notify__matrix-row dc-notify__scene-row" :style="gridStyle">
              <div class="dc-notify__scene-name">{{ sceneName(scene) }}</div>
              <div v-for="channel in enabledChannels" :key="channel.type" class="dc-notify__matrix-cell">
                <MfCheckbox :model-value="settingOf(channel.type).services.includes(scene)"
                            :disabled="saving"
                            @update:model-value="(checked: any) => setScenes(settingOf(channel.type), [scene], !!checked)"/>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { BellRing, Mail, MessageCircle, Plug, Send, Zap } from '@lucide/vue'
import UserService from '@/services/user.ts'
import { coverage, NotifyCategory, NotifyChannel, NotifyChannelSetting, NotifyField, setScenes, snapshot } from '@/model/notify.ts'

defineOptions({ name: 'NotifyForm' })

const MAX_VALUE_LENGTH = 2000

const { t, te } = useI18n()

const loading = ref(true)
const failed = ref('')
const saving = ref(false)
const testing = ref('')
const channels = ref<NotifyChannel[]>([])
const categories = ref<NotifyCategory[]>([])
const settings = ref<NotifyChannelSetting[]>([])
const saved = ref('')
const forms = new Map<string, any>()

const enabledChannels = computed(() => channels.value.filter(channel => settingOf(channel.type).enabled))
const dirty = computed(() => snapshot(settings.value) !== saved.value)
const gridStyle = computed(() => ({ gridTemplateColumns: `minmax(0, 1fr) repeat(${ enabledChannels.value.length }, 96px)` }))

const empty = (type: string): NotifyChannelSetting => ({ type, enabled: false, services: [], configure: {}, configured: [] })

// Every channel has an entry once loaded (see adopt), so rendering never adds one.
const settingOf = (type: string): NotifyChannelSetting => settings.value.find(item => item.type === type) || empty(type)

const registerForm = (type: string, form: any) => {
  if (form) {
    forms.set(type, form)
  }
  else {
    forms.delete(type)
  }
}

const translated = (key: string, fallback: string): string => te(key) ? t(key) : fallback

const channelIcon = (type: string): any => type === 'Internal' ? Mail : type === 'DingTalk' ? MessageCircle : Plug
const channelName = (type: string): string => translated(`notify.text.${ type.toLowerCase() }`, type)
const channelDesc = (type: string): string => translated(`notify.tip.${ type.toLowerCase() }`, t('notify.tip.channelFallback'))
const categoryName = (key: string): string => translated(`notify.category.${ key.toLowerCase() }`, key)
const categoryDesc = (key: string): string => translated(`notify.categoryTip.${ key.toLowerCase() }`, '')
const sceneName = (key: string): string => translated(`notify.scene.${ key }`, key)
const fieldLabel = (type: string, field: NotifyField): string => translated(`notify.field.${ type.toLowerCase() }.${ field.name }`, field.label)
const fieldTip = (type: string, field: NotifyField): string => translated(`notify.fieldTip.${ type.toLowerCase() }.${ field.name }`, field.description || '')

const isConfigured = (type: string, field: NotifyField): boolean => (settingOf(type).configured || []).includes(field.name)

const fieldPlaceholder = (type: string, field: NotifyField): string => {
  if (field.type === 'SECRET' && isConfigured(type, field)) {
    return t('notify.tip.secretKept')
  }
  return field.placeholder || ''
}

const fieldRules = (type: string, field: NotifyField) => {
  const label = fieldLabel(type, field)
  return [
    // A secret that is already stored may stay empty: the server keeps it.
    { required: field.required && !(field.type === 'SECRET' && isConfigured(type, field)), message: t('notify.validate.required', { field: label }) },
    { max: MAX_VALUE_LENGTH, message: t('notify.validate.tooLong', { field: label }) },
    ...(field.type === 'URL'
      ? [{ validator: (value: unknown) => !value || /^https?:\/\/\S+$/i.test(String(value)), message: t('notify.validate.url', { field: label }) }]
      : [])
  ]
}

const adopt = (data: NotifyChannelSetting[]) => {
  settings.value = channels.value.map(channel => {
    const item = (data || []).find(entry => entry.type === channel.type) || empty(channel.type)
    const configure: Record<string, string> = {}
    channel.fields.forEach(field => (configure[field.name] = item.configure?.[field.name] || ''))
    return {
      type: channel.type,
      enabled: !!item.enabled,
      services: [...(item.services || [])],
      configure,
      configured: [...(item.configured || [])]
    }
  })
  saved.value = snapshot(settings.value)
}

const load = async () => {
  loading.value = true
  failed.value = ''
  try {
    const [catalog, current] = await Promise.all([UserService.getNotifyCatalog(), UserService.getNotifySettings()])
    if (!catalog.status || !current.status) {
      failed.value = String((catalog.status ? current : catalog).message || '')
      return
    }
    channels.value = catalog.data?.channels || []
    categories.value = catalog.data?.categories || []
    adopt(current.data)
  }
  finally {
    loading.value = false
  }
}

const reset = () => load()

const validate = async (): Promise<boolean> => {
  for (const channel of enabledChannels.value) {
    const form = forms.get(channel.type)
    if (!form) {
      continue
    }
    try {
      await form.validate()
    }
    catch {
      MfMessage.error(t('notify.validate.channel', { channel: channelName(channel.type) }))
      return false
    }
  }
  return true
}

const save = async () => {
  if (!(await validate())) {
    return
  }
  saving.value = true
  try {
    const response = await UserService.saveNotifySettings(settings.value)
    if (response.status) {
      adopt(response.data)
      MfMessage.success(t('common.successfully'))
    }
    else {
      MfMessage.error(response.message)
    }
  }
  finally {
    saving.value = false
  }
}

const test = async (channel: NotifyChannel) => {
  const form = forms.get(channel.type)
  if (form) {
    try {
      await form.validate()
    }
    catch {
      return
    }
  }
  testing.value = channel.type
  try {
    const response = await UserService.testNotify(settingOf(channel.type))
    if (response.status) {
      MfMessage.success(t('notify.tip.testSent', { channel: channelName(channel.type) }))
    }
    else {
      MfMessage.error(response.message)
    }
  }
  finally {
    testing.value = ''
  }
}

onMounted(load)
</script>

<style scoped>
.dc-notify__pagehead {
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

.dc-notify__title {
    font-size: 22px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notify__desc {
    font-size: 13px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-notify__actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
    white-space: nowrap;
}

.dc-notify__card {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
    margin-bottom: 16px;
}

.dc-notify__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 240px;
}

.dc-notify__section-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
}

.dc-notify__section-icon {
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

.dc-notify__section-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notify__section-sub {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}

.dc-notify__channels {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.dc-notify__channel {
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-md);
    padding: 14px 16px;
    transition: border-color 0.15s ease;
}

.dc-notify__channel--on {
    border-color: var(--dc-primary-border, var(--dc-primary));
}

.dc-notify__channel-head {
    display: flex;
    align-items: center;
    gap: 12px;
}

.dc-notify__channel-head :deep(button) {
    white-space: nowrap;
    flex-shrink: 0;
}

.dc-notify__channel-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-surface-hover);
    color: var(--dc-text-secondary);
    flex-shrink: 0;
}

.dc-notify__channel--on .dc-notify__channel-icon {
    background: var(--dc-primary-light);
    color: var(--dc-primary);
}

.dc-notify__channel-name {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notify__channel-desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}

.dc-notify__fields {
    margin-top: 14px;
    padding-top: 14px;
    border-top: 1px dashed var(--dc-border-light);
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    column-gap: 16px;
}

.dc-notify__channel-off {
    margin-top: 10px;
    font-size: 12px;
    color: var(--dc-text-tertiary, var(--dc-text-secondary));
}

.dc-notify__matrix {
    overflow-x: auto;
}

.dc-notify__matrix-row {
    display: grid;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    min-width: max-content;
}

.dc-notify__matrix-head {
    font-size: 12px;
    font-weight: 600;
    color: var(--dc-text-secondary);
    border-bottom: 1px solid var(--dc-border-light);
}

.dc-notify__matrix-cell {
    display: flex;
    justify-content: center;
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.dc-notify__category + .dc-notify__category {
    border-top: 1px solid var(--dc-border-light);
}

.dc-notify__category-row {
    background: var(--dc-surface-hover);
    border-radius: var(--dc-radius-md);
    margin-top: 8px;
}

.dc-notify__category-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notify__category-desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-notify__scene-name {
    font-size: 13px;
    color: var(--dc-text-primary, var(--dc-text-title));
    padding-left: 12px;
}
</style>
